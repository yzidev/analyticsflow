APP_URL ?= http://localhost:8080
CHUNK_SIZE ?= 5000
WRITER_STRATEGY ?= jpa
SAMPLE_DIR ?=
REPORT_TYPE ?= SALES_PRODUCT_SUMMARY
REPORT_FORMAT ?= CSV
REPORT_ID ?=
JOB_ID ?=
BENCHMARK ?= blocking
BENCHMARK_SCRIPT ?= $(BENCHMARK)
VUS ?= 100
DURATION ?= 1m
SLEEP_SECONDS ?= 1
P95_THRESHOLD ?= p(95)<2000
P99_THRESHOLD ?= p(99)<5000
ERROR_RATE_THRESHOLD ?= rate<0.01
BENCHMARK_RESULTS_DIR ?= data/benchmark-results
BENCHMARK_RESULT_FILE ?= $(BENCHMARK_RESULTS_DIR)/$(BENCHMARK)-$(shell date +%Y%m%d-%H%M%S).json
BENCHMARK_STATS_FILE ?= $(BENCHMARK_RESULT_FILE:.json=.stats.csv)
STATS_INTERVAL_SECONDS ?= 2
BENCHMARK_RUNS ?= 3
BENCHMARK_COMPARE_RUNS ?= 1

COMMA := ,
SAMPLE_DIR_JSON := $(if $(strip $(SAMPLE_DIR)),"sampleDirectory":"$(SAMPLE_DIR)")
IMPORT_SAMPLE_JSON := $(if $(strip $(SAMPLE_DIR)),"sampleDirectory":"$(SAMPLE_DIR)",)
IMPORT_COMMA := $(if $(strip $(SAMPLE_DIR)),$(COMMA),)

.PHONY: help test package run db-up db-down db-logs db-tables compose-up compose-up-detached compose-down compose-logs health metrics validate validate-sample import import-jpa import-jdbc import-copy import-resume jobs reports report report-download benchmark benchmark-save benchmark-save-stats benchmark-all benchmark-all-save benchmark-all-save-stats benchmark-repeat-save-stats benchmark-export-repeat-save-stats benchmark-compare clean-reports

help:
	@printf '%s\n' 'AnalyticsFlow commands'
	@printf '%s\n' ''
	@printf '%s\n' 'Local development:'
	@printf '%s\n' '  make db-up                  Start PostgreSQL only'
	@printf '%s\n' '  make db-tables              List tables in Docker PostgreSQL'
	@printf '%s\n' '  make run                    Run Spring Boot app with Maven'
	@printf '%s\n' '  make compose-up             Build and run app + PostgreSQL'
	@printf '%s\n' '  make compose-up-detached    Build and run app + PostgreSQL in background'
	@printf '%s\n' '  make compose-down           Stop Docker Compose services'
	@printf '%s\n' '  make test                   Run Maven tests'
	@printf '%s\n' '  make package                Build jar with tests'
	@printf '%s\n' ''
	@printf '%s\n' 'App operations:'
	@printf '%s\n' '  make health                 Check actuator health'
	@printf '%s\n' '  make metrics                List actuator metrics'
	@printf '%s\n' '  make validate               Validate default data/files directory'
	@printf '%s\n' '  make validate SAMPLE_DIR=data/sample'
	@printf '%s\n' '  make validate-sample        Validate data/sample'
	@printf '%s\n' '  make import                 Start ETL with CHUNK_SIZE=5000 WRITER_STRATEGY=jpa'
	@printf '%s\n' '  make import WRITER_STRATEGY=jdbc'
	@printf '%s\n' '  make import WRITER_STRATEGY=copy'
	@printf '%s\n' '  make import-jpa             Start ETL with JPA staging writer'
	@printf '%s\n' '  make import-jdbc            Start ETL with JDBC batch staging writer'
	@printf '%s\n' '  make import-copy            Start ETL with PostgreSQL COPY staging writer'
	@printf '%s\n' '  make import-resume JOB_ID=<id>'
	@printf '%s\n' '  make import-resume JOB_ID=<id> WRITER_STRATEGY=copy'
	@printf '%s\n' '  make jobs                   List ETL jobs'
	@printf '%s\n' '  make report                 Generate CSV report'
	@printf '%s\n' '  make reports                List reports'
	@printf '%s\n' '  make report-download REPORT_ID=<id>'
	@printf '%s\n' ''
	@printf '%s\n' 'Benchmarks:'
	@printf '%s\n' '  make benchmark BENCHMARK=blocking'
	@printf '%s\n' '  make benchmark BENCHMARK=virtual-thread'
	@printf '%s\n' '  make benchmark BENCHMARK=completable-future'
	@printf '%s\n' '  make benchmark BENCHMARK=reactive'
	@printf '%s\n' '  make benchmark-all'
	@printf '%s\n' '  make benchmark-save BENCHMARK=blocking'
	@printf '%s\n' '  make benchmark-all-save'
	@printf '%s\n' '  make benchmark-all-save-stats'
	@printf '%s\n' '  make benchmark-repeat-save-stats BENCHMARK_RUNS=3'
	@printf '%s\n' '  make benchmark-export-repeat-save-stats BENCHMARK_RUNS=3'
	@printf '%s\n' '  make benchmark-compare'

