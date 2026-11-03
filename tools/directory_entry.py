"""Replay the straight-line FR block at 0x1f4122 on a synthetic FAT entry."""
import struct


def replay(image, entry):
    if len(entry) != 32:
        raise ValueError("A short FAT directory entry is 32 bytes")
    registers = [0] * 16
    registers[14] = 36  # input entry starts at frame pointer minus 36
    registers[10] = 0x100  # synthetic output descriptor
    stores = {}
    for pc in range(0x1f4122, 0x1f416c, 2):
        word = struct.unpack_from(">H", image, pc)[0]
        op, src, dst = word >> 8, (word >> 4) & 15, word & 15
        if word >> 12 == 0xc:
            registers[dst] = (word >> 4) & 255
        elif op == 0x8b:
            registers[dst] = registers[src]
        elif op == 0xa5:
            registers[dst] += src - 16
        elif op in (0x02, 0x06):
            addr = registers[src] + (registers[13] if op == 0x02 else 0)
            if not 0 <= addr < len(entry):
                raise ValueError(f"Unexpected input address {addr}")
            registers[dst] = entry[addr]
        elif op in (0xb4, 0xb5):
            registers[dst] <<= src + (16 if op == 0xb5 else 0)
        elif op == 0xa6:
            registers[dst] += registers[src]
        elif op in (0x10, 0x11):
            width = 4 if op == 0x10 else 2
            offset = registers[src] + registers[13] - registers[10]
            stores[offset] = registers[dst] & ((1 << (8 * width)) - 1)
        elif op == 0x97 and src == 8:  # extub
            registers[dst] &= 255
        else:
            raise ValueError(f"Unsupported instruction {word:04x} at {pc:x}")
        registers[dst] &= 0xffffffff
    return stores


def inspect(image):
    # Flip each input bit independently to verify position and byte order.
    for bit in range(256):
        entry = bytearray(32)
        entry[bit // 8] = 1 << (bit % 8)
        stores = replay(image, entry)
        expected = {14: int.from_bytes(entry[24:26], "little"),
                    16: int.from_bytes(entry[22:24], "little"),
                    20: int.from_bytes(entry[28:32], "little")}
        if stores != expected:
            raise ValueError(f"Field mismatch for input bit {bit}")
    entry = bytearray(32)
    entry[28:32] = len(image).to_bytes(4, "little")
    return dict(block=[0x1f4122, 0x1f416c], one_bit_cases=256,
                source_offsets=[24, 22, 28], descriptor_offsets=[14, 16, 20],
                sample_size_replayed=replay(image, entry)[20])
