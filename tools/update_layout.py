"""D90 combined-package probes. Offsets refer to the reference release."""
from binascii import crc_hqx
from struct import unpack_from


def inspect(data):
    # ldi:20 in the AD90 handler and ldi:32 in the BD90 handler, both to r1.
    opcode, low = unpack_from(">HH", data, 0x1aca20)
    op_b, size_b = unpack_from(">HI", data, 0x1ac9b0)
    if opcode & 0xff0f != 0x9b01 or op_b != 0x9f81:
        raise ValueError("Unexpected standalone size checks")
    size_a = ((opcode >> 4) & 15) << 16 | low
    if size_a + size_b != len(data):
        raise ValueError("Standalone sizes do not cover the combined package")
    blocks = []
    for label, start, end in (("A", 0, size_a), ("B", size_a, len(data))):
        checksum = crc_hqx(data[start:end - 2], 0)
        blocks.append(dict(label=label, start=start, end=end,
                           stored=data[end - 2:end].hex(), computed=f"{checksum:04x}"))
    # Each entry is a code address in the B image.
    table = 0x1d30c0 + 0x40002
    targets = [addr + 0x40002 for addr in unpack_from(">5I", data, table)]
    state = unpack_from(">I", data, 0x1abf86)[0]
    refs = [i for i in range(0, len(data) - 5, 2)
            if data[i] == 0x9f and data[i+1] >> 4 == 8
            and unpack_from(">I", data, i+2)[0] == state]
    base_load, base, mov = unpack_from(">HIH", data, size_a + 18)
    if (base_load, mov) != (0x9f80, 0xb300):
        raise ValueError("Unexpected vector base initialization")
    vector_start = base + 0x40002
    last = unpack_from(">I", data, vector_start + 1020)[0]
    return dict(blocks=blocks, state_address=state, literal_load_sites=refs,
                dispatch_offsets_by_state_1_to_5=targets, vector_address=base,
                vector_offset=vector_start, reset_candidate=last,
                reset_file_offset=last + 0x40002)
