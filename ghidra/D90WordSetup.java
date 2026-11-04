// Run B setup from its prologue through RET, using original ROM and synthetic RAM.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
public class D90WordSetup extends GhidraScript {
    private void sample(int input) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            long stop = 0x40000;
            int expected = (input & 0x20) == 0 ? 5 : 3;
            for (String r : new String[]{"R8","R9","R10","R11","FP"}) cpu.writeRegister(r,0);
            cpu.writeRegister("SP",0x8f9f0000L);
            cpu.writeRegister("RP",stop);
            cpu.writeRegister("PC",0x1012ce);
            cpu.writeMemory(toAddr(0x8f52f245L),new byte[]{(byte)input});
            cpu.writeMemory(toAddr(0x8f87156cL),new byte[]{-1,-1,-1,-1,-1,-1,-1,-1});
            int iterations=0;
            for (int steps=0; steps<650 && cpu.getExecutionAddress().getOffset()!=stop; steps++) {
                if (cpu.getExecutionAddress().getOffset()==0x1013b6) iterations++;
                if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            }
            byte[] result=cpu.readMemory(toAddr(0x8f87156cL),8);
            if (iterations!=expected || cpu.getExecutionAddress().getOffset()!=stop ||
                !java.util.Arrays.equals(result,new byte[]{0,0,0,(byte)expected,0,0,0,(byte)expected}))
                throw new IllegalStateException("Setup mismatch for input "+input);
        } finally { cpu.dispose(); }
    }
    public void run() throws Exception {
        for (int input : new int[]{0,0x20,0xdf,0xff}) sample(input);
        println("B word setup passed=4");
    }
}
