# Running AnalyticsFlow

Dokumen ini berisi command harian untuk menjalankan AnalyticsFlow. Semua command utama tersedia lewat `Makefile` di root project.

## Prerequisites

Pastikan tersedia:

```text
Java 21
Docker dan Docker Compose
curl
make
k6 optional, hanya untuk benchmark
```

## Quick Start With Local Spring Boot

Start PostgreSQL:

```bash
make db-up
```

Run app:

```bash
make run
```

Check health:

```bash
make health
```

Default app URL:

```text
http://localhost:8080
```

Override app URL when needed:

```bash
make health APP_URL=http://localhost:9090
```

## Quick Start With Docker Compose

Run PostgreSQL and the app container:

```bash
make compose-up
```

Run PostgreSQL and the app container in the background:

```bash
make compose-up-detached
```

Stop services:

```bash
make compose-down
```

Follow logs:

```bash
make compose-logs
```

Docker Compose mounts local data into the app container:

```text
./data:/app/data
```

Real large files should be placed in:

```text
data/files
```

Small testing files stay in:

```text
data/sample
```

## Test And Build

Run tests:

```bash
make test
```

Build the jar:

```bash
make package
```

## Validate CSV Files

Validate the default source directory, `data/files`:

```bash
make validate
```

Validate testing sample data:

```bash
make validate-sample
```

Validate a custom directory:

```bash
make validate SAMPLE_DIR=data/sample
```

## Run ETL Import

Start ETL using default source directory and default chunk size:

```bash
make import
```

Use a smaller sample directory:

```bash
make import SAMPLE_DIR=data/sample
```

Override chunk size:

```bash
make import CHUNK_SIZE=10000
```

List jobs:

```bash
make jobs
```

## Generate Reports

Generate default CSV report:

```bash
make report
```

Default values:

```text
REPORT_TYPE=SALES_PRODUCT_SUMMARY
REPORT_FORMAT=CSV
```

Generate another report type:

```bash
make report REPORT_TYPE=SALES_DAILY_SUMMARY
```

Available report types:

```text
SALES_DAILY_SUMMARY
SALES_PRODUCT_SUMMARY
SALES_CUSTOMER_SUMMARY
DELIVERY_PERFORMANCE_SUMMARY
PAYMENT_METHOD_SUMMARY
CHANNEL_SALES_SUMMARY
```

List generated reports:

```bash
make reports
```

Download report:

```bash
make report-download REPORT_ID=<report-id>
```

## Benchmarks

Run one benchmark strategy:

```bash
make benchmark BENCHMARK=blocking
make benchmark BENCHMARK=virtual-thread
make benchmark BENCHMARK=completable-future
make benchmark BENCHMARK=reactive
```

Run all benchmark strategies:

```bash
make benchmark-all
```

Override load parameters:

```bash
make benchmark BENCHMARK=reactive VUS=200 DURATION=2m SLEEP_SECONDS=1
```

Benchmark thresholds can also be overridden:

```bash
make benchmark \
  BENCHMARK=reactive \
  P95_THRESHOLD='p(95)<1500' \
  P99_THRESHOLD='p(99)<3000' \
  ERROR_RATE_THRESHOLD='rate<0.005'
```

## Useful Maintenance

Clean generated report files from `data/reports`, while keeping `.gitkeep`:

```bash
make clean-reports
```

Stop PostgreSQL:

```bash
make db-down
```

Check migrated tables in Docker PostgreSQL:

```bash
make db-tables
```

Expected application schemas:

```text
analyticsflow_staging  -> stg_* raw CSV tables
analyticsflow_oltp     -> transactional tables
analyticsflow_olap     -> analytical/reporting tables
analyticsflow_support  -> ETL support tables and Spring Batch metadata
```

`public` should only keep Flyway metadata such as `flyway_schema_history`.

Show available commands:

```bash
make help
```
