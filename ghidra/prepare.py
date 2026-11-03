"""Emit an Intel HEX memory map of D90 A or B for Ghidra's Intel Hex loader."""
import argparse
import hashlib
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from analyze import SAMPLES


def record(kind, address, payload):
    body = bytes([len(payload)]) + address.to_bytes(2, "big") + bytes([kind]) + payload
    return ":" + (body + bytes([-sum(body) & 255])).hex().upper()


def records(data, component="B"):
    # Both programmed spans (AB-15), plus the relocated startup copy (AB-14).
    spans = ((0, 0x80000, 0x80000),) if component == "A" else (
                            (0x80002, 0x120002, 0x40000),
                             (0x140002, 0x780002, 0x100000),
                             (0x2e583a, 0x2e622e, 0x8f52d6c0))
    for start, end, base in spans:
        for offset in range(start, end, 32):
            address = base + offset - start
            if offset == start or address & 0xffff == 0:
                yield record(4, 0, (address >> 16).to_bytes(2, "big"))
            yield record(0, address & 0xffff, data[offset:min(offset + 32, end)])
    yield record(1, 0, b"")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path)
    parser.add_argument("--component", choices=("A", "B"), default="B")
    args = parser.parse_args()
    data = args.image.read_bytes()
    if hashlib.sha256(data).hexdigest() != SAMPLES[0][1]:
        parser.error("Unexpected D90 SHA-256")
    for line in records(data, args.component):
        print(line)
