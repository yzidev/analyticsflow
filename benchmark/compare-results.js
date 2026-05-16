const fs = require('fs');
const path = require('path');

const resultsDir = process.env.BENCHMARK_RESULTS_DIR || 'data/benchmark-results';
const compareRuns = positiveInteger(process.env.BENCHMARK_COMPARE_RUNS, 1);
const strategies = ['blocking', 'virtual-thread', 'completable-future', 'reactive'];

if (!fs.existsSync(resultsDir)) {
  console.error(`Benchmark results directory not found: ${resultsDir}`);
  process.exit(1);
}

const files = fs.readdirSync(resultsDir)
  .filter((file) => file.endsWith('.json'))
  .map((file) => {
    const fullPath = path.join(resultsDir, file);
    return { file, fullPath, mtimeMs: fs.statSync(fullPath).mtimeMs };
  });

const rows = strategies.map((strategy) => {
  const results = latestResults(strategy, compareRuns);
  if (results.length === 0) {
    return emptyRow(strategy);
  }

  const samples = results.map(sampleFromResult);
  const aggregate = aggregateSamples(samples);

  return {
    Strategy: strategy,
    Runs: String(samples.length),
    Success: percent(aggregate.successAvg),
    Errors: percent(aggregate.errorMax),
    RPS: fixed(aggregate.rpsMedian, 2),
    'HTTP p95': ms(aggregate.httpP95Median),
    'HTTP p99': ms(aggregate.httpP99Median),
    'App p95': ms(aggregate.appP95Median),
    'App p99': ms(aggregate.appP99Median),
    'CPU avg': percentNumber(aggregate.cpuAvg),
    'CPU max': percentNumber(aggregate.cpuMax),
    'Mem avg': percentNumber(aggregate.memAvg),
    'Mem max': percentNumber(aggregate.memMax),
    File: results[0].file,
    score: {
      success: aggregate.successAvg ?? -1,
      errorRate: aggregate.errorMax ?? 1,
      httpP95: aggregate.httpP95Median ?? Infinity,
      rps: aggregate.rpsMedian ?? 0,
    },
  };
});

console.log(`Comparing latest ${compareRuns} run(s) per strategy from ${resultsDir}`);
console.log('Latency and RPS use median. Success uses average. Errors use max.');
console.log('');
printTable(rows.map(({ score, ...row }) => row));

const comparable = rows.filter((row) => row.score);
if (comparable.length > 0) {
  comparable.sort((left, right) => (
    right.score.success - left.score.success
    || left.score.errorRate - right.score.errorRate
    || left.score.httpP95 - right.score.httpP95
    || right.score.rps - left.score.rps
  ));
  const best = comparable[0];
  console.log('');
  console.log(`Best overall by reliability, median p95 latency, then median RPS: ${best.Strategy}`);
}

function emptyRow(strategy) {
  return {
    Strategy: strategy,
    Runs: '0',
    Success: '-',
    Errors: '-',
    RPS: '-',
    'HTTP p95': '-',
    'HTTP p99': '-',
    'App p95': '-',
    'App p99': '-',
    'CPU avg': '-',
    'CPU max': '-',
    'Mem avg': '-',
    'Mem max': '-',
    File: '-',
    score: null,
  };
}

function latestResults(strategy, limit) {
  return files
    .filter(({ file }) => file.startsWith(`${strategy}-`))
    .sort((left, right) => right.mtimeMs - left.mtimeMs)
    .slice(0, limit);
}

function sampleFromResult(result) {
  const summary = JSON.parse(fs.readFileSync(result.fullPath, 'utf8'));
  const metrics = summary.metrics || {};
  const stats = readStats(result.fullPath.replace(/\.json$/, '.stats.csv'));

  return {
    file: result.file,
    success: number(metric(metrics, 'benchmark_success', 'value')),
    errorRate: number(metric(metrics, 'http_req_failed', 'value')),
    httpP95: number(metric(metrics, 'http_req_duration', 'p(95)')),
    httpP99: number(metric(metrics, 'http_req_duration', 'p(99)')),
    appP95: number(metric(metrics, 'app_processing_time_ms', 'p(95)')),
    appP99: number(metric(metrics, 'app_processing_time_ms', 'p(99)')),
    rps: number(metric(metrics, 'http_reqs', 'rate')),
    cpuAvg: stats.cpuAvg,
    cpuMax: stats.cpuMax,
    memAvg: stats.memAvg,
    memMax: stats.memMax,
  };
}

