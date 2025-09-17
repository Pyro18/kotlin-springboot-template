.PHONY: help build test clean run docker-build docker-run docker-stop k8s-deploy k8s-delete

# Variables
APP_NAME := spring-app
REGISTRY := ghcr.io
NAMESPACE := yourusername
VERSION := $(shell grep version build.gradle.kts | head -1 | awk -F'"' '{print $$2}')
IMAGE := ${REGISTRY}/${NAMESPACE}/${APP_NAME}:${VERSION}
GRADLE := ./gradlew

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "${GREEN}Available targets:${NC}"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "${YELLOW}%-20s${NC} %s\n", $$1, $$2}'

# Development Commands
install: ## Install dependencies
	@echo "${GREEN}Installing dependencies...${NC}"
	${GRADLE} dependencies

build: ## Build the application
	@echo "${GREEN}Building application...${NC}"
	${GRADLE} clean build

test: ## Run tests
	@echo "${GREEN}Running tests...${NC}"
	${GRADLE} test

test-integration: ## Run integration tests
	@echo "${GREEN}Running integration tests...${NC}"
	${GRADLE} integrationTest

lint: ## Run linters (ktlint + detekt)
	@echo "${GREEN}Running linters...${NC}"
	${GRADLE} ktlintCheck detekt

format: ## Format code with ktlint
	@echo "${GREEN}Formatting code...${NC}"
	${GRADLE} ktlintFormat

run: ## Run application locally
	@echo "${GREEN}Starting application...${NC}"
	${GRADLE} bootRun

run-dev: ## Run with dev profile
	@echo "${GREEN}Starting in dev mode...${NC}"
	SPRING_PROFILES_ACTIVE=dev ${GRADLE} bootRun

clean: ## Clean build artifacts
	@echo "${RED}Cleaning build artifacts...${NC}"
	${GRADLE} clean
	rm -rf build/ out/ .gradle/

# Docker Commands
docker-build: ## Build Docker image with Jib
	@echo "${GREEN}Building Docker image: ${IMAGE}${NC}"
	${GRADLE} jib

docker-build-local: ## Build Docker image to local daemon
	@echo "${GREEN}Building Docker image locally...${NC}"
	${GRADLE} jibDockerBuild

docker-run: ## Run application with docker-compose
	@echo "${GREEN}Starting Docker containers...${NC}"
	docker-compose -f docker/docker-compose.yml up -d

docker-run-dev: ## Run development environment
	@echo "${GREEN}Starting development environment...${NC}"
	docker-compose -f docker/docker-compose.dev.yml up -d

docker-logs: ## Show docker logs
	docker-compose -f docker/docker-compose.yml logs -f app

docker-stop: ## Stop all containers
	@echo "${RED}Stopping Docker containers...${NC}"
	docker-compose -f docker/docker-compose.yml down

docker-clean: ## Stop and remove all containers, volumes
	@echo "${RED}Removing Docker containers and volumes...${NC}"
	docker-compose -f docker/docker-compose.yml down -v

docker-push: ## Push image to registry
	@echo "${GREEN}Pushing image to registry...${NC}"
	docker push ${IMAGE}

# Database Commands
db-migrate: ## Run database migrations
	@echo "${GREEN}Running database migrations...${NC}"
	${GRADLE} flywayMigrate

db-clean: ## Clean database
	@echo "${RED}Cleaning database...${NC}"
	${GRADLE} flywayClean

db-info: ## Show migration info
	@echo "${GREEN}Migration info:${NC}"
	${GRADLE} flywayInfo

db-validate: ## Validate migrations
	@echo "${GREEN}Validating migrations...${NC}"
	${GRADLE} flywayValidate

db-repair: ## Repair migration checksums
	@echo "${YELLOW}Repairing migration checksums...${NC}"
	${GRADLE} flywayRepair

# Kubernetes Commands
k8s-namespace: ## Create Kubernetes namespace
	@echo "${GREEN}Creating namespace...${NC}"
	kubectl create namespace spring-app --dry-run=client -o yaml | kubectl apply -f -

k8s-deploy: k8s-namespace ## Deploy to Kubernetes
	@echo "${GREEN}Deploying to Kubernetes...${NC}"
	kubectl apply -f k8s/

