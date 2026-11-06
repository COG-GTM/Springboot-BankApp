# MySQL username lookup performance

## Diagnosis and change

`AccountRepository.findByUsername` is used by authentication, registration,
dashboard, transaction history, and transfer recipient lookup. Hibernate emits:

```sql
select a1_0.id,a1_0.balance,a1_0.password,a1_0.username
from account a1_0 where a1_0.username=?;
```

The actual MySQL schema generated from the `DevOps` entities had only `PRIMARY(id)`
on `account`. It scanned every account even for a missing username. The change is
one **non-unique**, full-column B-tree index on `account(username)`, in
[`V001__account_username_index.sql`](../sql/performance/V001__account_username_index.sql).
No application Java, repository query, controller, security configuration, template,
database engine, response size, or ordering clause changes.

The second candidate, `TransactionRepository.findByAccountId`, already uses the
InnoDB-created foreign-key index `FK6g20fcr3bhr6bihgy24rq1r1b(account_id)`.
`SHOW CREATE TABLE`, `SHOW INDEX`, and `EXPLAIN ANALYZE` verified it in the actual
disposable database. Do **not** add a redundant transaction index. The generated
history SQL is:

```sql
select t1_0.id,t1_0.account_id,t1_0.amount,t1_0.timestamp,t1_0.type
from transaction t1_0 where t1_0.account_id=?;
```

Hibernate also loads the eager account by primary key when needed. That query was
captured but not changed. History has no `ORDER BY`; this change introduces none,
does not remove duplicates, and does not paginate. Observed row sequence is checked
on the fixture, but unordered SQL does not guarantee a sequence across environments.

## Reproduce

Requirements: Java 17, Maven, a running local Docker daemon, and access to Maven
Central and Docker Hub. No external database, credentials, or H2 substitutes are
used by the integration test. Testcontainers creates MySQL **8.0.40**, binds its
random port to loopback, generates a disposable password at runtime, and removes
the container at JVM exit. Kubernetes specifies MySQL 8.0 and the application uses
the MySQL 8 dialect; the root Compose/Helm `latest` tags are not used or changed.

From the repository root:

```bash
# 100,000 accounts, 1,000,000 generated transactions + 6 boundary rows;
# 5 warm-ups, then 21 measured samples per query per phase.
scripts/performance/benchmark.sh

# Smaller correctness fixture: entry point for an existing Jenkins job.
mvn -B -Pmysql-performance verify \
  -Dperf.accounts=10000 -Dperf.transactions=50000 -Dperf.samples=5

# Configurable volume and repetitions (same before/after/rollback data).
PERF_ACCOUNTS=25000 PERF_TRANSACTIONS=250000 PERF_SAMPLES=11 \
  scripts/performance/benchmark.sh
```

The script uses installed `mvn`; `./mvnw` is an equivalent entry point once its
Maven distribution is available. On the measurement VM, Java 17 was selected with
`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`. Maven Central returned HTTP 429;
the local invocation appended `-s /home/ubuntu/maven-settings.xml`, a VM-only
settings file mirroring `central` to
`https://maven-central.storage-download.googleapis.com/maven2`. This settings file
is not needed on normal runners and contains no credentials.

`perf.accounts` accepts 100–1,000,000; `perf.transactions` accepts 1,000–5,000,000;
`perf.samples` accepts 3–101; `perf.warmup` accepts 1–20. The default Maven fixture
is 10,000 accounts / 50,000 transactions / 11 samples / 3 warm-ups. To retain
separate local runs, append `-Dperf.output=target/mysql-ci` (or another output
directory) and copy the JUnit reports before the next run. Do not run two Maven
processes in the same checkout/output directory concurrently.

### Data and measurement method

The generator is deterministic, uses fixed dates and integer arithmetic, and
preserves the mapped `DECIMAL(38,2)`, `DATETIME(6)`, nullable fields and MySQL
`utf8mb4_0900_ai_ci` collation. Password fields contain 60 non-authenticating
filler characters, not usable passwords. Balances are synthetic, not a reconciled
ledger. There are deposits, withdrawals and both transfer labels, repeated values
and timestamps, and six explicit boundary rows. The large fixture has:

- 100,000 accounts, 80,000 with history, 1,000,006 total transaction rows;
- one high-volume account with 50,000 rows (5% of generated transactions);
- another 75% distributed across the remainder of the busiest 10% of accounts;
- 20% across the next 70% of accounts; the rest are inactive;
- duplicate usernames, a null username/balance, empty and 255-character usernames,
  accent/case comparisons, duplicate monetary/timestamp rows, null fields and
  null foreign keys, zero, negative cents, maximum decimals, leap day with
  microseconds, and 1970/2038 timestamp boundaries.

