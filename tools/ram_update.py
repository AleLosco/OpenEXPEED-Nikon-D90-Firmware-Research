"""Resolve the FR startup copy and trace argument setup in the relocated code."""
from struct import unpack_from


def inspect(data):
    pattern = ">HIHHHIHI"
    op, size, compare, skip, load_source, source, load_ram, ram = unpack_from(
        pattern, data, 0x80428)
    body = data[0x8043e:0x80446]
    if (op, compare, skip, load_source, load_ram) != (0x9f8d, 0xa80d, 0xe20a, 0x9f81, 0x9f80):
        raise ValueError("Unexpected RAM copy setup")
    if body.hex() != "a5ed011cfdfd110c" or size % 2:
        raise ValueError("Unexpected halfword copy loop")
    first = source + 0x40002
    target = unpack_from(">I", data, 0x17a1d4)[0]
    if not ram <= target < ram + size:
        raise ValueError("Call target outside RAM copy")
    entry = first + target - ram
    # Replay argument setup on the fall-through (success) path only.
    pc = entry + 0x13e
    stop = entry + 0x1ae
    regs = {}
    calls = []
    while pc < stop:
        word = unpack_from(">H", data, pc)[0]
        site = pc
        pc += 2
        reg = word & 15
        src = word >> 4 & 15
        if word & 0xff00 == 0x9b00:
            regs[reg] = src << 16 | unpack_from(">H", data, pc)[0]
            pc += 2
        elif word & 0xfff0 == 0x9f80:
            regs[reg] = unpack_from(">I", data, pc)[0]
            pc += 4
        elif word >> 8 in (0xa6, 0xac):
            regs[reg] += regs[src] if word >> 8 == 0xa6 else -regs[src]
        elif word & 0xf800 == 0xd000:
            disp = word & 2047
            dest = pc + 2 * (disp - 2048 if disp >= 1024 else disp)
            if not first <= dest < first + size:
                raise ValueError("Relative call outside copied block")
            args = [regs[r] for r in range(4, 7 if len(calls) >= 3 else 6)]
            calls.append(dict(site=site, target_file=dest, target_ram=ram + dest - first,
                              arguments=args))
            regs.clear()
        elif word >> 8 not in (0xa8, 0xe2):
            raise ValueError(f"Unknown instruction at {site:x}")
    return dict(source_file=first, source_address=source, ram_base=ram,
                size=size, called_address=target, entry_file=entry, calls=calls)
