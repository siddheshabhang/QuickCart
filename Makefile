SHELL := /bin/bash
.DEFAULT_GOAL := help

# ── Load .env → export all vars as real OS environment variables ──────────────
# Every service started by this Makefile inherits these as system env vars.
# Spring Boot resolves ${DB_USERNAME} etc. from the OS environment directly —
# no library needed, works regardless of which directory the JVM starts in.
ifneq (,$(wildcard ./.env))
    include .env
    export
endif

LOG_DIR := logs
MVN := ./mvnw
COMMON_LIB := common-lib

SERVICES := discovery-server config-server auth-service product-service \
	cart-service order-service payment-service delivery-service \
	notification-service api-gateway

.PHONY: help
help:
	@echo "QuickCart Makefile"
	@echo ""
	@echo "make start              Build common-lib and start all services"
	@echo "make stop               Stop all services"
	@echo "make restart            Stop and start again"
	@echo "make build              Build common-lib only"
	@echo "make logs               Tail all logs"
	@echo "make log s=<service>    Tail one service log"
	@echo "make status             Show running services"
	@echo "make health             Check health endpoints"
	@echo "make redis-otp id=<id>  Show delivery OTP from Redis"
	@echo "make redis-flush        Flush Redis"
	@echo "make clean              Delete log files"

.PHONY: build
build:
	$(MVN) install -pl $(COMMON_LIB) -q

.PHONY: start
start: build check-infra mklog
	$(MVN) -f discovery-server/pom.xml spring-boot:run > $(LOG_DIR)/discovery-server.log 2>&1 &
	sleep 12

	$(MVN) -f config-server/pom.xml spring-boot:run > $(LOG_DIR)/config-server.log 2>&1 &
	sleep 6

	$(MVN) -f auth-service/pom.xml spring-boot:run > $(LOG_DIR)/auth-service.log 2>&1 &
	$(MVN) -f product-service/pom.xml spring-boot:run > $(LOG_DIR)/product-service.log 2>&1 &
	$(MVN) -f cart-service/pom.xml spring-boot:run > $(LOG_DIR)/cart-service.log 2>&1 &
	$(MVN) -f order-service/pom.xml spring-boot:run > $(LOG_DIR)/order-service.log 2>&1 &
	$(MVN) -f payment-service/pom.xml spring-boot:run > $(LOG_DIR)/payment-service.log 2>&1 &
	$(MVN) -f delivery-service/pom.xml spring-boot:run > $(LOG_DIR)/delivery-service.log 2>&1 &
	$(MVN) -f notification-service/pom.xml spring-boot:run > $(LOG_DIR)/notification-service.log 2>&1 &

	sleep 8
	$(MVN) -f api-gateway/pom.xml spring-boot:run > $(LOG_DIR)/api-gateway.log 2>&1 &

	@echo "All services started"
	@echo "Gateway: http://localhost:8080"
	@echo "Eureka : http://localhost:8761"
	@echo "Config : http://localhost:8888"

.PHONY: stop
stop:
	-pkill -f "spring-boot:run"
	-pkill -f "quickcart"
	@echo "All services stopped"

.PHONY: restart
restart: stop
	sleep 3
	$(MAKE) start

.PHONY: logs
logs:
	tail -f $(LOG_DIR)/*.log

.PHONY: log
log:
ifndef s
	@echo "Usage: make log s=<service-name>"
else
	tail -f $(LOG_DIR)/$(s).log
endif

.PHONY: status
status:
	@for svc in $(SERVICES); do \
		if pgrep -f "$$svc" > /dev/null; then \
			echo "$$svc: running"; \
		else \
			echo "$$svc: stopped"; \
		fi; \
	done

.PHONY: health
health:
	@echo "Eureka:"
	@curl -s http://localhost:8761/actuator/health || echo "unreachable"
	@echo ""
	@echo "Config:"
	@curl -s http://localhost:8888/actuator/health || echo "unreachable"
	@echo ""
	@echo "Gateway:"
	@curl -s http://localhost:8080/actuator/health || echo "unreachable"
	@echo ""

.PHONY: redis-otp
redis-otp:
ifndef id
	@echo "Usage: make redis-otp id=<orderId>"
else
	@VALUE=$$(redis-cli GET otp:delivery:$(id)); \
	TTL=$$(redis-cli TTL otp:delivery:$(id)); \
	if [ -z "$$VALUE" ]; then \
		echo "No OTP found"; \
	else \
		OTP=$$(echo $$VALUE | cut -d: -f1); \
		ATTEMPTS=$$(echo $$VALUE | cut -d: -f2); \
		echo "OTP: $$OTP"; \
		echo "Attempts: $$ATTEMPTS / 3"; \
		echo "TTL: $$TTL seconds"; \
	fi
endif

.PHONY: redis-flush
redis-flush:
	redis-cli FLUSHALL

.PHONY: clean
clean:
	rm -f $(LOG_DIR)/*.log

.PHONY: mklog
mklog:
	mkdir -p $(LOG_DIR)

.PHONY: check-infra
check-infra:
	@echo "$(YELLOW)⚙  Checking infrastructure...$(RESET)"
	@pg_isready -q || (echo "$(RED)✘  PostgreSQL is not running$(RESET)" && exit 1)
	@redis-cli ping > /dev/null 2>&1 || (echo "$(RED)✘  Redis is not running. Run: brew services start redis$(RESET)" && exit 1)
	@nc -z localhost 9092 > /dev/null 2>&1 || (echo "$(RED)✘  Kafka is not running on localhost:9092$(RESET)" && exit 1)
	@echo "$(GREEN)✔  PostgreSQL, Redis and Kafka are running$(RESET)"