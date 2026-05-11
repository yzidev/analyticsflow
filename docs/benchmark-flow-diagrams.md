# Benchmark Report Flow Diagrams

Dokumen ini menggambarkan flow empat metode benchmark report di `BenchmarkReportController`.

Versi diagram visual bisa dibuka langsung di [benchmark-flow-diagrams.svg](benchmark-flow-diagrams.svg).

![Benchmark Flow Diagrams](benchmark-flow-diagrams.svg)

Semua metode pada akhirnya membaca analytical summary tables yang sama:

```text
sales_daily_summary
sales_product_summary
sales_customer_summary
delivery_performance_summary
payment_method_summary
channel_sales_summary
```

## Shared Controller Flow

```mermaid
flowchart TD
    A["Client or k6 script"] --> B["GET /api/benchmark/reports/{strategy}"]
    B --> C{"Strategy path"}
    C -->|"/blocking"| D["BlockingReportService"]
    C -->|"/virtual-thread"| E["VirtualThreadReportService"]
    C -->|"/completable-future"| F["CompletableFutureReportService"]
    C -->|"/reactive"| G["ReactiveReportService"]
    D --> H["ReportBenchmarkResponse"]
    E --> H
    F --> H
    G --> H
```

## 1. Blocking Method

Blocking method menjalankan semua query analytical secara sinkron pada request thread yang sama.

```mermaid
flowchart TD
    A["GET /api/benchmark/reports/blocking"] --> B["BlockingReportService.benchmark"]
    B --> C["Capture startedAt"]
    C --> D["Query salesDailySummary"]
    D --> E["Query salesProductSummary"]
    E --> F["Query salesCustomerSummary"]
    F --> G["Query deliveryPerformanceSummary"]
    G --> H["Query paymentMethodSummary"]
    H --> I["Query channelSalesSummary"]
    I --> J["Merge ReportSlice results"]
    J --> K["Build response with strategy BLOCKING"]
    K --> L["Return ReportBenchmarkResponse"]
```

Core behavior:

- Uses `BlockingAnalyticsReportRepository`.
- Queries run one by one.
- Thread name comes from the current HTTP request thread.

## 2. Virtual Thread Method

Virtual thread method tetap memakai blocking repository, tetapi setiap query dijalankan sebagai task di virtual thread executor.

```mermaid
flowchart TD
    A["GET /api/benchmark/reports/virtual-thread"] --> B["VirtualThreadReportService.benchmark"]
    B --> C["Capture startedAt"]
    C --> D["Create 6 Callable query tasks"]
    D --> E["Submit tasks with virtualThreadExecutor.invokeAll"]
    E --> F1["Virtual thread: salesDailySummary"]
    E --> F2["Virtual thread: salesProductSummary"]
    E --> F3["Virtual thread: salesCustomerSummary"]
    E --> F4["Virtual thread: deliveryPerformanceSummary"]
    E --> F5["Virtual thread: paymentMethodSummary"]
    E --> F6["Virtual thread: channelSalesSummary"]
    F1 --> G["Collect Future results"]
    F2 --> G
    F3 --> G
    F4 --> G
    F5 --> G
    F6 --> G
    G --> H["Merge ReportSlice results"]
    H --> I["Build response with strategy VIRTUAL_THREAD"]
    I --> J["Include thread detail with virtual=true"]
    J --> K["Return ReportBenchmarkResponse"]
```

Core behavior:

- Uses `Executors.newVirtualThreadPerTaskExecutor()`.
- Global virtual thread mode is not enabled.
- Blocking DB calls run inside virtual threads.
- Executor is closed through Spring bean lifecycle.

## 3. CompletableFuture Method

CompletableFuture method menjalankan semua query analytical secara parallel memakai executor khusus.

```mermaid
flowchart TD
    A["GET /api/benchmark/reports/completable-future"] --> B["CompletableFutureReportService.benchmark"]
    B --> C["Capture startedAt"]
    C --> D["Create 6 CompletableFuture tasks with supplyAsync"]
    D --> E1["analyticsflow-cf thread: salesDailySummary"]
    D --> E2["analyticsflow-cf thread: salesProductSummary"]
    D --> E3["analyticsflow-cf thread: salesCustomerSummary"]
    D --> E4["analyticsflow-cf thread: deliveryPerformanceSummary"]
    D --> E5["analyticsflow-cf thread: paymentMethodSummary"]
    D --> E6["analyticsflow-cf thread: channelSalesSummary"]
    E1 --> F["CompletableFuture.allOf"]
    E2 --> F
    E3 --> F
    E4 --> F
    E5 --> F
    E6 --> F
    F --> G{"Completed within 30 seconds?"}
    G -->|"Yes"| H["Join futures and collect results"]
    G -->|"No or failed"| I["Cancel pending futures"]
    I --> J["Throw benchmark failure"]
    H --> K["Merge ReportSlice results"]
    K --> L["Build response with strategy COMPLETABLE_FUTURE"]
    L --> M["Return ReportBenchmarkResponse"]
```

Core behavior:

- Uses dedicated `analyticsCompletableFutureExecutor`.
- Thread prefix is `analyticsflow-cf-`.
- Combines tasks with `CompletableFuture.allOf`.
- Applies 30-second timeout.
- Cancels pending futures on failure.

## 4. Reactive WebFlux Method

Reactive method memakai R2DBC repository dan menggabungkan semua query dengan `Mono.zip`.

```mermaid
flowchart TD
    A["GET /api/benchmark/reports/reactive"] --> B["ReactiveReportService.benchmark"]
    B --> C["Capture startedAt"]
    C --> D["Create 6 Mono query pipelines"]
    D --> E1["R2DBC Mono: salesDailySummary"]
    D --> E2["R2DBC Mono: salesProductSummary"]
    D --> E3["R2DBC Mono: salesCustomerSummary"]
    D --> E4["R2DBC Mono: deliveryPerformanceSummary"]
    D --> E5["R2DBC Mono: paymentMethodSummary"]
    D --> E6["R2DBC Mono: channelSalesSummary"]
    E1 --> F["Mono.zip"]
    E2 --> F
    E3 --> F
    E4 --> F
    E5 --> F
    E6 --> F
    F --> G{"Completed within 30 seconds?"}
    G -->|"Yes"| H["Collect ReportQueryResult tuple"]
    G -->|"No or failed"| I["Map to reactive benchmark error"]
    H --> J["Merge ReportSlice results"]
    J --> K["Build response with strategy REACTIVE"]
    K --> L["Return Mono<ReportBenchmarkResponse>"]
```

Core behavior:

- Uses `ReactiveAnalyticsReportRepository`.
- Actual implementation uses R2DBC `DatabaseClient`.
- No JPA blocking call is wrapped here.
- If this endpoint is ever changed to JPA, the blocking work must run on a bounded elastic scheduler.

## k6 Script Flow

Semua k6 scripts memakai helper yang sama di `benchmark/lib/benchmark.js`.

```mermaid
flowchart TD
    A["k6 run benchmark/{strategy}.js"] --> B["Load createOptions"]
    B --> C["Read env: BASE_URL, VUS, DURATION, thresholds"]
    C --> D["Run virtual users"]
    D --> E["HTTP GET benchmark endpoint"]
    E --> F["Parse JSON response"]
    F --> G["Check status, strategy, processingTimeMs, databaseProfile"]
    G --> H["Record benchmark_success"]
    H --> I["Record app_processing_time_ms"]
    I --> J["Sleep"]
    J --> D
```
