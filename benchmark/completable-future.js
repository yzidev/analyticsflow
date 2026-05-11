import { createOptions, runBenchmark } from './lib/benchmark.js';

export const options = createOptions('completable-future');

export default function () {
  runBenchmark('completable-future', 'COMPLETABLE_FUTURE', '/api/benchmark/reports/completable-future');
}
