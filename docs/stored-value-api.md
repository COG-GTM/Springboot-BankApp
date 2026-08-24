# Stored-Value (Gift Card) API

Partner-facing stored-value surface for issuing, inquiring on, and redeeming gift-card balances.
The spec below is the contract; the implementation in `com.example.bankapp.controller.StoredValueController`
and `com.example.bankapp.service.StoredValueService` follows it.

Base path: `/api/v1/stored-value`

## Authentication

Stateless HTTP Basic against an existing bank account (`Authorization: Basic ...`). The API filter
chain (`SecurityConfig.storedValueApiFilterChain`) is session-less, so no CSRF token is required or
accepted; the browser/Thymeleaf chain is unchanged. Missing *and* invalid credentials both get a
`401` with the JSON error body — never a redirect to the HTML login page.

## Invariants enforced

| Invariant | Where |
|---|---|
| Balance can never go negative (no over-redemption) | `StoredValueService.redeem` → `INSUFFICIENT_BALANCE` |
| No double-spend under concurrent redemption | `SELECT ... FOR UPDATE` row lock (`StoredValueCardRepository.findByCardTokenForUpdate`) inside a single `@Transactional` boundary |
| A redemption is applied at most once per `Idempotency-Key` per card | unique key `uk_stored_value_txn_idempotency (card_id, idempotency_key)` + replay lookup |
| Reusing a key with a different amount is rejected, never silently re-priced | `IDEMPOTENCY_KEY_CONFLICT` |
| Expired cards cannot be redeemed and report `EXPIRED` on inquiry | `StoredValueCard.applyExpiry` / `CARD_EXPIRED` |
| Amounts are positive, ≤ 2 decimal places, ≤ 10,000.00 at issue | bean validation on the DTOs + service-level re-check |
| The PAN-equivalent `card_reference` is never returned by the API and never logged; logs carry a masked token only | `StoredValueCard.maskToken`, no `cardReference` in any DTO |
| Every balance movement produces an immutable ledger row with the resulting balance | `stored_value_transaction` |

## Card lifecycle

`ACTIVE` → `DEPLETED` (balance reaches zero) or `EXPIRED` (now ≥ `expiresAt`). Expiry is evaluated on
read as well as on redemption, so a balance inquiry after the expiry instant reports `EXPIRED`.

## Consumer disclosure

Card representations embed a `disclosure` object stating that no purchase, dormancy, inactivity or
service fees are assessed, plus the expiry policy (either "funds do not expire" or the exact instant
after which redemption is refused).

## Errors

All errors share one shape:

```json
{ "code": "INSUFFICIENT_BALANCE", "message": "...", "details": [], "timestamp": "2026-01-01T00:00:00Z" }
```

| HTTP | code |
|---|---|
| 400 | `VALIDATION_FAILED`, `MISSING_HEADER`, `MALFORMED_REQUEST`, `INVALID_AMOUNT`, `INVALID_EXPIRY` |
| 401 | `UNAUTHORIZED` |
| 404 | `CARD_NOT_FOUND` |
| 409 | `CARD_EXPIRED`, `INSUFFICIENT_BALANCE`, `IDEMPOTENCY_KEY_CONFLICT` |
| 500 | `INTERNAL_ERROR` |

## OpenAPI 3.0 specification

