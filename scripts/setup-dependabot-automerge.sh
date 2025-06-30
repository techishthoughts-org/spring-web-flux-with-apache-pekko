#!/bin/bash

# 🤖 Setup Dependabot Auto-merge Configuration
# This script helps configure GitHub repository settings for automatic merging of Dependabot PRs

set -e

echo "🤖 Setting up Dependabot Auto-merge Configuration"
echo "================================================"

# Check if GitHub CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed. Please install it first:"
    echo "   https://cli.github.com/"
    exit 1
fi

# Check if user is authenticated with GitHub CLI
if ! gh auth status &> /dev/null; then
    echo "❌ Not authenticated with GitHub CLI. Please run: gh auth login"
    exit 1
fi

# Get repository info
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "🏠 Repository: $REPO"

echo ""
echo "🔧 Configuring branch protection rules for auto-merge..."

# Enable branch protection for main branch with required status checks
gh api repos/$REPO/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"checks":[{"context":"🧪 Unit Tests"},{"context":"🔍 Fast Validation"},{"context":"🏗️ Build Verification"}]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"required_approving_review_count":1,"dismiss_stale_reviews":true,"require_code_owner_reviews":false}' \
  --field restrictions=null \
  --field allow_auto_merge=true \
  --field allow_squash_merge=true \
  --field allow_merge_commit=false \
  --field allow_rebase_merge=false

echo "✅ Branch protection configured for main branch"

echo ""
echo "🏷️ Creating auto-merge label..."

# Create auto-merge label if it doesn't exist
gh label create "auto-merge" \
  --description "Pull requests that can be automatically merged" \
  --color "0E8A16" || echo "ℹ️  Label 'auto-merge' already exists"

echo ""
echo "🤖 Dependabot Auto-merge Setup Complete!"
echo "========================================"
echo ""
echo "📋 Configuration Summary:"
echo "  ✅ Branch protection enabled for main branch"
echo "  ✅ Required status checks configured"
echo "  ✅ Auto-merge enabled for the repository"
echo "  ✅ Auto-merge label created"
echo "  ✅ Dependabot workflow will automatically:"
echo "     • Approve patch and minor version updates"
echo "     • Enable auto-merge for approved updates"
echo "     • Require manual review for major updates"
echo ""
echo "🚀 Next Dependabot PRs will be automatically merged!"
echo ""
echo "💡 Tips:"
echo "  • Monitor the Actions tab for auto-merge workflow runs"
echo "  • Major version updates will still require manual approval"
echo "  • You can disable auto-merge for specific PRs by removing the 'auto-merge' label"
