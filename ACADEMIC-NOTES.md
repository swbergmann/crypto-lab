# Academic vulnerability mapping

All three exercises map to **OWASP Top 10:2025 A04 — Cryptographic Failures**. In the 2021 edition, the equivalent category is **A02 — Cryptographic Failures**. State the edition in the case study so the category number is unambiguous.

- [OWASP Top 10:2025 A04 — Cryptographic Failures](https://owasp.org/Top10/2025/A04_2025-Cryptographic_Failures/)
- [OWASP Top 10:2021 A02 — Cryptographic Failures](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/)

## 1. Weak encryption algorithm or mode

**Practical example.** The React client submits a synthetic customer identifier to Spring Boot. `WeakCryptoService` encrypts it with `AES/ECB/PKCS5Padding` before the ciphertext is written to Oracle. ECB is deterministic for identical plaintext blocks, so repeated values can reveal patterns. The UI makes this visible by encrypting the same value twice and comparing the results.

**Detection.** CodeQL's Java `java/weak-cryptographic-algorithm` query and SonarQube's Java cryptography rules can identify risky cipher construction. A reviewer should also inspect every `Cipher.getInstance(...)` call and verify nonce handling.

- [CodeQL: Use of a broken or risky cryptographic algorithm](https://codeql.github.com/codeql-query-help/java/java-weak-cryptographic-algorithm/)
- [SonarQube: security-related rules](https://docs.sonarsource.com/sonarqube-server/user-guide/rules/security-related-rules)

**Remediation.** The supplied fix uses `AES/GCM/NoPadding`, creates a fresh unpredictable 96-bit IV for every operation, uses a 128-bit authentication tag, and stores the IV beside the ciphertext. IVs are not secret, but they must not be reused with the same GCM key.

- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
- [NIST SP 800-38D: Galois/Counter Mode](https://csrc.nist.gov/pubs/sp/800/38/d/final)

## 2. Hard-coded encryption key

**Practical example.** `HardCodedKeyService` contains the AES key as a Java literal. The key therefore travels through GitHub into the compiled Spring Boot JAR and Docker image, and would be shared by every ECS Fargate task. Access to any of those artifacts exposes the key and all data encrypted with it.

**Detection.** SonarQube can flag hard-coded secrets, while CodeQL can track hard-coded values passed to sensitive API parameters. Detection is pattern-dependent, so code review must also inspect source, configuration, Git history, CI logs, container definitions, and key-loading code.

- [CodeQL Java hard-coded credential library](https://codeql.github.com/codeql-standard-libraries/java/semmle/code/java/security/HardcodedCredentials.qll/module.HardcodedCredentials.html)
- [SonarQube: security-related rules](https://docs.sonarsource.com/sonarqube-server/user-guide/rules/security-related-rules)

**Remediation.** The local fix removes key material from the class and injects a process-local key provider. That is suitable for demonstrating the code change, but production should use envelope encryption backed by AWS KMS, narrowly scoped ECS task-role permissions, rotation, and auditing. A real committed key must be rotated; merely deleting the literal does not make it secret again.

- [AWS KMS cryptography and envelope encryption](https://docs.aws.amazon.com/kms/latest/developerguide/kms-cryptography.html)
- [AWS KMS best practices](https://docs.aws.amazon.com/prescriptive-guidance/latest/aws-kms-best-practices/welcome.html)

## 3. Missing encryption in transit

**Practical example.** The starting React application sends the customer value to Spring Boot over `http://localhost:8080`. A network observer could read or change it before application-level encryption happens. In the production reference, the same risk exists at each hop unless TLS covers viewer-to-CloudFront, CloudFront-to-load-balancer/ECS, and Spring Boot-to-Oracle.

**Detection.** Burp Suite Professional reports unencrypted communications during a crawl/audit. ZAP verifies transport-related response controls such as HSTS after HTTPS is enabled. Code review must examine CloudFront origin policies, load-balancer listeners, ECS ports, and the Oracle JDBC URL because an external scan cannot prove encryption on private back-end hops.

- [Burp Suite: Unencrypted communications](https://portswigger.net/kb/issues/01000200_unencrypted-communications)
- [ZAP alert 10035: Strict-Transport-Security Header](https://www.zaproxy.org/docs/alerts/10035/)

**Remediation.** The local fix enables Spring Boot HTTPS with a trusted `mkcert` certificate and adds HSTS. The AWS reference redirects viewers to HTTPS, requires HTTPS to the origin, and sets a modern minimum TLS version. Oracle connections should use TCPS with certificate and hostname validation.

- [AWS CloudFront: Require HTTPS for viewers](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/using-https-viewers-to-cloudfront.html)
- [AWS CloudFront: Require HTTPS to a custom origin](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/using-https-cloudfront-to-custom-origin.html)
- [Oracle JDBC Developer's Guide](https://docs.oracle.com/en/database/oracle/oracle-database/23/jjdbc/jdbc-developers-guide.pdf)

## Tool coverage boundaries

| Tool | Lab 1: weak mode | Lab 2: source key | Lab 3: cleartext transport |
|---|---:|---:|---:|
| CodeQL | Primary | Possible; review required | Configuration review only |
| SonarQube | Primary | Primary/hotspot; review required | Limited configuration coverage |
| Burp Suite Professional | Runtime behavior only | No | Primary |
| OWASP ZAP | Runtime behavior only | No | HSTS/TLS verification |
| OWASP Dependency-Check | No | No | No |
| OpenSCA | No | No | No |

OWASP Dependency-Check and OpenSCA remain useful for vulnerable third-party components. They are not the correct evidence for these three first-party cryptographic design and configuration flaws. Use them as a separate software-composition control, not as proof that these findings are fixed.
