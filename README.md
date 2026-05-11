# AnalyticsFlow

AnalyticsFlow is a Spring Boot multi-CSV ETL and reporting performance lab. It validates a relational CSV dataset, streams large files into staging tables, migrates clean rows into normalized transactional tables, transforms transactional data into analytical summary tables, generates downloadable reports, and benchmarks report generation through blocking, virtual-thread, CompletableFuture, and reactive R2DBC strategies.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC and WebFlux
- Spring Data JPA and Spring Data R2DBC
- PostgreSQL JDBC and R2DBC drivers
- Spring Boot Actuator
- Flyway
- Micrometer
- Docker Compose
- k6 benchmark scripts

## Database Configuration

The app defaults to real PostgreSQL mode:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/analyticsflow
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/analyticsflow
analyticsflow:
  db:
    blocking:
      url: jdbc:postgresql://localhost:5432/analyticsflow
    reactive:
      url: r2dbc:postgresql://localhost:5432/analyticsflow
```

Blocking and reactive DB configs are split under `analyticsflow.db.blocking` and `analyticsflow.db.reactive`. The URLs are local placeholders; replace them with the real PostgreSQL JDBC/R2DBC URLs when the database is ready.

## Architecture

```text
CSV files
  -> File validation
  -> Spring Batch chunk readers
  -> stg_* staging tables
  -> SQL validation and upsert migration
  -> transactional tables
  -> analytical summary tables
  -> CSV/XLSX report metadata and files
  -> benchmark APIs
```

The app keeps blocking/JPA and reactive/R2DBC paths separate:

- Batch imports, transactional migration, transformations, and report file generation use JDBC/JPA style blocking access.
- Reactive benchmark reporting uses R2DBC `DatabaseClient` for non-blocking PostgreSQL access.
- Virtual-thread and CompletableFuture benchmarks intentionally run blocking analytical queries with different concurrency strategies.

## Required CSV Files

Place the real large dataset under `data/files`. Keep `data/sample` only for small test CSVs:

- `users.csv`
- `product_categories.csv`
- `products.csv`
- `product_details.csv`
- `orders.csv`
- `order_items.csv`
- `transactions.csv`
- `deliveries.csv`

The validator checks file presence, `.csv` extension, readability, exact headers, and rejects directories outside `data/files` or `data/sample`.

## Database Tables

Staging tables:

```text
stg_users
stg_product_categories
stg_products
stg_product_details
stg_orders
stg_order_items
stg_transactions
stg_deliveries
```

Transactional tables:

```text
users
product_categories
products
product_details
orders
order_items
transactions
deliveries
```

Analytical tables:

```text
sales_daily_summary
sales_product_summary
sales_customer_summary
delivery_performance_summary
payment_method_summary
channel_sales_summary
```

Support tables:

```text
invalid_records
etl_job
etl_job_step
report_metadata
```

## Batch Flow

The job name is `analyticsflow-etl-job`.

```text
VALIDATE_REQUIRED_FILES
IMPORT_USERS_TO_STAGING
IMPORT_PRODUCT_CATEGORIES_TO_STAGING
IMPORT_PRODUCTS_TO_STAGING
IMPORT_PRODUCT_DETAILS_TO_STAGING
IMPORT_ORDERS_TO_STAGING
IMPORT_ORDER_ITEMS_TO_STAGING
IMPORT_TRANSACTIONS_TO_STAGING
IMPORT_DELIVERIES_TO_STAGING
MIGRATE_MASTER_DATA
MIGRATE_ORDER_DATA
MIGRATE_TRANSACTION_AND_DELIVERY_DATA
TRANSFORM_TO_ANALYTICAL_TABLES
GENERATE_INITIAL_REPORT
```

The staging import steps stream rows from disk and process chunks. Malformed rows are recorded in `invalid_records`; transient database errors are retried; parser-level malformed rows can be skipped according to the batch skip policy.

## Benchmark Flow Diagrams

Flow diagram visual untuk masing-masing metode benchmark ada di [docs/benchmark-flow-diagrams.svg](docs/benchmark-flow-diagrams.svg). Versi Markdown detail ada di [docs/benchmark-flow-diagrams.md](docs/benchmark-flow-diagrams.md).

## Reporting Flow Diagrams

Flow diagram visual untuk proses reporting, initial reports, download, dan masing-masing report type ada di [docs/reporting-flow-diagrams.svg](docs/reporting-flow-diagrams.svg). Versi Markdown detail ada di [docs/reporting-flow-diagrams.md](docs/reporting-flow-diagrams.md).

## API Summary

```http
GET  /api/files/sample
GET  /api/files/required
POST /api/files/validate