Each run creates the unchanged entity schema, captures generated SQL, inspects
indexes, seeds once, and runs **before → add index → after → drop index → rollback**.
It re-applies the index and verifies its presence at the end. All phases use the
same server, connection, dataset, resource limits, predicates and complete result
sets. `ANALYZE TABLE` runs in each phase. Each query receives its own warm-up; there
is no artificial cold-cache baseline. No concurrent benchmark workload runs.

Three timing types are kept separate:

1. **Database execution:** the last-row time at the root iterator of repeated
   `EXPLAIN ANALYZE` calls. This includes MySQL's instrumentation overhead, not
   network transfer, Hibernate hydration, or rendering.
2. **JDBC:** wall-clock preparation, execution, transfer and consumption of every
   column of every row on a reused connection. This is *not* HTTP latency.
3. **Application:** in-process MockMvc request/response wall time through the
   security filter, controller, repository and Thymeleaf renderer, using a reused
   synthetic authenticated identity. It excludes TCP, browser rendering and
   password hashing; it is *not* end-user login latency. `/transactions` here uses
   an ordinary account, not a truncated high-volume response.

The control history query returns all 50,000 rows. Data digests are calculated
outside timed samples. The test records raw samples and medians, not just the
best run. The reverse/rollback phase helps expose warm-cache-only explanations;
it is not a randomized load test. There are no wall-clock pass/fail thresholds.

## Recorded local result

MySQL 8.0.40 image ID
`sha256:d58ac93387f644e4e040c636b8f50494e78e5afc27ca0a87348b2f577da2b7ff`,
InnoDB, repeatable read, 256 MiB buffer pool. MySQL container limited to 2 CPUs /
2 GiB RAM. Host: Linux x86-64, Intel Xeon, 8 visible CPUs, approximately 31 GiB RAM,
no swap; Java 17.0.19, JVM maximum heap 8,413,773,824 bytes. One client, warmed
dataset; 21 samples after 5 warm-ups per query in each phase.

| Query / parameter | Rows | DB before → after (ms) | JDBC before → after (ms) | DB after rollback (ms) |
|---|---:|---:|---:|---:|
| Username / `user-0050000` | 1 | 22.3 → 0.00719 | 17.574 → 0.347 | 23.8 |
| Username / `absent-user` | 0 | 19.0 → 0.00247 | 14.224 → 0.151 | 37.0 |
| Username / `duplicate` | 2 | 19.1 → 0.00543 | 14.169 → 0.190 | 19.0 |
| History / account `1` | 50,000 | 42.3 → 43.1 | 72.247 → 68.981 | 41.8 |

Username lookup has a demonstrated improvement; history SQL has **no meaningful
speedup** and is unchanged. Instrumented microsecond values should not be treated
as precise production latency forecasts. The existing-username JDBC median fell
about 51-fold; that is not an application-wide speedup claim.

Separately, MockMvc response medians were `/dashboard` **35.620 → 2.432 ms** and
`/transactions` **37.652 → 3.655 ms** for account `user-0050000`. The latter benefits
from the initial username lookup, not a faster transaction query. JVM warm-up and
scheduling noise also affect these in-process measurements.

Representative execution shapes (full plans, including estimates/actual times,
are archived by the harness):

```text
Before: Filter username = 'user-0050000' (rows=1)
          Table scan on account (actual rows=100000 loops=1)
After:  Index lookup using idx_account_username (actual rows=1 loops=1)
Rollback: Table scan on account again (actual rows=100000 loops=1)
History in all phases: Index lookup using FK6g20fcr3bhr6bihgy24rq1r1b
                      (account_id=1; actual rows=50000 loops=1)
```

## Correctness and artifacts

`MySqlPerformanceIT` is one end-to-end JUnit integration scenario with many
assertions. It compares full ordered row digests and counts for all queried columns
before/after/rollback, including empty, duplicate, collation, null and boundary
cases, and checks the real Spring Data repositories in both index states.
Duplicate usernames still cause `IncorrectResultSizeDataAccessException`; the
index is deliberately **not unique**. High-volume history retains all rows,
owner IDs, decimals and timestamps. A complete valid history HTML response is
compared before/after. Null transaction fields are tested at SQL/repository level,
not in the browser renderer.