k8s-delete: ## Delete from Kubernetes
	@echo "${RED}Deleting from Kubernetes...${NC}"
	kubectl delete -f k8s/

k8s-status: ## Check deployment status
	@echo "${GREEN}Deployment status:${NC}"
	kubectl -n spring-app get all

k8s-logs: ## Show pod logs
	@echo "${GREEN}Pod logs:${NC}"
	kubectl -n spring-app logs -l app=spring-app --tail=100 -f

k8s-rollout: ## Rollout new version
	@echo "${GREEN}Rolling out new version...${NC}"
	kubectl -n spring-app set image deployment/spring-app app=${IMAGE}
	kubectl -n spring-app rollout status deployment/spring-app

k8s-rollback: ## Rollback deployment
	@echo "${YELLOW}Rolling back deployment...${NC}"
	kubectl -n spring-app rollout undo deployment/spring-app

k8s-scale: ## Scale deployment (usage: make k8s-scale REPLICAS=5)
	@echo "${GREEN}Scaling to ${REPLICAS} replicas...${NC}"
	kubectl -n spring-app scale deployment/spring-app --replicas=${REPLICAS}

# CI/CD Commands
ci: clean lint test build ## Run CI pipeline locally
	@echo "${GREEN}CI pipeline completed successfully!${NC}"

release: ci docker-build docker-push ## Create release
	@echo "${GREEN}Release ${VERSION} completed!${NC}"
	@echo "Don't forget to tag: git tag v${VERSION} && git push --tags"

# Monitoring Commands
metrics: ## Show application metrics
	@echo "${GREEN}Opening metrics endpoint...${NC}"
	open http://localhost:8081/actuator/metrics

health: ## Check application health
	@echo "${GREEN}Health check:${NC}"
	@curl -s http://localhost:8081/actuator/health | jq .

prometheus: ## Open Prometheus UI
	@echo "${GREEN}Opening Prometheus...${NC}"
	open http://localhost:9090

grafana: ## Open Grafana UI
	@echo "${GREEN}Opening Grafana...${NC}"
	open http://localhost:3000

kibana: ## Open Kibana UI
	@echo "${GREEN}Opening Kibana...${NC}"
	open http://localhost:5601

# Utility Commands
env-setup: ## Setup local environment
	@echo "${GREEN}Setting up local environment...${NC}"
	cp .env.example .env
	@echo "Please edit .env file with your configuration"

generate-secret: ## Generate random secret
	@echo "${GREEN}Generated secret:${NC}"
	@openssl rand -hex 32

port-check: ## Check if ports are available
	@echo "${GREEN}Checking ports...${NC}"
	@for port in 8080 8081 5432 6379 9090 3000 5601; do \
		lsof -i :$$port >/dev/null 2>&1 && echo "${RED}Port $$port is in use${NC}" || echo "${GREEN}Port $$port is available${NC}"; \
	done

deps-update: ## Check for dependency updates
	@echo "${GREEN}Checking for updates...${NC}"
	${GRADLE} dependencyUpdates

deps-tree: ## Show dependency tree
	@echo "${GREEN}Dependency tree:${NC}"
	${GRADLE} dependencies

# Performance Commands
benchmark: ## Run benchmarks
	@echo "${GREEN}Running benchmarks...${NC}"
	${GRADLE} jmh

profile: ## Run with profiler
	@echo "${GREEN}Starting with profiler...${NC}"
	JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" ${GRADLE} bootRun

# Documentation
docs: ## Generate documentation
	@echo "${GREEN}Generating documentation...${NC}"
	${GRADLE} dokkaHtml
	@echo "Documentation available at: build/dokka/html/index.html"

api-docs: ## Open API documentation
	@echo "${GREEN}Opening API docs...${NC}"
	open http://localhost:8080/swagger-ui.html

# All-in-one commands
all: clean install lint test build docker-build-local ## Build everything

fresh-start: docker-clean docker-run db-migrate run-dev ## Fresh start with clean environment

destroy-all: clean docker-clean k8s-delete ## Destroy everything (use with caution!)
	@echo "${RED}All resources destroyed!${NC}"