#!/usr/bin/env python3
"""Compute Flyway migration checksum (Flyway 10 ChecksumCalculator compatible).

Flyway strips BOM on the first line, strips all line endings via readLine(),
then CRC32-updates the concatenated line bytes (no separators), cast to Java int.
"""

from __future__ import annotations

import argparse
import sys
import zlib


def flyway_checksum(path: str) -> int:
    with open(path, "rb") as f:
        raw = f.read()
    text = raw.decode("utf-8")
    # BomFilter.FilterBomFromString on first line equivalent
    if text.startswith("\ufeff"):
        text = text[1:]
    # BufferedReader.readLine() drops endings; lines concatenated with no separator
    payload = "".join(text.splitlines()).encode("utf-8")
    crc = zlib.crc32(payload) & 0xFFFFFFFF
    if crc >= 2**31:
        crc -= 2**32
    return int(crc)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("file", help="Path to a Flyway SQL migration file")
    args = parser.parse_args()
    try:
        print(flyway_checksum(args.file))
    except OSError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
