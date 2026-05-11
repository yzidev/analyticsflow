import { createOptions, runBenchmark } from './lib/benchmark.js';

export const options = createOptions('blocking');

export default function () {
  runBenchmark('blocking', 'BLOCKING', '/api/benchmark/reports/blocking');
}
