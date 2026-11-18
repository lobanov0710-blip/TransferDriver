#!/usr/bin/env python3
"""Validate actual native captures of the read-only classifier card."""
import sys
from check_registration_evidence import validate
NAMES = tuple('classification-' + stage + theme for theme in ('-light', '-dark-large')
              for stage in ('current', 'history', 'unverified'))
if __name__ == '__main__':
    validate(sys.argv[1], NAMES)
