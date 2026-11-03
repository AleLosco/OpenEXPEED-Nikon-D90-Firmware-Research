"""Bounded FR execution of the D90 program, poll and readback paths."""
from struct import unpack_from


class Bus:
    def __init__(self, image, scenario):
        self.words = {0x80000000: unpack_from(">H", image, 0x80002)[0],
                      0x80000002: unpack_from(">H", image, 0x80004)[0]}
        self.flash = {0x40000: 0xffff, 0x40002: 0xffff}
        self.words.update({0x8f532ea4: 1, 0x8f532ec4: 0,
                           0x8f532ea8: 0, 0x8f532eac: 0x10000})
        self.scenario = scenario
        self.pending = None
        self.phase = 0
        self.erasing = False
        self.poll_reads = []
        self.readbacks = 0
        self.reset = False

    def read(self, addr):
        if addr not in self.flash:
            return self.words[addr]
        if self.pending is None:
            self.readbacks += 1
            return self.flash[addr]
        target, value = self.pending
        if target != addr:
            raise ValueError("Polling an unexpected NOR address")
        self.poll_reads[-1] += 1
        reads = self.poll_reads[-1]
        if reads <= 2:
            return (value ^ 0x80) & 0x80
        if self.scenario == "error" or (self.scenario == "late_ready" and reads == 3):
            return ((value ^ 0x80) & 0x80) | 0x20
        if self.erasing:
            for address in self.flash:
                if target <= address < target + 0x20000:
                    self.flash[address] = 0xffff
            self.erasing = False
        else:
            self.flash[addr] &= value
        self.pending = None
        return self.flash[addr]

    def write(self, addr, value):
        if addr == 0x40aaa and value == 0xf0:
            self.reset = True
            self.erasing = False
            self.pending = None
            self.phase = 0
        elif self.phase == 2 and (addr, value) == (0x40aaa, 0x80):
            self.phase = 4
        elif self.phase in (4, 5):
            if (addr, value) != [(0x40aaa, 0xaa), (0x40554, 0x55)][self.phase - 4]:
                raise ValueError("Unexpected erase unlock")
            self.phase += 1
        elif self.phase == 6 and addr in self.flash and addr % 0x20000 == 0 and value == 0x30:
            self.pending = addr, 0xffff
            self.erasing = True
            self.poll_reads.append(0)
            self.phase = 0
        elif self.phase == 3 and addr in self.flash:
            self.pending = addr, value
            self.poll_reads.append(0)
            self.phase = 0
        elif addr in (0x40aaa, 0x40554):
            expected = [(0x40aaa, 0xaa), (0x40554, 0x55), (0x40aaa, 0xa0)]
            if self.phase >= 3 or expected[self.phase] != (addr, value):
                raise ValueError("Unexpected NOR command")
            self.phase += 1
        elif 0xff0000 <= addr < 0x1000000 or addr in self.words:
            self.words[addr] = value
        else:
            raise ValueError(f"Unexpected store to {addr:x}")


def signed(value, bits):
    return value - (1 << bits) if value & (1 << (bits - 1)) else value


