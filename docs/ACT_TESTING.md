# 🎭 Act - Local GitHub Actions Testing

This document explains how to use [Act](<https://github.com/nektos/act>) to test
GitHub Actions workflows locally for the Spring Boot + Pekko Stocks Service.

## 📋 Table of Contents

1. [Overview](#-overview)
1. [Prerequisites](#-prerequisites)
1. [Configuration](#️-configuration)
1. [Usage](#-usage)
1. [Available Commands](#-available-commands)
1. [Troubleshooting](#-troubleshooting)
1. [Best Practices](#-best-practices)

## 🎯 Overview

Act allows you to run GitHub Actions locally using Docker containers. This
enables:

- **Fast Feedback**: Test workflow changes without pushing to GitHub
- **Cost Savings**: Reduce GitHub Actions minutes usage
- **Debugging**: Debug workflows locally with full control
- **Offline Development**: Work on CI/CD without internet connectivity

## 📦 Prerequisites

### Required Software

- **Docker**: Container runtime for Act
- **Act**: GitHub Actions local runner
- **Git**: Version control (for workflow context)

### Installation

Act is already installed via Homebrew. If you need to install it elsewhere:

```bash

# macOS (Homebrew)

brew install act

# Linux/macOS (curl)

curl -s <https://raw.githubusercontent.com/nektos/act/master/install.sh> | sudo
bash

# Windows (Chocolatey)

choco install act-cli

```text

## ⚙️ Configuration

### Configuration Files

The project includes several Act configuration files:

#### `.actrc` - Main Configuration

```bash

# Runner images for better compatibility

-P ubuntu-latest=catthehacker/ubuntu:act-latest
-P ubuntu-22.04=catthehacker/ubuntu:act-22.04
-P ubuntu-20.04=catthehacker/ubuntu:act-20.04

# Environment variables

--env JAVA_VERSION=21
--env MAVEN_OPTS=-Xmx2g
--env FINNHUB_API_KEY=test-key-for-local-testing

# Container settings for Apple Silicon

--container-architecture linux/amd64
--verbose

```text

#### `.secrets` - Local Secrets

```bash

# Simulated GitHub secrets for local testing

GITHUB_TOKEN=fake-github-token-for-local-testing
SONAR_TOKEN=fake-sonar-token-for-local-testing
FINNHUB_API_KEY=test-key-for-local-testing

```text

### Environment Variables

Act uses these environment variables for local testing:

| Variable | Purpose | Local Value |
|----------|---------|-------------|
| `JAVA_VERSION` | Java version for builds | `21` |
| `MAVEN_OPTS` | Maven JVM options | `-Xmx2g` |
| `FINNHUB_API_KEY` | Stock API key | `test-key-for-local-testing` |
| `SKIP_SONAR` | Skip SonarCloud analysis | `true` |
| `SKIP_CODECOV` | Skip Codecov upload | `true` |

## 🚀 Usage

### Helper Script

Use the provided helper script for common operations:

```bash

# Show available workflows and jobs

./scripts/act-helper.sh list

# Run specific job types

./scripts/act-helper.sh unit           # Unit tests
./scripts/act-helper.sh integration    # Integration tests (with WireMock)
./scripts/act-helper.sh pr             # PR validation
./scripts/act-helper.sh security       # Security checks
./scripts/act-helper.sh build          # Build job
./scripts/act-helper.sh ci             # Full CI pipeline

# Dry run (validate without execution)

./scripts/act-helper.sh dry-run

# Cleanup containers and images

./scripts/act-helper.sh cleanup

```text

### Direct Act Commands

You can also use Act directly:

```bash

# List all workflows and jobs

act --list

# Run specific job

act --job unit-tests

# Run specific workflow

act --workflows .github/workflows/ci.yml

# Run with specific event

act pull_request --job fast-validation

# Dry run (validate only)

act --job unit-tests --dryrun

# Run with custom secrets

act --job unit-tests --secret-file .secrets

```text

### NPM Scripts

Convenient NPM scripts are available:

```bash
npm run act:list          # List workflows
npm run act:ci            # Run unit tests
npm run act:pr            # Run PR validation
npm run act:build         # Run build job
npm run act:security      # Run security checks
npm run act:integration   # Run integration tests
npm run act:component     # Run component tests
npm run act:all           # Run full CI workflow
npm run act:dry-run       # Dry run validation

```text

## 📋 Available Commands

### Workflow Jobs

| Job | Description | Command |
|-----|-------------|---------|
| `unit-tests` | Run unit tests | `act --job unit-tests` |
| `integration-tests` | Run integration tests | `act --job integration-tests` |
| `component-tests` | Run BDD component tests | `act --job component-tests` |
| `code-quality` | Code quality & security | `act --job code-quality` |
| `build` | Build & package | `act --job build` |
| `fast-validation` | Quick PR validation | `act pull_request --job fast-validation` |

### Workflow Files

| Workflow | Purpose | Command |
|----------|---------|---------|
| `ci.yml` | Continuous Integration | `act --workflows .github/workflows/ci.yml` |
| `pr.yml` | Pull Request Validation | `act pull_request --workflows .github/workflows/pr.yml` |
| `security.yml` | Security Scanning | `act --workflows .github/workflows/security.yml` |
| `deploy-production.yml` | Production Deployment | `act release --workflows .github/workflows/deploy-production.yml` |

## 🔧 Troubleshooting

### Common Issues

#### 1. Authentication Errors

```text
authentication required: Support for password authentication was removed

```text
**Solution**: This is expected for local testing. Actions that require GitHub
authentication will fail, but the workflow structure is validated.

#### 2. Service Container Issues

```text
panic: runtime error: invalid memory address or nil pointer dereference

```text
**Solution**: Act has issues with service containers. Use the helper script
which handles WireMock separately:

```bash
./scripts/act-helper.sh integration

```text

#### 3. Apple Silicon Compatibility

```text
You are using Apple M-series chip and you have not specified container
architecture

```text
**Solution**: Already configured in `.actrc` with `--container-architecture
linux/amd64`.

#### 4. Docker Socket Issues

```text
DOCKER_HOST is set, but socket is invalid '/var/run/docker.sock'

```text
**Solution**: Ensure Docker Desktop is running and accessible.

#### 5. Missing Dependencies

```text
Error: unknown flag: --dry-run

```text
**Solution**: Update Act to the latest version:

```bash
brew upgrade act

```text

### Debugging Tips

1. **Increase Verbosity**:

   ```bash
   act --job unit-tests --verbose
   ```

1. **Check Container Logs**:

   ```bash
   docker logs <container-id>
   ```

1. **Interactive Debugging**:

   ```bash
   act --job unit-tests --bind
   ```

1. **Skip Problematic Steps**:
   Add conditions to workflow steps:

   ```yaml

   - name: Skip in Act
     if: ${{ !env.ACT }}
     run: echo "This runs only in GitHub"
   ```

## 🎯 Best Practices

### 1. Workflow Design

- **Conditional Steps**: Use `if: ${{ !env.ACT }}` for GitHub-only steps
- **Local Alternatives**: Provide local alternatives for external services
- **Environment Variables**: Use environment variables for configuration

### 1. Testing Strategy

- **Start Small**: Test individual jobs before full workflows
- **Use Dry Runs**: Validate workflow syntax with `--dryrun`
- **Mock External Services**: Use WireMock for API dependencies
- **Skip Heavy Operations**: Skip time-consuming operations locally

### 1. Performance Optimization

- **Cache Images**: Reuse Docker images across runs
- **Parallel Jobs**: Test jobs independently when possible
- **Resource Limits**: Set appropriate memory limits for containers

### 1. Security Considerations

- **Fake Secrets**: Use fake values in `.secrets` file
- **No Real Credentials**: Never use real credentials in local testing
- **Gitignore Secrets**: Ensure `.secrets` is in `.gitignore`

## 📊 Workflow Coverage

### Supported Workflows

✅ **Unit Tests**: Full support with Maven
✅ **Code Quality**: Checkstyle and basic security checks
✅ **Build**: Maven package and Docker build
✅ **PR Validation**: Fast validation and compilation

### Limited Support

⚠️ **Integration Tests**: Service container issues, use helper script
⚠️ **Security Scans**: External service dependencies
⚠️ **Deployment**: Requires real credentials and infrastructure

### Not Supported Locally

❌ **SonarCloud**: Requires authentication and cloud service
❌ **Codecov**: Requires authentication and cloud service
❌ **Production Deployment**: Requires real infrastructure

## 🔗 References

- [Act Documentation](<https://github.com/nektos/act>)
- [GitHub Actions Documentation](<https://docs.github.com/en/actions>)
- [Docker Documentation](<https://docs.docker.com/>)
- [Project CI/CD Documentation](./CI_CD.md)
