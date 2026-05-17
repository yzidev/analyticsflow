import { createOptions, runExportBenchmark } from './lib/benchmark.js';

export const options = createOptions('reactive');

export default function () {
  runExportBenchmark('reactive', 'REACTIVE', '/api/benchmark/reports/exports/reactive');
}
