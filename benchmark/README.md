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
