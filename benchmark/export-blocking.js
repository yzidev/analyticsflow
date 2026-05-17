import { createOptions, runExportBenchmark } from './lib/benchmark.js';

export const options = createOptions('blocking');

export default function () {
  runExportBenchmark('blocking', 'BLOCKING', '/api/benchmark/reports/exports/blocking');
}
