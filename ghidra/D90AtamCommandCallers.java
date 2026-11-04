// Reproduce three callers of the register-fed ATAM command path on component B.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;

public class D90AtamCommandCallers extends GhidraScript {
    private void put(EmulatorHelper cpu, long address, int value) {
        cpu.writeMemory(toAddr(address), ByteBuffer.allocate(4).putInt(value).array());
    }

    private int word(EmulatorHelper cpu, long address) {
        return ByteBuffer.wrap(cpu.readMemory(toAddr(address), 4)).getInt();
    }

    private void check(long start, int command, int argument, boolean busy) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            cpu.writeRegister("SP", 0x81004000L);
            cpu.writeRegister("FP", 0x81005000L);
            for (int r = 8; r <= 11; r++) cpu.writeRegister("R" + r, 0);
            cpu.writeRegister("R6", 3);
            cpu.writeRegister("R8", 0x81001000L);
            put(cpu, 0x81001000L, 5);
            put(cpu, 0x81001004L, 2);
            put(cpu, 0x63000024, busy ? 1 : 0);
            put(cpu, 0x63000034, 0);
            put(cpu, 0x63000038, 0);
            put(cpu, 0x63000008, -1);
            put(cpu, 0x6300000c, -1);
            cpu.writeMemory(toAddr(0x6800cf1e), new byte[]{-1});
            cpu.writeRegister("PC", start);
            long stop = busy ? 0x19f2c8 : 0x19f2b2;
            for (int step = 0; step < 200; step++) {
                if (cpu.getExecutionAddress().getOffset() == stop) break;
                if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            }
            int selector = cpu.readMemory(toAddr(0x6800cf1e), 1)[0] & 255;
            if (cpu.getExecutionAddress().getOffset() != stop ||
                word(cpu, 0x63000008) != (busy ? -1 : argument) ||
                word(cpu, 0x6300000c) != (busy ? -1 : command) ||
                selector != (busy ? 255 : command >>> 24))
                throw new IllegalStateException("Caller " + Long.toHexString(start) + ", busy=" + busy);
        } finally {
            cpu.dispose();
        }
    }

    public void run() throws Exception {
        for (boolean busy : new boolean[]{false, true}) {
            check(0x1a0d92, 0x201a0000, 3 << 9, busy);
            check(0x1a0dd2, 0x211a0000, 7 << 9, busy);
            check(0x1a0e18, 0x261b0000, 7 << 9, busy);
        }
        println("All three firmware callers passed with the gate open and closed");
    }
}
