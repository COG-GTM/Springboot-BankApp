package com.example.bankapp.performance;

import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.example.bankapp.performance.MySqlPerformanceIT$SqlCapture",
        "logging.level.org.testcontainers=ERROR",
        "logging.level.com.github.dockerjava=ERROR"
})
@AutoConfigureMockMvc
class MySqlPerformanceIT {
    private static final String IMAGE = "mysql:8.0.40";
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(IMAGE)
            .withDatabaseName("query_fixture")
            .withUsername("fixture")
            .withPassword(UUID.randomUUID().toString())
            .withCommand("--innodb-buffer-pool-size=268435456")
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                    .withMemory(2L * 1024 * 1024 * 1024).withNanoCPUs(2_000_000_000L)
                    .withPortBindings(new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 0), new ExposedPort(3306))));
    private static final Path OUT = Paths.get(System.getProperty("perf.output", "target/mysql-performance"));
    private static final int ACCOUNTS = Integer.getInteger("perf.accounts", 10_000);
    private static final int TRANSACTIONS = Integer.getInteger("perf.transactions", 50_000);
    private static final int SAMPLES = Integer.getInteger("perf.samples", 11);
    private static final int WARMUP = Integer.getInteger("perf.warmup", 3);
    private static final Pattern ACTUAL = Pattern.compile("actual time=[0-9.]+\\.\\.([0-9.]+) rows=([0-9]+)");
    private static final List<String> TIMINGS = new ArrayList<>();
    private static final List<String> SUMMARY = new ArrayList<>();

    @Autowired DataSource dataSource;
    @Autowired AccountRepository accounts;
    @Autowired TransactionRepository transactions;
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        MYSQL.start();
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

    public static class SqlCapture implements StatementInspector {
        static final List<String> SQL = new CopyOnWriteArrayList<>();
        @Override public String inspect(String sql) {
            if (sql.startsWith("select") && !SQL.contains(sql)) SQL.add(sql);
            return sql;
        }
    }

    @Test
    void baselineAndIndexedResultsAreEquivalent() throws Exception {
        assertThat(ACCOUNTS).isBetween(100, 1_000_000);
        assertThat(TRANSACTIONS).isBetween(1_000, 5_000_000);
        assertThat(SAMPLES).isBetween(3, 101);
        assertThat(WARMUP).isBetween(1, 20);
        Files.createDirectories(OUT);
        Files.deleteIfExists(OUT.resolve("equivalence.txt"));
        TIMINGS.add("phase\tquery\tsample\tdb_explain_analyze_ms\tjdbc_materialize_ms\trows");
        SUMMARY.add("phase\tquery\tsamples\twarmup\tmedian_db_explain_analyze_ms\tmedian_jdbc_materialize_ms\trows");
        try (Connection c = dataSource.getConnection()) {
            seed(c);
            // The baseline is the schema at DevOps, before the versioned migration.
            if (indexExists(c)) execute(c, "DROP INDEX idx_account_username ON account");
            analyze(c);
            metadata(c);
            captureGeneratedSql();
            Map<String, Query> queries = queries();
            Map<String, String> before = snapshots(c, queries);
            verifyRepositorySemantics();
            String beforePage = verifyWebSemantics(c);
            schema(c, "before");
            benchmark(c, "before", queries);
            benchmarkApplication("before");
            try {
                migration(c, "V001__account_username_index.sql");
                analyze(c);
                schema(c, "after");
                benchmark(c, "after", queries);
                benchmarkApplication("after");
                assertThat(snapshots(c, queries)).isEqualTo(before);
                verifyRepositorySemantics();
                assertThat(verifyWebSemantics(c)).isEqualTo(beforePage);
                // Rollback itself is executable and must preserve the same results.
                migration(c, "U001__account_username_index.sql");
                assertThat(indexExists(c)).isFalse();
                assertThat(snapshots(c, queries)).isEqualTo(before);
                analyze(c);
                schema(c, "rollback");
                benchmark(c, "rollback", queries);
                migration(c, "V001__account_username_index.sql");
                assertThat(indexExists(c)).isTrue();
                write("equivalence.txt", "PASS: full JDBC row digests/counts before = after = rollback; "
                        + "repository edge cases and authenticated HTTP behavior checked in both states.\n" + before);
            } finally {
                if (!indexExists(c)) migration(c, "V001__account_username_index.sql");
            }
        } finally {
            write("samples.tsv", String.join("\n", TIMINGS) + "\n");
            write("summary.tsv", String.join("\n", SUMMARY) + "\n");
        }
    }

    private void seed(Connection c) throws SQLException {
        execute(c, "CREATE TABLE digit(n INT PRIMARY KEY)");
        execute(c, "INSERT INTO digit VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)");
        execute(c, "CREATE TEMPORARY TABLE seq(n INT PRIMARY KEY)");
        int size = Math.max(ACCOUNTS, TRANSACTIONS);
        execute(c, "INSERT INTO seq SELECT 1+a.n+10*b.n+100*c.n+1000*d.n+10000*e.n+100000*f.n+1000000*g.n n "
                + "FROM digit a CROSS JOIN digit b CROSS JOIN digit c CROSS JOIN digit d CROSS JOIN digit e "
                + "CROSS JOIN digit f CROSS JOIN digit g HAVING n <= " + size);
        execute(c, "INSERT INTO account(id,username,password,balance) SELECT n,CONCAT('user-',LPAD(n,7,'0')),"
                + "REPEAT('!',60),1000.00+MOD(n,900000)/100 FROM seq WHERE n <= " + ACCOUNTS);
        int hot = ACCOUNTS / 10 - 2;
        int cold = ACCOUNTS * 7 / 10;
        execute(c, "INSERT INTO `transaction`(id,account_id,amount,type,timestamp) SELECT n,"
                + "CASE WHEN MOD(n,20)=0 THEN 1 WHEN MOD(n,10)<8 THEN 2+MOD(FLOOR(n/10)*7919+MOD(n,10)," + hot + ") "
                + "ELSE " + (ACCOUNTS / 10) + "+MOD(FLOOR(n/10)*104729+MOD(n,10)," + cold + ") END,"
                + "CAST(MOD(n,43)*1.25 AS DECIMAL(38,2)),"
                + "CASE MOD(n,4) WHEN 0 THEN 'Deposit' WHEN 1 THEN 'Withdrawal' "
                + "WHEN 2 THEN 'Transfer Out to synthetic' ELSE 'Transfer In from synthetic' END,"
                + "TIMESTAMPADD(SECOND,FLOOR(n/3), '2024-01-01 00:00:00') FROM seq WHERE n <= " + TRANSACTIONS);
        // Last accounts are outside the generated transaction range.
        execute(c, "UPDATE account SET username='duplicate' WHERE id IN (" + (ACCOUNTS - 1) + "," + (ACCOUNTS - 2) + ")");
        execute(c, "UPDATE account SET username=NULL, balance=NULL WHERE id=" + (ACCOUNTS - 3));
        execute(c, "UPDATE account SET username='' WHERE id=" + (ACCOUNTS - 4));
        execute(c, "UPDATE account SET username='Caf\u00e9' WHERE id=" + (ACCOUNTS - 5));
        execute(c, "UPDATE account SET username=REPEAT('x',255), balance=999999999999999999999999999999999999.99 WHERE id=" + (ACCOUNTS - 6));
        execute(c, "INSERT INTO `transaction`(account_id,amount,type,timestamp) VALUES "
                + "(" + ACCOUNTS + ",0.00,'Deposit','2024-02-29 23:59:59.999999'),"
                + "(" + ACCOUNTS + ",0.00,'Deposit','2024-02-29 23:59:59.999999'),"
                + "(" + ACCOUNTS + ",NULL,NULL,NULL),"
                + "(" + ACCOUNTS + ",-0.01,'Withdrawal','1970-01-01 00:00:00'),"
                + "(" + ACCOUNTS + ",999999999999999999999999999999999999.99,'Deposit','2038-01-19 03:14:07'),"
                + "(NULL,1.00,NULL,NULL)");
        execute(c, "DROP TEMPORARY TABLE seq");
        execute(c, "DROP TABLE digit");
    }

    private void captureGeneratedSql() throws IOException {
        SqlCapture.SQL.clear();
        accounts.findByUsername(username(1));
        transactions.findByAccountId((long) ACCOUNTS);
        write("hibernate-sql.txt", String.join(";\n", SqlCapture.SQL) + ";\n");
    }

    private Map<String, Query> queries() {
        String accountSql = SqlCapture.SQL.stream().filter(s -> s.contains(".username=?")).findFirst().orElseThrow(AssertionError::new);
        String historySql = SqlCapture.SQL.stream().filter(s -> s.contains(".account_id=?")).findFirst().orElseThrow(AssertionError::new);
        Map<String, Query> q = new LinkedHashMap<>();
        q.put("account-existing", new Query(accountSql, username(ACCOUNTS / 2)));
        q.put("account-missing", new Query(accountSql, "absent-user"));
        q.put("account-duplicate", new Query(accountSql, "duplicate"));
        q.put("account-collation", new Query(accountSql, "CAFE"));
        q.put("account-empty", new Query(accountSql, ""));
        q.put("account-max-length", new Query(accountSql, String.join("", Collections.nCopies(255, "x"))));
        q.put("history-high-volume", new Query(historySql, 1L));
        q.put("history-empty", new Query(historySql, (long) ACCOUNTS - 7));
        q.put("history-boundaries", new Query(historySql, (long) ACCOUNTS));
        return q;
    }

    private void benchmark(Connection c, String phase, Map<String, Query> queries) throws Exception {
        for (Map.Entry<String, Query> entry : queries.entrySet()) {
            Query q = entry.getValue();
            String plan = queryText(c, "EXPLAIN ANALYZE " + q.sql, q.parameter);
            write(phase + "-" + entry.getKey() + "-plan.txt", "SQL: " + q.sql + "\nparameter: " + q.parameter + "\n" + plan);
            if (entry.getKey().equals("account-existing")) {
                assertThat(plan).contains(phase.equals("after") ? "using idx_account_username" : "Table scan");
            }
            for (int i = 0; i < WARMUP; i++) {
                queryText(c, "EXPLAIN ANALYZE " + q.sql, q.parameter);
                digest(c, q);
            }
            List<Double> db = new ArrayList<>();
            List<Double> client = new ArrayList<>();
            long rows = 0;
            for (int i = 0; i < SAMPLES; i++) {
                String explain = queryText(c, "EXPLAIN ANALYZE " + q.sql, q.parameter);
                Matcher match = ACTUAL.matcher(explain);
                assertThat(match.find()).as(explain).isTrue();
                double execution = Double.parseDouble(match.group(1));
                rows = Long.parseLong(match.group(2));
                long start = System.nanoTime();
                materialize(c, q);
                double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                db.add(execution);
                client.add(elapsed);
                TIMINGS.add(phase + "\t" + entry.getKey() + "\t" + i + "\t" + execution + "\t" + elapsed + "\t" + rows);
            }
            SUMMARY.add(phase + "\t" + entry.getKey() + "\t" + SAMPLES + "\t" + WARMUP + "\t" + median(db) + "\t" + median(client) + "\t" + rows);
        }
    }

    private void verifyRepositorySemantics() {
        assertThat(accounts.findByUsername("absent-user")).isEmpty();
        assertThat(accounts.findByUsername(null).get().getBalance()).isNull();
        assertThat(accounts.findByUsername("").get().getId()).isEqualTo((long) ACCOUNTS - 4);
        assertThat(accounts.findByUsername("CAFE").get().getId()).isEqualTo((long) ACCOUNTS - 5);
        assertThat(accounts.findByUsername(username(1)).get().getBalance()).isEqualByComparingTo("1000.01");
        assertThatThrownBy(() -> accounts.findByUsername("duplicate")).isInstanceOf(IncorrectResultSizeDataAccessException.class);
        assertThat(transactions.findByAccountId((long) ACCOUNTS - 7)).isEmpty();
        List<Transaction> high = transactions.findByAccountId(1L);
        assertThat(high).hasSize(TRANSACTIONS / 20);
        assertThat(high).allSatisfy(t -> assertThat(t.getAccount().getId()).isEqualTo(1L));
        List<Transaction> edge = transactions.findByAccountId((long) ACCOUNTS);
        assertThat(edge).hasSize(5);
        assertThat(edge.stream().filter(t -> BigDecimal.ZERO.equals(t.getAmount() == null ? null : t.getAmount().stripTrailingZeros())).count()).isEqualTo(2);
        assertThat(edge).anySatisfy(t -> {
            assertThat(t.getAmount()).isNull();
            assertThat(t.getType()).isNull();
            assertThat(t.getTimestamp()).isNull();
        });
        assertThat(edge).anySatisfy(t -> {
            assertThat(t.getAmount()).isEqualByComparingTo("-0.01");
            assertThat(t.getTimestamp()).isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));
        });
        assertThat(edge).anySatisfy(t -> assertThat(t.getAmount()).isEqualByComparingTo("999999999999999999999999999999999999.99"));
        assertThat(edge).anySatisfy(t -> assertThat(t.getTimestamp()).isEqualTo(LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999999000)));
        assertThat(transactions.findByAccountId(null)).hasSize(1);
    }

    private void benchmarkApplication(String phase) throws Exception {
        List<String> samples = new ArrayList<>();
        samples.add("endpoint\tsample\tmockmvc_response_ms\tresponse_bytes");
        List<String> summary = new ArrayList<>();
        summary.add("endpoint\tsamples\twarmup\tmedian_mockmvc_response_ms");
        for (String endpoint : Arrays.asList("/dashboard", "/transactions")) {
            for (int i = 0; i < WARMUP; i++) mvc.perform(get(endpoint).with(user(username(ACCOUNTS / 2)))).andExpect(status().isOk());
            List<Double> elapsed = new ArrayList<>();
            for (int i = 0; i < SAMPLES; i++) {
                long start = System.nanoTime();
                byte[] response = mvc.perform(get(endpoint).with(user(username(ACCOUNTS / 2))))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
                double ms = (System.nanoTime() - start) / 1_000_000.0;
                elapsed.add(ms);
                samples.add(endpoint + "\t" + i + "\t" + ms + "\t" + response.length);
            }
            summary.add(endpoint + "\t" + SAMPLES + "\t" + WARMUP + "\t" + median(elapsed));
        }
        write(phase + "-application-samples.tsv", String.join("\n", samples) + "\n");
        write(phase + "-application-summary.tsv", String.join("\n", summary) + "\n");
    }

    private String verifyWebSemantics(Connection c) throws Exception {
        String sender = username(ACCOUNTS - 7);
        String recipient = username(ACCOUNTS - 8);
        execute(c, "UPDATE account SET balance=1000.00 WHERE id IN (" + (ACCOUNTS - 7) + "," + (ACCOUNTS - 8) + ")");
        mvc.perform(get("/transactions")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/transfer").param("toUsername", recipient).param("amount", "10.01"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/transfer").with(user(sender)).param("toUsername", recipient).param("amount", "10.01"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
        assertThat(accounts.findByUsername(sender).get().getBalance()).isEqualByComparingTo("989.99");
        assertThat(accounts.findByUsername(recipient).get().getBalance()).isEqualByComparingTo("1010.01");
        List<Transaction> debit = transactions.findByAccountId((long) ACCOUNTS - 7);
        assertThat(debit).hasSize(1);
        assertThat(debit.get(0).getAmount()).isEqualByComparingTo("10.01");
        assertThat(debit.get(0).getType()).isEqualTo("Transfer Out to " + recipient);
        List<Transaction> credit = transactions.findByAccountId((long) ACCOUNTS - 8);
        assertThat(credit).hasSize(1);
        assertThat(credit.get(0).getAmount()).isEqualByComparingTo("10.01");
        assertThat(credit.get(0).getType()).isEqualTo("Transfer In from " + sender);
        mvc.perform(post("/transfer").with(user(sender)).param("toUsername", recipient).param("amount", "10000.00"))
                .andExpect(status().isOk()).andExpect(model().attribute("error", "Insufficient funds"));
        mvc.perform(post("/transfer").with(user(sender)).param("toUsername", "absent-user").param("amount", "1.00"))
                .andExpect(status().isOk()).andExpect(model().attribute("error", "Recipient account not found"));
        mvc.perform(get("/transactions").with(user(sender)).param("accountId", "1"))
                .andExpect(status().isOk()).andExpect(model().attribute("transactions", org.hamcrest.Matchers.hasSize(1)));
        execute(c, "DELETE FROM `transaction` WHERE id > " + (TRANSACTIONS + 6));
        execute(c, "ALTER TABLE `transaction` AUTO_INCREMENT=" + (TRANSACTIONS + 7));
        execute(c, "UPDATE account SET balance=1000.00+MOD(id,900000)/100 WHERE id IN (" + (ACCOUNTS - 7) + "," + (ACCOUNTS - 8) + ")");
        return mvc.perform(get("/transactions").with(user(username(ACCOUNTS / 2))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private Map<String, String> snapshots(Connection c, Map<String, Query> queries) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, Query> q : queries.entrySet()) values.put(q.getKey(), digest(c, q.getValue()));
        values.put("null-username", digest(c, new Query("SELECT id,balance,username FROM account WHERE username IS NULL", null)));
        values.put("null-account", digest(c, new Query("SELECT * FROM `transaction` WHERE account_id IS NULL", null)));
        return values;
    }

    private String digest(Connection c, Query q) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        long count = 0;
        try (PreparedStatement s = prepare(c, q.sql, q.parameter); ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                count++;
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    String value = rs.getString(i);
                    byte[] bytes = (value == null ? "N;" : value.length() + ":" + value + ";").getBytes(StandardCharsets.UTF_8);
                    hash.update(bytes);
                }
            }
        }
        return count + ":" + Base64.getEncoder().encodeToString(hash.digest());
    }

    private void materialize(Connection c, Query q) throws SQLException {
        try (PreparedStatement s = prepare(c, q.sql, q.parameter); ResultSet rs = s.executeQuery()) {
            while (rs.next()) for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) rs.getObject(i);
        }
    }

    private void metadata(Connection c) throws Exception {
        write("environment.txt", queryText(c, "SELECT VERSION(), @@version_comment, @@innodb_buffer_pool_size, @@collation_server, @@transaction_isolation", null)
                + "\nimage=" + IMAGE + "\ncontainerImageId=" + MYSQL.getContainerInfo().getImageId()
                + "\nMySQL limits: 2 CPUs, 2 GiB RAM; buffer pool: 256 MiB\n"
                + "JVM=" + System.getProperty("java.version") + "; os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + "; visible CPUs=" + Runtime.getRuntime().availableProcessors() + "; JVM max heap=" + Runtime.getRuntime().maxMemory()
                + "\naccounts=" + ACCOUNTS + "; generated transactions=" + TRANSACTIONS + "; boundary transactions=6\n"
                + "samples=" + SAMPLES + "; warmup=" + WARMUP + "\n"
                + queryText(c, "SELECT COUNT(*) accounts FROM account", null)
                + queryText(c, "SELECT COUNT(*) transactions, COUNT(DISTINCT account_id) active_accounts FROM `transaction`", null)
                + queryText(c, "SELECT account_id,COUNT(*) row_count FROM `transaction` GROUP BY account_id ORDER BY row_count DESC LIMIT 10", null));
        Path cpu = Paths.get("/proc/cpuinfo");
        Path mem = Paths.get("/proc/meminfo");
        if (Files.exists(cpu)) write("host-cpu.txt", new String(Files.readAllBytes(cpu), StandardCharsets.UTF_8));
        if (Files.exists(mem)) write("host-memory.txt", new String(Files.readAllBytes(mem), StandardCharsets.UTF_8));
    }

    private void schema(Connection c, String phase) throws Exception {
        execute(c, "SET SESSION information_schema_stats_expiry=0");
        write(phase + "-schema.txt", queryText(c, "SHOW CREATE TABLE account", null)
                + queryText(c, "SHOW CREATE TABLE `transaction`", null)
                + queryText(c, "SHOW INDEX FROM account", null)
                + queryText(c, "SHOW INDEX FROM `transaction`", null)
                + queryText(c, "SELECT TABLE_NAME,TABLE_ROWS,DATA_LENGTH,INDEX_LENGTH FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()", null));
    }

    private boolean indexExists(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SHOW INDEX FROM account WHERE Key_name='idx_account_username'")) { return rs.next(); }
    }

    private void migration(Connection c, String file) throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get("sql/performance", file)), StandardCharsets.UTF_8);
        execute(c, sql);
    }

    private void analyze(Connection c) throws SQLException {
        execute(c, "ANALYZE TABLE account, `transaction`");
    }

    private void execute(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.execute(sql); }
    }

    private PreparedStatement prepare(Connection c, String sql, Object parameter) throws SQLException {
        PreparedStatement s = c.prepareStatement(sql);
        if (sql.contains("?")) s.setObject(1, parameter);
        return s;
    }

    private String queryText(Connection c, String sql, Object parameter) throws SQLException {
        StringBuilder out = new StringBuilder();
        try (PreparedStatement s = prepare(c, sql, parameter); ResultSet rs = s.executeQuery()) {
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) out.append(rs.getMetaData().getColumnLabel(i)).append('\t');
            out.append('\n');
            while (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) out.append(rs.getString(i)).append('\t');
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static String username(int n) { return String.format(Locale.ROOT, "user-%07d", n); }
    private static double median(List<Double> values) {
        Collections.sort(values);
        int middle = values.size() / 2;
        return values.size() % 2 == 0 ? (values.get(middle - 1) + values.get(middle)) / 2 : values.get(middle);
    }
    private static void write(String name, String body) throws IOException { Files.write(OUT.resolve(name), body.getBytes(StandardCharsets.UTF_8)); }
    private static class Query {
        final String sql;
        final Object parameter;
        Query(String sql, Object parameter) { this.sql = sql; this.parameter = parameter; }
    }
}
