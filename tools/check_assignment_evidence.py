#!/usr/bin/env python3
"""Only actual complete native Admin captures are eligible for UI review."""
import sys
from check_registration_evidence import validate
NAMES = tuple("assignment-" + stage + theme for theme in ("-light", "-dark-large")
              for stage in ("orders", "options", "form", "confirm", "uncertain"))
if __name__ == "__main__":
    validate(sys.argv[1], NAMES)