test:
	./mvnw -q test

package:
	./mvnw package

run:
	./mvnw spring-boot:run

db-up:
	docker compose up -d postgres

db-down:
	docker compose down

db-logs:
	docker compose logs -f postgres

db-tables:
	docker compose exec -T postgres psql -U analyticsflow -d analyticsflow -c "select table_schema, table_name from information_schema.tables where table_schema not in ('pg_catalog','information_schema') order by table_schema, table_name;"

compose-up:
	docker compose up --build

compose-up-detached:
	docker compose up --build -d

compose-down:
	docker compose down

compose-logs:
	docker compose logs -f

health:
	curl -s "$(APP_URL)/actuator/health"
	@printf '\n'

metrics:
	curl -s "$(APP_URL)/actuator/metrics"
	@printf '\n'

validate:
	curl -s -X POST "$(APP_URL)/api/files/validate" \
		-H 'Content-Type: application/json' \
		-d '{$(SAMPLE_DIR_JSON)}'
	@printf '\n'

validate-sample:
	$(MAKE) validate SAMPLE_DIR=data/sample

import:
	curl -s -X POST "$(APP_URL)/api/jobs/import" \
		-H 'Content-Type: application/json' \
		-d '{$(IMPORT_SAMPLE_JSON)$(IMPORT_COMMA)"chunkSize":$(CHUNK_SIZE),"writerStrategy":"$(WRITER_STRATEGY)"}'
	@printf '\n'

import-jpa:
	$(MAKE) import WRITER_STRATEGY=jpa

import-jdbc:
	$(MAKE) import WRITER_STRATEGY=jdbc

import-copy:
	$(MAKE) import WRITER_STRATEGY=copy

import-resume:
	@test -n "$(JOB_ID)" || (printf '%s\n' 'JOB_ID is required. Example: make import-resume JOB_ID=<id>' && exit 1)
	curl -s -X POST "$(APP_URL)/api/jobs/$(JOB_ID)/resume" \
		-H 'Content-Type: application/json' \
		-d '{"chunkSize":$(CHUNK_SIZE),"writerStrategy":"$(WRITER_STRATEGY)"}'
	@printf '\n'

jobs:
	curl -s "$(APP_URL)/api/jobs"
	@printf '\n'

reports:
	curl -s "$(APP_URL)/api/reports"
	@printf '\n'

report:
	curl -s -X POST "$(APP_URL)/api/reports/generate" \
		-H 'Content-Type: application/json' \
		-d '{"reportType":"$(REPORT_TYPE)","format":"$(REPORT_FORMAT)"}'
	@printf '\n'

report-download:
	@test -n "$(REPORT_ID)" || (printf '%s\n' 'REPORT_ID is required. Example: make report-download REPORT_ID=<id>' && exit 1)
	curl -L -o "data/reports/$(REPORT_ID)" "$(APP_URL)/api/reports/$(REPORT_ID)/download"

benchmark:
	BASE_URL="$(APP_URL)" VUS="$(VUS)" DURATION="$(DURATION)" SLEEP_SECONDS="$(SLEEP_SECONDS)" \
	P95_THRESHOLD='$(P95_THRESHOLD)' P99_THRESHOLD='$(P99_THRESHOLD)' ERROR_RATE_THRESHOLD='$(ERROR_RATE_THRESHOLD)' \
	k6 run "benchmark/$(BENCHMARK).js"

benchmark-save:
	@mkdir -p "$(BENCHMARK_RESULTS_DIR)"
	BASE_URL="$(APP_URL)" VUS="$(VUS)" DURATION="$(DURATION)" SLEEP_SECONDS="$(SLEEP_SECONDS)" \
	P95_THRESHOLD='$(P95_THRESHOLD)' P99_THRESHOLD='$(P99_THRESHOLD)' ERROR_RATE_THRESHOLD='$(ERROR_RATE_THRESHOLD)' \
	k6 run --summary-export "$(BENCHMARK_RESULT_FILE)" "benchmark/$(BENCHMARK).js"
	@printf '%s\n' "Saved benchmark summary to $(BENCHMARK_RESULT_FILE)"

