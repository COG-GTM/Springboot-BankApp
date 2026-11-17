# Configuration and secret contract

Control reference: **ITGC-SEC-06** (no secrets or credentials committed to source or
configuration) and **ITGC-DATA-10** (schema changes are controlled).

## Required environment variables

The application reads all database credentials from the environment. There are no
credential defaults in `src/main/resources/application.properties`; if a required variable is
missing the context fails to start rather than falling back to a shipped credential.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `SPRING_DATASOURCE_URL` | no | `jdbc:mysql://localhost:3306/bankappdb?useSSL=false&serverTimezone=UTC` | JDBC URL. Not a secret. |
| `SPRING_DATASOURCE_USERNAME` | **yes** | none | Database user. Injected from the platform secret store. |
| `SPRING_DATASOURCE_PASSWORD` | **yes** | none | Database password. Injected from the platform secret store. **Never** commit a value. |

## Where each environment gets its values

| Environment | Source of the credential |
| --- | --- |
| Local development | Shell environment or an untracked `.env.local`; see below. |
| CI (GitHub Actions) | Repository secret `CI_MYSQL_ROOT_PASSWORD`, injected into the ephemeral MySQL service container. Falls back to a throwaway value if unset. |
| Kubernetes (`kubernetes/`) | Secret `mysql-secret`, created out-of-band from the platform secret store — see `kubernetes/secrets.example.yaml`. The manifest with real values is not in this repository. |
| Helm (`helm/bankapp`) | `secret.data.*` values supplied at install time (`--set` or a values file held in the secret store), or an existing secret. `helm/bankapp/values.yaml` ships empty and the template fails the render when a value is missing. |
| Docker Compose | `MYSQL_ROOT_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` from the shell or an untracked `.env.local`; `docker-compose.yml` has no literal values. |

## Local development

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/bankappdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='<your local password>'
./mvnw spring-boot:run
```

`.env.local` is git-ignored; `.env.example` documents the variable names only.

## Enforcement

Reintroducing a credential fails the build:

* GitHub Actions job **Secret scan (gitleaks)** on every pull request and push.
* Jenkins stage **Gitleaks: Secret scan** before the build runs.
* Both use `.gitleaks.toml`, which extends the default rule set with a
  `database-password-assignment` rule: any `MYSQL_ROOT_PASSWORD` /
  `spring.datasource.password` assignment is a finding unless the value is resolved
  outside the repository (`$...`, `{{ ... }}`, `<placeholder>` or empty).

Run the same check locally before pushing:

```bash
docker run --rm -v "$PWD:/repo" -w /repo zricethezav/gitleaks:v8.18.4 \
  detect --source=/repo --no-git --config=/repo/.gitleaks.toml --redact --no-banner --exit-code 1
```

## Rotation

The credentials that were previously committed to
`src/main/resources/application.properties`, `kubernetes/secrets.yaml` and
`helm/bankapp/values.yaml` must be treated as disclosed and rotated in every
environment where they were ever used. Removing them from `HEAD` does not remove them
from the git history.

## Schema changes

The schema is owned by Flyway (`src/main/resources/db/migration`) and
`spring.jpa.hibernate.ddl-auto` is `validate`, so the runtime can never mutate the schema.
Add a new, append-only `V<n>__<description>.sql`; never edit an applied migration.

Migrations run automatically at application startup, and can be applied to a database
without starting the application using the same credentials:

```bash
./mvnw flyway:migrate
```