```yaml
openapi: 3.0.3
info:
  title: BankApp Stored-Value API
  version: 1.0.0
  description: Issue, inquire on, and redeem stored-value (gift card) balances.
servers:
  - url: /api/v1
security:
  - basicAuth: []
paths:
  /stored-value/cards:
    post:
      summary: Issue a stored-value card
      operationId: issueCard
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/IssueCardRequest'
      responses:
        '201':
          description: Card issued
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Card'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
  /stored-value/cards/{token}/balance:
    get:
      summary: Balance inquiry
      operationId: getBalance
      parameters:
        - $ref: '#/components/parameters/CardToken'
      responses:
        '200':
          description: Current balance
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Balance'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
  /stored-value/cards/{token}/redeem:
    post:
      summary: Redeem part or all of the card balance
      operationId: redeemCard
      parameters:
        - $ref: '#/components/parameters/CardToken'
        - name: Idempotency-Key
          in: header
          required: true
          schema:
            type: string
            maxLength: 128
          description: >-
            Unique per logical redemption. Replaying the same key with the same amount returns the
            original ledger entry with replayed=true and does not move money.
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RedeemRequest'
      responses:
        '200':
          description: Redemption applied (or replayed)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Redemption'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
        '409':
          description: CARD_EXPIRED, INSUFFICIENT_BALANCE or IDEMPOTENCY_KEY_CONFLICT
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'
  /stored-value/cards/{token}/transactions:
    get:
      summary: Ledger history for a card
      operationId: getCardTransactions
      parameters:
        - $ref: '#/components/parameters/CardToken'
      responses:
        '200':
          description: Ledger entries, oldest first
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/LedgerEntry'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
components:
  securitySchemes:
    basicAuth:
      type: http
      scheme: basic
  parameters:
    CardToken:
      name: token
      in: path
      required: true
      schema:
        type: string
      description: Opaque card token. The PAN-equivalent card reference is never exposed.
  responses:
    BadRequest:
      description: Validation failure
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    Unauthorized:
      description: Authentication required
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    NotFound:
      description: Card not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
  schemas:
    IssueCardRequest:
      type: object
      required: [amount, currency]
      properties:
        amount:
          type: number
          format: double
          minimum: 0.01
          maximum: 10000.00
          description: Load amount, at most 2 decimal places.
        currency:
          type: string
          pattern: '^[A-Z]{3}$'
        expiresAt:
          type: string
          format: date-time
          nullable: true
          description: Optional expiry; must be in the future. Omit for non-expiring value.
    RedeemRequest:
      type: object
      required: [amount]
      properties:
        amount:
          type: number
          format: double
          minimum: 0.01
    Card:
      type: object
      properties:
        cardToken: { type: string }
        currency: { type: string }
        initialAmount: { type: number, format: double }
        balance: { type: number, format: double }
        status: { $ref: '#/components/schemas/CardStatus' }
        issuedAt: { type: string, format: date-time }
        expiresAt: { type: string, format: date-time, nullable: true }
        disclosure: { $ref: '#/components/schemas/Disclosure' }
    Balance:
      type: object
      properties:
        cardToken: { type: string }
        currency: { type: string }
        balance: { type: number, format: double }
        status: { $ref: '#/components/schemas/CardStatus' }
        expiresAt: { type: string, format: date-time, nullable: true }
        asOf: { type: string, format: date-time }
    Redemption:
      type: object
      properties:
        transactionId: { type: integer, format: int64 }
        cardToken: { type: string }
        amountRedeemed: { type: number, format: double }
        remainingBalance:
          type: number
          format: double
          description: >-
            Balance settled by this redemption. On a replay this is the original value and never
            changes, even if later redemptions have moved the card on.
        currency: { type: string }
        status:
          allOf: [ { $ref: '#/components/schemas/CardStatus' } ]
          description: Current card status, not the status at the time of the original redemption.
        idempotencyKey: { type: string }
        redeemedAt: { type: string, format: date-time }
        replayed:
          type: boolean
          description: True when the response replays a previously applied redemption.
    LedgerEntry:
      type: object
      properties:
        transactionId: { type: integer, format: int64 }
        type: { type: string, enum: [ISSUE, REDEMPTION] }
        amount: { type: number, format: double }
        balanceAfter: { type: number, format: double }
        idempotencyKey: { type: string, nullable: true }
        createdAt: { type: string, format: date-time }
    Disclosure:
      type: object
      properties:
        expiresAt: { type: string, format: date-time, nullable: true }
        feesAssessed: { type: boolean }
        feePolicy: { type: string }
        expiryPolicy: { type: string }
    CardStatus:
      type: string
      enum: [ACTIVE, DEPLETED, EXPIRED]
    Error:
      type: object
      properties:
        code: { type: string }
        message: { type: string }
        details:
          type: array
          items: { type: string }
        timestamp: { type: string, format: date-time }
```

## Example session

```bash
BASE=http://localhost:8080/api/v1/stored-value/cards
AUTH='-u partner:Partner@123'

# Issue
curl $AUTH -X POST $BASE -H 'Content-Type: application/json' \
  -d '{"amount":50.00,"currency":"USD","expiresAt":"2027-01-01T00:00:00Z"}'

TOKEN=<cardToken from the response>

# Balance
curl $AUTH $BASE/$TOKEN/balance

# Redeem 20.00 (retry-safe: same key => same result, money moves once)
curl $AUTH -X POST $BASE/$TOKEN/redeem \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: order-4711' \
  -d '{"amount":20.00}'

# Ledger
curl $AUTH $BASE/$TOKEN/transactions
```

## Persistence

Schema follows the repo's existing approach: JPA entities with `spring.jpa.hibernate.ddl-auto=update`
creating the tables, and a checked-in DDL script for environments that provision the schema up front —
`src/main/resources/static/mysql/stored_value_schema.sql`.

## Tests

Run with a MySQL reachable at `jdbc:mysql://localhost:3306/bankappdb` (see the repo README /
`docker-compose.yml`), then:

```bash
./mvnw clean test
```

- `StoredValueServiceTest` — unit tests for expiry, over-redemption, idempotent replay, key conflict,
  amount validation and token masking.
- `StoredValueApiIntegrationTest` — end-to-end HTTP tests over MySQL: issue → balance → partial and
  full redeem → ledger, idempotency, disclosure, error codes, auth.
- `StoredValueConcurrencyTest` — 20 threads redeeming 10.00 from a 100.00 card: exactly 10 succeed,
  balance lands on 0.00, ledger has 11 rows; and 20 concurrent replays of one key debit once.