def execute(image, bus, pc, args, controller=None):
    r = [0] * 16
    r[4:7] = args
    r[15] = 0xffff00
    slots = (0x18, 0x1c, 0x20, 0x818, 0x81c, 0x820)
    if controller is not None:
        # Live-ins from ldi:32 r8/r11 in the same updater prologue.
        if image[0x2e5994:0x2e5996] != bytes.fromhex("9f88") or image[0x2e5942:0x2e5944] != bytes.fromhex("9f8b"):
            raise ValueError("Unexpected outcome base loads")
        r[8] = unpack_from(">I", image, 0x2e5996)[0]
        r[11] = unpack_from(">I", image, 0x2e5944)[0]
        for offset in slots:
            bus.words[r[8] + offset] = None
    rp, delayed = -1, None
    z, negative, carry = False, False, False
    for steps in range(2000):
        if pc == -1:
            return dict(result=r[4], instructions=steps, poll_reads=bus.poll_reads,
                        verification_reads=bus.readbacks, reset=bus.reset,
                        flash=[bus.flash[0x40000], bus.flash[0x40002]])
        if controller is not None:
            if pc == 0x2e5b5c:
                values = [bus.words[r[8] + offset] for offset in slots]
                if None in values:
                    raise ValueError("Missing outcome register write")
                return dict(attempts_left=r[9], calls=controller.calls,
                            outcome_base=r[8], outcome_offsets=slots, outcome_values=values)
            if pc in (0x2e5c06, 0x2e5c4c):
                r[4] = controller.invoke(pc, r[4:7])
                pc = rp
                continue
        if not ((controller is not None and 0x2e5a54 <= pc < 0x2e5b5c)
                or 0x2e5c06 <= pc < 0x2e5f5c):
            raise ValueError(f"Unexpected PC {pc:x}")
        site = pc
        w = unpack_from(">H", image, pc)[0]
        pc += 2
        target, delayed = delayed, None
        op, j, i = w >> 8, (w >> 4) & 15, w & 15
        if op in (0x8e, 0x8f):
            for k in range(8):
                if w & (1 << k):
                    value = r[(15 if op == 0x8f else 7) - k]
                    r[15] -= 4
                    bus.write(r[15], value)
        elif op == 0x8d:
            for k in range(8):
                if w & (1 << k):
                    r[8+k] = bus.read(r[15])
                    r[15] += 4
        elif w == 0x1781:
            r[15] -= 4
            bus.write(r[15], rp)
        elif w == 0x0781:
            rp = bus.read(r[15])
            r[15] += 4
        elif op == 0x0f:
            bus.write(r[15]-4, r[14])
            r[14], r[15] = r[15]-4, r[15] - (w & 255)*4
        elif w == 0x9f90:
            r[15], r[14] = r[14]+4, bus.read(r[14])
        elif op == 0xa3:
            r[15] += signed(w & 255, 8)*4
        elif w == 0x9720:
            pc = rp
        elif w >> 12 == 0xd:
            dest = pc + 2*signed(w & 2047, 11)
            rp = pc + (2 if w & 0x800 else 0)
            if w & 0x800:
                delayed = dest
            else:
                pc = dest
        elif w >> 12 in (0xe, 0xf):
            cond = {0: True, 2: z, 3: not z, 12: negative or z, 13: not (negative or z),
                    15: not (carry or z)}[(w >> 8) & 15]
            if cond:
                dest = pc + signed(w & 255, 8)*2
                if w >> 12 == 15:
                    delayed = dest
                else:
                    pc = dest
        elif w >> 12 == 0xc:
            r[i] = (w >> 4) & 255
        elif w & 0xfff0 == 0x9f80:
            r[i] = unpack_from(">I", image, pc)[0]
            pc += 4
        elif op == 0x9b:
            r[i] = (j << 16) | unpack_from(">H", image, pc)[0]
            pc += 2
        elif op in (0x04, 0x05, 0x06):
            r[i] = bus.read(r[j]) & {4:0xffffffff, 5:0xffff, 6:255}[op]
        elif op == 0x10:
            bus.write(r[13] + r[j], r[i])
        elif op in (0x14, 0x15):
            bus.write(r[j], r[i] & (0xffff if op == 0x15 else 0xffffffff))
        elif w >> 12 in (2, 3):
            addr = r[14] + signed((w >> 4) & 255, 8)*4
            if w >> 12 == 2:
                r[i] = bus.read(addr)
            else:
                bus.write(addr, r[i])
        elif op == 0x8b:
            r[i] = r[j]
        elif op in (0xac, 0xa6):
            r[i] = (r[i] + (r[j] if op == 0xa6 else -r[j])) & 0xffffffff
        elif op in (0xaa, 0xa8, 0x82, 0xa4, 0xa5):
            if op in (0xaa, 0xa8):
                rhs = r[j] if op == 0xaa else j
                v = r[i] - rhs
                carry = r[i] < rhs
            else:
                v = r[i] & r[j] if op == 0x82 else r[i] + (j if op == 0xa4 else j-16)
                r[i] = v & 0xffffffff
            z, negative = (v & 0xffffffff) == 0, bool(v & 0x80000000)
        elif op == 0xb0:
            r[i] >>= j
        else:
            raise ValueError(f"Unsupported FR instruction {w:04x} at {site:x}")
        if target is not None:
            pc = target
    raise ValueError("Polling exceeded instruction budget")


def run(image, scenario):
    return execute(image, Bus(image, scenario), 0x2e5c4c, [0x40000, 0x80000000, 4])


