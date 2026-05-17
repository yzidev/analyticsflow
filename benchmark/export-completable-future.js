import { createOptions, runExportBenchmark } from './lib/benchmark.js';

export const options = createOptions('completable-future');

export default function () {
  runExportBenchmark('completable-future', 'COMPLETABLE_FUTURE', '/api/benchmark/reports/exports/completable-future');
}
