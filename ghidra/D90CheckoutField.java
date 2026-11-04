// Observe the field after every CPU step from the caller through checkout.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.util.Arrays;

public class D90CheckoutField extends GhidraScript {
    private static final long ELEMENT = 0x8f8715bcL;
    private void runBlock(EmulatorHelper cpu, long start, long stop) throws Exception {
        cpu.writeRegister("PC", start);
        for (int steps = 0; steps < 650; steps++) {
            if (cpu.getExecutionAddress().getOffset() == stop) return;
            if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Block did not reach " + Long.toHexString(stop));
    }

    private void sample(int selector, byte[] initial) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            for (String reg : new String[]{"R8","R9","R10","R11","FP"}) cpu.writeRegister(reg,0);
            cpu.writeRegister("SP",0x8f9f0000L);
            cpu.writeRegister("RP",0x40000);
            cpu.writeMemory(toAddr(0x8f52f245L),new byte[]{(byte)selector});
            runBlock(cpu, 0x1012ce, 0x40000);
            cpu.writeRegister("R4", 1);
            runBlock(cpu, 0x102b92, 0x102c52);
            if (!Arrays.equals(cpu.readMemory(toAddr(0x8f52e0d4L), 4),
                               new byte[]{(byte)0x8f,(byte)0x87,0x15,(byte)0xbc}))
                throw new IllegalStateException("Unexpected first pool pointer");
            cpu.writeMemory(toAddr(ELEMENT+0x4a),initial);
            cpu.writeRegister("PC",0x10702c);
            for(int steps=0; cpu.getExecutionAddress().getOffset()!=0x10707a; steps++){
                if(steps>=250 || !cpu.step(monitor)) throw new IllegalStateException("Checkout failed");
                if(!Arrays.equals(cpu.readMemory(toAddr(ELEMENT+0x4a),2),initial))
                    throw new IllegalStateException("Field changed at step "+steps);
            }
            if(cpu.readRegister("R8").longValue()!=ELEMENT) throw new IllegalStateException("Wrong element");
            cpu.writeMemory(toAddr(ELEMENT+0xad), new byte[]{0});
            cpu.writeMemory(toAddr(ELEMENT+0x105), new byte[]{0});
            runBlock(cpu, 0x118d60, 0x118d94);
            if (cpu.readRegister("R8").longValue() != ELEMENT ||
                !Arrays.equals(cpu.readMemory(toAddr(ELEMENT+0x4a),2),initial))
                throw new IllegalStateException("Unexpected pre-store state");
            runBlock(cpu, 0x118d94, 0x118d96);
            if (!Arrays.equals(cpu.readMemory(toAddr(ELEMENT+0x4a),2), new byte[]{0,1}))
                throw new IllegalStateException("Setter failed");
        } finally {cpu.dispose();}
    }
    public void run() throws Exception {
        for(int selector:new int[]{0,32})
            for(byte[] initial:new byte[][]{{0,0},{0x12,0x34}}) sample(selector,initial);
        println("Pool pointer, checkout and field store verified in 4 cases");
    }
}
