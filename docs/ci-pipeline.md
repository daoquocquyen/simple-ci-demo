# CI Pipeline Documentation – Spring Boot Application

This document describes the Jenkins CI pipeline used for building, testing, and releasing the Spring Boot application.
It outlines the purpose of each stage, the best practices applied, and which stages run depending on the branch type.

---

## Branch Execution Matrix

| Stage                                    | Feature branch (no PR)  | Pull Request | `main` branch |
|------------------------------------------|-------------------------|--------------|---------------|
| Cleanup & Checkout                       | YES                     | YES          | YES           |
| Warm Maven cache                         | YES                     | YES          | YES           |
| Static Analysis (Checkstyle/PMD/SpotBugs)| YES                     | YES          | YES           |
| Secrets Scan (Gitleaks)                  | YES                     | YES          | YES           |
| Build, Unit Tests & Coverage             | YES                     | YES          | YES           |
| Dependency Audit (OWASP)                 | YES                     | YES          | YES           |
| Integration Tests                        | NO                      | YES          | YES           |
| Build Image                              | NO                      | NO           | YES           |
| Trivy Scan & Image SBOM                  | NO                      | NO           | YES           |
| Push Image                               | NO                      | NO           | YES           |

---

## General Best Practices applied

- **Fail fast, fail cheap:** Run static analysis and secrets scans in parallel before builds and tests.
- **Use per-stage containers:** Each stage runs in a Docker container pinned to a specific image in order to ensure consistency and isolation.
- **Cache dependencies:** Persist Maven and Trivy caches to reduce build times.
- **Everything as code:** Jenkinsfile and tool configurations are stored in source control.
- **Reproducibility:** Build artifacts and images are tied to Git SHAs and tagged immutably.
- **Supply-chain security:** Run dependency audits, vulnerability scans, and generate SBOMs.
- **Secrets hygiene:** Access credentials only through Jenkins credentials bindings.
- **Observability:** Archive all reports and enforce timeouts on long-running stages.

Note: In production, the image should include an attached SBOM and a signature. This demo does not implement those steps.
