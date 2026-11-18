#!/usr/bin/env python3
"""Validate native parameter review/OWNER exception captures."""
import sys
from check_registration_evidence import validate
NAMES = tuple('assessment-' + stage + theme for theme in ('-light', '-dark-large')
              for stage in ('form', 'conditions', 'confirm', 'uncertain', 'recorded'))
if __name__ == '__main__':
    validate(sys.argv[1], NAMES)
