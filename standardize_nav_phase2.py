#!/usr/bin/env python3
"""
Phase 2: Add navigation to templates that only have header but no nav.
"""

import os
import re
from pathlib import Path

# Template directory
TEMPLATES_DIR = Path("src/main/resources/templates")

# Pattern to match just the header (without nav)
HEADER_ONLY_PATTERN = re.compile(
    r'(<div class="header">.*?</div>)',
    re.DOTALL
)

# Replacement with header and nav fragments
FRAGMENTS_REPLACEMENT = '''<!-- Include Page Header and Navigation -->
    <div th:replace="~{fragments/navigation :: page-header}"></div>
    <div th:replace="~{fragments/navigation :: navbar}"></div>'''

def should_process_file(filepath):
    """Check if file should be processed."""
    if 'fragments' in str(filepath):
        return False
    return filepath.suffix == '.html'

def add_navigation(filepath):
    """Add navigation to files that only have header."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Skip if already using fragments
        if 'fragments/navigation' in content:
            return False
        
        # Skip if has nav (already processed in phase 1)
        if '<nav class="nav">' in content:
            return False
        
        # Check if has header but no nav
        if '<div class="header">' not in content:
            return False
        
        # Replace just the header with both header and nav fragments
        new_content, count = HEADER_ONLY_PATTERN.subn(FRAGMENTS_REPLACEMENT, content)
        
        if count > 0:
            # Add styles fragment if not present
            if '<th:block th:replace="~{fragments/navigation :: common-styles}"></th:block>' not in new_content:
                new_content = new_content.replace(
                    '</head>',
                    '    <th:block th:replace="~{fragments/navigation :: common-styles}"></th:block>\n</head>'
                )
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"✓ {filepath.relative_to(TEMPLATES_DIR)} - Added navigation")
            return True
        else:
            return False
            
    except Exception as e:
        print(f"✗ {filepath.relative_to(TEMPLATES_DIR)} - Error: {e}")
        return False

def main():
    """Main function."""
    print("=" * 60)
    print("Phase 2: Adding Navigation to Header-Only Templates")
    print("=" * 60)
    print()
    
    updated_count = 0
    
    for filepath in TEMPLATES_DIR.rglob('*.html'):
        if should_process_file(filepath):
            if add_navigation(filepath):
                updated_count += 1
    
    print()
    print("=" * 60)
    print(f"Summary: Added navigation to {updated_count} templates")
    print("=" * 60)

if __name__ == '__main__':
    main()

