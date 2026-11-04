// Exercise the original AB-51 call, its wrapper and the signed preparation check.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;

public class D90CallerHelper extends GhidraScript {
    private void sample(int probe, int second) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        long fp = 0x8f9f0000L, descriptor = fp - 0x24;
        boolean dispatched = false, cleaned = false;
        try {
            for (String r : new String[]{"R8","R9","R10","R11","R5"}) cpu.writeRegister(r,0);
            cpu.writeRegister("FP",fp);
            cpu.writeRegister("SP",fp-0x100);
            cpu.writeRegister("PC",0x130164);
            cpu.writeMemory(toAddr(descriptor),new byte[]{0,0,0,42});
            for (int step=0; step<220; step++) {
                long pc = cpu.getExecutionAddress().getOffset();
                if (pc == 0x130176 || pc == 0x130182) {
                    int result = probe < 0 ? 0 : second;
                    if ((pc == 0x130182) != (result != 0) ||
                        cpu.readRegister("R4").intValue() != result ||
                        dispatched != (probe >= 0) || cleaned != dispatched)
                        throw new IllegalStateException("Caller outcome mismatch");
                    return;
                }
                switch ((int)pc) {
                    case 0x11ecc0: case 0x11ed1e:
                        cpu.writeRegister("PC",pc+2); continue;
                    case 0x13ea36: case 0x14de8c: case 0x127b30:
                    case 0x127bb6: case 0x14dece:
                        break;
                    case 0x13b05a: cpu.writeRegister("R4",probe); break;
                    case 0x1204a4:
                        if (cpu.readRegister("R4").intValue()!=probe ||
                            cpu.readRegister("R5").intValue()!=42 || cpu.readRegister("R6").intValue()!=0)
                            throw new IllegalStateException("Dispatch arguments");
                        dispatched=true;
                        cpu.writeRegister("R4",second);
                        break;
                    case 0x11eeba:
                        if (cpu.readRegister("R4").intValue()!=probe)
                            throw new IllegalStateException("Cleanup argument");
                        cleaned=true;
                        cpu.writeRegister("R4",99);
                        break;
                    default:
                        if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
                        continue;
                }
                cpu.writeRegister("PC",cpu.readRegister("RP"));
            }
            throw new IllegalStateException("Step limit");
        } finally {cpu.dispose();}
    }
    public void run() throws Exception {
        for(int probe:new int[]{-1,0,7})
            for(int second:new int[]{0,3}) sample(probe, second);
        println("Caller helper passed: 6 cases");
    }
}
