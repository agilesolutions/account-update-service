# CBACT04C → Java Migration

## What this is

A direct Java translation of the Amazon CardDemo COBOL Accept and process credit card detail request program `COCRDUPC.CBL `.

## Why I did it 
Migration is the best way to learn how to do code migration. By doing a direct line-by-line.
These are my design decisions and implementation details, which I hope will be helpful to anyone looking to do a similar migration.
- The goal was to produce a Java Spring Boot application that behaves identically to the original COBOL program, including the same business logic, data handling, and output.
- I focused on a direct translation rather than a redesign, to preserve the original program's structure
- This is not a production-ready application — it's a learning exercise to demonstrate the migration process and the mapping of COBOL concepts to Java. A real production migration would involve additional considerations like error handling, logging, configuration management, and more robust testing.
- The original COBOL program reads from indexed sequential files and writes to an output file. In the Java version, I used in-memory repositories to simulate this file I/O, which allows for easier testing and demonstration of the core logic without needing to set up actual file readers/writers or a database. A production migration would need to implement proper data access layers (e.g., JDBC, JPA) to read/write from the actual data sources.
- The Java version includes a REST API to trigger the batch process and check results, which is a common pattern for modernizing batch applications. The original COBOL program would typically be run as a scheduled job on the mainframe, but exposing it via REST allows for more flexible integration and testing.
- I implemented a "dry run" mode in the Java version that computes the interest without writing any transactions, which is useful for testing and validation. The original COBOL program does not have this feature, but it can be a valuable addition during migration to verify the logic before affecting any data.
- I used BigDecimal for all monetary calculations in Java to ensure precision, whereas COBOL's COMPUTE would handle this with its own numeric types. This is an important consideration when migrating financial applications to ensure that rounding and precision are handled correctly.
- I included comprehensive test cases in the Java version to validate the business logic against expected outcomes. The original COBOL program would typically be tested with batch test runs and comparing output files, but having automated unit tests in Java allows for faster feedback and regression testing during development.
- I mapped COBOL concepts like WORKING-STORAGE variables, FILE-CONTROL, and PROCEDURE DIVISION to Java fields, repository interfaces, and service methods respectively. This mapping is crucial for understanding how to translate the structure and flow of a COBOL program into Java.
- I maintained the same data model as the COBOL program, with entities representing accounts, transactions, and category balances. This helps ensure that the Java version can work with the same data structures and logic as the original.
- I included detailed comments in the Java code to explain how each part corresponds to the original COBOL program, which can be helpful for anyone familiar with COBOL who is learning how to read and write Java.
- Overall, the goal was to create a Java application that faithfully reproduces the behavior of the original COBOL program while also demonstrating best practices for structuring a modern Java application with Spring Boot. This includes using repositories for data access, services for business logic, and REST controllers for API exposure, which are common patterns in modern Java applications.
- This exercise is meant to be a learning tool for developers who are new to Java or Spring Boot, as well as those who are experienced in COBOL and want to understand how to approach migration. It is not intended to be a complete modernization solution, but rather a starting point for understanding the process and the mapping of concepts between the two languages.
- The original COBOL program's logic is preserved as much as possible, but some adjustments were made to fit Java's paradigms and best practices. For example, the use of repositories and services allows for better separation of concerns and easier testing compared to the more monolithic structure of a COBOL program.
- The Java version also includes additional features like the REST API and dry run mode, which are not present in the original COBOL program but can enhance the functionality and usability of the application in a modern context.
- The migration process involved careful analysis of the original COBOL code to understand its logic and data flow, followed by a systematic translation into Java while ensuring that the core functionality remains intact. This included mapping COBOL data structures to Java entities, translating procedural logic into service methods, and implementing the necessary data access layers to simulate the original file-based I/O.
- The testing strategy for the Java version included creating unit tests that cover various scenarios of the interest calculation logic, as well as integration tests to validate the overall behavior of the application. This is crucial for ensuring that the migration is successful and that the Java version produces the same results as the original COBOL program when given the same input data.
- In summary, this migration exercise demonstrates how to take a COBOL batch program and translate it into a modern Java Spring Boot application while preserving the original logic and behavior. It serves as a practical example of the migration process and the considerations involved in mapping COBOL concepts to Java.

[Please read my architectural design decisions I put up with the support of Caude Code](docus/prompt.md) 


**before diving into the code. It will give you important context on how I approached the migration and why I made certain design choices!!!...**



