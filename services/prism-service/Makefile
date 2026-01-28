.PHONY: install dev build test lint docker-build docker-run clean

# Development
install:
	npm install

dev:
	npm run start:dev

build:
	npm run build

test:
	npm run test

test-e2e:
	npm run test:e2e

lint:
	npm run lint

format:
	npm run format

# Docker
docker-build:
	docker build -t prism-service .

docker-run:
	docker run -p 8085:8085 --env-file .env prism-service

# Clean
clean:
	rm -rf dist node_modules

# Migration
migration-generate:
	npm run typeorm migration:generate -- -n $(name)

migration-run:
	npm run typeorm migration:run

migration-revert:
	npm run typeorm migration:revert
