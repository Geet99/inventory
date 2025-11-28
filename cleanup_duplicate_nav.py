#!/usr/bin/env python3
"""
Remove duplicate inline navigation from templates.
"""

import re
from pathlib import Path

# Files that need cleanup
FILES_TO_CLEAN = [
    "src/main/resources/templates/stock/upper-list.html",
    "src/main/resources/templates/stock/movements.html",
    "src/main/resources/templates/stock/finished-list.html",
    "src/main/resources/templates/colors/edit.html",
    "src/main/resources/templates/articles/edit.html",
    "src/main/resources/templates/plans/edit.html",
    "src/main/resources/templates/vendors/report.html",
    "src/main/resources/templates/vendors/edit.html",
    "src/main/resources/templates/vendors/add.html",
]

# Pattern to match the duplicate inline nav
DUPLICATE_NAV_PATTERN = re.compile(
    r'\s*<div class="nav">.*?</div>\s*</div>\s*',
    re.DOTALL
)

def clean_file(filepath):
    """Remove duplicate inline navigation from a file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if file has both fragment include and inline nav
        has_fragment = 'fragments/navigation :: navbar' in content
        has_inline_nav = '<div class="nav">' in content
        
        if not has_fragment or not has_inline_nav:
            print(f"○ {filepath} - No duplicate nav found")
            return False
        
        # Find and remove the inline nav that comes after the fragment includes
        # Look for pattern: fragment includes followed by inline nav
        pattern = re.compile(
            r'(<!-- Include Page Header and Navigation -->.*?<div th:replace="~\{fragments/navigation :: navbar\}"></div>\s*)\s*<div class="nav">.*?</div>\s*</div>\s*',
            re.DOTALL
        )
        
        new_content = pattern.sub(r'\1\n    ', content)
        
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"✓ {filepath} - Removed duplicate navigation")
            return True
        else:
            print(f"⚠ {filepath} - Pattern not matched, manual check needed")
            return False
            
    except Exception as e:
        print(f"✗ {filepath} - Error: {e}")
        return False

def main():
    """Main function."""
    print("=" * 60)
    print("Cleaning Duplicate Navigation from Templates")
    print("=" * 60)
    print()
    
    cleaned = 0
    for filepath in FILES_TO_CLEAN:
        if clean_file(filepath):
            cleaned += 1
    
    print()
    print("=" * 60)
    print(f"Summary: Cleaned {cleaned} out of {len(FILES_TO_CLEAN)} files")
    print("=" * 60)

if __name__ == '__main__':
    main()

