# 🤖 Dependabot Auto-Merge Setup Guide

This guide helps you configure Dependabot for automatic merging of dependency
updates.

## 📋 Prerequisites

- Repository admin access
- Dependabot enabled on the repository
- GitHub Actions enabled

## 🔧 Manual Setup Instructions

### 1. 📊 Configure Repository Settings

Go to your GitHub repository settings and configure the following:

#### **🔀 General → Pull Requests**

- ✅ **Allow auto-merge**
- ✅ **Allow squash merging**
- ❌ **Allow merge commits** (optional, but recommended to disable)
- ❌ **Allow rebase merging** (optional, but recommended to disable)

#### **🏷️ Issues → Labels**

Create a new label:

- **Name**: `auto-merge`
- **Description**: `Pull requests that can be automatically merged`
- **Color**: `#0E8A16` (green)

### 2. 🛡️ Branch Protection Rules

Configure branch protection for the `main` branch:

#### **🔒 Protection Rules**

1. Go to **Settings → Branches**
1. Add rule for `main` branch:

   - ✅ **Require a pull request before merging**
   - ❌ **Require approvals: 0** (disabled for auto-merge to work)
   - ❌ **Dismiss stale PR approvals when new commits are pushed** (not needed)
   - ✅ **Require status checks to pass before merging**
   - ✅ **Require branches to be up to date before merging**
   - **Required status checks**: Add these exact names:
     - `🧪 Unit Tests`
     - `🔍 Fast Validation`
     - `🏗️ Build Verification`
   - ✅ **Restrict pushes that create files matching**
   - ✅ **Allow auto-merge**

### 3. 🎯 How Auto-Merge Works

Once configured, the system will:

#### **✅ Automatic Merge**

- **Patch updates** (1.0.0 → 1.0.1): Auto-merge enabled (no approval needed)
- **Minor updates** (1.0.0 → 1.1.0): Auto-merge enabled (no approval needed)

#### **🚨 Manual Review Required**

- **Major updates** (1.0.0 → 2.0.0): Requires manual approval & merge

#### **🔄 Workflow Process**

```text
Dependabot PR → Enable Auto-Merge → Tests Pass → Merged ✅
                     ↓
                Major Update → Manual Review Required → Manual Approval

```text
**ℹ️ Note**: GitHub Actions cannot approve PRs using the default token, so we
use direct auto-merge for patch/minor updates and manual review for major
updates.

## 🔍 Monitoring Auto-Merge

### **Check Dependabot Activity**

```bash

# List Dependabot PRs

gh pr list --author="app/dependabot"

# Check auto-merge workflow runs

gh run list --workflow="dependabot-auto-merge.yml"

# View specific workflow run

gh run view <run-id>

```text

### **Control Auto-Merge**

```bash

# Disable auto-merge for a specific PR

gh pr edit <PR-NUMBER> --remove-label="auto-merge"

# Re-enable auto-merge for a PR

gh pr edit <PR-NUMBER> --add-label="auto-merge"

# Manually merge a PR

gh pr merge <PR-NUMBER> --squash

```text

## 🚨 Troubleshooting

### **Auto-Merge Not Working**

1. **Check branch protection**: Ensure `main` branch has correct protection
rules
1. **Verify status checks**: All required checks must pass
1. **Check labels**: PR should have `auto-merge` label
1. **Review workflow logs**: Check Actions tab for errors

### **PRs Not Auto-Merged**

1. **Verify workflow**: Check `.github/workflows/dependabot-auto-merge.yml`
exists
1. **Check permissions**: Workflow needs `contents: write` and `pull-requests:
write`
1. **Review actor**: Workflow should only run for `dependabot[bot]`
1. **Status checks**: All required CI checks must pass before auto-merge
1. **Branch protection**: Ensure approvals are disabled for auto-merge to work

### **Major Updates Not Flagged**

1. **Check metadata action**: Ensure `dependabot/fetch-metadata@v2` is working
1. **Review update type**: Major updates should be marked as
`version-update:semver-major`

## 📊 Expected Behavior

### **Weekly Schedule**

- **Monday 06:00**: Dependabot creates dependency update PRs
- **Immediate**: Auto-merge workflow processes new PRs
- **After tests pass**: Safe updates are automatically merged

### **Update Classification**

| Type | Example | Action |
|------|---------|--------|
| Patch | 1.0.0 → 1.0.1 | ✅ Auto-merge |
| Minor | 1.0.0 → 1.1.0 | ✅ Auto-merge |
| Major | 1.0.0 → 2.0.0 | 🚨 Manual review |

## 🎉 Success Indicators

- ✅ Dependabot PRs created weekly
- ✅ Safe updates auto-approved within minutes
- ✅ Auto-merge enabled after approval
- ✅ PRs merged automatically after tests pass
- ✅ Major updates flagged for manual review

---

## 🤝 Alternative: Repository Admin Setup

If you have repository admin access, you can use the automated script:

```bash

# Run the setup script (requires admin access)

./scripts/setup-dependabot-automerge.sh

```text
**Note**: This requires authentication with an account that has admin access to
the repository.
