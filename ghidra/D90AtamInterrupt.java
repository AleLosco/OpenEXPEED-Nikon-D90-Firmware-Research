// Follow component B's vector 0x2c to the first OS notification using RAM-backed MMIO.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;

public class D90AtamInterrupt extends GhidraScript {
    private int passedCases;
    private void put(EmulatorHelper cpu, long address, int value) {
        cpu.writeMemory(toAddr(address), ByteBuffer.allocate(4).putInt(value).array());
    }

    private int word(EmulatorHelper cpu, long address) {
        return ByteBuffer.wrap(cpu.readMemory(toAddr(address), 4)).getInt();
    }

    private void check(long entry, int mask, int status, int selector,
                       int handledBit, boolean expectsPost) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            cpu.writeRegister("SP", 0x81004000L);
            for (String reg : new String[]{"R0","R1","R2","R3","R4","R5","R6","R7",
                                           "R8","R9","R12","AC","MDH","MDL","RP"})
                cpu.writeRegister(reg, 0);
            put(cpu, 0x63000038, mask);
            put(cpu, 0x63000030, status);
            put(cpu, 0x6800cd00, 0x800);
            cpu.writeMemory(toAddr(0x6800cf1e), new byte[]{(byte) selector});
            cpu.writeRegister("PC", entry);
            boolean entered = false;
            for (int i = 0; i < 160; i++) {
                long pc = cpu.getExecutionAddress().getOffset();
                if (pc == 0x19a71e) entered = true;
                if (pc == 0x19a780 || pc == 0x19afe6) {
                    if (entered != (handledBit == 2) || (pc == 0x19a780) != expectsPost ||
                        cpu.readRegister("R5").intValue() != (expectsPost ? 0x20000000 : 0x800000) ||
                        cpu.readRegister("R4").intValue() != 7 || cpu.readRegister("R12").intValue() != -48 ||
                        word(cpu, 0x63000038) != (mask & ~handledBit) ||
                        word(cpu, 0x63000030) != (handledBit == 0 ? status : handledBit) ||
                        word(cpu, 0x6800cd00) != (0x800 | handledBit))
                        throw new IllegalStateException("Unexpected branch, arguments or MMIO");
                    passedCases++;
                    println(String.format("Case %d mask=0x%x status=0x%x selector=0x%x entered=%s stop=0x%x R5=0x%x finalStatus=0x%x",
                        passedCases, mask, status, selector, entered, pc,
                        cpu.readRegister("R5").intValue(), word(cpu, 0x63000030)));
                    return;
                }
                if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            }
            throw new IllegalStateException("OS boundary not reached");
        } finally {
            cpu.dispose();
        }
    }

    public void run() throws Exception {
        passedCases = 0;
        long entry = currentProgram.getMemory().getInt(toAddr(0xdff4c));
        if (entry != 0x42de8) throw new IllegalStateException("Vector mismatch");
        for (int selector : new int[]{0x12,0x19,0x22,0x23})
            check(entry, 2, 2, selector, 2, selector != 0x23);
        check(entry, 0, 2, 0x22, 0, false);
        check(entry, 2, 0, 0x22, 0, false);
        check(entry, 0x32, 0x32, 0x22, 0x10, false);
        check(entry, 0x22, 0x22, 0x22, 0x20, false);
        check(entry, 0x32, 2, 0x22, 2, true);
        println("Vector 0x2c and " + passedCases + " interrupt dispatch cases passed");
    }
}
