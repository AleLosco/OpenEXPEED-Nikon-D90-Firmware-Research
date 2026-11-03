"""Read-only structural probes for the supplied D90 A/B update."""
import binascii
import re

import address_map
import a_receiver
import a_commands
import b_transaction
import a_transport
import a_port84


def inspect(data):
    """All offsets refer to the original file, all ranges are half-open."""
    if len(data) < 2:
        raise ValueError("A two-byte trailer is required")
    trailer = int.from_bytes(data[-2:], "big")
    calculated = binascii.crc_hqx(memoryview(data)[:-2], 0)
    markers = []
    for pattern in (rb"D90  Firmware [AB] Copyright Nikon Corp\.",
                    rb"Ver\.[0-9][\x20-\x7e]{0,95}"):
        for match in re.finditer(pattern, data):
            markers.append({"offset": match.start(), "text": match[0].decode("ascii")})
    fills = [{"start": m.start(), "end": m.end(), "byte": m[0][0]}
             for m in re.finditer(rb"\xff{65536,}|\x00{65536,}", data)]
    return {
        "markers": markers,
        "a_serial_receiver": a_receiver.inspect(data),
        "a_channels": a_receiver.channels(data),
        "a_commands": a_commands.inspect(data),
        "b_transaction": b_transaction.inspect(data),
        "a_transport": a_transport.inspect(data),
        "a_d4_frame": a_transport.frame_inspect(data),
        "a_port84": a_port84.inspect(data),
        "address_map": address_map.inspect(data),
        "uniform_runs_min_65536": fills,
        "trailer_crc16_xmodem": {
            "covered_start": 0,
            "covered_end": len(data) - 2,
            "stored_big_endian": f"{trailer:04x}",
            "computed": f"{calculated:04x}",
            "matches": calculated == trailer,
        },
    }