POST /api/jobs/import
GET  /api/jobs
GET  /api/jobs/{jobId}
GET  /api/jobs/{jobId}/steps
GET  /api/jobs/{jobId}/errors

POST /api/reports/generate
GET  /api/reports
GET  /api/reports/{reportId}
GET  /api/reports/{reportId}/download

GET /api/benchmark/reports/blocking
GET /api/benchmark/reports/virtual-thread
GET /api/benchmark/reports/completable-future
GET /api/benchmark/reports/reactive

GET /api/config/database
GET /actuator/health
GET /actuator/metrics
```

## Run Locally

Recommended command reference is available in [docs/running.md](docs/running.md). The project also includes a root `Makefile`, so common operations can be run with `make`.

Show all available commands:

```bash
make help
```

Start PostgreSQL:

```bash
make db-up
```

Run the application locally:

```bash
make run
```

Or run PostgreSQL and the app through Docker Compose:

```bash
make compose-up
```

The compose app mounts `./data:/app/data`, so real large files stay in `data/files` and generated reports stay in `data/reports`.

Validate the default real files directory:

```bash
make validate
```

Validate the small testing sample:

```bash
make validate-sample
```

Start the Spring Batch ETL pipeline:

```bash
make import
```

Generate a CSV report:

```bash
make report REPORT_TYPE=SALES_PRODUCT_SUMMARY
```

Run a benchmark:

```bash
make benchmark BENCHMARK=blocking
make benchmark BENCHMARK=virtual-thread
make benchmark BENCHMARK=completable-future
make benchmark BENCHMARK=reactive
```

Override runtime load:

```bash
make benchmark BENCHMARK=reactive VUS=100 DURATION=1m
```

## Observability

Actuator endpoints:

```http
GET /actuator/health
GET /actuator/metrics
```

Custom metrics:

```text
analyticsflow.etl.jobs.total
analyticsflow.etl.jobs.completed
analyticsflow.etl.jobs.failed
analyticsflow.etl.rows.processed
analyticsflow.etl.steps.total
analyticsflow.etl.step.rows.read
analyticsflow.etl.step.rows.written
analyticsflow.etl.step.rows.skipped
analyticsflow.etl.job.duration
analyticsflow.etl.step.duration
analyticsflow.reports.generated
analyticsflow.report.generation.duration
```

## Reliability

- File validation runs before a job is launched.
- Validation failures are persisted to `invalid_records`.
- Staging imports use chunk processing and retry transient database errors.
- Parser-level malformed rows can be skipped without stopping the whole import.
- Staging-to-transactional migration validates required fields, enum/status values, foreign keys, duplicate IDs, and type casts.
- Transactional and analytical writes use upsert-style SQL for reruns.
- Job and step failures are persisted in `etl_job` and `etl_job_step`.

## Testing

Current automated coverage includes:

- Spring context bootstrapping
- CSV parser behavior
- staging processor invalid-row handling
- custom metrics recording
- CompletableFuture benchmark success and failure paths
- reactive benchmark success and failure paths

Run tests:

```bash
mvn test
```

## Phase Status

Phase 1 is now initialized with the Spring Boot application, Java 21, PostgreSQL/R2DBC configuration, root data folders, Docker Compose, `.gitignore`, and README.
Phase 2 is now wired with JPA entities, JPA repositories, and Flyway DDL for staging, transactional, analytical, and support tables.
Phase 3 is now wired with required multi-CSV dataset validation for file presence, extensions, readability, exact headers, and safe directory boundaries.
Phase 4 is now wired with File API endpoints for listing sample files, listing required files, and validating a requested dataset directory.
Phase 5 is now wired with the `analyticsflow-etl-job` Spring Batch job, 14 orchestration steps, job parameters, and job/step progress listeners backed by JPA tracking tables.
Phase 6 is now wired for master-data staging imports. The `users.csv`, `product_categories.csv`, `products.csv`, and `product_details.csv` steps stream rows from disk, process them per chunk, write staging rows through JPA repositories, and record malformed rows in `invalid_records`.
Phase 7 is now wired for transaction-data staging imports. The `orders.csv`, `order_items.csv`, `transactions.csv`, and `deliveries.csv` steps use the same streaming/chunk path and persist raw rows into their staging tables.
Phase 8 is now wired for staging-to-transactional migration. The migration steps validate required business fields, normalize enum/status values, convert string fields into PostgreSQL numeric/date/timestamp types, validate foreign keys, handle duplicate staging IDs, upsert into transactional tables, and record rejected rows in `invalid_records`.
Phase 9 is now wired for transactional-to-analytical transformations. The transform step aggregates normalized transactional data into all six analytical summary tables using PostgreSQL `insert-select` queries with `ON CONFLICT` updates.
Phase 10 is now wired for initial CSV report generation. The final batch step generates one CSV file for each report type from analytical tables, stores files under `data/reports`, persists metadata in `report_metadata`, and exposes the same metadata/download API.
Phase 11 is now tightened for job monitoring. Job detail responses combine persisted job metadata with current step progress, validation failures are recorded in `invalid_records`, and the monitoring endpoints expose job status, step durations, row counts, progress percentage, and failed records.
Phase 12 is now tightened for the Report API. Manual report generation reads analytical tables, supports date filters for date-grained summaries, validates date ranges, persists report metadata, lists stored report metadata, and downloads report files with the correct content type.
Phase 13 is now tightened for benchmark report APIs. The blocking, virtual-thread, CompletableFuture, and reactive endpoints return the same response structure, query the same analytical summary tables, measure processing time, and expose strategy-specific thread names.
Phase 14 is now tightened for the dedicated virtual-thread report strategy. The virtual-thread endpoint runs blocking analytical queries through `Executors.newVirtualThreadPerTaskExecutor()`, does not enable global virtual threads, returns worker thread details with `virtual=true`, and closes the executor through the Spring bean lifecycle.
Phase 15 is now tightened for the CompletableFuture strategy. The endpoint uses a dedicated `analyticsflow-cf-` executor, runs all analytical summary queries in parallel through `CompletableFuture.supplyAsync`, combines with `CompletableFuture.allOf`, applies a 30-second timeout, cancels pending futures on failure, and returns the shared benchmark response shape.
Phase 16 is now tightened for the reactive WebFlux strategy. The reactive endpoint returns `Mono<ReportBenchmarkResponse>`, combines all analytical summary queries with `Mono.zip`, uses the R2DBC `DatabaseClient` repository for true non-blocking PostgreSQL access, applies a 30-second timeout, and maps reactive failures into clear benchmark errors. If this endpoint is ever backed by JPA instead of R2DBC, blocking calls must be wrapped on a bounded elastic scheduler.
Phase 17 is now wired for k6 benchmarks. The `benchmark/` folder contains one script per strategy plus a shared helper for configurable base URL, virtual users, duration, sleep interval, thresholds, response checks, benchmark success rate, and application-reported processing-time metrics.
Phase 18 is now wired for observability. Actuator health/metrics are exposed and custom Micrometer metrics track jobs, completed jobs, failed jobs, processed rows, step rows, generated reports, and report/job/step durations.
Phase 19 is now hardened for reliability. The import path validates before launch, records invalid rows, tracks failed jobs/steps, retries transient database errors, skips parser-level malformed rows, and uses idempotent upsert-style migration/transformation SQL.
Phase 20 is now covered with focused automated tests for parser behavior, staging processor invalid-row handling, custom metrics, CompletableFuture benchmarks, reactive benchmarks, and Spring context startup.
Phase 21 is now wired for Docker local development. The repo has a Dockerfile, PostgreSQL and app services in Docker Compose, health checks, and a `./data:/app/data` mount.
Phase 22 is now documented in this README with overview, architecture, stack, folder/data expectations, database tables, batch flow, APIs, local run commands, validation/import/report/benchmark usage, observability, limitations, and future work.

## Known Limitations

- XLSX generation currently returns a placeholder workbook payload; CSV is the primary implemented output.
- Runtime smoke tests against your real PostgreSQL data are intentionally left for your local environment.
- k6 scripts require `k6` to be installed on the machine running benchmarks.
- Prometheus and Grafana are not wired yet; Actuator/Micrometer metrics are ready for that next step.

## Future Improvements

- Add true XLSX workbook generation.
- Add Testcontainers-backed PostgreSQL integration tests.
- Add Prometheus scrape endpoint and Grafana dashboard compose profile.
- Add richer performance reports that combine k6 output with JVM CPU, memory, thread, and GC snapshots.
