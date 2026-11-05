# End-to-end tests (Cypress)

The `e2e/` folder holds a Cypress smoke suite that drives the real application
in a browser against the real MySQL database. It runs in GitHub Actions
(`.github/workflows/e2e.yml`) on every pull request and on pushes to `DevOps`,
and as the `E2E: Cypress smoke suite` stage of the `Jenkinsfile`.

## What is covered

| Spec | Checks |
| --- | --- |
| `cypress/e2e/01-register.cy.js` | `/register` renders; a new user can register, log in and land on the dashboard with a `$0` balance; duplicate usernames are rejected |
| `cypress/e2e/02-login.cy.js` | seeded user logs in and lands on `/dashboard`; wrong password stays on `/login?error`; anonymous `/dashboard` redirects to login |
| `cypress/e2e/03-transactions.cy.js` | `/transactions` lists exactly the seeded rows for the signed-in account with the right sign/colour |
| `cypress/e2e/04-transfer.cy.js` | the Transfer Money form debits the balance shown on the dashboard and writes a `Transfer Out` row; insufficient funds and unknown recipient show the error text |

## Layout

```
e2e/
  cypress.config.js          baseUrl, video, retries, seeded credentials (Cypress.env)
  cypress/e2e/*.cy.js        the specs, numbered in the order they run
  cypress/support/commands.js cy.login(username, password) and cy.readBalance()
  docker-compose.e2e.yml     override for the root docker-compose.yml (build from source, wget health check, throw-away volume, own container names and port)
  seed/seed.sql              fixture accounts and transactions
  scripts/stack-up.sh        docker compose up --build --wait, then poll /login
  scripts/seed.sh            pipes seed.sql into the mysql container
  scripts/stack-logs.sh      docker compose logs (archived by CI on failure)
  scripts/stack-down.sh      docker compose down -v
```

### Seed data

`seed/seed.sql` is loaded **after** the application has booted, because
Hibernate creates the `account` and `transaction` tables on start-up
(`spring.jpa.hibernate.ddl-auto=update`). The file deletes and re-inserts its
rows, so running it again resets the fixtures.

| Username | Password | Balance | Transactions |
| --- | --- | --- | --- |
| `alice` | `Password123!` | 500.00 | Deposit 1000.00, Withdrawal 300.00, Transfer Out to bob 200.00 |
| `bob` | `Password123!` | 100.00 | Transfer In from alice 200.00, Withdrawal 100.00 |

The password hash is bcrypt cost 10, the same encoder `SecurityConfig` uses.
To seed a different password, generate a new hash
(`htpasswd -bnBC 10 "" 'NewPassword' | tr -d ':\n'` or
`python3 -c "import bcrypt;print(bcrypt.hashpw(b'NewPassword',bcrypt.gensalt(10)).decode())"`)
and update both `seed.sql` and `seededPassword` in `cypress.config.js`.

## Running locally

Requirements: Docker with the Compose v2 plugin (`docker compose`, 2.24 or newer), Node 20+,
and the usual Cypress system libraries (already present on Ubuntu desktop
images; see <https://docs.cypress.io/app/get-started/install-cypress#Linux-Prerequisites>).

```bash
cd e2e
npm ci                 # installs Cypress (binary is cached under ~/.cache/Cypress)
npm run stack:up       # builds bankapp:e2e from the working tree, starts MySQL + app on :8081, waits until healthy
npm run stack:seed     # loads seed/seed.sql
npm run cy:run         # headless run, videos in cypress/videos, screenshots on failure in cypress/screenshots
npm run stack:down     # stops containers and drops the database volume
```

Useful variations:

```bash
npm run cy:open                                   # interactive runner, pick a spec and watch it
npx cypress run --spec cypress/e2e/04-transfer.cy.js   # one spec only
npx cypress run --browser chrome                  # a specific browser (default is Electron)
CYPRESS_BASE_URL=http://host:8080 npm run cy:run  # point at an app that is already running
E2E_APP_PORT=9090 npm run stack:up && CYPRESS_BASE_URL=http://localhost:9090 npm run cy:run  # another host port
```

The stack runs as Compose project `bankapp-e2e` (containers
`bankapp-e2e-mainapp-1` and `bankapp-e2e-mysql-1`, host port 8081), so it does not interfere with a
regular `docker compose up` of the root file on port 8080, and `stack:down`
only removes the e2e containers and volume. Set `E2E_COMPOSE_PROJECT` and `E2E_APP_PORT` to run
several e2e stacks side by side.

If you change Java code, rerun `npm run stack:up`; `--build` rebuilds the
image from the working tree. If the app comes up but the specs fail with
`Account not found`, you forgot `npm run stack:seed`.

## Adding a test

1. Create `e2e/cypress/e2e/NN-feature.cy.js`. Keep the numeric prefix so the
   run order stays readable in the report.
2. Need to be logged in? Call `cy.login(Cypress.env("seededUser"), Cypress.env("seededPassword"))`
   in `beforeEach`. It uses `cy.session`, so the login form is only submitted
   once per spec.
3. Select elements by `name` attribute or visible text (`cy.get('input[name="amount"]')`,
   `cy.contains("button", "Transfer Money")`). The templates have no
   `data-cy` attributes; if you add some for a new feature, prefer them.
4. Assert on what the user sees: URL (`cy.location("pathname")`), headings,
   table rows, the `.error` paragraph on the dashboard.
5. Tests that move money must work from the seeded balances no matter which
   order they run in. Read the balance first (`cy.readBalance()`) and assert
   relative to it, or register a throw-away user as `01-register.cy.js` does.
6. If the test needs new fixture rows, add them to `seed/seed.sql` (and to the
   table above) rather than creating them through the UI in a `before` hook,
   so every run starts from the same state.
7. Run it headless with `npx cypress run --spec cypress/e2e/NN-feature.cy.js`
   and watch it once with `npm run cy:open` before opening the PR.

## CI

**GitHub Actions** (`.github/workflows/e2e.yml`) installs Node 20, restores the
npm and Cypress-binary caches, then runs the same four npm scripts as above.
On failure it uploads `cypress-failure-artifacts` (screenshots, videos and
`docker compose logs`); on success it uploads `cypress-videos`. Download them
from the run's *Summary* page.

**Jenkins** (`Jenkinsfile`, stage `E2E: Cypress smoke suite`) runs between the
image build and the Docker Hub push so a failing suite blocks the push. The
agent needs Docker Compose v2, Node 20+ and the Cypress Linux prerequisites.
Screenshots, videos and `e2e/stack.log` are archived with `archiveArtifacts`
when the stage fails; the stack is torn down in `post { always }` either way.
