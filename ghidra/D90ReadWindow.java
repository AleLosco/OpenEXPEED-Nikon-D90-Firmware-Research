// Follow the successful single-block copy from the resolved object's buffer.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90ReadWindow extends GhidraScript {
    private void word(EmulatorHelper cpu, long address, int value) {
        cpu.writeMemory(toAddr(address), ByteBuffer.allocate(4).putInt(value).array());
    }
    private void sample(int offset) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        int frame=0x81001000, object=0x81002000, source=0x81003000, dest=0x81004004;
        byte[] bytes=new byte[32];
        for (int i=0; i<bytes.length; i++) bytes[i]=(byte)(i+1);
        try {
            cpu.writeRegister("FP",Integer.toUnsignedLong(frame));
            cpu.writeRegister("SP",0x81005000L);
            cpu.writeRegister("R8",Integer.toUnsignedLong(object));
            cpu.writeRegister("R10",8);
            word(cpu,Integer.toUnsignedLong(object)+0x80,source);
            word(cpu,Integer.toUnsignedLong(frame)-8,offset);
            word(cpu,Integer.toUnsignedLong(frame)+0x20,dest);
            cpu.writeMemory(toAddr(Integer.toUnsignedLong(source)),bytes);
            cpu.writeMemory(toAddr(Integer.toUnsignedLong(dest)-1),new byte[10]);
            cpu.writeRegister("PC",0x1b19ec);
            for (int i=0; i<300; i++) {
                if (cpu.getExecutionAddress().getOffset()==0x1b1a00) {
                    byte[] expected=new byte[10];
                    System.arraycopy(bytes,offset,expected,1,8);
                    if (!Arrays.equals(expected,cpu.readMemory(toAddr(Integer.toUnsignedLong(dest)-1),10)))
                        throw new IllegalStateException("Destination mismatch");
                    return;
                }
                if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            }
            throw new IllegalStateException("Step limit");
        } finally { cpu.dispose(); }
    }
    public void run() throws Exception {
        sample(0); sample(3);
        println("Read window passed: offsets 0 and 3");
    }
}
