import { createOptions, runBenchmark } from './lib/benchmark.js';

export const options = createOptions('virtual-thread');

export default function () {
  runBenchmark('virtual-thread', 'VIRTUAL_THREAD', '/api/benchmark/reports/virtual-thread');
}