function aggregateSamples(samples) {
  return {
    successAvg: average(numbers(samples.map((sample) => sample.success))),
    errorMax: max(numbers(samples.map((sample) => sample.errorRate))),
    rpsMedian: median(numbers(samples.map((sample) => sample.rps))),
    httpP95Median: median(numbers(samples.map((sample) => sample.httpP95))),
    httpP99Median: median(numbers(samples.map((sample) => sample.httpP99))),
    appP95Median: median(numbers(samples.map((sample) => sample.appP95))),
    appP99Median: median(numbers(samples.map((sample) => sample.appP99))),
    cpuAvg: average(numbers(samples.map((sample) => sample.cpuAvg))),
    cpuMax: max(numbers(samples.map((sample) => sample.cpuMax))),
    memAvg: average(numbers(samples.map((sample) => sample.memAvg))),
    memMax: max(numbers(samples.map((sample) => sample.memMax))),
  };
}

function metric(metrics, name, field) {
  return metrics[name] ? metrics[name][field] : undefined;
}

function readStats(statsPath) {
  if (!fs.existsSync(statsPath)) {
    return {};
  }

  const byTimestamp = new Map();
  const lines = fs.readFileSync(statsPath, 'utf8').trim().split(/\r?\n/).slice(1);
  for (const line of lines) {
    if (!line.trim()) {
      continue;
    }
    const [timestamp, container, cpuPercent, memPercent] = line.split(',');
    if (!timestamp || !container) {
      continue;
    }
    const sample = byTimestamp.get(timestamp) || { cpu: 0, mem: 0 };
    sample.cpu += parsePercent(cpuPercent);
    sample.mem += parsePercent(memPercent);
    byTimestamp.set(timestamp, sample);
  }

  const samples = [...byTimestamp.values()];
  if (samples.length === 0) {
    return {};
  }

  return {
    cpuAvg: average(samples.map((sample) => sample.cpu)),
    cpuMax: Math.max(...samples.map((sample) => sample.cpu)),
    memAvg: average(samples.map((sample) => sample.mem)),
    memMax: Math.max(...samples.map((sample) => sample.mem)),
  };
}

function parsePercent(value) {
  const parsed = Number(String(value || '').replace('%', '').trim());
  return Number.isFinite(parsed) ? parsed : 0;
}

function numbers(values) {
  return values.filter((value) => Number.isFinite(value));
}

function average(values) {
  if (values.length === 0) {
    return null;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function median(values) {
  if (values.length === 0) {
    return null;
  }
  const sorted = [...values].sort((left, right) => left - right);
  const middle = Math.floor(sorted.length / 2);
  if (sorted.length % 2 === 1) {
    return sorted[middle];
  }
  return (sorted[middle - 1] + sorted[middle]) / 2;
}

function max(values) {
  if (values.length === 0) {
    return null;
  }
  return Math.max(...values);
}

function number(value) {
  return Number.isFinite(Number(value)) ? Number(value) : null;
}

function positiveInteger(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function fixed(value, digits) {
  return value == null ? '-' : value.toFixed(digits);
}

function ms(value) {
  return value == null ? '-' : `${value.toFixed(0)}ms`;
}

function percent(value) {
  return value == null ? '-' : `${(value * 100).toFixed(2)}%`;
}

function percentNumber(value) {
  return value == null ? '-' : `${value.toFixed(2)}%`;
}

function printTable(tableRows) {
  const headers = Object.keys(tableRows[0]);
  const widths = headers.map((header) => Math.max(
    header.length,
    ...tableRows.map((row) => String(row[header]).length),
  ));

  const line = widths.map((width) => '-'.repeat(width + 2)).join('+');
  console.log(formatRow(headers, widths));
  console.log(line);
  for (const row of tableRows) {
    console.log(formatRow(headers.map((header) => row[header]), widths));
  }
}

function formatRow(values, widths) {
  return values.map((value, index) => ` ${String(value).padEnd(widths[index])} `).join('|');
}