benchmark-save-stats:
	BENCHMARK="$(BENCHMARK)" APP_URL="$(APP_URL)" VUS="$(VUS)" DURATION="$(DURATION)" SLEEP_SECONDS="$(SLEEP_SECONDS)" \
	P95_THRESHOLD='$(P95_THRESHOLD)' P99_THRESHOLD='$(P99_THRESHOLD)' ERROR_RATE_THRESHOLD='$(ERROR_RATE_THRESHOLD)' \
	BENCHMARK_RESULTS_DIR="$(BENCHMARK_RESULTS_DIR)" BENCHMARK_RESULT_FILE="$(BENCHMARK_RESULT_FILE)" \
	BENCHMARK_STATS_FILE="$(BENCHMARK_STATS_FILE)" BENCHMARK_SCRIPT="$(BENCHMARK_SCRIPT)" \
	STATS_INTERVAL_SECONDS="$(STATS_INTERVAL_SECONDS)" REPORT_TYPE="$(REPORT_TYPE)" REPORT_FORMAT="$(REPORT_FORMAT)" \
	bash benchmark/run-with-stats.sh

benchmark-all:
	$(MAKE) benchmark BENCHMARK=blocking
	$(MAKE) benchmark BENCHMARK=virtual-thread
	$(MAKE) benchmark BENCHMARK=completable-future
	$(MAKE) benchmark BENCHMARK=reactive

benchmark-all-save:
	-$(MAKE) benchmark-save BENCHMARK=blocking
	-$(MAKE) benchmark-save BENCHMARK=virtual-thread
	-$(MAKE) benchmark-save BENCHMARK=completable-future
	-$(MAKE) benchmark-save BENCHMARK=reactive

benchmark-all-save-stats:
	-$(MAKE) benchmark-save-stats BENCHMARK=blocking
	-$(MAKE) benchmark-save-stats BENCHMARK=virtual-thread
	-$(MAKE) benchmark-save-stats BENCHMARK=completable-future
	-$(MAKE) benchmark-save-stats BENCHMARK=reactive
	$(MAKE) benchmark-compare

benchmark-repeat-save-stats:
	@run=1; while [ $$run -le "$(BENCHMARK_RUNS)" ]; do \
		printf '%s\n' "Benchmark suite run $$run/$(BENCHMARK_RUNS)"; \
		$(MAKE) benchmark-save-stats BENCHMARK=blocking || true; \
		$(MAKE) benchmark-save-stats BENCHMARK=virtual-thread || true; \
		$(MAKE) benchmark-save-stats BENCHMARK=completable-future || true; \
		$(MAKE) benchmark-save-stats BENCHMARK=reactive || true; \
		run=$$((run + 1)); \
	done
	$(MAKE) benchmark-compare BENCHMARK_COMPARE_RUNS="$(BENCHMARK_RUNS)"

benchmark-export-repeat-save-stats:
	@run=1; while [ $$run -le "$(BENCHMARK_RUNS)" ]; do \
		printf '%s\n' "Report export benchmark suite run $$run/$(BENCHMARK_RUNS)"; \
		$(MAKE) benchmark-save-stats BENCHMARK_RESULTS_DIR=data/benchmark-export-results BENCHMARK=blocking BENCHMARK_SCRIPT=export-blocking || true; \
		$(MAKE) benchmark-save-stats BENCHMARK_RESULTS_DIR=data/benchmark-export-results BENCHMARK=virtual-thread BENCHMARK_SCRIPT=export-virtual-thread || true; \
		$(MAKE) benchmark-save-stats BENCHMARK_RESULTS_DIR=data/benchmark-export-results BENCHMARK=completable-future BENCHMARK_SCRIPT=export-completable-future || true; \
		$(MAKE) benchmark-save-stats BENCHMARK_RESULTS_DIR=data/benchmark-export-results BENCHMARK=reactive BENCHMARK_SCRIPT=export-reactive || true; \
		run=$$((run + 1)); \
	done
	$(MAKE) benchmark-compare BENCHMARK_RESULTS_DIR=data/benchmark-export-results BENCHMARK_COMPARE_RUNS="$(BENCHMARK_RUNS)"

benchmark-compare:
	BENCHMARK_RESULTS_DIR="$(BENCHMARK_RESULTS_DIR)" BENCHMARK_COMPARE_RUNS="$(BENCHMARK_COMPARE_RUNS)" node benchmark/compare-results.js

clean-reports:
	find data/reports -type f ! -name '.gitkeep' -delete
