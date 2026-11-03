"""Reference-bound B transaction branch and checksum observations."""
from struct import unpack_from


def inspect(image):
    delta = 0x40002
    def check(pc, expected):
        expected = bytes.fromhex(expected)
        if image[pc + delta:pc + delta + len(expected)] != expected:
            raise ValueError(f"B transaction probe changed at {pc:x}")

    check(0x10feca, "06cd1932")
    check(0x110008, "06909f8c8f52f7f416c006309f8c8f52f7f5")
    check(0x110080, "9f8c8f52f7fb06c0a800f205c0201670c000f02d1560c080167006201560")
    check(0x1119e2, "0650cff1aa10e310")
    check(0x111c78, "0650cff1aa10f307c034c105cd0c978c")
    check(0x111c8e, "c205cd0c978c1f40")
    # Peer index guard and table addressing, independently of entry 0x46.
    check(0x10ff3c, "c471aa10")
    check(0x10ff86, "9f8c001c3fa4b440a6c0")
    descriptor_base = unpack_from(">I", image, 0x10ff88 + delta)[0]
    descriptor_count = (unpack_from(">H", image, 0x10ff3c + delta)[0] >> 4) & 255
    descriptors = []
    for index in range(descriptor_count):
        address = descriptor_base + index * 16
        pointer, size, kind, callback = unpack_from(">4I", image, address + delta)
        descriptors.append(dict(index=index, descriptor_cpu=address,
                                data_cpu=pointer, size_field=size,
                                type_field=kind, callback_cpu=callback))
    states = list(unpack_from(">14I", image, 0x1c4424 + delta))
    if states[2:14] != [0x111938, 0x111978, 0x1119c4, 0x111a1c,
                         0x111a7c, 0x111ade, 0x111af4, 0x111b48,
                         0x111ba0, 0x111bbe, 0x111c0c, 0x111c62]:
        raise ValueError("B state handlers changed")
    return dict(descriptor_table_cpu=descriptor_base,
                descriptor_stride=16, descriptor_count=descriptor_count,
                descriptors=descriptors,
                state_handlers_0_to_13=states, data_port=0x64,
                request_byte_cpu=0x8f52f7fc, initial_reply_cpu=0x8f52f7fb,
                state_cpu=0x8f52f7fe, tx_sum_cpu=0x8f52f7f4,
                rx_sum_cpu=0x8f52f7f5, rx_buffer_cpu=0x8f52f5f4,
                accepted_sum_low_byte=255, completion_event=0x10, error_event=0x20,
                descriptor_type_1_zero_reply_first_state=8,
                descriptor_type_1_nonzero_reply_first_state=2,
                scope="Static observations; bounded replay in D90BTransaction.java; physical peer unknown")