## What I did 
- Claude code direct migration CBACT04C COBOL module to java. [See original COBOL source on github](https://github.com/hpatel-appliedai/aws-mainframe-modernization-carddemo/blob/main/app/cbl/CBACT04C.cbl)
- Replaced maven with gradle
- Spring Boot and Cloud enabled a project in Intellij
- Added all generated java sources (model, repository, service and other components)
- Added junit test. 
- Ported indexed sequential files to PostgreSQL storage and JPA
- Enabled Webflux and swagger API endpoints
- Enable lombok, remove all generated getters and setters
- Enable postgresql test container tests
- Good — I have a complete picture of every column in every table. Now I'll write a comprehensive, realistic population script with a full set of accounts, cards, disclosure rates, category balances, and historical transactions — enough to meaningfully exercise the batch.

Ran 2 commandsCheck what seed data already existsScriptReview all entity fields to ensure full, realistic population of every columnScriptDoneGood — I have a complete picture of every column in every table. Now I'll write a comprehensive, realistic population script with a full set of accounts, cards, disclosure rates, category balances, and historical transactions — enough to meaningfully exercise the batch.Ran 2 commands, created a file, read a fileRan 2 commands, created a file, read a fileCreate dedicated seed directoryScriptComprehensive H2 seed - all 5 tables fully populated with realistic CardDemo datapopulate_local_h2.sqlCopy seed file to outputsScriptPresented fileDoneHere's what's in the script and why it's structured the way it is:
Insertion order matters — the script inserts in FK-dependency order: disclosure_group and account first (no dependencies), then card_xref (FK → account), then tran_cat_bal (FK → account), then transaction (FK → card_xref). Reversing any of these would violate a constraint.
20 accounts across 5 groups — PLATINUM, GOLD, SILVER, SECURED, BUSINESS — each with realistic credit limits, open/expiration dates, and cycle balances. Account 00000000020 uses group NEWPROG which has no disclosure group rows, deliberately triggering the DEFAULT fallback path in CBACT04C.

## tran_cat_bal covers all interesting scenarios:

- Multiple categories per account (interest accumulates across the run)
- Zero balances (no interest transaction written)
- Cash advance rows with a higher rate than purchases
- A promotional 0% APR category (PR/6) — rate is zero so no transaction is written
- A credit/refund row (CR/1) — rate is zero, no interest

Transaction table contains 28 historical interest postings from the March 2025 run — giving you existing data to query and compare against what April 2025's batch will produce. The inline comments show the expected interest amount so you can verify the April run's output against the formula (balance × APR) / 1200.

The 5 REST endpoints:
MethodPathPurposePOST/api/batch/interestFull or single-account batch runPOST/api/batch/interest/dry-runCompute only, zero writesGET/api/batch/statusHealth check — DB row counts + server timeGET/api/accounts/{acctId}Account balance + cycle totalsGET/api/accounts/{acctId}/transactionsInterest transactions for an account

Typical workflow via Swagger UI:

Call GET /api/batch/status to confirm the DB is up and accounts are loaded
Call POST /api/batch/interest/dry-run with parmDate: "2025-04-29" — see every interest line item and the formula output with zero side effects
Call POST /api/batch/interest to actually run it — balances update, transactions are written
Call GET /api/accounts/00000000001 to verify the balance changed
Call GET /api/accounts/00000000001/transactions to see the written interest records

Key design decisions: InterestCalculatorBatchService is @Transactional so the entire run is one atomic commit — no partial updates if an account lookup fails mid-run. dryRun: true skips all save() and txRepo.save() calls while still running the full calculation and returning the line items. The BatchRunResponse includes recordsRead, accountsProcessed, transactionsWritten, and totalInterestComputed for post-run audit, plus the full lineItems list showing exactly which APR was applied to each category.

## COBOL → Java mapping

| COBOL concept | Java equivalent |
|---|---|
| `WORKING-STORAGE` variables | Fields on `InterestCalculatorService` |
| `FILE-CONTROL` / `FD` | Repository interfaces (`TranCatBalRepository`, etc.) |
| `PERFORM` paragraphs | Private methods |
| `COMPUTE` arithmetic | `BigDecimal.multiply().divide(1200, HALF_UP)` |
| `REWRITE` account | `AccountRepository.save()` |
| `WRITE` transaction | `TransactionRepository.write()` |
| JCL `PARM=` | Constructor argument `parmDate` |
| Status `'23'` fallback | `findByKeyWithDefaultFallback()` tries DEFAULT group |

## Project structure

```
src/
  main/java/com/carddemo/
    model/          # Data records (maps to COBOL copybooks)
    repository/     # I/O interfaces (maps to FILE-CONTROL)
    service/        # InterestCalculatorService (maps to PROCEDURE DIVISION)
    Cbact04cMain.java
  test/java/com/carddemo/service/
    InterestCalculatorServiceTest.java   # 8 test cases
```

## Running the tests

```bash
mvn test
```

All 8 tests should pass. They cover:

1. Single account, single category — interest amount correct
2. Zero interest rate — no transaction written
3. Two accounts — independent interest accumulation (account-break logic)
4. Multiple categories for one account — totals accumulated and written
5. Disclosure group DEFAULT fallback
6. Empty input file — no output
7. Transaction ID format (`PARM-DATE` + 6-digit suffix)
8. Interest formula precision (BigDecimal HALF_UP rounding)

## Migration validation strategy

To prove the Java output matches COBOL:

1. Run `CBACT04C` on your mainframe with a test dataset and capture:
   - Output `TRANSACT` file records
   - Updated `ACCOUNT-FILE` balances
2. Feed the same test data into the Java in-memory repositories
3. Compare Java output against the captured COBOL output field-by-field

The repository interfaces make it easy to plug in file-based or JDBC implementations
that read the same data files the COBOL program uses.

## Known gaps / TO-DOs

- `1400-COMPUTE-FEES` is not implemented (stub in COBOL too)
- `9999-ABEND-PROGRAM` maps to `RuntimeException` — production code should use a proper batch error handler
- Real repository implementations (VSAM file readers or JDBC) need to be written
