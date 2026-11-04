// Follow the original selector-0x22 preparation to command issue, using RAM-backed MMIO.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;

public class D90AtamCommandMask extends GhidraScript {
    private void put(EmulatorHelper cpu, long address, int value) {
        cpu.writeMemory(toAddr(address), ByteBuffer.allocate(4).putInt(value).array());
    }

    private void expect(EmulatorHelper cpu, long address, int expected) {
        int actual = ByteBuffer.wrap(cpu.readMemory(toAddr(address), 4)).getInt();
        if (actual != expected)
            throw new IllegalStateException(String.format("At %x expected %08x, got %08x",
                address, expected, actual));
    }

    private void advance(EmulatorHelper cpu, long stop) throws Exception {
        for (int i = 0; i < 200; i++) {
            if (cpu.getExecutionAddress().getOffset() == stop) return;
            if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Missed " + Long.toHexString(stop));
    }

    private void sample(int initial, boolean busy) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            long control = 0x63000034, mask = 0x63000038, command = 0x6300000c;
            int other = 0x80000000;
            cpu.writeRegister("SP", 0x81004000L);
            cpu.writeRegister("FP", 0x81005000L);
            for (String reg : new String[]{"RP","R4","R8","R9","R10"}) cpu.writeRegister(reg, 0);
            put(cpu, control, other);
            put(cpu, mask, initial);
            put(cpu, command, -1);
            put(cpu, 0x63000024, busy ? 1 : 0);
            put(cpu, 0x6800cd08, 1);
            put(cpu, 0x6800cd10, 0x6800cd14);
            cpu.writeMemory(toAddr(0xdc0), new byte[28]);
            cpu.writeMemory(toAddr(0x6800cf1e), new byte[]{-1});
            cpu.writeRegister("PC", 0x1a07ae);
            if (busy) {
                advance(cpu, 0x1a08aa);
                expect(cpu, mask, initial);
                expect(cpu, control, other);
                expect(cpu, command, -1);
                if (cpu.readMemory(toAddr(0x6800cf1e), 1)[0] != -1)
                    throw new IllegalStateException("Selector changed on retry");
                return;
            }
            advance(cpu, 0x1a07e6);
            expect(cpu, mask, initial | 0x007f0021);
            expect(cpu, control, other | 0x007f0021);
            advance(cpu, 0x1a07f6);
            int masked = (initial | 0x007f0021) & ~0x20;
            expect(cpu, mask, masked);
            advance(cpu, 0x1a080a);
            expect(cpu, mask, masked);
            advance(cpu, 0x1a0818);
            expect(cpu, mask, masked | 0x01700022);
            expect(cpu, control, other | 0x007f0021 | 0x01700022);
            expect(cpu, command, -1);
            advance(cpu, 0x1a0826);
            expect(cpu, command, 0x223a0011);
            expect(cpu, mask, masked | 0x01700022);
            if (cpu.readMemory(toAddr(0x6800cf1e), 1)[0] != 0x22)
                throw new IllegalStateException("Selector mismatch");
        } finally {
            cpu.dispose();
        }
    }

    public void run() throws Exception {
        sample(0, false);
        sample(0x80000002, false);
        sample(0x20, true);
        println("Selector 0x22 preparation: two accepted paths and one rejected attempt passed");
    }
}
