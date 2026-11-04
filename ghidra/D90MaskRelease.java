// Exercise consecutive bit removals on one synthetic element.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;

public class D90MaskRelease extends GhidraScript {
    private void runTo(EmulatorHelper cpu, long start, long stop) throws Exception {
        cpu.writeRegister("PC", start);
        for (int i = 0; i < 200; i++) {
            if (cpu.getExecutionAddress().getOffset() == stop) return;
            if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Expected stop " + Long.toHexString(stop));
    }
    public void run() throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        long element = 0x8f8715bcL;
        try {
            cpu.writeRegister("R8", element);
            cpu.writeMemory(toAddr(element + 0x4a), new byte[]{0, 3});
            for (int mask : new int[]{1, 2}) {
                cpu.writeRegister("R6", mask);
                runTo(cpu, 0x10305c, mask == 1 ? 0x1030a0 : 0x103070);
                byte[] field = cpu.readMemory(toAddr(element + 0x4a), 2);
                if (field[0] != 0 || field[1] != (mask == 1 ? 2 : 0))
                    throw new IllegalStateException("Field mismatch");
            }
            println("Mask release passed: 3 -> 2 -> 0, both branch destinations reached");
        } finally { cpu.dispose(); }
    }
}
