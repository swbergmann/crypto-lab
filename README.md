# Customer Data Crypto Lab

A deliberately vulnerable, local-only application for learning a complete security workflow:

1. Reproduce a cryptographic weakness.
2. Verify it with an application-security tool.
3. Change the implementation.
4. Rebuild and use the same tool to verify the fix.

The runnable application uses React 19, Java 21 with Spring Boot, and Oracle Database Free. It also includes a container image, an AWS ECS Fargate/CloudFront deployment reference, and a GitHub CodeQL workflow.

> **Safety:** The starting code is intentionally vulnerable. Run it only on your own Mac, keep it bound to localhost, and use synthetic data. Do not deploy the vulnerable version to AWS or any shared environment.

## What you will see

| Lab | Starting vulnerability | Primary verification | Fix |
|---|---|---|---|
| 1 | AES in ECB mode produces deterministic ciphertext | CodeQL or SonarQube | AES-GCM with a fresh IV |
| 2 | Encryption key is committed in Java source | SonarQube or CodeQL | Inject key material; use KMS envelope encryption in AWS |
| 3 | Customer data is sent over HTTP | Burp Suite Professional or ZAP | Local HTTPS, HSTS, and HTTPS-only CloudFront/origin policies |

The Oracle table stores ciphertext observations only. The submitted plaintext is not persisted.

## Mac setup

From this directory:

```bash
make install
export PATH="$(brew --prefix openjdk@21)/bin:$PATH"
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
colima start --cpu 4 --memory 8
```

`make install` uses a project-local npm cache under `.local/`, so it never needs `sudo` to repair a user-level npm cache. If you already use Docker Desktop, start it instead of Colima.

## Start the vulnerable application

Terminal 1:

```bash
make db
docker-compose ps
```

Wait until Oracle reports `healthy`, then start the application:

```bash
make run
```

Open [http://localhost:8080](http://localhost:8080). The first Oracle start can take several minutes.

For a quick command-line observation:

```bash
./security/runtime-check.sh http://localhost:8080
```

Expected starting state:

- `weakLabAlgorithm` is `AES/ECB/PKCS5Padding`.
- Two encryptions of the same value are identical.
- `hardcodedLabKeySource` reports a constant in source code.
- `secureTransport` is `false`.

## Run the exercises

Follow [SECURITY-LAB.md](SECURITY-LAB.md). The secure replacements are kept as `.txt` files under `solutions/`, so Java static analysis scans only the implementation currently compiled under `backend/src/main/java`.

For case-study-ready OWASP classification, stack examples, detector and remediation sources, and the coverage limits of every named tool, see [ACADEMIC-NOTES.md](ACADEMIC-NOTES.md).

The shortcut commands are:

```bash
make fix-1
make fix-2
make cert
make fix-3
```

Stop and restart Spring Boot after a source change. Review every change rather than treating the replacement as a black box. If this directory is a Git repository, `git diff` gives a convenient review.

To restore the initial vulnerable state:

```bash
make reset
```

## Project map

```text
frontend/                 React interface
backend/                  Spring Boot API and Oracle persistence
security/                 ZAP, Burp, SonarQube and runtime instructions
solutions/                Vulnerable snapshots and secure replacements
infra/aws/                CloudFront and ECS Fargate deployment reference
.github/workflows/        CodeQL analysis
compose.yaml              Oracle and optional local SonarQube
```

The local key provider intentionally generates an ephemeral key at startup. That is sufficient to prove removal of a source-code key, but it is not a production key-management design. The AWS reference uses KMS permissions and documents where a KMS envelope-encryption provider must be connected.
