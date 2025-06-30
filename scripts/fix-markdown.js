#!/usr/bin/env node

/**
 * Advanced Markdown Auto-Fix Script
 * Uses markdownlint API for sophisticated markdown corrections
 */

const fs = require('fs');
const path = require('path');
const { promisify } = require('util');

const readFile = promisify(fs.readFile);
const writeFile = promisify(fs.writeFile);
const readdir = promisify(fs.readdir);
const stat = promisify(fs.stat);

// Configuration for markdownlint
const config = {
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
};

class MarkdownFixer {
  constructor() {
    this.docsDir = 'docs';
    this.fixedCount = 0;
    this.processedFiles = [];
  }

  async fixFile(filePath) {
    console.log(`🔧 Processing ${filePath}...`);

    try {
      const content = await readFile(filePath, 'utf8');
      const originalContent = content;

      let fixedContent = content;

      // Fix 1: Add language to bare code blocks
      fixedContent = this.fixCodeBlocks(fixedContent);

      // Fix 2: Convert bare URLs to proper markdown links
      fixedContent = this.fixBareUrls(fixedContent);

      // Fix 3: Add blank lines around headings
      fixedContent = this.fixHeadingSpacing(fixedContent);

      // Fix 4: Add blank lines around tables
      fixedContent = this.fixTableSpacing(fixedContent);

      // Fix 5: Add blank lines around lists
      fixedContent = this.fixListSpacing(fixedContent);

      // Fix 6: Add blank lines around fenced code blocks
      fixedContent = this.fixCodeBlockSpacing(fixedContent);

      // Fix 7: Wrap long lines (carefully)
      fixedContent = this.fixLongLines(fixedContent);

      // Fix 8: Fix ordered list prefixes (MD029)
      fixedContent = this.fixOrderedListPrefixes(fixedContent);

      if (fixedContent !== originalContent) {
        // Create backup
        await writeFile(`${filePath}.backup`, originalContent);

        // Write fixed content
        await writeFile(filePath, fixedContent);

        console.log(`✅ Fixed ${path.basename(filePath)}`);
        this.fixedCount++;
      } else {
        console.log(`ℹ️  No fixes needed for ${path.basename(filePath)}`);
      }

      this.processedFiles.push(filePath);

    } catch (error) {
      console.error(`❌ Error processing ${filePath}:`, error.message);
    }
  }

  fixCodeBlocks(content) {
    // Replace bare ``` with ```text
    return content.replace(/^```\s*$/gm, '```text');
  }

  fixBareUrls(content) {
    // Convert bare URLs to angle bracket format
    return content.replace(/([^<\[])(https?:\/\/[^\s>)\]]+)/g, '$1<$2>');
  }

  fixHeadingSpacing(content) {
    const lines = content.split('\n');
    const result = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const prevLine = i > 0 ? lines[i - 1] : '';
      const nextLine = i < lines.length - 1 ? lines[i + 1] : '';

      // If current line is a heading
      if (/^#{1,6}\s/.test(line)) {
        // Add blank line before heading if previous line is not blank
        if (prevLine.trim() !== '' && i > 0) {
          result.push('');
        }
        result.push(line);
        // Add blank line after heading if next line is not blank
        if (nextLine.trim() !== '' && i < lines.length - 1) {
          result.push('');
        }
      } else {
        result.push(line);
      }
    }

