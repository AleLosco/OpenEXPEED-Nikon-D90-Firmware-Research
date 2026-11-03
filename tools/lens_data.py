"""Read-only structural inspection of Nikon's NKLD 2.018 sample.

The fixed seed values are algorithm parameters from the Nikon Hacker
project. This independent implementation uses neither its code nor its
lookup tables. Source of the recurrence and parameters:
https://github.com/simeonpilgrim/nikon-firmware-tools/blob/main/firmware%20decode/firmware%20decode/Decrypt.cs
https://github.com/simeonpilgrim/nikon-firmware-tools/blob/main/firmware%20decode/firmware%20decode/Program.cs
"""

from binascii import crc_hqx
from collections import Counter
from struct import unpack_from


def _decode_records(data):
    # Fixed seeds obtained by evaluating Nikon Hacker Decrypt.cs init_cj
    # for 0xB401C81B and init_ci for NCDSLR. Only the low eight bits matter.
    output = bytearray()
    key = 148
    delta = 223 * 0x260
    for value in data:
        key = (key + delta) & 255
        delta = (delta + 223) & 255
        output.append(value ^ key)
    return output


def _check_crc(data, start, end):
    stored = int.from_bytes(data[end:end + 2], "big")
    calculated = crc_hqx(data[start:end], 0)
    return {"start": start, "end_exclusive": end, "stored": stored,
            "calculated": calculated, "matches": stored == calculated}


def inspect(data):
    """Return metrics for the tested 2.018 layout; reject invalid boundaries."""
    if len(data) < 32:
        raise ValueError("NKLD header truncated")
    start, major, minor, count, magic = unpack_from(">H2xBBHI", data)
    span = int.from_bytes(data[12:16], "big")
    suffix_start = int.from_bytes(data[28:32], "big")
    if (major, minor, magic, start) != (2, 18, 0x87c7caac, 32):
        raise ValueError("Unsupported NKLD header")
    if span < 2 or suffix_start != start + span + 2 or len(data) != suffix_start + 256:
        raise ValueError("Unexpected NKLD 2.018 bounds")
    record_end = start + span - 2
    decoded = _decode_records(data[start:record_end])
    sizes = Counter()
    cursor = 0
    for _ in range(count):
        size = int.from_bytes(decoded[cursor:cursor + 2], "big")
        if size < 2 or cursor + size > len(decoded):
            raise ValueError("NKLD record exceeds declared region")
        sizes[size] += 1
        cursor += size
    if cursor != len(decoded):
        raise ValueError("NKLD records do not fill declared region")

    suffix_crc = suffix_start + 228
    return {
        "version": f"{major}.{minor:03d}",
        "magic": hex(magic),
        "record_region": [start, record_end],
        "declared_record_count": count,
        "decoded_record_lengths": dict(sorted(sizes.items())),
        "crc16_xmodem_main": _check_crc(data, 0, record_end),
        "suffix_region": [suffix_start, len(data)],
        "crc16_xmodem_suffix": _check_crc(data, suffix_start, suffix_crc),
        "trailing_cd_bytes": len(data) - len(data.rstrip(b"\xcd")),
    }
