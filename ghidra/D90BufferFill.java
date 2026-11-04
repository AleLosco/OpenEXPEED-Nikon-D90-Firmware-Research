// Exercise the original buffer gate, address calculation and indirect dispatch.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90BufferFill extends GhidraScript {
    private void word(EmulatorHelper cpu, long address, int value) {
        cpu.writeMemory(toAddr(address), ByteBuffer.allocate(4).putInt(value).array());
    }
    private void sample(int flags, int status) throws Exception {
        EmulatorHelper cpu = new EmulatorHelper(currentProgram);
        long object=0x81002000L, buffer=0x81003000L, entry=0x8f6e8f6cL+0x60;
        byte[] payload={11,12,13,14};
        try {
            for(String r : new String[]{"FP","R8","R9","R10"}) cpu.writeRegister(r,0);
            cpu.writeRegister("SP",0x81005000L);
            cpu.writeRegister("RP",0x40000);
            cpu.writeRegister("R4",1);
            cpu.writeRegister("R5",object);
            cpu.writeRegister("R6",5);
            cpu.writeRegister("R7",10);
            cpu.writeMemory(toAddr(object),new byte[]{(byte)flags});
            word(cpu,object+0x80,(int)buffer);
            word(cpu,buffer,0);
            word(cpu,entry,42);
            word(cpu,entry+4,0);
            word(cpu,entry+0x18,0x41000);
            word(cpu,entry+0x4c,100);
            cpu.writeMemory(toAddr(entry+0x31),new byte[]{4});
            cpu.writeMemory(toAddr(entry+0x28),new byte[]{9,0,0,11});
            cpu.writeRegister("PC",0x1b156e);
            int calls=0;
            for(int step=0;step<240;step++){
                long pc=cpu.getExecutionAddress().getOffset();
                if(pc==0x41000){
                    calls++;
                    if(cpu.readRegister("R4").intValue()!=42 || cpu.readRegister("R5").intValue()!=114 ||
                       cpu.readRegister("R6").intValue()!=1 || cpu.readRegister("R7").longValue()!=buffer)
                        throw new IllegalStateException("Callback arguments");
                    if(status>=0) cpu.writeMemory(toAddr(buffer),payload);
                    cpu.writeRegister("R4",status);
                    cpu.writeRegister("PC",cpu.readRegister("RP"));
                } else if(pc==0x40000){
                    boolean miss=(flags&2)==0, error=miss && status<0;
                    if(calls!=(miss?1:0) || cpu.readRegister("R4").intValue()!=(error?0x80005011:0) ||
                       cpu.readMemory(toAddr(object),1)[0]!=(error?flags:flags|2) ||
                       ByteBuffer.wrap(cpu.readMemory(toAddr(entry+4),4)).getInt()!=(error?status:0) ||
                       !Arrays.equals(cpu.readMemory(toAddr(buffer),4),miss&&!error?payload:new byte[4]))
                        throw new IllegalStateException("Unexpected buffer state");
                    return;
                } else if(!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
            }
            throw new IllegalStateException("Step limit");
        } finally {cpu.dispose();}
    }
    public void run() throws Exception {
        sample(0x42,0); sample(0x40,0); sample(0x40,3); sample(0x40,-5);
        println("Buffer gate passed: reuse, zero, positive, negative callback");
    }
}
