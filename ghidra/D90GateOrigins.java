// B, after INT 0x40. Synthetic service result, stop before epilogue or helper body.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;

public class D90GateOrigins extends GhidraScript {
    private void sample(int status, int events, int busy, int flags) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            cpu.writeRegister("FP", 0x8f9f0010L);
            cpu.writeRegister("SP", 0x8f9f0000L);
            cpu.writeRegister("R12", status);
            cpu.writeRegister("R9", 0x8f52f1aaL);
            cpu.writeRegister("R10", 5);
            cpu.writeRegister("R11", 19);
            cpu.writeMemory(toAddr(0x8f9f000cL), new byte[]{0,0,0,(byte)events});
            cpu.writeMemory(toAddr(0x8f5340dcL), new byte[]{0,0,0,(byte)busy});
            cpu.writeMemory(toAddr(0x8f52f1aaL), new byte[]{(byte)flags});
            cpu.writeRegister("PC", 0x14e316);
            boolean calls = status == 0 && events == 4 && busy == 0 && (flags & 12) == 0;
            long stop = calls ? 0x14e34c : 0x14e344;
            for (int step=0; step<35 && cpu.getExecutionAddress().getOffset()!=stop; step++)
                if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            if (cpu.getExecutionAddress().getOffset()!=stop ||
                cpu.readRegister("R4").intValue()!=(calls ? 19 : 0) ||
                (calls && cpu.readRegister("R5").intValue()!=5))
                throw new IllegalStateException("Unexpected branch or result");
        } finally { cpu.dispose(); }
    }
    public void run() throws Exception {
        for (int status : new int[]{0,1})
            for (int events : new int[]{0,4})
                for (int busy : new int[]{0,1})
                    for (int flags : new int[]{0,4,8,12,64})
                        sample(status,events,busy,flags);
        println("Post-service gate passed=40");
    }
}
