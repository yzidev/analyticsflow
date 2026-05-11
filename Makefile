APP_URL ?= http://localhost:8080
CHUNK_SIZE ?= 5000
SAMPLE_DIR ?=
REPORT_TYPE ?= SALES_PRODUCT_SUMMARY
REPORT_FORMAT ?= CSV
REPORT_ID ?=
BENCHMARK ?= blocking
VUS ?= 100
DURATION ?= 1m
SLEEP_SECONDS ?= 1
P95_THRESHOLD ?= p(95)<2000
P99_THRESHOLD ?= p(99)<5000
ERROR_RATE_THRESHOLD ?= rate<0.01

COMMA := ,
SAMPLE_DIR_JSON := $(if $(strip $(SAMPLE_DIR)),"sampleDirectory":"$(SAMPLE_DIR)")
IMPORT_SAMPLE_JSON := $(if $(strip $(SAMPLE_DIR)),"sampleDirectory":"$(SAMPLE_DIR)",)
IMPORT_COMMA := $(if $(strip $(SAMPLE_DIR)),$(COMMA),)

.PHONY: help test package run db-up db-down db-logs compose-up compose-down compose-logs health metrics validate validate-sample import jobs reports report report-download benchmark benchmark-all clean-reports

help:
	@printf '%s\n' 'AnalyticsFlow commands'
	@printf '%s\n' ''
	@printf '%s\n' 'Local development:'
	@printf '%s\n' '  make db-up                  Start PostgreSQL only'
	@printf '%s\n' '  make run                    Run Spring Boot app with Maven'
	@printf '%s\n' '  make compose-up             Build and run app + PostgreSQL'
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
	@printf '%s\n' '  make import                 Start ETL with CHUNK_SIZE=5000'
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

compose-up:
	docker compose up --build

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
		-d '{$(IMPORT_SAMPLE_JSON)$(IMPORT_COMMA)"chunkSize":$(CHUNK_SIZE)}'
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

benchmark-all:
	$(MAKE) benchmark BENCHMARK=blocking
	$(MAKE) benchmark BENCHMARK=virtual-thread
	$(MAKE) benchmark BENCHMARK=completable-future
	$(MAKE) benchmark BENCHMARK=reactive

clean-reports:
	find data/reports -type f ! -name '.gitkeep' -delete
