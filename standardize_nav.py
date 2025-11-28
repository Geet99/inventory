#!/usr/bin/env python3
"""
Script to standardize navigation bars across all Thymeleaf templates.
Replaces inline headers and navigation with fragment includes.
"""

import os
import re
from pathlib import Path

# Template directory
TEMPLATES_DIR = Path("src/main/resources/templates")

# Pattern to match the header section
HEADER_PATTERN = re.compile(
    r'<div class="header">.*?</div>\s*<nav class="nav">.*?</nav>',
    re.DOTALL
)

# Replacement with fragment includes
FRAGMENT_REPLACEMENT = '''<!-- Include Page Header and Navigation -->
    <div th:replace="~{fragments/navigation :: page-header}"></div>
    <div th:replace="~{fragments/navigation :: navbar}"></div>'''

def should_process_file(filepath):
    """Check if file should be processed."""
    # Skip fragments directory
    if 'fragments' in str(filepath):
        return False
    # Only process HTML files
    return filepath.suffix == '.html'

def standardize_template(filepath):
    """Standardize a single template file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if already using fragments
        if 'fragments/navigation' in content:
            print(f"✓ {filepath.relative_to(TEMPLATES_DIR)} - Already using fragments")
            return False
        
        # Check if has inline nav
        if not ('<nav class="nav">' in content or '<div class="header">' in content):
            print(f"○ {filepath.relative_to(TEMPLATES_DIR)} - No navigation found")
            return False
        
        # Replace header and nav with fragments
        new_content, count = HEADER_PATTERN.subn(FRAGMENT_REPLACEMENT, content)
        
        if count > 0:
            # Also need to add the styles fragment in the <head>
            # Find the <head> tag and add styles if not already there
            if '<th:block th:replace="~{fragments/navigation :: common-styles}"></th:block>' not in new_content:
                # Add before </head>
                new_content = new_content.replace(
                    '</head>',
                    '    <th:block th:replace="~{fragments/navigation :: common-styles}"></th:block>\n</head>'
                )
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"✓ {filepath.relative_to(TEMPLATES_DIR)} - Updated")
            return True
        else:
            print(f"⚠ {filepath.relative_to(TEMPLATES_DIR)} - Pattern not matched")
            return False
            
    except Exception as e:
        print(f"✗ {filepath.relative_to(TEMPLATES_DIR)} - Error: {e}")
        return False

def main():
    """Main function to process all templates."""
    print("=" * 60)
    print("Standardizing Navigation Bars Across Templates")
    print("=" * 60)
    print()
    
    updated_count = 0
    total_count = 0
    
    # Process all HTML files
    for filepath in TEMPLATES_DIR.rglob('*.html'):
        if should_process_file(filepath):
            total_count += 1
            if standardize_template(filepath):
                updated_count += 1
    
    print()
    print("=" * 60)
    print(f"Summary: Updated {updated_count} out of {total_count} templates")
    print("=" * 60)

if __name__ == '__main__':
    main()

