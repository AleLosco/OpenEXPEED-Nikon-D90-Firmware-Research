"""Link the two updater preparation calls to the candidate English text table."""
from struct import unpack_from


def locale_record(image, selector, resource):
    # The dispatcher indexes 32-bit code addresses; each case sets descriptor +16.
    def address(site):
        return unpack_from(">I", image, site)[0] + 0x40002

    case = address(0x21728a + 4 * selector)
    limit = address(0x21728a + 4 * (selector + 1))
    store = image.index(bytes.fromhex("c10d9f8c00187a2a9f0c1050"), case, limit)
    if image[store - 6:store - 4] != bytes.fromhex("9f80"):
        raise ValueError("Unexpected locale table load")
    table = address(store - 4)
    slot = table + 4 * (resource & 0x1fff)
    start, end = address(slot), address(slot + 4)
    lines = image[start + 1:end].decode("ascii").splitlines()
    if not lines or len(lines) != image[start]:
        raise ValueError("Unexpected localized resource boundaries")
    return dict(selector=selector, case_offset=case, table_offset=table,
                table_slot=slot, record_offset=start, lines=lines)


def inspect(image):
    def u32(site):
        return unpack_from(">I", image, site)[0]

    rows = []
    for site, prep_site in ((0x27ae40, 0x27ae4c), (0x27ae54, 0x27ae60)):
        opcode, resource = unpack_from(">HH", image, site)
        if opcode != 0x9b04 or image[site + 4:site + 6].hex() != "9f8c":
            raise ValueError("Unexpected message selection")
        prep = u32(prep_site + 2) + 0x40002
        english = locale_record(image, 1, resource)
        italian = locale_record(image, 4, resource)
        rows.append(dict(resource=hex(resource), call_site=site, renderer_candidate=
                         u32(site + 6) + 0x40002, preparation=prep,
                         table_slot=english["table_slot"], record_offset=english["record_offset"],
                         lines=english["lines"], localized=dict(en=english, it=italian)))
    return rows
