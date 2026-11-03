"""Reference-bound observations of A's port 0x84 buffer and checksum."""


def inspect(image):
    def check(cpu, expected):
        expected = bytes.fromhex(expected)
        offset = cpu - 0x80000
        if image[offset:offset + len(expected)] != expected:
            raise ValueError(f"A84 probe changed at {cpu:x}")

    check(0xac59a, "9b379dc5c005a804f209c006")
    check(0xac5b6, "8b5da41602709796aa60e2039b3069309110")
    check(0xac87e, "09428bd006ad128006a0a41016a006a0c0018b0d1281")
    check(0xab2c2, "80d59015c0001670c1201620ca109bacb81e9f0c1640")
    check(0xab80e, "80d59015c0001670c1201620ca101640")
    return dict(data_port=0x84, receive_copy_cpu=0xac87e,
                rx_buffer_cpu=0x39dc5, rx_index_cpu=0x39dfb,
                checksum_routine_cpu=0xac59a, checksum_rule="(sum(data) + 1) & 255",
                checksum_flags_cpu=0x36930, checksum_error_mask=0x10,
                valid_checksum_clears_error=False,
                builder_cpu=0xab00a, builder_identifier_cpu=0x37476,
                tx_buffer_cpu=0x39d93, rx_remaining_cpu=0x39dfa,
                observed_builder_cases=[dict(identifier=key, header=header, rx_count=count)
                    for key, header, count in ((0xa000,0xa0,22),(0xa100,0xa1,18),
                                               (0xa200,0xa2,46),(0xa300,0xa3,2),
                                               (0x0046,0xa1,18),(0x4600,0xa1,18))],
                scope="Selected builder and receive/checksum paths; no physical peer or whole-channel equivalence established")
