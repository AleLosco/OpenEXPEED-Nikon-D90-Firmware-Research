"""Trace D90's ENDM +0x150 setter and its one-byte output record."""
from struct import unpack_from


def inspect(image):
    delta = 0x40002

    def immediate(pc, reg):
        op, value = unpack_from(">HI", image, pc + delta)
        if op != 0x9f80 + reg:
            raise ValueError(f"Unexpected FR immediate at {pc:x}")
        return value

    def word(pc):
        return unpack_from(">H", image, pc + delta)[0]

    code, reserved, handler = unpack_from(">HHI", image, 0x212956)
    if reserved or handler != 0x168bf2 or word(0x168c32) != 0x1108:
        raise ValueError("Unexpected ENDM setter layout")
    field = word(0x168c30)
    limit = (word(0x168c0e) >> 4) & 255
    notification = (word(0x106c70) >> 4) & 255
    shadow = immediate(0x106c60, 12)
    descriptor = immediate(0x10ff86, 12) + notification * 16
    pointer, size, kind, callback = unpack_from(">4I", image, descriptor + delta)
    if pointer != shadow or size != 1 or field != 0x150:
        raise ValueError("Setter shadow and descriptor disagree")
    isr = unpack_from(">I", image, 0x11fc02 + 228 * 4)[0]
    table = immediate(0x111f5a, 13)
    states = list(unpack_from(">3I", image, table + delta + 4 * 5))
    # DMOVH AC,@dir9 and DMOVH @dir9,AC scale the encoded offset by two.
    send = word(0x111a6a)
    if send >> 8 != 0x19 or word(0x111f46) != (send & 255 | 0x0900):
        raise ValueError("Unexpected B input/output address")
    port = (send & 255) * 2
    return dict(interrupt_entry=isr, state_table=table, states_5_to_7=states,
                data_register=port, direct_store_pc=0x111a6a, handler=handler, table_code=code, input_word=immediate(handler, 12),
                low16_max=limit, field_offset=field, response_ok=word(0x168c40),
                response_invalid=word(0x168c16), notification=notification,
                queue_slot=immediate(0x11041e, 8) + notification * 12,
                descriptor=descriptor, data_address=pointer, size=size,
                descriptor_type=kind, callback=callback)
