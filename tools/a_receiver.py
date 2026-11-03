"""Follow the A serial input at file offset 0x3f8a on the reference sample."""
from struct import unpack_from


def inspect(image):
    def word(site):
        return unpack_from(">H", image, site)[0]

    def call_target(site):
        op = word(site)
        if op >> 11 != 0x1a:
            raise ValueError("Unexpected relative call")
        displacement = op & 0x7ff
        if displacement >= 1024:
            displacement -= 2048
        return site + 2 + 2 * displacement

    # FR DMOVH, pointer load, and byte store, with the low byte in r0.
    if image[0x3f8a:0x3f96].hex() != "09328bd09b3c620c04c11610":
        raise ValueError("Receiver input sequence changed")
    dispatch = call_target(0x3fae)
    parser = call_target(0x400a)
    if (dispatch, parser) != (0x3fe2, 0x4056):
        raise ValueError("Receiver dispatch changed")
    if (word(0x405e), word(0x4060)) != (0x0600, 0xaa10):
        raise ValueError("Unexpected delimiter comparison")
    groups = []
    for site, count in ((0x40ec, 3), (0x412c, 3), (0x416c, 5), (0x41ac, 3)):
        expected = []
        for _ in range(count):
            offset, load, char, cmp, branch = unpack_from(">5H", image, site)
            if (offset & 0xff0f, load, char & 0xf00f, cmp, branch >> 8) != (
                    0xc00d, 0x0250, 0xc001, 0xaa10, 0xe3):
                raise ValueError(f"Unexpected character test at {site:x}")
            expected.append(((offset >> 4) & 15, chr((char >> 4) & 255)))
            site += 10
        groups.append(dict(first_position=expected[0][0], text="".join(c for _, c in expected)))
    return dict(port=(word(0x3f8a) & 255) * 2, dispatch_offset=dispatch,
                delimiter=chr((word(0x405c) >> 4) & 255), character_tests=groups)


def channels(image):
    """Byte-checked map and callback installations, not peripheral identification."""
    def check(site, expected):
        if image[site:site + len(bytes.fromhex(expected))] != bytes.fromhex(expected):
            raise ValueError(f"A channel probe changed at {site:x}")

    def ldi20(site, register):
        op, low = unpack_from(">HH", image, site)
        if op & 0xff0f != 0x9b00 | register:
            raise ValueError(f"Expected LDI20 at {site:x}")
        return ((op >> 4) & 15) << 16 | low

    reset = unpack_from(">I", image, 0x7fffc)[0]
    if reset != 0xfc00e:
        raise ValueError("A reset changed")
    check(0x7c00e, "9f8f0003e8cc87109f80000ffc00b300")
    check(0x7c026, "9f8d00080042971d")
    check(0x13ac, "0440a800e20304409710")
    bindings = []
    for site in (0x3f1a, 0x4dd6, 0x4f8c, 0x7cbc):
        target = ldi20(site, 0)
        slot = ldi20(site + 4, 12)
        check(site + 8, "14c0")
        if slot != 0x3609c:
            raise ValueError("Unexpected receive callback slot")
        bindings.append(dict(installation_cpu=site + 0x80000,
                             callback_cpu=target, slot_cpu=slot))
    vectors = []
    for index, expected in ((233, 0x813ca), (234, 0x8139e), (235, 0x808a4)):
        target = unpack_from(">I", image, 0x7fc00 + index * 4)[0]
        if target != expected:
            raise ValueError("A vector changed")
        vectors.append(dict(index=index, target_cpu=target))
    check(0x5134, "09328bd004411610")
    check(0x7d02, "09328bd004411610")
    check(0x2c87e, "09428bd006ad1280")
    return dict(file_to_cpu_delta=0x80000, mapped_size=0x80000,
                reset_cpu=reset, vector_base_cpu=0xffc00,
                vectors=vectors, receive_callback_installations=bindings,
                port64_buffer_pointer_cpu=0x36208,
                port84_read_cpu=0xac87e,
                scope="Static map and callback selection; physical peer and B index 0x46 unresolved")
