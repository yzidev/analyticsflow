# AnalyticsFlow k6 Benchmarks

These scripts benchmark the four report endpoints independently:

```bash
k6 run benchmark/blocking.js
k6 run benchmark/virtual-thread.js
k6 run benchmark/completable-future.js
k6 run benchmark/reactive.js
```

Runtime configuration can be overridden with environment variables:

```bash
BASE_URL=http://localhost:8080 \
VUS=100 \
DURATION=1m \
SLEEP_SECONDS=1 \
P95_THRESHOLD='p(95)<2000' \
P99_THRESHOLD='p(99)<5000' \
ERROR_RATE_THRESHOLD='rate<0.01' \
k6 run benchmark/reactive.js
```

The scripts record:

- k6 request throughput and latency
- k6 HTTP failure rate
- p95 and p99 request duration thresholds
- application-reported `processingTimeMs` as `app_processing_time_ms`
- endpoint correctness through status, strategy, processing time, and database profile checks

Track infrastructure metrics during each run with your preferred local tooling:

```bash
curl http://localhost:8080/actuator/metrics
jcmd <pid> Thread.print
jcmd <pid> GC.heap_info
docker stats
```

Run one strategy at a time against the same loaded analytical dataset, then compare request rate, average latency, p95, p99, error rate, CPU, memory, thread count, and GC activity.

## Saving Results

Use the Makefile targets to save k6 summary JSON files under `data/benchmark-results`:

```bash
make benchmark-save BENCHMARK=blocking
make benchmark-all-save
```

`benchmark-all-save` continues running the remaining strategies even when one strategy crosses a threshold, so every strategy still gets a result file.

To also sample Docker CPU and memory usage while each benchmark runs:

```bash
make benchmark-all-save-stats
```

For a more stable comparison, run the full suite several times and aggregate the latest N results per strategy:

```bash
make benchmark-repeat-save-stats BENCHMARK_RUNS=3
make benchmark-repeat-save-stats BENCHMARK_RUNS=5
```

This writes paired files like:

- `data/benchmark-results/blocking-20260517-031015.json`
- `data/benchmark-results/blocking-20260517-031015.stats.csv`

Print the latest comparison table:

```bash
make benchmark-compare
make benchmark-compare BENCHMARK_COMPARE_RUNS=3
```

Compare these fields in each JSON summary:

- `metrics.http_req_duration.avg`, `p(95)`, and `p(99)` for end-to-end latency.
- `metrics.app_processing_time_ms.avg`, `p(95)`, and `p(99)` for server-side processing time reported by the app.
- `metrics.http_reqs.rate` for throughput.
- `metrics.http_req_failed.value` and `metrics.benchmark_success.value` for correctness and reliability.
