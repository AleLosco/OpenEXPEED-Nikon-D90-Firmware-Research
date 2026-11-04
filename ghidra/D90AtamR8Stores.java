// Verify the two R8 status stores from AB-65 and the branches that select them.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.util.Arrays;

public class D90AtamR8Stores extends GhidraScript {
    private void step(EmulatorHelper cpu) throws Exception {
        if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
    }

    private void check(long initializer, long compare, long stop, int result,
                       int status, boolean writes) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            byte[] initial = {0x33, (byte) status};
            cpu.writeMemory(toAddr(0x6800cf1e), initial);
            cpu.writeRegister("R8", 0);
            cpu.writeRegister("PC", initializer);
            step(cpu);
            if (cpu.readRegister("R8").longValue() != 0x6800cf1fL)
                throw new IllegalStateException("R8 initializer");
            cpu.writeRegister("R4", result);
            cpu.writeRegister("PC", compare);
            for (int i = 0; i < 12 && cpu.getExecutionAddress().getOffset() != stop; i++)
                step(cpu);
            byte[] expected = {0x33, (byte) (writes ? status | 1 : status)};
            if (cpu.getExecutionAddress().getOffset() != stop ||
                !Arrays.equals(expected, cpu.readMemory(toAddr(0x6800cf1e), 2)))
                throw new IllegalStateException("Branch or byte mismatch at " + Long.toHexString(compare));
        } finally {
            cpu.dispose();
        }
    }

    public void run() throws Exception {
        for (int status : new int[]{0, 0x80, 0xa5}) {
            check(0x1a0a6a, 0x1a0ac6, 0x1a0b44, 1, status, true);
            check(0x1a0a6a, 0x1a0ac6, 0x1a0aca, 0, status, false);
            check(0x1a0ba2, 0x1a0bd6, 0x1a0c36, 0, status, true);
            check(0x1a0ba2, 0x1a0bd6, 0x1a0bda, 1, status, false);
        }
        println("Both R8 status stores and alternate branches passed for three initial bytes");
    }
}
