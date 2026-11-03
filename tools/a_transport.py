"""Bounded A transport probes, with instruction-boundary counterexamples."""


def inspect(image):
    def check(cpu, expected):
        expected = bytes.fromhex(expected)
        offset = cpu - 0x80000
        if image[offset:offset + len(expected)] != expected:
            raise ValueError(f"A transport probe changed at {cpu:x}")

    check(0x89914, "9b809b329b3c61189f2014c0")
    check(0x89b32, "9b3468cc9b3573889b367380")
    check(0x89b4c, "9b0000d28880e337096a8bd004611610")
    check(0x89b5c, "0460a41014600640a41016400640a810e303")
    check(0x89b8c, "9b001388f00a1550")
    check(0x89bc2, "9b0000d291809b3068ce9020c0849b058000cd0c978c")
    check(0xf839a, "09328bd016a0a41a")
    check(0xfac1e, "09328bd016a0a41a")
    # These apparent DMOVH words are actually immediate operands of LDI:20.
    check(0x88b1a, "9b040928")
    check(0xd05c6, "9bdc096e")
    return dict(d4_receive=dict(entry_cpu=0x89b32, read_cpu=0x89b54,
                               data_port=0xd4, status_port=0xd2, error_mask=8,
                               callback_slot_cpu=0x36118,
                               buffer_pointer_cpu=0x37380, count_cpu=0x368cc,
                               delay_counter_cpu=0x37388,
                               ordinary_delay_count=1, special_delay_count=5000,
                               error_event=0x8000),
                additional_polling_reads_cpu=[0xf839a, 0xfac1e],
                false_raw_opcode_matches=[dict(cpu=0x88b1c, apparent_port=0x50,
                                               containing_instruction_cpu=0x88b1a),
                                          dict(cpu=0xd05c8, apparent_port=0xdc,
                                               containing_instruction_cpu=0xd05c6)],
                scope="Partial inventory; indirect I/O and unexamined candidates remain; no B peer identified")


def frame_inspect(image):
    """D4 buffer setup and zero-sum frame check, distinct from B's 0xff sum."""
    from struct import unpack_from

    def check(cpu, expected):
        expected = bytes.fromhex(expected)
        offset = cpu - 0x80000
        if image[offset:offset + len(expected)] != expected:
            raise ValueError(f"A D4 frame probe changed at {cpu:x}")

    check(0x89464, "147514269b3073ea9b3c738414c0e0041474c88ca6c41424")
    check(0x89854, "049506a0c014ac051495d81506a6")
    check(0x8988e, "a5f69796a806fc09c007")
    check(0x89898, "0650a6079797a5f69796a806fdf9a415")
    check(0x898ac, "c000ac70f00916500650a6709790a800e30280b1e0019041")
    handlers = (0x894a0, 0x894aa, 0x894b4, 0x894be, 0x894c8, 0x894d2, 0x894dc)
    settings = {}
    for handler in handlers:
        rx, tx = (unpack_from(">H", image, handler - 0x80000 + off)[0] for off in (0, 4))
        if rx & 0xf00f != 0xc000 or tx & 0xf00f != 0xc000:
            raise ValueError("Expected D4 count immediates")
        settings[handler] = dict(tx_count=(tx >> 4) & 255, rx_count=(rx >> 4) & 255)
    modes = []
    for index in range(50):
        target = unpack_from(">I", image, 0x69324 + 4 * index)[0]
        if target not in settings and target != 0x894e4:
            raise ValueError("Unexpected D4 mode target")
        modes.append(dict(mode=index + 1, target_cpu=target,
                          counts=settings.get(target)))
    return dict(checksum_routine_cpu=0x8988a, accepted_sum_low_byte=0,
                checksum_error_flag=4, flags_cpu=0x368ce,
                ordinary_tx_buffer_cpu=0x3738a, ordinary_rx_buffer_cpu=0x373ba,
                ordinary_clear_size_per_buffer=48,
                mode_0x32_tx_buffer_cpu=0x365c0, mode_0x32_rx_buffer_cpu=0x36648,
                mode_count_table_cpu=0xe9324, modes=modes,
                scope="Observed D4 framing; unchanged B frames fail this checksum; physical peer unresolved")