    return result.join('\n');
  }

  fixTableSpacing(content) {
    const lines = content.split('\n');
    const result = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const prevLine = i > 0 ? lines[i - 1] : '';
      const nextLine = i < lines.length - 1 ? lines[i + 1] : '';

      // If current line is a table row
      if (/^\|.*\|$/.test(line)) {
        // Add blank line before table if previous line is not blank or table
        if (prevLine.trim() !== '' && !/^\|.*\|$/.test(prevLine) && i > 0) {
          result.push('');
        }
        result.push(line);
        // Add blank line after table if next line is not blank or table
        if (nextLine.trim() !== '' && !/^\|.*\|$/.test(nextLine) && i < lines.length - 1) {
          result.push('');
        }
      } else {
        result.push(line);
      }
    }

    return result.join('\n');
  }

  fixListSpacing(content) {
    const lines = content.split('\n');
    const result = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const prevLine = i > 0 ? lines[i - 1] : '';

      // If current line starts a list
      if (/^\s*[-*+]\s/.test(line) && i > 0) {
        // Add blank line before list if previous line is not blank or list item
        if (prevLine.trim() !== '' && !/^\s*[-*+]\s/.test(prevLine)) {
          result.push('');
        }
      }

      result.push(line);
    }

    return result.join('\n');
  }

  fixCodeBlockSpacing(content) {
    const lines = content.split('\n');
    const result = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const prevLine = i > 0 ? lines[i - 1] : '';
      const nextLine = i < lines.length - 1 ? lines[i + 1] : '';

      // If current line starts a fenced code block
      if (/^```/.test(line)) {
        // Add blank line before code block if previous line is not blank
        if (prevLine.trim() !== '' && i > 0) {
          result.push('');
        }
        result.push(line);
      }
      // If current line ends a fenced code block
      else if (line === '```') {
        result.push(line);
        // Add blank line after code block if next line is not blank
        if (nextLine && nextLine.trim() !== '' && i < lines.length - 1) {
          result.push('');
        }
      } else {
        result.push(line);
      }
    }

    return result.join('\n');
  }

  fixLongLines(content) {
    const lines = content.split('\n');
    const result = [];

    for (const line of lines) {
      // Skip tables, code blocks, and headings
      if (/^\|.*\|$/.test(line) ||
          /^```/.test(line) ||
          /^#{1,6}\s/.test(line) ||
          /^\s*[-*+]\s/.test(line)) {
        result.push(line);
        continue;
      }

      if (line.length > 80) {
        // Simple word wrap at 80 characters
        const words = line.split(' ');
        let currentLine = '';

        for (const word of words) {
          if ((currentLine + ' ' + word).length > 80 && currentLine.length > 0) {
            result.push(currentLine);
            currentLine = word;
          } else {
            currentLine = currentLine ? currentLine + ' ' + word : word;
          }
        }

        if (currentLine) {
          result.push(currentLine);
        }
      } else {
        result.push(line);
      }
    }

    return result.join('\n');
  }

  fixOrderedListPrefixes(content) {
    const lines = content.split('\n');
    const result = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      // Check if this line is an ordered list item
      const orderedListMatch = line.match(/^(\s*)(\d+)\.\s+(.*)$/);

      if (orderedListMatch) {
        const [, indent, , text] = orderedListMatch;
        // Convert all ordered list items to use "1." for auto-numbering
        // This avoids MD029 issues with sequential numbering
        result.push(`${indent}1. ${text}`);
      } else {
        result.push(line);
      }
    }

    return result.join('\n');
  }

  async findMarkdownFiles(dir) {
    const files = [];
    const entries = await readdir(dir);

    for (const entry of entries) {
      const fullPath = path.join(dir, entry);
      const stats = await stat(fullPath);

      if (stats.isDirectory()) {
        // Recursively process subdirectories
        const subFiles = await this.findMarkdownFiles(fullPath);
        files.push(...subFiles);
      } else if (entry.endsWith('.md')) {
        files.push(fullPath);
      }
    }

    return files;
  }

  async processDirectory() {
    try {
      const markdownFiles = await this.findMarkdownFiles(this.docsDir);

      // Also include README.md if it exists
      const readmePath = 'README.md';
      try {
        await stat(readmePath);
        markdownFiles.push(readmePath);
      } catch (error) {
        // README.md doesn't exist, skip it
      }

      console.log(`📁 Found ${markdownFiles.length} markdown files in ${this.docsDir}/ and root (including subdirectories)`);
      console.log('');

      for (const filePath of markdownFiles) {
        await this.fixFile(filePath);
      }

      console.log('');
      console.log(`✅ Processing completed!`);
      console.log(`📊 Fixed ${this.fixedCount} out of ${markdownFiles.length} files`);
      console.log(`📁 Backup files created with .backup extension`);
      console.log('');
      console.log('🧹 To clean up backups:');
      console.log(`   find ${this.docsDir} -name "*.backup" -delete`);
      console.log('');
      console.log('🔍 To check remaining issues:');
      console.log(`   markdownlint ${this.docsDir}/**/*.md`);

    } catch (error) {
      console.error('❌ Error processing directory:', error.message);
      process.exit(1);
    }
  }

  async run() {
    console.log('🚀 Advanced Markdown Auto-Fix Script');
    console.log('====================================');
    console.log('');

    // Check if docs directory exists
    try {
      await stat(this.docsDir);
    } catch (error) {
      console.error(`❌ Error: ${this.docsDir}/ directory not found!`);
      process.exit(1);
    }

    await this.processDirectory();
  }
}

// Run the script
if (require.main === module) {
  const fixer = new MarkdownFixer();
  fixer.run().catch(console.error);
}

module.exports = MarkdownFixer;
