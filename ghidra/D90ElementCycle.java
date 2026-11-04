// Exercise pool setup, a consumer, unlink, and return routing on the same synthetic RAM.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;

public class D90ElementCycle extends GhidraScript {
    private long word(EmulatorHelper cpu, long address) throws Exception {
        long value = 0;
        for (byte b : cpu.readMemory(toAddr(address), 4)) value = (value << 8) | (b & 255);
        return value;
    }
    private void runTo(EmulatorHelper cpu, long entry, long stop) throws Exception {
        cpu.writeRegister("RP", 0x40000);
        cpu.writeRegister("PC", entry);
        for (int steps = 0; steps < 650; steps++) {
            if (cpu.getExecutionAddress().getOffset() == stop) return;
            if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Step limit");
    }
    private void sample(int selector, int field) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        try {
            for (String reg : new String[]{"R8","R9","R10","R11","FP"}) cpu.writeRegister(reg,0);
            cpu.writeRegister("SP",0x8f9f0000L);
            cpu.writeMemory(toAddr(0x8f52f245L),new byte[]{(byte)selector});
            runTo(cpu,0x1012ce,0x40000);
            int count = selector == 0 ? 5 : 3;
            long element = word(cpu,0x8f8715b8L);
            runTo(cpu,0x1020fc,0x102120);
            if (element != 0x8f8715bcL || cpu.readRegister("R8").longValue()!=element ||
                word(cpu,0x8f871594L)!=element || word(cpu,0x8f871598L)!=element ||
                cpu.readMemory(toAddr(element+0x4f),1)[0]!=3 || word(cpu,0x8f87156cL)!=count-1)
                throw new IllegalStateException("Checkout mismatch");
            // Bridge the untraced processing interval by invoking the real unlink helper.
            cpu.writeRegister("R4",element);
            runTo(cpu,0x105b36,0x40000);
            cpu.writeRegister("R8",element);
            cpu.writeMemory(toAddr(element+0x4a),new byte[]{(byte)(field>>8),(byte)field});
            runTo(cpu,0x102ebe,0x102eda);
            long head = field==0 ? 0x8f8715b4L : 0x8f8715acL;
            if (word(cpu,head)!=element || word(cpu,0x8f87156cL)!=count ||
                cpu.readMemory(toAddr(element+0x4f),1)[0]!=(field==0 ? 0 : 1))
                throw new IllegalStateException("Return mismatch");
        } finally { cpu.dispose(); }
    }
    public void run() throws Exception {
        for (int selector : new int[]{0,32}) for (int field : new int[]{0,1,256}) sample(selector,field);
        println("Element cycle passed=6");
    }
}
