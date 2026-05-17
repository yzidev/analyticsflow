#!/usr/bin/env bash
set -u

results_dir="${BENCHMARK_RESULTS_DIR:-data/benchmark-results}"
benchmark="${BENCHMARK:-blocking}"
benchmark_script="${BENCHMARK_SCRIPT:-${benchmark}}"
result_file="${BENCHMARK_RESULT_FILE:-${results_dir}/${benchmark}-$(date +%Y%m%d-%H%M%S).json}"
stats_file="${BENCHMARK_STATS_FILE:-${result_file%.json}.stats.csv}"
stats_interval="${STATS_INTERVAL_SECONDS:-2}"
containers="${BENCHMARK_STATS_CONTAINERS:-analyticsflow-app analyticsflow-postgres}"

mkdir -p "${results_dir}"
printf 'timestamp,container,cpu_percent,mem_percent,mem_usage\n' > "${stats_file}"

stats_pid=""
if command -v docker >/dev/null 2>&1; then
	(
		while true; do
			timestamp="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
			docker stats --no-stream --format "{{.Name}},{{.CPUPerc}},{{.MemPerc}},{{.MemUsage}}" ${containers} \
				2>/dev/null \
				| while IFS= read -r line; do
					printf '%s,%s\n' "${timestamp}" "${line}" >> "${stats_file}"
				done
			sleep "${stats_interval}"
		done
	) &
	stats_pid="$!"
fi

cleanup() {
	if [ -n "${stats_pid}" ]; then
		kill "${stats_pid}" 2>/dev/null || true
		wait "${stats_pid}" 2>/dev/null || true
	fi
}
trap cleanup EXIT INT TERM

BASE_URL="${APP_URL:-${BASE_URL:-http://localhost:8080}}" \
VUS="${VUS:-100}" \
DURATION="${DURATION:-1m}" \
SLEEP_SECONDS="${SLEEP_SECONDS:-1}" \
P95_THRESHOLD="${P95_THRESHOLD:-p(95)<2000}" \
P99_THRESHOLD="${P99_THRESHOLD:-p(99)<5000}" \
ERROR_RATE_THRESHOLD="${ERROR_RATE_THRESHOLD:-rate<0.01}" \
k6 run --summary-export "${result_file}" "benchmark/${benchmark_script}.js"
status="$?"

cleanup
printf '%s\n' "Saved benchmark summary to ${result_file}"
printf '%s\n' "Saved resource samples to ${stats_file}"
exit "${status}"
