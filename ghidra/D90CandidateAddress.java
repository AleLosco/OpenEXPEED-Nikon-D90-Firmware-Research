// Check the base load and terminal store in two bounded B-image blocks.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.util.Arrays;

public class D90CandidateAddress extends GhidraScript {
    private void runTo(EmulatorHelper cpu, long start, long stop) throws Exception {
        cpu.writeRegister("PC", start);
        for (int i = 0; i < 512; i++) {
            if (cpu.getExecutionAddress().getOffset() == stop) return;
            if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Step limit");
    }

    public void run() throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            runTo(cpu, 0x10897c, 0x10898e);
            long base = cpu.readRegister("R9").longValue();
            if (base != 0x8f52ebd8L) throw new IllegalStateException("Unexpected base");
            cpu.writeRegister("SP", 0x8f9f0000L);
            cpu.writeMemory(toAddr(0x8f52e48aL), new byte[]{0, 12});
            byte[] sentinel = {(byte)0xa5, 0x5a};
            cpu.writeMemory(toAddr(base + 0x4a), sentinel);
            long poolField = 0x8f871606L;
            cpu.writeMemory(toAddr(poolField), sentinel);
            runTo(cpu, 0x108df6, 0x108e06);
            int value = cpu.readRegister("R4").intValue() & 0xffff;
            runTo(cpu, 0x108e06, 0x108e08);
            byte[] expected = {(byte)(value >> 8), (byte)value};
            if (Arrays.equals(sentinel, expected) ||
                !Arrays.equals(expected, cpu.readMemory(toAddr(base + 0x4a), 2)) ||
                !Arrays.equals(sentinel, cpu.readMemory(toAddr(poolField), 2)))
                throw new IllegalStateException("Unexpected write");
            println("Destination 8f52ec22, value=" + value + ", pool sentinel intact");
        } finally { cpu.dispose(); }
    }
}
