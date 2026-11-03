"""Evaluate a candidate FR mapping using distinct string targets in the D90 sample."""
from collections import Counter
import re
import struct

import directory_entry
import update_layout
import ram_update
import nor_sector
import fr_poll
import outcome_text
import startup
import endm_setting


def update_patterns(data):
    """Find FR ldi:32 r12 / call:d @r12 pairs in the filename dispatch window."""
    pairs = re.compile(rb"\x9f\x8c(.{4})\x9f\x1c", re.DOTALL)
    loads = re.compile(rb"\x9f\x84(.{4})", re.DOTALL)
    rows = []
    for load in loads.finditer(data, 0x1ab924, 0x1ab9d4):
        site = load.start()
        if site % 2:
            continue
        calls = [m for m in pairs.finditer(data, load.end(), site + 34)
                 if m.start() % 2 == 0]
        ptr = int.from_bytes(load[1], "big") + 0x40002
        name = data[ptr:ptr + 16].split(b"\0")[0].decode("ascii")
        rows.append(dict(site=site, pattern=name, string_offset=ptr,
                         call_target_offsets=[int.from_bytes(m[1], "big") + 0x40002
                                              for m in calls]))
    return rows


def main_size_check(data):
    """Recover the operands of the sample-specific combined update guard."""
    def word(site):
        return struct.unpack_from(">H", data, site)[0]

    def imm32(site):
        return struct.unpack_from(">I", data, site + 2)[0]

    def branch(site):
        displacement = struct.unpack_from(">b", data, site + 1)[0]
        return site + 2 + 2 * displacement

    # Caller loads the descriptor into r8; the call delay slot copies r8 to r4.
    if (word(0x1ab92a), word(0x1ab988), word(0x1ab98a)) != (0x9f88, 0x9f1c, 0x8b84):
        raise ValueError("Unexpected descriptor call sequence")
    if (word(0x1aca8c), word(0x1aca8e), word(0x1aca94)) != (0x0040, 0x9f81, 0xaa10):
        raise ValueError("Unexpected field load or size comparison")
    field_offset = (word(0x1aca8a) >> 4) & 0xff
    descriptor = imm32(0x1ab92a)
    return dict(descriptor=descriptor, field_offset=field_offset,
                field_address=descriptor + field_offset, compared_value=imm32(0x1aca8e),
                equals_target=branch(0x1aca96), mismatch_target=branch(0x1aca98),
                mismatch_slot=hex(word(0x1aca9a)))


def combined_reads(data):
    """Resolve the three seek-like immediate arguments in the combined handler."""
    def argument(site):
        op, value = struct.unpack_from(">HI", data, site)
        if op != 0x9f85:
            raise ValueError("Expected ldi:32 into r5")
        return value

    marker = argument(0x1acb06)
    pointer_site = argument(0x1acb6e)
    pointer = struct.unpack_from(">I", data, pointer_site)[0]
    # ldi:20 0x40002,r0; add r0,r5
    if data[0x1acbaa:0x1acbb0] != bytes.fromhex("9b400002a605"):
        raise ValueError("Pointer adjustment changed")
    version_site = pointer + 0x40002
    return dict(marker_site=marker, marker_hex=data[marker:marker + 4].hex(),
                pointer_site=pointer_site, pointer=pointer,
                version_site=version_site, prefix=data[version_site:version_site + 9].decode("ascii"))


def inspect(data):
    start, end = 0x140002, 0x604cc5
    strings = {m.start(): m.group() for m in re.compile(
        rb"(?<=\x00)[ -~]{6,}(?=\x00)").finditer(data, start, end)}
    # Count a value only once, irrespective of repeated loads of the same address.
    loads = {}
    for offset in range(start, end - 5, 2):
        if data[offset] == 0x9f and data[offset + 1] >> 4 == 8:
            immediate = int.from_bytes(data[offset + 2:offset + 6], "big")
            loads.setdefault(immediate, offset)
    scores = {}
    examples = []
    for delta in (-0x40002, -0x40000, -0x40004, 0):
        hits = sorted(addr for addr in loads if addr - delta in strings)
        scores[str(delta)] = len(hits)
        if delta == -0x40002:
            seen_text = set()
            for addr in hits:
                target = addr - delta
                if strings[target] in seen_text:
                    continue
                if len(examples) < 5 and any(c in strings[target] for c in (b".c", b"Nikon", b"D90", b"Error")):
                    seen_text.add(strings[target])
                    examples.append(dict(site=loads[addr], immediate=addr, target_offset=target,
                                         text=strings[target][:60].decode("ascii")))
    words = struct.unpack_from(">256I", data, 0x11fc02)
    frequent, count = Counter(words).most_common(1)[0]
    return dict(endm_setting=endm_setting.inspect(data), startup=startup.inspect(data), outcome_text=outcome_text.inspect(data), fr_poll=fr_poll.inspect(data), nor_sector=nor_sector.inspect(data), ram_update=ram_update.inspect(data),
                update_layout=update_layout.inspect(data),
                combined_reads=combined_reads(data),
                directory_replay=directory_entry.inspect(data),
                main_size_check=main_size_check(data), update_patterns=update_patterns(data),
                candidate_vector_table=dict(start=0x11fc02, end=0x120002,
                most_frequent=frequent, occurrences=count, last_word=words[-1]),
                build_pointer=dict(site=0x11fbf2,
                    value=struct.unpack_from(">I", data, 0x11fbf2)[0],
                    target_offset=struct.unpack_from(">I", data, 0x11fbf2)[0] + 0x40002),
                search_region=[start, end], distinct_string_targets_by_delta=scores,
                examples=examples)
