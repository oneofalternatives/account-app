# Account App

## Description

A fictional microservice made in scope of a home assignment.

### Features:

- List accounts by client ID
- List transactions by account ID
- Transfer funds between two accounts
    - If accounts have different currencies, use 3rd party currency conversion service

### Notes

There is a [to-do section](#TODO) where several possible improvements and questions are listed.

#### Assumptions and Simplifications

Some leftovers that should have been done, but had no more time to do that:

- Implemented account creation endpoint at the last moment, so that part (especially
  [AccountService.java](src%2Fmain%2Fjava%2Fcom%2Foneofalternatives%2Faccountapp%2Fservice%2FAccountService.java))
  is not covered with tests, as well as the endpoint doesn't have informative validation
- Only in-memory H2 database is available — all data is lost after app shutdown
- In `/transaction/history` validate if account with the given ID exists
- Currency format is `java.util.Currency`, which does not include some currencies from exchangerate.host

Other notes are given in the next chapters

## Setup and Run

### Prerequisites

Project requires JDK 21 to build and run.

Tested on:

- OS: Windows 10 Pro, version 22H2, build 19045.3930
- JDK: `openjdk 21.0.1 2023-10-17 (build 21.0.1+12-29)`

Gradle Wrapper is included with the project.

### Configuration

Project recognizes these Spring profiles:

- `fakedata` - loads fake accounts and transactions (hardcoded
  in [fake-accounts.csv](src%2Fmain%2Fresources%2Fdb%2Fchangelog%2Ffake-accounts.csv)
  and [fake-transactions.csv](src%2Fmain%2Fresources%2Fdb%2Fchangelog%2Ffake-transactions.csv))
  into the database (for manual testing)
- `mockexchange` - enables currency exchange client mock (to avoid "spending" monthly limit), uses fake data hardcoded
  in [application-mockexchange.yml](src%2Fmain%2Fresources%2Fapplication-mockexchange.yml).\
  Otherwise, real integration with http://api.exchangerate.host will be used.

To configure profiles, edit `spring.profiles.active` property
in external [application.properties](config%2Fapplication.properties).

_Important note_: for some reason, despite having empty property value, Liquibase still executes the scripts.
So if you don't want to leave any profiles, comment-out the property.

#### Important:

If using a real integration (i.e. _without_ `mockexchange` profile), add exchangerate.host access key
to `account-app.currency-converter.service.exchangeratehost.access-key` property
in [application.properties](config%2Fapplication.properties).

### Build and Run

From project root directory:

1. Build using `./gradlew clean build`
2. Run using `./gradlew bootRun`
3. Open http://localhost:8080/swagger-ui/index.htm in browser to observe and try the API of the microservice.

# TODO

- Cover all uncovered Services, Controllers and Repositories with Unit tests
    - Cover Pageable mapping
      in [TransactionServiceTest.java](src%2Ftest%2Fjava%2Fcom%2Foneofalternatives%2Faccountapp%2Fservice%2FTransactionServiceTest.java)
- Add validation to PUT `/account`
- Add validation of account existence in GET `/transaction/history`
    - Probably add "@OneToMany" to Account, then just load it and get its transactions
- Change currency type to `String` because Java's currency doesn't support at least Bitcoin,
  although exchangerate.host does.
- Are projections (DTO-s) needed for transaction returned by POST `/transaction/fund-transfer` endpoint,
  for accounts returned by GET `/account`?
- Review error messages (e.g. "Conversion from/to any of these currencies is not supported: \[GBP\]" could specify which
  account has the unsupported currency)
- Add validation for offset and limit pagination parameters
- Change type of IDs from Integer to UUID
  (no need to worry about existing IDs in tests, are more suitable for microservices)
- Hide dev/test endpoints not requested in requirements (via Spring profile?)
- How to implement/improve 3rd party resilience?
- Use Postgres in container instead of H2
- Switch from H2 to Postgres via TestContainers
- Re-implement integration tests in Cucumber
