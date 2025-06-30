#!/bin/bash

# Markdown Auto-Fix Script
# This script automatically fixes common markdown linting issues

set -e

DOCS_DIR="docs"
CONFIG_FILE=".markdownlint.json"

echo "🔧 Markdown Auto-Fix Script"
echo "============================"

# Create markdownlint config if it doesn't exist
if [ ! -f "$CONFIG_FILE" ]; then
    echo "📝 Creating markdownlint configuration..."
    cat > "$CONFIG_FILE" << 'EOF'
{
  "MD013": {
    "line_length": 80,
    "headings": false,
    "tables": false,
    "code_blocks": false
  },
  "MD040": true,
  "MD022": true,
  "MD032": true,
  "MD058": true,
  "MD031": true,
  "MD034": false,
  "MD033": false
}
EOF
fi

# Function to fix long lines by adding line breaks
fix_long_lines() {
    local file="$1"
    echo "  📏 Fixing long lines in $file..."

    # Use awk to wrap long lines at word boundaries
    awk '
    {
        if (length($0) > 80 && $0 !~ /^\s*\|/ && $0 !~ /^```/ && $0 !~ /^\s*[-*]/ && $0 !~ /^#/) {
            # Split long lines at word boundaries
            line = $0
            while (length(line) > 80) {
                # Find the last space before position 80
                pos = 80
                while (pos > 0 && substr(line, pos, 1) != " ") {
                    pos--
                }
                if (pos > 0) {
                    print substr(line, 1, pos-1)
                    line = substr(line, pos+1)
                } else {
                    # No space found, break at 80 characters
                    print substr(line, 1, 80)
                    line = substr(line, 81)
                }
            }
            if (length(line) > 0) print line
        } else {
            print $0
        }
    }' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
}

# Function to add language to fenced code blocks
fix_code_blocks() {
    local file="$1"
    echo "  🔤 Adding language specifications to code blocks in $file..."

    # Add 'text' to bare code blocks that don't have language specified
    sed -i.bak 's/^```$/```text/g' "$file" && rm -f "$file.bak"
}

# Function to add blank lines around headings and lists
fix_spacing() {
    local file="$1"
    echo "  📐 Fixing spacing around headings and lists in $file..."

    # Add blank lines before and after headings, lists, and tables
    awk '
    BEGIN { prev_blank = 1 }
    {
        # Current line is a heading
        if ($0 ~ /^#{1,6} /) {
            if (!prev_blank) print ""
            print $0
            if (getline > 0) {
                if ($0 != "") print ""
                print $0
            }
            prev_blank = ($0 == "")
            next
        }
        # Current line starts a list
        else if ($0 ~ /^\s*[-*+] / || $0 ~ /^\s*[0-9]+\. /) {
            if (!prev_blank && prev_line !~ /^\s*[-*+] / && prev_line !~ /^\s*[0-9]+\. /) print ""
            print $0
            prev_blank = 0
        }
        # Current line is a table
        else if ($0 ~ /^\|.*\|$/) {
            if (!prev_blank) print ""
            print $0
            prev_blank = 0
        }
        else {
            print $0
            prev_blank = ($0 == "")
        }
        prev_line = $0
    }' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
}

# Function to convert bare URLs to proper markdown links
fix_bare_urls() {
    local file="$1"
    echo "  🔗 Converting bare URLs to proper markdown links in $file..."

    # Convert bare URLs to proper markdown link format
    sed -i.bak -E 's|([^<])(https?://[^[:space:]>)]+)||\1<\2>|g' "$file" && rm -f "$file.bak"
}

# Main processing function
process_file() {
    local file="$1"
    echo "🔧 Processing $file..."

    # Create backup
    cp "$file" "$file.backup"

    # Apply fixes
    fix_code_blocks "$file"
    fix_bare_urls "$file"
    fix_spacing "$file"
    # Note: Commenting out long line fixes as they can break formatting
    # fix_long_lines "$file"

    echo "✅ Completed processing $file"
}

# Check if docs directory exists
if [ ! -d "$DOCS_DIR" ]; then
    echo "❌ Error: $DOCS_DIR directory not found!"
    exit 1
fi

# Process all markdown files
echo "🚀 Starting markdown auto-fix..."
echo ""

for file in "$DOCS_DIR"/*.md; do
    if [ -f "$file" ]; then
        process_file "$file"
        echo ""
    fi
done

# Run markdownlint with --fix to handle remaining issues
echo "🔧 Running markdownlint --fix..."
if command -v markdownlint >/dev/null 2>&1; then
    markdownlint --fix "$DOCS_DIR"/*.md || true
else
    echo "⚠️  markdownlint not found. Install with: npm install -g markdownlint-cli"
fi

echo ""
echo "✅ Markdown auto-fix completed!"
echo "📁 Backup files created with .backup extension"
echo "🧹 To clean up backups: rm -f $DOCS_DIR/*.backup"
echo ""
echo "🔍 To check remaining issues:"
echo "   markdownlint $DOCS_DIR/*.md"