MockMvc checks anonymous redirects, ignores a supplied foreign `accountId` in
favor of the signed-in username, verifies successful transfer balances and debit /
credit rows, insufficient funds, and missing recipients, in both index states.
The existing H2 context smoke test also runs but is **not MySQL correctness or
performance evidence**. Browser verification is separate and recorded on the PR;
it uses synthetic signed-in accounts and the applied index.

Artifacts in `target/mysql-performance/`: captured Hibernate SQL, parameters and
full `EXPLAIN ANALYZE` plans for each phase/case, actual schemas/indexes/table sizes,
database and host metadata, all timing samples, medians, and `equivalence.txt`
(written only after all comparisons and rollback/reapply succeed). JUnit reports
are in `target/surefire-reports/` and `target/failsafe-reports/`. Preserve these
directories before another run. Only a successful current Maven run and its
equivalence marker are evidence of a pass; a failed run can leave partial plans.

The new pull-request-only GitHub Actions job runs real MySQL in Testcontainers
with the smaller fixture and uploads all three directories even on failure.
It has read-only repository permissions, no publishing/deployment step, and no
performance threshold. The existing Jenkins and deployment files are unchanged
and were not invoked. A maintainer can use this test-only stage in an appropriate
non-publishing job; do not append it to a publishing job and trigger that job just
to run tests:

```groovy
stage('MySQL correctness') {
    steps {
        sh 'mvn -B -Pmysql-performance verify -Dperf.accounts=10000 -Dperf.transactions=50000 -Dperf.samples=5'
    }
    post {
        always {
            junit 'target/surefire-reports/*.xml,target/failsafe-reports/TEST-*.xml'
            archiveArtifacts artifacts: 'target/mysql-performance/**,target/*-reports/**', allowEmptyArchive: true
        }
    }
}
```

## Apply / rollback and tradeoffs

These are explicit, versioned **manual SQL migrations**, not a newly introduced
migration framework. Application startup does not discover this directory.
Deploying the application artifact alone does **not** apply the improvement.
The benchmark applies the actual files automatically on its owned disposable DB.
For a separately started disposable MySQL 8.0 database, first inspect:

```sql
SELECT VERSION();
SHOW CREATE TABLE account;
SHOW INDEX FROM account;
SHOW INDEX FROM `transaction`;
```

Do not apply if an equivalent visible full-column index with `username` as its
leading column already exists, or if this index name is occupied. Do not make a
unique index or change collation to force a plan. Apply once; these files are not
idempotent and there is no migration-history table. Use the target schema and an
interactive password prompt (never a command-line password):

```bash
mysql --host=127.0.0.1 --port="$MYSQL_PORT" --user="$MYSQL_USER" --password "$MYSQL_DATABASE" \
  < sql/performance/V001__account_username_index.sql
# Verify SHOW INDEX and EXPLAIN ANALYZE, then rollback if needed:
mysql --host=127.0.0.1 --port="$MYSQL_PORT" --user="$MYSQL_USER" --password "$MYSQL_DATABASE" \
  < sql/performance/U001__account_username_index.sql
```

Rollback removes only `idx_account_username`; no rows or constraints change and
the original scan returns. No Java rollback or restart is needed. Both SQL files
use `ALGORITHM=INPLACE, LOCK=NONE`; MySQL still needs metadata locks and the build
uses I/O, temporary space, CPU and redo. They were exercised only on disposable
infrastructure, not under concurrent production writes or long transactions.

For the large fixture, `account.INDEX_LENGTH` rose from 0 to **3,686,400 bytes
(3.52 MiB)** after `ANALYZE TABLE` with metadata cache expiry disabled. Allocation
is page-granular and dataset-specific, not a universal per-row forecast. The index
adds storage/buffer-pool pressure and maintenance on account inserts, deletes and
username updates. Balance-only updates do not change the indexed username value.
It adds no new transaction-table index/write path. Write-throughput cost was not
benchmarked, and full 255-character usernames would consume more space.

## Limits

These measurements concern **this application and this synthetic MySQL dataset**,
**not Oracle 19c or a 10-TB deployment**. No production data/access, load/concurrency
test, cold-cache test, distributed network timing, multiple server versions,
online migration under contention, or write-throughput benchmark was performed.
Browser correctness is not a browser-latency benchmark. Existing transactionality,
security and unbounded-history design were not altered. No deployment/publishing
pipeline was changed or triggered, and no changes were made to
[PR #361](https://github.com/COG-GTM/Springboot-BankApp/pull/361).
