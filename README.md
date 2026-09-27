# ATM Banking API — Learning Project

A REST API that simulates an ATM / banking backend, built with **Java 21**, **Spring Boot 4.1**
and **MySQL 8**, as a hands-on project for learning modern Java web development.

## Tech stack

| Layer        | Technology                                   |
|--------------|----------------------------------------------|
| Language     | Java 21 (LTS)                                |
| Framework    | Spring Boot 4.1 (Web MVC, Data JPA, Validation) |
| Database     | MySQL 8 via JDBC + Hibernate ORM             |
| Build tool   | Maven (with Maven Wrapper — no install needed) |
| Tests        | JUnit 5 + Mockito + AssertJ                  |

## Requirements

- JDK 21 (installed via winget: Eclipse Temurin 21)
- MySQL 8 running on `localhost:3306`

Database credentials live in `src/main/resources/application.properties`
(currently `root` / `pankaj1234`, database `atm_db` is auto-created on first start).

## Run it

```powershell
cd D:\ATM
.\mvnw.cmd spring-boot:run
```

The server starts on **http://localhost:8080** and now serves a **web UI** — open
`http://localhost:8080` in your browser for a working ATM screen (login, balance,
deposit, withdraw, transfer, statement). It also creates the schema and seeds three
demo accounts:

| Account | PIN  | Owner        | Balance |
|---------|------|--------------|---------|
| 1001    | 1234 | Alice Sharma | 5000.00 |
| 1002    | 5678 | Bob Verma    | 2500.00 |
| 1003    | 4321 | Carol Singh  | 1000.00 |

## API

| Method | Path                          | Auth | Purpose                                   |
|--------|-------------------------------|------|-------------------------------------------|
| POST   | `/api/auth/login`             | no   | Exchange account number + PIN for a token |
| POST   | `/api/auth/logout`            | yes  | Invalidate the token                      |
| POST   | `/api/accounts`               | no   | Open a new account                        |
| GET    | `/api/accounts/{id}`          | yes  | Account details                           |
| GET    | `/api/accounts/{id}/balance`  | yes  | Current balance                           |
| GET    | `/api/accounts/{id}/transactions` | yes | Statement, newest first               |
| POST   | `/api/accounts/{id}/deposit`  | yes  | Add money                                 |
| POST   | `/api/accounts/{id}/withdraw` | yes  | Take money out (PIN re-checked)           |
| POST   | `/api/transfers`              | yes  | Move money to another account             |

Authenticated calls need the header: `Authorization: Bearer <token>`.

### Try it (PowerShell)

```powershell
# 1. Login
$login = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method Post `
    -ContentType 'application/json' -Body '{"accountNumber":"1001","pin":"1234"}'
$token = $login.token
$auth = @{ Authorization = "Bearer $token" }

# 2. Balance
Invoke-RestMethod -Uri http://localhost:8080/api/accounts/1/balance -Headers $auth

# 3. Withdraw 200 (needs the PIN again)
Invoke-RestMethod -Uri http://localhost:8080/api/accounts/1/withdraw -Method Post `
    -Headers $auth -ContentType 'application/json' -Body '{"amount":200,"pin":"1234"}'

# 4. Transfer 50 to account 1002
Invoke-RestMethod -Uri http://localhost:8080/api/transfers -Method Post `
    -Headers $auth -ContentType 'application/json' `
    -Body '{"toAccountNumber":"1002","amount":50,"pin":"1234"}'

# 5. Statement
Invoke-RestMethod -Uri http://localhost:8080/api/accounts/1/transactions -Headers $auth
```

## Project structure

```
src/main/java/com/learning/atm/
├── AtmApplication.java        Spring Boot entry point
├── config/                    WebConfig (interceptor wiring), DataSeeder (demo data)
├── controller/                HTTP endpoints — parse requests, call services, return DTOs
├── dto/                       Records for requests/responses (never expose entities directly)
├── exception/                 Exception hierarchy + GlobalExceptionHandler (JSON errors)
├── model/                     JPA entities: Account, Transaction
├── repository/                Spring Data JPA interfaces (SQL lives here)
├── security/                  AuthInterceptor — token check before every guarded request
└── service/                   Business rules: PIN checks, balances, locks, transfers
```

**Request flow:**

```
HTTP request → AuthInterceptor (token check) → Controller (validation)
            → Service (@Transactional, business rules) → Repository (SQL) → MySQL
```

## Concepts this project teaches

1. **Layered architecture** — controllers never touch the database, services never parse HTTP.
2. **DTO records** — Java 21 records as immutable request/response shapes; secrets (PIN)
   never leak because entities aren't serialized.
3. **Bean Validation** — annotations (`@NotBlank`, `@DecimalMin`, `@Digits`) reject bad input
   before any business code runs.
4. **ACID transactions** — `@Transactional` makes balance change + ledger row atomic.
5. **Pessimistic locking** — `SELECT ... FOR UPDATE` prevents two concurrent withdrawals from
   double-spending (`AccountRepository.findByIdForUpdate`).
6. **Deadlock avoidance** — transfers lock both accounts in ascending id order.
7. **REST error handling** — one `@RestControllerAdvice` maps exceptions to consistent JSON.
8. **Interceptors** — cross-cutting auth without polluting every controller method.
9. **Money as `BigDecimal`** — never `double` for currency.
10. **Testing** — unit tests with Mockito prove the money logic without a database.

## Ideas for next steps

- [ ] Hash PINs with BCrypt (Spring Security Crypto)
- [ ] Replace the in-memory token map with JWT
- [ ] Daily withdrawal limits and overdraft rules
- [ ] Pagination for the transaction history
- [ ] Flyway migrations instead of `ddl-auto=update`
- [ ] Integration tests with `@SpringBootTest` + MockMvc
- [ ] Front-end or OpenAPI/Swagger documentation
"# ATMS-1.0" 
