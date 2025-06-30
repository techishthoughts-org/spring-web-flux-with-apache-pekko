# 🚀 GitHub Actions CI/CD Pipeline

This repository uses a comprehensive GitHub Actions CI/CD pipeline designed for
enterprise-grade Spring Boot applications with Pekko Actors.

## 📋 Table of Contents

- [Workflow Overview](#-workflow-overview)
- [Workflows](#️-workflows)
- [Security Configuration](#-security-configuration)
- [Environment Setup](#-environment-setup)
- [Monitoring & Alerting](#-monitoring--alerting)
- [Troubleshooting](#-troubleshooting)

## 🔄 Workflow Overview

Our CI/CD pipeline follows these principles:

- **🔒 Security First**: Every workflow includes security scanning and gates
- **🧪 Comprehensive Testing**: Multi-level testing strategy (Unit → Integration → Component → Smoke → Sanity)
- **📊 Quality Gates**: Code quality, security, and performance validation
- **🚀 Automated Deployment**: Seamless deployment to staging and production
- **📈 Observability**: Monitoring, logging, and alerting throughout the pipeline

## 🛠️ Workflows

### 1. 🔄 Continuous Integration (`ci.yml`)

**Trigger**: Push to `main`/`develop`, Pull Requests

**Pipeline Stages**:

```mermaid
graph LR
    A[Code Quality] --> B[Unit Tests]
    A --> C[Integration Tests]
    A --> D[Component Tests]
    A --> E[Smoke Tests]
    B --> F[Build & Package]
    C --> F
    D --> F
    E --> F
    F --> G[Docker Build]
    G --> H[Security Scan]
    H --> I[Deploy Staging]
    I --> J[Performance Tests]
    J --> K[Mutation Tests]

```text
**Key Features**:

- ✅ Parallel test execution for faster feedback
- 📊 Comprehensive test reporting with JUnit/Cucumber
- 🔒 Security scanning with OWASP, Trivy, and CodeQL
- 📈 Code coverage reporting with JaCoCo
- 🐳 Docker image building and security scanning
- 🚀 Automatic deployment to staging (main branch only)

### 2. 🔍 Pull Request Validation (`pr.yml`)

**Trigger**: Pull Request creation/updates

**Pipeline Stages**:

```mermaid
graph LR
    A[Fast Validation] --> B[Code Analysis]
    A --> C[Integration Tests]
    A --> D[Component Tests]
    B --> E[Build Verification]
    C --> E
    D --> E
    E --> F[Docker Test]
    F --> G[Documentation Check]
    G --> H[Dependency Review]
    H --> I[PR Summary]

```text
**Key Features**:

- ⚡ Fast feedback with essential checks first
- 📊 Automated PR summary with test results
- 📈 Coverage reporting and quality gate enforcement
- 👥 Automatic reviewer assignment based on changed files
- 🔒 Security and dependency vulnerability scanning
- 📚 Documentation validation and link checking

### 3. 🔒 Security Scanning (`security.yml`)

**Trigger**: Daily scheduled runs, Push to `main`, Security-related PR changes

**Security Checks**:

- 🔍 **CodeQL**: Static code analysis for security vulnerabilities
- 📦 **OWASP Dependency Check**: Known vulnerability scanning
- 🐳 **Container Security**: Docker image vulnerability scanning
- 🔐 **Secrets Scanning**: Detection of exposed secrets/credentials
- 📊 **Security Reporting**: Comprehensive security dashboard

### 4. 🏷️ Automatic Tagging (`auto-tag.yml`)

**Trigger**: Push to `main` branch

**Release Process**:

```mermaid
graph LR
    A[Push to Main] --> B[Checkout & Setup Java]
    B --> C[Run Tests]
    C --> D[Extract Version from pom.xml]
    D --> E[Check if Tag Exists]
    E --> F{Tag Exists?}
    F -->|No| G[Create & Push Tag]
    F -->|Yes| H[Skip Tag Creation]
    G --> I[Tag Available for Release]

```text
**Key Features**:

- 🏷️ **Version-based tagging**: Uses Maven project version from `pom.xml`
- 🔒 **Quality assurance**: Runs full test suite before tagging
- 📦 **Automatic packaging**: Builds JAR and Docker artifacts
- 🚀 **GitHub Releases**: Creates releases with downloadable assets
- ⚡ **Duplicate prevention**: Checks for existing tags to avoid conflicts
- 🤖 **Fully automated**: No manual intervention required
- 📋 **Smart naming**: Creates tags with `v` prefix (e.g., `v1.0.0`)

**Release Workflow**:
1. **Code Quality**: All tests must pass before tag creation
1. **Version Detection**: Automatically extracts version from Maven `pom.xml`
1. **Tag Validation**: Prevents duplicate tags and conflicts
1. **Package Building**: Creates JAR file and Docker image
1. **Release Creation**: Publishes GitHub release with artifacts
1. **Asset Upload**: Attaches JAR and Docker tar files

### 5. 🗑️ Auto Delete Merged Branches (`auto-delete-branch.yml`)

**Trigger**: Pull Request closure (when merged)

**Branch Cleanup Process**:

```mermaid
graph LR
    A[PR Merged] --> B{Branch != main?}
    B -->|Yes| C[Delete Branch]
    B -->|No| D[Skip Deletion]
    C --> E[Add PR Comment]
    E --> F[Log Cleanup]

```text
**Key Features**:

- 🧹 **Automatic cleanup**: Deletes merged feature branches
- 🔒 **Protected branches**: Never deletes `main` or `master`
- 💬 **PR notifications**: Comments on PR about branch deletion
- 📊 **Logging**: Comprehensive cleanup logging
- ⚡ **Error handling**: Graceful handling of already-deleted branches

### 6. 🚀 Production Deployment (`deploy-production.yml`)

**Trigger**: GitHub Release creation, Manual workflow dispatch

**Deployment Stages**:

```mermaid
graph LR
    A[Security Gate] --> B[Build Production Image]
    B --> C[Deploy to Production]
    C --> D[Post-deployment Tests]
    D --> E[Setup Monitoring]
    E --> F[Create Release Notes]

    G[Rollback] -.-> C
    G -.-> |"On Failure"| H[Notification]

```text
**Key Features**:

- 🔒 Pre-deployment security validation
- 🏗️ Production-ready Docker image building
- 🚀 Blue-green deployment to Kubernetes
- 🧪 Comprehensive post-deployment validation
- 📊 Automatic monitoring and alerting setup
- 🔄 Automated rollback on failure
- 📦 Release notes generation

## 🔒 Security Configuration

### Required Secrets

Configure these secrets in your GitHub repository settings:

#### 🔐 Authentication & Access

```bash
GITHUB_TOKEN          # GitHub API access (auto-provided)
SONAR_TOKEN           # SonarCloud integration
FOSSA_API_KEY         # License compliance scanning

```text

#### 📧 Notifications

```bash
SLACK_WEBHOOK         # Slack notifications
EMAIL_USERNAME        # Email notifications
EMAIL_PASSWORD        # Email authentication
NOTIFICATION_EMAIL    # Target email for alerts
SECURITY_EMAIL        # Security team notifications

```text

#### ☁️ Deployment

```bash
KUBE_CONFIG_STAGING   # Kubernetes config for staging (base64 encoded)
KUBE_CONFIG_PROD      # Kubernetes config for production (base64 encoded)

```text

#### 🔑 External Services

```bash
FINNHUB_API_KEY       # Stock data API (for integration tests)

```text

### Security Best Practices

1. **🔐 Secret Management**: All secrets are encrypted and never logged
1. **🛡️ Least Privilege**: Workflows use minimum required permissions
1. **🔒 Signed Commits**: Verification of commit signatures
1. **📊 Audit Trail**: Complete audit trail of all deployments
1. **🚨 Threat Detection**: Automated vulnerability scanning

## 🌍 Environment Setup

### GitHub Environments

Configure these environments in your repository:

#### 📍 Staging Environment

```yaml
Name: staging
URL: <https://stocks-staging.example.com>
Protection Rules:

  - Required reviewers: 1
  - Wait timer: 0 minutes
  - Deployment branch: main

```text

#### 📍 Production Environment

```yaml
Name: production
URL: <https://stocks-api.example.com>
Protection Rules:

  - Required reviewers: 2
  - Wait timer: 30 minutes
  - Deployment branch: main
  - Restrict to protected branches: true

```text

#### 📍 Production Rollback Environment

```yaml
Name: production-rollback
Protection Rules:

  - Required reviewers: 1 (emergency access)
  - Wait timer: 0 minutes

```text

### Branch Protection Rules

Configure branch protection for `main` and `develop`:

```yaml
main:
  required_status_checks:

    - Fast Validation
    - Code Analysis
    - Integration Tests
    - Component Tests
    - Build Verification
  enforce_admins: true
  required_pull_request_reviews:
    required_approving_review_count: 2
    dismiss_stale_reviews: true
    require_code_owner_reviews: true
  restrictions:
    users: []
    teams: [senior-developers, tech-leads]

```text

## 📊 Monitoring & Alerting

### Notification Channels

#### 📢 Slack Integration

- **Channel**: `#ci-cd`
- **Notifications**: Build failures, deployment status, security alerts
- **Format**: Rich messages with build details and links

#### 📧 Email Notifications

- **Security Issues**: Immediate alerts to security team
- **Deployment Failures**: Notifications to on-call team
- **Critical Errors**: Escalation to engineering management

### Monitoring Dashboards

#### 📊 GitHub Actions Insights

- **Build Success Rate**: Track pipeline reliability
- **Build Duration**: Monitor performance trends
- **Test Coverage**: Ensure quality maintenance
- **Deployment Frequency**: Measure delivery velocity

#### 🔒 Security Metrics

- **Vulnerability Trends**: Track security posture
- **Dependency Health**: Monitor outdated dependencies
- **Secret Scanning**: Detect exposed credentials
- **Compliance Status**: Ensure regulatory requirements

## 🏷️ Release Process

Our automated release process ensures consistent, high-quality releases through
version-controlled tagging and streamlined deployment.

### 📋 Release Workflow

#### 1. **Development & Integration**

```bash

# Make changes on feature branches

git checkout -b feature/new-functionality

# ... make changes ...

git commit -m "feat: add new functionality"
git push origin feature/new-functionality

# Create pull request and merge to main after review

```text

#### 2. **Automatic Tagging & Packaging**

Once changes are merged to `main`:

- ✅ Auto-tag workflow triggers automatically
- 🧪 Complete test suite runs to ensure quality
- 📦 Version extracted from `pom.xml` (`<version>1.0.0-SNAPSHOT</version>`)
- 🏗️ JAR package built (`stocks-demo-1.0.0-SNAPSHOT.jar`)
- 🐳 Docker image created and saved (`stocks-service-v1.0.0-SNAPSHOT.tar`)
- 🏷️ Git tag created: `v1.0.0-SNAPSHOT`
- 🚀 GitHub Release published with downloadable artifacts

#### 3. **Version Management**

**For Development Releases**:

```xml
<!-- pom.xml -->
<version>1.0.0-SNAPSHOT</version>
<!-- Results in tag: v1.0.0-SNAPSHOT -->

```text
**For Production Releases**:

```xml
<!-- pom.xml -->
<version>1.0.0</version>
<!-- Results in tag: v1.0.0 -->

```text
**Update version for next development cycle**:

```bash

# After release, update to next version

./mvnw versions:set -DnewVersion=1.1.0-SNAPSHOT
git add pom.xml
git commit -m "chore: bump version to 1.1.0-SNAPSHOT"
git push origin main

```text

#### 4. **Release Artifacts**

**Automatic Release Creation**: GitHub releases are created automatically with:

- **📦 JAR Package**: Ready-to-run Spring Boot application
- **🐳 Docker Image**: Container image saved as tar file
- **📋 Release Notes**: Auto-generated with features and documentation links
- **📚 Source Code**: Automatic source code archives (zip/tar.gz)

**Manual Release Commands** (if needed):

```bash

# View releases

gh release list

# Download specific release assets

gh release download v1.0.0

# Create manual release (emergency only)

gh release create v1.0.0 \
  --title "Release v1.0.0" \
  --notes-file RELEASE_NOTES.md \
  --generate-notes

```text

### 🎯 Release Best Practices

#### **Semantic Versioning**

Follow [SemVer](<https://semver.org/>) guidelines:

- `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

#### **Version Lifecycle**

```text
1.0.0-SNAPSHOT → 1.0.0 → 1.1.0-SNAPSHOT → 1.1.0 → 2.0.0-SNAPSHOT
      ↓              ↓           ↓              ↓           ↓
  Development    Release    Development    Release    Development

```text

#### **Release Checklist**

- [ ] All tests pass in CI/CD pipeline
- [ ] Code review completed and approved
- [ ] Documentation updated
- [ ] Version bumped in `pom.xml`
- [ ] CHANGELOG.md updated (if applicable)
- [ ] Release notes prepared

### 🔍 Monitoring Releases

#### **Check Tag Creation**

```bash

# List recent tags

git tag --sort=-version:refname | head -10

# Check if auto-tag workflow succeeded

gh run list --workflow=auto-tag.yml --limit=5

# View specific auto-tag run

gh run view <run-id> --log

```text

#### **Verify Release Artifacts**

```bash

# List releases

gh release list

# View specific release

gh release view v1.0.0

# Download release assets

gh release download v1.0.0

```text

### 🚨 Release Troubleshooting

#### **Tag Already Exists**

```bash

# If you need to recreate a tag

git tag -d v1.0.0           # Delete local tag
git push origin :v1.0.0     # Delete remote tag

# Then push to main again to trigger auto-tagging

```text

#### **Failed Auto-Tagging**

```bash

# Check workflow logs

gh run list --workflow=auto-tag.yml --limit=1 --status=failure
gh run view <failed-run-id> --log

# Common issues:


# - Test failures: Fix tests and push again


# - Version conflicts: Ensure pom.xml version is unique


# - Permission issues: Check repository settings

```text

#### **Manual Tag Creation** (Emergency)

```bash

# Only if auto-tagging fails and manual intervention needed

git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

```text
---

## 🔧 Troubleshooting

### Common Issues

#### 🚨 Build Failures

**Problem**: Tests failing intermittently

```bash

# Check test logs

gh run list --workflow=ci.yml --limit=10
gh run view <run-id> --log

```text
**Solution**: Review test reports and fix flaky tests

#### 🔒 Security Scan Failures

**Problem**: Dependency vulnerabilities detected

```bash

# Check OWASP report

gh run download <run-id> --name owasp-dependency-check-report

```text
**Solution**: Update vulnerable dependencies

#### 🚀 Deployment Issues

**Problem**: Kubernetes deployment failing

```bash

# Check deployment logs

kubectl logs -n stocks-production deployment/stocks-service
kubectl describe deployment stocks-service -n stocks-production

```text
**Solution**: Verify configuration and resource availability

### Debug Commands

#### 📝 Check Workflow Status

```bash

# List recent workflow runs

gh run list --workflow=ci.yml --limit=5

# View specific run details

gh run view <run-id>

# Download artifacts

gh run download <run-id>

```text

#### 🔍 Investigate Failures

```bash

# View workflow logs

gh run view <run-id> --log

# Check specific job

gh run view <run-id> --job=<job-name> --log

# Re-run failed jobs

gh run rerun <run-id> --failed

```text

#### 🚀 Manual Deployment

```bash

# Trigger manual deployment

gh workflow run deploy-production.yml \
  --field environment=staging \
  --field version=v1.0.0

```text

### Support Contacts

- **🔧 CI/CD Issues**: @devops-team
- **🔒 Security Concerns**: @security-team
- **🧪 Test Failures**: @qa-team
- **🚀 Deployment Problems**: @infrastructure-team

## 📈 Metrics and KPIs

### Pipeline Performance

- **⚡ Average Build Time**: < 15 minutes
- **✅ Build Success Rate**: > 95%
- **🔄 Deployment Frequency**: Multiple times per day
- **⏱️ Lead Time**: < 2 hours from commit to production

### Quality Metrics

- **📊 Test Coverage**: > 80%
- **🔒 Security Scan Pass Rate**: 100%
- **🐛 Defect Escape Rate**: < 5%
- **🔄 Rollback Rate**: < 2%

---

## 🤝 Contributing

When contributing to this repository:

1. **📝 Follow the PR template** for consistent reviews
1. **✅ Ensure all checks pass** before requesting review
1. **📊 Monitor coverage reports** and maintain quality
1. **🔒 Address security issues** immediately
1. **📚 Update documentation** for workflow changes

For detailed development guidelines, see our [Development
Guide](../docs/GETTING_STARTED.md).

---

**🚀 Happy Coding! The CI/CD pipeline is here to help you deliver high-quality
software efficiently and securely.**
