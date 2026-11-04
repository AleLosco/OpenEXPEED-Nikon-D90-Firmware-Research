// Exercise the command writer and the notification predicate on the same synthetic memory.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;

public class D90AtamCommand extends GhidraScript {
    private void trace(EmulatorHelper cpu, long start, long stop) throws Exception {
        cpu.writeRegister("PC", start);
        for (int i = 0; i < 64; i++) {
            if (cpu.getExecutionAddress().getOffset() == stop) return;
            if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Missed stop " + Long.toHexString(stop));
    }

    private void check(EmulatorHelper cpu, long start, long stop, int command) throws Exception {
        int selector = (command >>> 24) & 63;
        int expected = selector == 0x12 || selector == 0x19 ? command | 6 : command;
        cpu.writeRegister("SP", 0x81004000L);
        cpu.writeRegister("FP", 0x81005000L);
        cpu.writeRegister("R4", start == 0x19f762 ? Integer.toUnsignedLong(command) : 0);
        cpu.writeMemory(toAddr(0x6800cf1e), new byte[]{-1});
        cpu.writeMemory(toAddr(0x6300000c), new byte[4]);
        trace(cpu, start, stop);
        int saved = cpu.readMemory(toAddr(0x6800cf1e), 1)[0] & 255;
        int written = ByteBuffer.wrap(cpu.readMemory(toAddr(0x6300000c), 4)).getInt();
        if (saved != selector || written != expected)
            throw new IllegalStateException("Command " + Integer.toHexString(command));
        boolean posts = selector == 0x12 || selector == 0x19 || selector == 0x22;
        cpu.writeRegister("R8", 0x6800cf1e);
        cpu.writeRegister("R5", 0);
        trace(cpu, 0x19a75c, posts ? 0x19a780 : 0x19a784);
        if (posts && cpu.readRegister("R5").intValue() != 0x20000000)
            throw new IllegalStateException("Unexpected event mask");
    }

    public void run() throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            check(cpu, 0x19f04a, 0x19f052, 0x123a0037);
            check(cpu, 0x19f152, 0x19f15a, 0x193a0027);
            check(cpu, 0x1a0818, 0x1a0826, 0x223a0011);
            check(cpu, 0x1a0916, 0x1a0924, 0x233a0000);
            for (int selector = 0; selector < 64; selector++) {
                int command = 0xc0120001 | (selector << 24);
                check(cpu, 0x19f762, 0x19f79c, command);
            }
            println("Four firmware call sites and all 64 synthetic selectors passed");
        } finally {
            cpu.dispose();
        }
    }
}
