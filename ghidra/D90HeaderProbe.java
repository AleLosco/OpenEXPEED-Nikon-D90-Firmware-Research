// Exercise the original B header parser and caller branch with synthetic read results.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;

public class D90HeaderProbe extends GhidraScript {
    private void sample(byte[] header, int status, int count, int expected) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            for (String reg : new String[]{"R8","R9","R10","R11","FP","R5","R6","RP"})
                cpu.writeRegister(reg, 0);
            cpu.writeRegister("R4", 7);
            cpu.writeRegister("SP", 0x8f9f0000L);
            cpu.writeRegister("PC", 0x1204a4);
            for (int steps = 0; steps < 180; steps++) {
                long pc = cpu.getExecutionAddress().getOffset();
                if (pc == 0x1204cc || pc == 0x1204d6) {
                    if ((pc == 0x1204cc) != (expected == 0) ||
                        cpu.readRegister("R8").intValue() != expected)
                        throw new IllegalStateException("Unexpected parser result");
                    return;
                }
                // FR ADD2 immediates 4 and 6 denote -12 and -10. The pinned
                // language uses +4 and +6. Adjust only the outgoing pointers.
                if (pc == 0x1272b0 || pc == 0x1272b4) {
                    cpu.writeRegister("R4", cpu.readRegister("R4").longValue() - 16);
                }
                if (pc == 0x11dcd4) {
                    if (cpu.readRegister("R6").intValue() != 8 || cpu.readRegister("R4").intValue() != 7)
                        throw new IllegalStateException("Unexpected read arguments");
                    cpu.writeMemory(toAddr(cpu.readRegister("R5").longValue() - 16), header);
                    cpu.writeMemory(toAddr(cpu.readRegister("R7").longValue()), new byte[]{0,0,0,(byte)count});
                    cpu.writeRegister("R4", status);
                    cpu.writeRegister("PC", cpu.readRegister("RP"));
                } else if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            }
            throw new IllegalStateException("Step limit");
        } finally { cpu.dispose(); }
    }
    public void run() throws Exception {
        byte[] mm = {77,77,0,42,1,2,3,4};
        sample(mm,0,8,0x01020304);
        sample(new byte[]{73,73,42,0,4,3,2,1},0,8,0x01020304);
        sample(mm,1,8,0);
        sample(mm,0,7,0);
        sample(new byte[]{73,77,0,42,1,2,3,4},0,8,0);
        sample(new byte[]{77,77,42,0,1,2,3,4},0,8,0);
        sample(new byte[]{77,77,0,42,0,0,0,0},0,8,0);
        println("Header probe passed: 7 cases");
    }
}
