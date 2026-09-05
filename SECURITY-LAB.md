# Verify → fix → verify

This guide deliberately keeps the three findings separate. Fix one vulnerability at a time and retain the before-and-after report for your case-study evidence.

## Baseline preparation

Initialize a local Git repository so each remediation can be reviewed:

```bash
git init
git add .
git commit -m "Intentionally vulnerable cryptography lab baseline"
```

Run the source preflight to locate the deliberately planted patterns:

```bash
./security/source-preflight.sh
```

This preflight is only a convenience; it is not a substitute for CodeQL or SonarQube.

## Lab 1 — weak cryptographic mode

### Reproduce

Open the application and select **Encrypt twice** in lab 1. The two Base64 ciphertexts are identical because the implementation uses `AES/ECB/PKCS5Padding`.

The vulnerable file is:

```text
backend/src/main/java/com/example/cryptolab/crypto/WeakCryptoService.java
```

### Verify with CodeQL

This lab uses **GitHub-hosted CodeQL**. The workflow file at
`.github/workflows/codeql.yml` tells GitHub Actions to compile the Spring Boot
code and scan it. Nothing needs to be installed locally for this scan.

CodeQL is available for public GitHub repositories. A private repository needs
GitHub Code Security to be enabled for that repository. The easiest academic
lab setup is therefore a new public repository containing synthetic data only.
Before publishing, confirm that you have not added real passwords, tokens,
customer data, certificates, or other secrets.

If an Actions run ends with `Resource not accessible by integration` and the
Code scanning page says it is not enabled, first check the repository visibility
under **Settings → General**. Either make this synthetic lab public under
**Danger Zone → Change repository visibility**, or keep it private and enable
GitHub Code Security under **Settings → Code security** if your account or
organization has the required license. The workflow cannot upload results to a
private repository for which Code Security is unavailable.

#### A. Create and push the vulnerable baseline

Open a second terminal and run:

```bash
cd "$HOME/Documents/Codex/2026-09-05/for/outputs/crypto-lab"

gh auth status
```

If that reports that you are not logged in, authenticate:

```bash
gh auth login --web --git-protocol https
```

Then create the baseline commit and repository:

```bash
git init
git branch -M main
git add .
git commit -m "Add intentionally vulnerable cryptography lab baseline"
gh repo create crypto-lab --public --source=. --remote=origin --push
```

If you already created the GitHub repository in the browser, replace the last
command with the repository URL shown by GitHub:

```bash
git remote add origin https://github.com/YOUR-GITHUB-NAME/crypto-lab.git
git push -u origin main
```

#### B. Wait for the CodeQL workflow

1. Open the repository on GitHub.
2. Select **Actions** in the repository navigation.
3. Select the workflow named **CodeQL**.
4. Open the newest run created by the push.
5. Wait for both jobs to finish. In particular, **Analyze java-kotlin** must
   display a green check mark. The Oracle container does not need to be running
   for this GitHub build.

The scan normally starts automatically because the workflow listens for pushes
to `main`. If it did not start, select **Actions → CodeQL → Run workflow**, leave
the branch as `main`, and select **Run workflow**.

#### C. Locate and record the finding

1. Select the repository's **Security** tab.
2. In the left navigation, select **Code scanning** or **Code scanning alerts**.
3. Open the alert titled **Use of a broken or risky cryptographic algorithm**.
4. Confirm these details:

   - Language: Java/Kotlin
   - Query ID: `java/weak-cryptographic-algorithm`
   - File: `backend/src/main/java/com/example/cryptolab/crypto/WeakCryptoService.java`
   - The data-flow path ends at `Cipher.getInstance(...)` and includes
     `AES/ECB/PKCS5Padding`.

Save a screenshot or export containing the alert title, query ID, affected file,
severity, commit, and date. This is the **before** evidence. Do not dismiss the
alert; the follow-up scan should close it as fixed.

CodeQL documents this query and explicitly recommends avoiding ECB mode:

- [CodeQL: Use of a broken or risky cryptographic algorithm](https://codeql.github.com/codeql-query-help/java/java-weak-cryptographic-algorithm/)
- [GitHub: Configuring code scanning for a repository](https://docs.github.com/en/code-security/code-scanning/creating-an-advanced-setup-for-code-scanning/configuring-code-scanning-for-a-repository)
- [GitHub: Assessing code scanning alerts](https://docs.github.com/en/code-security/code-scanning/managing-code-scanning-alerts/assessing-code-scanning-alerts-for-your-repository)

### Verify with SonarQube locally

Start the local server:

```bash
make sonar
```

Open http://localhost:9000, sign in, create a user token, and run:

```bash
cd backend
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN
```

Review the cryptography Security Hotspot or vulnerability referencing ECB or CWE-327.

### Fix and retest

Inspect the secure reference and apply it:

```bash
diff -u \
  backend/src/main/java/com/example/cryptolab/crypto/WeakCryptoService.java \
  solutions/secure/WeakCryptoService.java.txt
make fix-1
```

Review and build the exact change:

```bash
git diff -- backend/src/main/java/com/example/cryptolab/crypto/WeakCryptoService.java
make build
```

Stop the running application with `Control-C`, run `make run` again, and select
**Encrypt twice**. The two ciphertexts must now differ.

Commit and push the remediation:

```bash
git add backend/src/main/java/com/example/cryptolab/crypto/WeakCryptoService.java
git commit -m "Replace AES-ECB with authenticated AES-GCM"
git push
```

Return to **GitHub → Actions → CodeQL** and wait for **Analyze java-kotlin** to
succeed again. Then open **Security → Code scanning alerts**, change the filter
from **Open** to **Closed**, and open the original alert. It should show that the
alert was closed as **Fixed** by the remediation commit. Save this page as the
**after** evidence.

If the workflow succeeds but the original alert remains open, verify that the
default branch is `main`, that the remediation commit appears on GitHub, and
that the new run analyzed that commit SHA.

The remediation uses `AES/GCM/NoPadding`, a newly generated IV for each encryption, and stores the IV with the authenticated ciphertext.

## Lab 2 — hard-coded encryption key

### Reproduce

Open lab 2 and observe that the key source is reported as `Hard-coded constant in source code`. Inspect:

```text
backend/src/main/java/com/example/cryptolab/crypto/HardCodedKeyService.java
```

The Java literal would be available to anyone who could read the GitHub repository, compiled JAR, or Fargate container image.

### Verify

Run SonarQube again and review findings for hard-coded credentials or hard-coded secrets. CodeQL can also model hard-coded values passed to sensitive API parameters, including cryptographic-key parameters:

https://codeql.github.com/codeql-standard-libraries/java/semmle/code/java/security/HardcodedCredentials.qll/module.HardcodedCredentials.html

Static detection is pattern-dependent. A human code review must still search cryptographic construction, configuration, container definitions, Git history, and secret-loading paths.

### Fix and retest

```bash
diff -u \
  backend/src/main/java/com/example/cryptolab/crypto/HardCodedKeyService.java \
  solutions/secure/HardCodedKeyService.java.txt
make fix-2
```

Restart the application. Lab 2 should now report `Ephemeral key generated at application startup`. Rerun SonarQube or CodeQL and confirm that the hard-coded-key finding no longer exists.

The local replacement proves that key material is injected rather than embedded. In AWS, replace `LabKeyProvider` with envelope encryption backed by AWS KMS. The ECS task role—not the execution role—should receive narrowly scoped `kms:GenerateDataKey` and `kms:Decrypt` permissions. See `infra/aws/README.md`.

If a real key has ever been committed, removing the literal is insufficient: revoke or rotate the key and consider the old ciphertext exposed.

## Lab 3 — cleartext transport

### Verify with Burp Suite Professional

1. Keep the vulnerable application running at `http://localhost:8080`.
2. Open Burp Suite Professional and create a temporary project.
3. Select **Dashboard → New scan**.
4. Enter `http://localhost:8080` and use a lightweight crawl and audit.
5. Retain the **Unencrypted communications** issue as the before evidence.

PortSwigger describes this finding at:

https://portswigger.net/kb/issues/01000200_unencrypted-communications

### Optional ZAP baseline

With Docker running:

```bash
APP_URL=http://localhost:8080 make zap
```

Open `reports/zap-report.html`. ZAP is particularly useful after HTTPS is enabled because rule 10035 verifies the HSTS response header. The Docker baseline scan is passive; Burp Professional is the clearer starting-state detector for an HTTP-only application.

### Fix

Stop Spring Boot, then run:

```bash
make cert
make fix-3
```

Review both changes:

```bash
git diff -- backend/src/main/resources/application.yml
git diff -- backend/src/main/java/com/example/cryptolab/web/TransportSecurityFilter.java
```

Restart with `make run` and open:

https://localhost:8443

The mkcert certificate is locally trusted and uses the PKCS#12 password `changeit`. It is development material only and is ignored by Git.

### Verify the fix

Run the runtime check:

```bash
./security/runtime-check.sh https://localhost:8443
```

Then rescan `https://localhost:8443` in Burp. The **Unencrypted communications** issue should be absent, the certificate should validate on this Mac, and the response should contain `Strict-Transport-Security`.

Run ZAP against the secure endpoint:

```bash
APP_URL=https://localhost:8443 make zap
```

Confirm that ZAP rule 10035 does not report a missing or malformed HSTS header. ZAP documents that rule at:

https://www.zaproxy.org/docs/alerts/10035/

The local HTTPS change protects the browser-to-application path. In AWS, the equivalent controls are CloudFront `https-only` or `redirect-to-https`, an HTTPS-only origin policy, a valid certificate, and TLS from the application to Oracle using TCPS.

## Evidence table for the case study

For each exercise, preserve:

| Stage | Evidence |
|---|---|
| Before | Tool, rule or issue name, affected file/URL, severity, and screenshot/report |
| Change | Reviewed Git diff and short explanation of the chosen control |
| After | Same tool and scope, closed/absent finding, runtime observation, and date |
| Limitations | What the tool could not inspect, such as private CloudFront-origin or ECS-Oracle traffic |

Do not claim that a vulnerability is fixed merely because a scanner is silent. Confirm the expected security property as well: randomized authenticated ciphertext, no key in the repository, and TLS across every relevant network hop.
