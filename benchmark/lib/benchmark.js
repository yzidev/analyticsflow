import { check, sleep } from 'k6';
import http from 'k6/http';
import { Rate, Trend } from 'k6/metrics';

export const appProcessingTime = new Trend('app_processing_time_ms');
export const benchmarkSuccess = new Rate('benchmark_success');

const DEFAULT_BASE_URL = 'http://localhost:8080';
const DEFAULT_VUS = 100;
const DEFAULT_DURATION = '1m';
const DEFAULT_SLEEP_SECONDS = 1;

export function createOptions(strategy) {
  const p95Threshold = __ENV.P95_THRESHOLD || 'p(95)<2000';
  const p99Threshold = __ENV.P99_THRESHOLD || 'p(99)<5000';
  const errorRateThreshold = __ENV.ERROR_RATE_THRESHOLD || 'rate<0.01';

  return {
    vus: numberEnv('VUS', DEFAULT_VUS),
    duration: __ENV.DURATION || DEFAULT_DURATION,
    thresholds: {
      http_req_failed: [errorRateThreshold],
      http_req_duration: [p95Threshold, p99Threshold],
      benchmark_success: ['rate>0.99'],
      app_processing_time_ms: [p95Threshold, p99Threshold],
    },
    tags: {
      strategy,
    },
  };
}

export function runBenchmark(strategy, expectedStrategy, path) {
  const url = `${__ENV.BASE_URL || DEFAULT_BASE_URL}${path}`;
  const response = http.get(url, {
    tags: {
      strategy,
      endpoint: path,
    },
  });

  const payload = json(response);
  const success = check(response, {
    'status is 200': (res) => res.status === 200,
    'strategy matches': () => payload && payload.strategy === expectedStrategy,
    'processing time exists': () => payload && Number.isFinite(payload.processingTimeMs),
    'database profile exists': () => payload && Boolean(payload.databaseProfile),
  });

  benchmarkSuccess.add(success);
  if (payload && Number.isFinite(payload.processingTimeMs)) {
    appProcessingTime.add(payload.processingTimeMs, { strategy });
  }

  sleep(numberEnv('SLEEP_SECONDS', DEFAULT_SLEEP_SECONDS));
}

function json(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function numberEnv(name, fallback) {
  const value = Number(__ENV[name]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}
