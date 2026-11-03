"""Replay one synchronous x16 NOR sector using the D90 low-address command path."""
from hashlib import sha256
from struct import unpack_from


def bus_trace(image, start, end):
    """Decode straight-line writes, including stores in call delay slots."""
    regs = {4: "target", 8: "target", 5: "data"}
    writes = []
    pc = start
    while pc < end:
        word = unpack_from(">H", image, pc)[0]
        pc += 2
        dst = word & 15
        src = (word >> 4) & 15
        if word >> 12 == 0xc:
            regs[dst] = word >> 4 & 255
        elif word >> 8 == 0x9b:
            regs[dst] = src << 16 | unpack_from(">H", image, pc)[0]
            pc += 2
        elif word >> 8 == 0x15:
            writes.append((regs[src], regs[dst]))
        elif word & 0xf800 != 0xd800:
            raise ValueError(f"Unexpected bus instruction at {pc - 2:x}")
    return writes


def verify_words(image, source, sector):
    # FR loads two halfwords, compares them, and exits on the mismatch path.
    if image[0x2e5cf4:0x2e5d00].hex() != "05000511aa10f203a42af01e":
        raise ValueError("Unexpected readback comparison")
    for offset in range(0, len(source), 2):
        if sector[offset:offset + 2] != source[offset:offset + 2]:
            return offset
    return None


def inspect(image):
    base, size = 0x40000, 0x20000
    start = base + 0x40002
    source = image[start:start + size]
    erase = bus_trace(image, 0x2e5dd0, 0x2e5df8)
    program = bus_trace(image, 0x2e5eb6, 0x2e5ed2)
    unlock = [(0x40aaa, 0xaa), (0x40554, 0x55)]
    if erase != unlock + [(0x40aaa, 0x80)] + unlock + [("target", 0x30)]:
        raise ValueError("Unexpected sector erase bus sequence")
    if program != unlock + [(0x40aaa, 0xa0), ("target", "data")]:
        raise ValueError("Unexpected halfword programming sequence")

    sector = bytearray(size)
    sector[:] = b"\xff" * size  # effect of the verified erase sequence
    programmed = 0
    for offset in range(0, size, 2):
        value = unpack_from(">H", source, offset)[0]
        if value == 0xffff:  # fast path in the helper at 0x2e5e84
            continue
        previous = unpack_from(">H", sector, offset)[0]
        sector[offset:offset + 2] = (previous & value).to_bytes(2, "big")
        programmed += 1
    if verify_words(image, source, sector) is not None:
        raise ValueError("Sector readback differs from the staged source")
    digest = sha256(sector).hexdigest()
    sector[size - 1] ^= 1
    mismatch = verify_words(image, source, sector)
    if mismatch != size - 2:
        raise ValueError("Injected readback fault was not detected")
    return dict(injected_mismatch_offset=mismatch, destination=base, source_offset=start, length=size,
                erase_writes=erase, program_writes=program, programmed_words=programmed,
                skipped_words=size // 2 - programmed, readback_sha256=digest)
