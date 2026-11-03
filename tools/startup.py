"""Static FR reset probes for the reference D90 B address map."""
from struct import unpack_from


def inspect(image):
    def immediate(site, reg):
        op, value = unpack_from(">HI", image, site)
        if op != 0x9f80 + reg:
            raise ValueError(f"Unexpected immediate at {site:x}")
        return value

    copies = []
    for site in (0x803bc, 0x803e0, 0x80404, 0x80428, 0x80446):
        size = immediate(site, 13)
        source = immediate(site + 10, 1)
        target = immediate(site + 16, 0)
        copies.append(dict(setup_file=site, size=size, source_cpu=source,
                           source_file=source + 0x40002, target_cpu=target))
    calls = []
    for site in range(0x80714, 0x8072c, 2):
        op = unpack_from(">H", image, site)[0]
        if op & 0xf800 != 0xd000:
            raise ValueError("Unexpected stage-2 relative call sequence")
        disp = op & 2047
        calls.append(site - 0x40002 + 2 + 2 * (disp - 2048 if disp >= 1024 else disp))
    tables = []
    for entry in calls[4:]:
        site = entry + 0x40002
        if image[site:site + 2] == bytes.fromhex("1781"):
            site += 2
        table = immediate(site, 2)
        offset = table + 0x40002
        count, stride = unpack_from(">HH", image, offset)
        destinations = [unpack_from(">I", image, offset + 4 + i * stride)[0]
                        for i in range(count)]
        tables.append(dict(entry=entry, table=table, header_count=count, stride=stride,
                           first_words=destinations))
    handoff = immediate(0x807be, 0)
    if image[0x807ec:0x807ee] != bytes.fromhex("9730"):
        raise ValueError("Expected FR RETI after context restore")
    task_table = tables[0]
    ready = []
    for index in range(task_table["header_count"] + 1):
        address = task_table["table"] + 4 + index * task_table["stride"]
        control, ids, _, flags, entry, stack, arg = unpack_from(">7I", image, address + 0x40002)
        if flags & 0x80000000:
            ready.append(dict(record=address, control=control, priority_field=ids & 65535,
                              entry=entry, stack=stack, argument=arg))
    initial = next(record for record in ready if record["record"] == handoff)
    # The destination PC comes from R6, loaded from descriptor +0x10.
    if image[0x81544:0x81546].hex() != "3006" or image[initial["entry"] + 0x40002:
            initial["entry"] + 0x40004].hex() != "e0ff":
        raise ValueError("Unexpected initial PC store or idle branch")
    entry = next(record["entry"] for record in ready if record["priority_field"] == 1)
    pc = entry + 0x40002 + 30  # prologue and four ldi:32 global-address loads
    startup_calls = []
    stop = 0x14015e
    while pc < stop:
        opcode = unpack_from(">H", image, pc)[0]
        if opcode == 0x9f8c and image[pc + 6:pc + 8].hex() == "971c":
            startup_calls.append(immediate(pc, 12))
            pc += 8
        elif opcode & 0xf800 == 0xd000:
            disp = opcode & 0x7ff
            startup_calls.append(pc - 0x40002 + 2 + 2 * (disp - 2048 if disp >= 1024 else disp))
            pc += 2
        else:
            raise ValueError(f"Unexpected startup call at {pc:x}")
    vector = unpack_from(">I", image, 0x11fc02 + 192 * 4)[0]
    bitmap = immediate(0x80e2c, 12)
    if image[0x80e34:0x80e36].hex() != "97dd":
        raise ValueError("Unexpected dispatcher opcode")
    block_ram = immediate(0x1da544, 4)
    defaults = immediate(0x1da54a, 6)
    shared = immediate(0x1da5e4, 12)
    if image[0x1da5ea:0x1da5ee].hex() != "9f2014c4":
        raise ValueError("ENDM base publication changed")
    refs = [i - 0x40002 for i in range(0x80002, len(image) - 5, 2)
            if image[i] == 0x9f and image[i + 1] >> 4 == 8
            and unpack_from(">I", image, i + 2)[0] == shared]
    storage_calls = [immediate(pc + 0x40002, 12) for pc in
                     (0x19a624, 0x19a63c, 0x19a65a, 0x19a672)]
    endm = dict(ram=block_ram, defaults=defaults, shared_pointer=shared,
                pointer_loads=refs, persistence_calls=storage_calls,
                default_u16={hex(offset): unpack_from(">H", image, defaults + 0x40002 + offset)[0]
                             for offset in (0x1e, 0x20, 0x150, 0x15a, 0x15c)})
    return dict(endm=endm, startup_task_calls=startup_calls, dispatcher_vector_slot_192=vector,
                ready_bitmap_address=bitmap, prepared_contexts=ready, first_frame=dict(base=initial["stack"],
                pc=initial["entry"], ps=immediate(0x8154e, 8), rp=immediate(0x81546, 8)),
                init_tables=tables, context_pointer_site=handoff,
                context_pointer=unpack_from(">I", image, handoff + 0x40002)[0],
                stack=immediate(0x8000e, 15), vector_base=immediate(0x80014, 0),
                copies=copies, stage2=immediate(0x80464, 0), stage2_calls=calls)