def erase_then_program(image):
    bus = Bus(image, "ready")
    bus.flash = dict.fromkeys(range(0x40000, 0x60000, 2), 0)
    erase = execute(image, bus, 0x2e5da6, [0x40000, 0, 0])
    if erase["result"] != 1 or set(bus.flash.values()) != {0xffff}:
        raise ValueError("Sector erase failed")
    erase_reads = bus.poll_reads.pop()
    write = execute(image, bus, 0x2e5c4c, [0x40000, 0x80000000, 4])
    expected = list(unpack_from(">2H", image, 0x80002))
    if write["result"] != 1 or write["flash"] != expected:
        raise ValueError("Programming after erase failed")
    if any(bus.flash[a] != 0xffff for a in range(0x40004, 0x60000, 2)):
        raise ValueError("Programming changed an unrelated word")
    failed = Bus(image, "error")
    failed.flash = dict.fromkeys(bus.flash, 0)
    rejected = execute(image, failed, 0x2e5da6, [0x40000, 0, 0])
    if rejected["result"] != 0 or not failed.reset or any(failed.flash.values()):
        raise ValueError("Unexpected failed erase outcome")
    return dict(failed_erase_result=rejected["result"], failed_erase_reset=failed.reset,
                sector_bytes=len(bus.flash)*2, programmed_bytes=4,
                erase_instructions=erase["instructions"], erase_poll_reads=erase_reads,
                write_instructions=write["instructions"], final_words=write["flash"])


class RangeController:
    """Supply deterministic range-operation results to the real retry loop."""
    def __init__(self, failures, error_bus=None, image=None):
        self.error_bus = error_bus
        self.image = image
        self.failures = failures
        self.calls = []

    def invoke(self, target, args):
        index = len(self.calls)
        erase = target == 0x2e5c06
        if self.error_bus is not None and erase:
            result = execute(self.image, self.error_bus, target, args)["result"]
        else:
            result = int(index not in self.failures)
        self.calls.append(dict(operation="erase" if erase else "program",
                               arguments=args[:2] if erase else args, result=result))
        return result


def retry_paths(image):
    results = {}
    for name, failures, sequence, remaining in (
        ("ready", (), "eeepp", 3),
        ("erase_error", (0, 1, 2), "eee", 0),
        ("second_erase_recovers", (1,), "eeeeepp", 2),
        ("program_error", (3, 7, 11), "eeep" * 3, 0),
        ("last_program_recovers", (4,), "eeepp" * 2, 2),
    ):
        ctl = RangeController(failures)
        trace = execute(image, Bus(image, "ready"), 0x2e5a54, [0, 0, 0], ctl)
        operations = "".join(call["operation"][0] for call in ctl.calls)
        first = 0x895136c0 + (0 if remaining else 0x70800)
        expected_values = [first + offset for offset in (0, 0x4b000, 0x5dc00, 0xe1000, 0x12c000, 0x13ec00)]
        if trace["outcome_base"] != 0x40040000 or trace["outcome_values"] != expected_values:
            raise ValueError(f"Unexpected outcome writes for {name}")
        if operations != sequence or trace["attempts_left"] != remaining:
            raise ValueError(f"Unexpected retry trace: {name} {trace}")
        results[name] = trace
    error_bus = Bus(image, "error")
    error_bus.flash[0xc0000] = 0
    ctl = RangeController((), error_bus, image)
    trace = execute(image, error_bus, 0x2e5a54, [0, 0, 0], ctl)
    if (trace["attempts_left"] != 0 or len(ctl.calls) != 3
            or any(c["operation"] != "erase" or c["result"] for c in ctl.calls)
            or error_bus.poll_reads != [4, 4, 4]):
        raise ValueError("Unexpected end-to-end erase failure")
    results["actual_dq5_failure"] = dict(**trace, poll_reads=error_bus.poll_reads)
    return results


def inspect(image):
    good, bad = run(image, "ready"), run(image, "error")
    late = run(image, "late_ready")
    expected = list(unpack_from(">2H", image, 0x80002))
    if good['result'] != 1 or good['flash'] != expected or good['poll_reads'] != [3, 3]:
        raise ValueError(f"Unexpected successful path: {good}")
    if bad['result'] != 0 or not bad['reset'] or bad['flash'] != [0xffff]*2:
        raise ValueError(f"Unexpected failure path: {bad}")
    if late["result"] != 1 or late["flash"] != expected or late["poll_reads"] != [4, 4]:
        raise ValueError(f"Unexpected DQ5 recovery path: {late}")
    return dict(controller=retry_paths(image), erase_then_program=erase_then_program(image), busy_then_ready=good, dq5_error=bad, ready_on_recheck=late)
