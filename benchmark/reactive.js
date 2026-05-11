import { createOptions, runBenchmark } from './lib/benchmark.js';

export const options = createOptions('reactive');

export default function () {
  runBenchmark('reactive', 'REACTIVE', '/api/benchmark/reports/reactive');
}
