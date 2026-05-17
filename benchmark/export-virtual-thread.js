import { createOptions, runExportBenchmark } from './lib/benchmark.js';

export const options = createOptions('virtual-thread');

export default function () {
  runExportBenchmark('virtual-thread', 'VIRTUAL_THREAD', '/api/benchmark/reports/exports/virtual-thread');
}
