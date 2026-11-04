// Exercise the local branch conditions and real call into the pool setter.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.util.Arrays;

public class D90SetterCallerGate extends GhidraScript {
    private void sample(int flags, int mode, int helperResult) throws Exception {
        long fp = 0x8f9f0000L, element = 0x8f8715bcL;
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            cpu.writeRegister("FP", fp);
            cpu.writeRegister("SP", fp - 0x100);
            cpu.writeRegister("R8", 0x8f980000L);
            cpu.writeRegister("R9", mode);
            cpu.writeRegister("R10", 0);
            cpu.writeRegister("PC", 0x1300f2);
            cpu.writeMemory(toAddr(fp - 17), new byte[]{(byte)flags});
            cpu.writeMemory(toAddr(0x8f52e0d4L), new byte[]{(byte)0x8f,(byte)0x87,0x15,(byte)0xbc});
            for (int offset : new int[]{0xad, 0x105, 0x2d, 0x4a, 0x4b})
                cpu.writeMemory(toAddr(element + offset), new byte[]{0});
            long expected = (flags & 1) != 0 ? 0x1300fc : (flags & 2) == 0 ? 0x1301b2 :
                            (mode & 2) == 0 ? 0x13013c : helperResult == 0 ? 0x130176 : 0x118d96;
            for (int step = 0; step < 100; step++) {
                long pc = cpu.getExecutionAddress().getOffset();
                if (pc == expected) {
                    byte[] field = cpu.readMemory(toAddr(element + 0x4a), 2);
                    if (!Arrays.equals(field, new byte[]{0,(byte)(expected == 0x118d96 ? 1 : 0)}))
                        throw new IllegalStateException("Unexpected pool field");
                    return;
                }
                if (pc == 0x11e0c6 || pc == 0x103d46) {
                    // Supply the helper return at its entry after the real call and delay slot.
                    cpu.writeRegister("R4", pc == 0x11e0c6 ? helperResult : 7);
                    cpu.writeRegister("PC", cpu.readRegister("RP"));
                } else if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            }
            throw new IllegalStateException("Expected path not reached");
        } finally { cpu.dispose(); }
    }
    public void run() throws Exception {
        for (int flags : new int[]{0,1,2,3,6})
            for (int mode : new int[]{0,2})
                for (int result : new int[]{0,1}) sample(flags,mode,result);
        println("Setter caller gate passed: 20 cases");
    }
}
