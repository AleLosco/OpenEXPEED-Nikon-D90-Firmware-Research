"""Reference-bound observations of A's two-byte command lookup and mode gate."""
from struct import unpack_from


def inspect(image):
    def check(offset, expected):
        expected = bytes.fromhex(expected)
        if image[offset:offset + len(expected)] != expected:
            raise ValueError(f"A command probe changed at {offset:x}")

    check(0x507c, "9bea1fd4")  # Table CPU 0xe1fd4 -> R10.
    check(0x508a, "c0c0af05b7508b0d01a6aa64")  # Stride 12, compare u16 key.
    check(0x50c2, "c02d01a0cff1821016b001a09b0180008210")
    check(0x4e46, "c08d0104c04d00009710")  # u16 argument +8, callback +4.
    check(0x4dbc, "8928e214d028")  # Bit 0x20 gates callback installation.
    check(0x514e, "0660c531aa10e312c01d0260c531aa10e30d")
    check(0x5166, "9b306db89120")  # Set bit 0x20 after SS.
    check(0x5210, "0654c01d0250b4849204df2d97b4")  # Big-endian key into lookup.
    entries = []
    for index in range(53):
        offset = 0x61fd4 + index * 12
        key, descriptor, callback, argument, tail = unpack_from(">HHIHH", image, offset)
        if key == 0:
            if index != 52 or any((descriptor, callback, argument, tail)):
                raise ValueError("Unexpected A command terminator")
            break
        if index == 52 or not 0x80000 <= callback < 0x100000:
            raise ValueError("Unexpected A command entry")
        entries.append(dict(index=index, key=key, descriptor=descriptor,
                            initial_count=descriptor & 255,
                            variable_length_phase=bool(descriptor & 0x8000),
                            callback_cpu=callback, argument_u16=argument,
                            trailing_u16=tail))
    if len({e["key"] for e in entries}) != len(entries):
        raise ValueError("Duplicate A command keys")
    return dict(table_cpu=0xe1fd4, stride=12, entry_count=len(entries),
                lookup_cpu=0x85076, handler_dispatch_cpu=0x84e32,
                mode_flags_cpu=0x36db8, command_enable_mask=0x20,
                handshake_word=0x5353, entries=entries,
                scope="One A command table; no established mapping to B descriptor indices")
