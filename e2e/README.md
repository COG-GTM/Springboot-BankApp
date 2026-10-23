# Manual regression plan, automated

## Prerequisites

- JDK 17
- Node 20
- Docker with the `mysql:8.0` image
- Maven via the included `./mvnw`, no install needed
- Playwright Chromium, installed with `npx playwright install chromium`

Fresh machine: `demo/setup-demo.sh` installs all of the above, pre-builds the app and runs a smoke check. Reset between demos: `demo/start-demo.sh`.

`qa/manual-test-plan.csv` is the 60-case manual regression plan for login, deposit, withdraw, transfer and history.
This folder turns it into a Playwright browser suite. Every test title starts with its manual case ID (`MT-001` ...).
Cases that cannot be automated, or that need a QA decision first, are listed with a reason in `qa/not-automated.json`.

## Run locally

```bash
docker run -d --name bankapp-mysql -e MYSQL_ROOT_PASSWORD=Test@123 -e MYSQL_DATABASE=bankappdb -p 3306:3306 mysql:8.0 --default-authentication-plugin=mysql_native_password
./mvnw -q -DskipTests spring-boot:run        # Java 17, app on http://localhost:8080
cd e2e && npm ci && npx playwright install chromium
npx playwright test                          # 54 tests, about 20 seconds with 4 workers
npm run matrix                               # writes qa/coverage-matrix.md
```

Every test registers its own customers with unique usernames, so runs never collide and no data reset is needed between runs.
To start from an empty database: `docker rm -f bankapp-mysql` and run the `docker run` line again.

CI: `.github/workflows/e2e.yml` runs the same suite against MySQL 8 on every pull request and publishes the coverage matrix in the job summary.
