#!/bin/bash

# Setup script for enhanced git hooks with automatic markdown processing
# This script configures git to use the enhanced pre-commit hooks

set -e

echo "🔧 Setting up enhanced git hooks with automatic markdown processing..."
echo ""

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Error: Not in a git repository!"
    echo "Please run this script from the root of your git repository."
    exit 1
fi

# Check if .githooks directory exists
if [ ! -d ".githooks" ]; then
    echo "❌ Error: .githooks directory not found!"
    echo "Please ensure you're running this from the project root."
    exit 1
fi

# Check if pre-commit hook exists
if [ ! -f ".githooks/pre-commit" ]; then
    echo "❌ Error: Enhanced pre-commit hook not found!"
    echo "Please ensure .githooks/pre-commit exists."
    exit 1
fi

# Make sure the pre-commit hook is executable
echo "📋 Making pre-commit hook executable..."
chmod +x .githooks/pre-commit

# Configure git to use the .githooks directory
echo "⚙️  Configuring git to use enhanced hooks..."
git config core.hooksPath .githooks

# Check if Node.js and npm are available
echo "🔍 Checking dependencies..."
MISSING_DEPS=()

if ! command -v node >/dev/null 2>&1; then
    MISSING_DEPS+=("Node.js")
fi

if ! command -v npm >/dev/null 2>&1; then
    MISSING_DEPS+=("npm")
fi

if [ ${#MISSING_DEPS[@]} -gt 0 ]; then
    echo "⚠️  Warning: Missing dependencies: ${MISSING_DEPS[*]}"
    echo "The pre-commit hook will attempt to install missing packages automatically."
    echo ""
fi

# Install npm dependencies if package.json exists
if [ -f "package.json" ]; then
    echo "📦 Installing npm dependencies..."
    if npm install >/dev/null 2>&1; then
        echo "✅ npm dependencies installed successfully"
    else
        echo "⚠️  Warning: npm install failed, but continuing..."
    fi
else
    echo "⚠️  Warning: package.json not found, skipping npm install"
fi

# Install markdownlint-cli globally if not present
if ! command -v markdownlint >/dev/null 2>&1; then
    echo "📦 Installing markdownlint-cli..."
    if npm install -g markdownlint-cli >/dev/null 2>&1; then
        echo "✅ markdownlint-cli installed successfully"
    else
        echo "⚠️  Warning: Failed to install markdownlint-cli globally"
        echo "The pre-commit hook will attempt to install it locally when needed."
    fi
fi

# Test the pre-commit hook setup
echo ""
echo "🧪 Testing pre-commit hook setup..."

# Create a temporary test file
TEST_FILE="test-markdown-hook.md"
echo "# Test Markdown File" > "$TEST_FILE"
echo "2. Bad numbering" >> "$TEST_FILE"
echo "3. More bad numbering" >> "$TEST_FILE"

# Stage the test file
git add "$TEST_FILE" 2>/dev/null || true

# Test the pre-commit hook (dry run)
echo "Running pre-commit test..."
if .githooks/pre-commit 2>/dev/null; then
    echo "✅ Pre-commit hook test passed!"
else
    echo "⚠️  Pre-commit hook test had issues, but setup is complete"
fi

# Clean up test file
git reset HEAD "$TEST_FILE" 2>/dev/null || true
rm -f "$TEST_FILE" 2>/dev/null || true

echo ""
echo "🎉 Enhanced git hooks setup completed successfully!"
echo ""
echo "📋 What's been configured:"
echo "  ✅ Enhanced pre-commit hook with automatic markdown processing"
echo "  ✅ Git configured to use .githooks directory"
echo "  ✅ Dependencies checked and installed where possible"
echo ""
echo "🚀 Features enabled:"
echo "  📝 Automatic markdown linting and fixing"
echo "  🔧 Advanced markdown processing (8 types of fixes)"
echo "  🧹 Automatic backup cleanup"
echo "  📊 Comprehensive validation and reporting"
echo "  ✨ Zero-configuration automatic formatting"
echo ""
echo "💡 Usage:"
echo "  Just commit normally - the hooks will automatically:"
echo "  1. Fix markdown formatting issues"
echo "  2. Apply advanced fixes (URLs, spacing, lists, etc.)"
echo "  3. Clean up backup files"
echo "  4. Validate all changes"
echo "  5. Stage fixed files automatically"
echo ""
echo "🔧 Manual commands available:"
echo "  npm run docs:fix-advanced    # Run advanced fixes manually"
echo "  npm run docs:clean-backups   # Clean backup files"
echo "  markdownlint docs/*.md       # Check markdown issues"
echo ""
echo "Happy coding! Your markdown will now be automatically perfect! ✨"
