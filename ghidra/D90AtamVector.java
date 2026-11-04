// Resolve the ATAM vector from the B startup image and exercise installation.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;
public class D90AtamVector extends GhidraScript {
    private int word(EmulatorHelper cpu,long a) {
        return ByteBuffer.wrap(cpu.readMemory(toAddr(a),4)).getInt();
    }
    private void runTo(EmulatorHelper cpu,long start,long stop) throws Exception {
        cpu.writeRegister("PC",start);
        for(int i=0;i<600;i++){
            if(cpu.getExecutionAddress().getOffset()==stop) return;
            if(!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Step limit");
    }
    public void run() throws Exception {
        EmulatorHelper cpu=new EmulatorHelper(currentProgram);
        try {
            for(String r:new String[]{"R8","R9","R10","R11","FP"}) cpu.writeRegister(r,0);
            cpu.writeRegister("SP",0x81005000L);
            cpu.writeRegister("RP",0x40000);
            long table=0x8f6f2ce8L;
            long source=0x287a6cL+table-0x8f6e94c8L;
            cpu.writeMemory(toAddr(table),getBytes(toAddr(source),28));
            cpu.writeMemory(toAddr(0x8f533068L),new byte[40]);
            runTo(cpu,0x13a8ae,0x40000);
            if(word(cpu,0x8f53308cL)!=0x8f533068) throw new IllegalStateException("Registry");
            runTo(cpu,0x14cda8,0x13abe8);
            // Supply the successful open result and output context.
            cpu.writeMemory(toAddr(cpu.readRegister("R5").longValue()),new byte[]{0,64,0,0});
            cpu.writeRegister("R4",0);
            runTo(cpu,cpu.readRegister("RP").longValue(),0x14cdc8);
            if(word(cpu,0x8f534144L)!=0x8f53307c || word(cpu,0x8f534148L)!=0x400000)
                throw new IllegalStateException("Lookup outputs");
            runTo(cpu,0x14cdea,0x1b81f2);
            cpu.writeMemory(toAddr(0x8f6e8f78L),new byte[]{0});
            cpu.writeRegister("RP",0x40000);
            runTo(cpu,0x1b7ece,0x40000);
            if(cpu.readRegister("R4").intValue()!=0 || word(cpu,0x8f6e8f84L)!=0x13acb4)
                throw new IllegalStateException("Installed slot");
            println("ATAM vector 8f53307c, read callback 13acb4");
        } finally {cpu.dispose();}
    }
}
