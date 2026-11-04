// Check the R10 initializer and the six isolated byte-store blocks from AB-65.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.util.Arrays;

public class D90AtamStatusStores extends GhidraScript {
    private void step(EmulatorHelper cpu) throws Exception {
        if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
    }

    private void check(long initializer, long block, int code, int status) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            long base = 0x6800cf1bL;
            byte[] initial = {(byte) 0xff, 0x55, 0x66, 0x33, (byte) status};
            cpu.writeMemory(toAddr(base), initial);
            cpu.writeRegister("R10", 0);
            cpu.writeRegister("PC", initializer);
            step(cpu);
            if (cpu.readRegister("R10").longValue() != base + 4)
                throw new IllegalStateException("Unexpected R10 initializer");

            cpu.writeRegister("PC", block);
            for (int i = 0; i < 7; i++) step(cpu);
            byte[] expected = initial.clone();
            expected[0] = (byte) code;
            expected[4] = (byte) (status | 1);
            if (!Arrays.equals(expected, cpu.readMemory(toAddr(base), expected.length)))
                throw new IllegalStateException("Unexpected writes at " + Long.toHexString(block));
        } finally {
            cpu.dispose();
        }
    }

    public void run() throws Exception {
        for (int status : new int[]{0, 0x80, 0xa5}) {
            check(0x19e1d6, 0x19e204, 0x10, status);
            check(0x19e1d6, 0x19e26a, 0x52, status);
            check(0x19e1d6, 0x19e2ce, 0x50, status);
            check(0x19e2fe, 0x19e350, 0x10, status);
            check(0x19e2fe, 0x19e498, 0x50, status);
            check(0x19e2fe, 0x19e594, 0x52, status);
        }
        println("Six R10 blocks passed for three initial status bytes each");
    }
}
