SHELL := /bin/zsh
APP_URL ?= http://localhost:8080
NPM_CACHE ?= $(CURDIR)/.local/npm-cache
COMPOSE ?= docker-compose

.PHONY: help install check-docker db db-stop ui build run test cert zap sonar fix-1 fix-2 fix-3 reset status

help:
	@echo "Crypto Lab"
	@echo "  make install  Install Homebrew dependencies and npm packages"
	@echo "  make db       Start Oracle Database Free"
	@echo "  make build    Build React and Spring Boot"
	@echo "  make run      Run the lab (starts vulnerable on http://localhost:8080)"
	@echo "  make zap      Run a ZAP baseline scan; override APP_URL after TLS fix"
	@echo "  make fix-1    Replace ECB code with AES-GCM"
	@echo "  make fix-2    Remove the hard-coded encryption key"
	@echo "  make cert     Create a trusted local TLS certificate"
	@echo "  make fix-3    Enable HTTPS and HSTS"
	@echo "  make reset    Restore all three vulnerable source files"

install:
	brew bundle
	cd frontend && npm install --cache "$(NPM_CACHE)"

check-docker:
	@docker info >/dev/null 2>&1 || { \
		echo "Docker is not running. Start it with: colima start --cpu 4 --memory 8"; \
		exit 1; \
	}

db: check-docker
	$(COMPOSE) up -d oracle
	@echo "Oracle is starting. Run '$(COMPOSE) ps' until it reports healthy."

db-stop: check-docker
	$(COMPOSE) stop oracle

ui:
	cd frontend && npm run build

build: ui
	cd backend && mvn clean verify

run: ui
	cd backend && \
	LAB_DB_USER=crypto_lab \
	LAB_DB_PASSWORD=crypto_lab_local_only \
	mvn spring-boot:run

test:
	cd backend && mvn test

cert:
	mkdir -p .local
	mkcert -install
	mkcert -pkcs12 -p12-file .local/localhost.p12 localhost 127.0.0.1 ::1
	@echo "Created .local/localhost.p12 (mkcert PKCS#12 password: changeit)."

zap:
	mkdir -p reports
	./security/zap-baseline.sh "$(APP_URL)"

sonar: check-docker
	$(COMPOSE) --profile security up -d sonarqube
	@echo "Open http://localhost:9000, create a token, then run:"
	@echo "  cd backend && mvn sonar:sonar -Dsonar.token=YOUR_TOKEN"

fix-1:
	cp solutions/secure/WeakCryptoService.java.txt backend/src/main/java/com/example/cryptolab/crypto/WeakCryptoService.java
	@echo "Applied fix 1. Review the diff, rebuild, and rerun CodeQL/SonarQube."

fix-2:
	cp solutions/secure/HardCodedKeyService.java.txt backend/src/main/java/com/example/cryptolab/crypto/HardCodedKeyService.java
	@echo "Applied fix 2. Review the diff, rebuild, and rerun CodeQL/SonarQube."

fix-3:
	cp solutions/secure/application.yml.txt backend/src/main/resources/application.yml
	cp solutions/secure/TransportSecurityFilter.java.txt backend/src/main/java/com/example/cryptolab/web/TransportSecurityFilter.java
	@echo "Applied fix 3. Run 'make cert', restart, then use https://localhost:8443."

reset:
	cp solutions/vulnerable/WeakCryptoService.java.txt backend/src/main/java/com/example/cryptolab/crypto/WeakCryptoService.java
	cp solutions/vulnerable/HardCodedKeyService.java.txt backend/src/main/java/com/example/cryptolab/crypto/HardCodedKeyService.java
	cp solutions/vulnerable/application.yml.txt backend/src/main/resources/application.yml
	rm -f backend/src/main/java/com/example/cryptolab/web/TransportSecurityFilter.java
	@echo "Restored the intentionally vulnerable starting point."

status:
	curl --silent "$(APP_URL)/api/labs/status" | jq
