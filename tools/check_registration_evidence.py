#!/usr/bin/env python3
"""Accept only complete native PNG fixtures, never adb stdout disguised as an image."""
import hashlib
from pathlib import Path
import struct
import sys
import zlib

directory = Path(sys.argv[1])
for name in ("profile-light", "profile-dark-large", "vehicle-light", "vehicle-dark-large",
             "availability-offline-light", "availability-online-light",
             "availability-offline-dark-large", "availability-online-dark-large"):
    data = (directory / (name + ".png")).read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", name + ": not a PNG"
    assert len(data) <= 12_000_000, name + ": image too large"
    offset, width, height, seen_data, ended = 8, 0, 0, False, False
    while offset + 12 <= len(data):
        length = struct.unpack_from(">I", data, offset)[0]
        end = offset + 12 + length
        assert end <= len(data), name + ": truncated chunk"
        kind = data[offset + 4:offset + 8]
        payload = data[offset + 8:end - 4]
        crc = struct.unpack_from(">I", data, end - 4)[0]
        assert zlib.crc32(kind + payload) & 0xFFFFFFFF == crc, name + ": invalid CRC"
        if offset == 8:
            assert kind == b"IHDR" and length == 13, name + ": missing header"
            width, height = struct.unpack_from(">II", payload)
            assert 320 <= width <= 8000 and 640 <= height <= 8000, name + ": not a native viewport"
        if kind == b"IDAT":
            seen_data = True
        if kind == b"IEND":
            assert length == 0 and end == len(data) and seen_data, name + ": incomplete PNG"
            ended = True
            break
        offset = end
    assert ended, name + ": missing image end"
    print(name + ": PNG " + str(width) + "x" + str(height) + " sha256=" + hashlib.sha256(data).hexdigest())
