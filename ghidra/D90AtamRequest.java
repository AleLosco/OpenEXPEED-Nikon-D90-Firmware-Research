// Trace the ATAM reader through request submission, with synthetic completion.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;
public class D90AtamRequest extends GhidraScript {
    private void word(EmulatorHelper cpu,long address,int value) {
        cpu.writeMemory(toAddr(address),ByteBuffer.allocate(4).putInt(value).array());
    }
    private int word(EmulatorHelper cpu,long address) {
        return ByteBuffer.wrap(cpu.readMemory(toAddr(address),4)).getInt();
    }
    private void sample(int count,int limit,int ready,int expectedCalls) throws Exception {
        EmulatorHelper cpu=new EmulatorHelper(currentProgram);
        long object=0x81000000L, request=0x6800cd08L;
        int buffer=0x410000, sent=0, calls=0, failures=0, cleanups=0;
        try {
            for(String r:new String[]{"FP","R8","R9","R10","R11"}) cpu.writeRegister(r,0);
            cpu.writeRegister("SP",0x81005000L);
            cpu.writeRegister("RP",0x40000);
            cpu.writeRegister("R4",object);
            cpu.writeRegister("R5",7);
            cpu.writeRegister("R6",count);
            cpu.writeRegister("R7",buffer);
            word(cpu,object+16,100);
            cpu.writeMemory(toAddr(object+20),new byte[]{1});
            word(cpu,0x6800cc44L,limit);
            cpu.writeMemory(toAddr(0x6800cf1fL),new byte[]{(byte)ready});
            cpu.writeRegister("PC",0x13acb4);
            for(int i=0;i<600;i++){
                long pc=cpu.getExecutionAddress().getOffset();
                if(pc==0x40000){
                    boolean success=expectedCalls>0;
                    if(calls!=expectedCalls || sent!=(success?count:0) || failures!=(success?0:3) ||
                       cleanups!=(success?0:1) || cpu.readRegister("R4").intValue()!=(success?0:0xfffffcce))
                        throw new IllegalStateException("Unexpected final state");
                    return;
                }
                if(pc==0x19dc7c){
                    int chunk=Math.min(1024,count-sent);
                    if(word(cpu,request)!=chunk || word(cpu,request+4)!=107+sent ||
                       word(cpu,request+8)!=buffer+sent*512) throw new IllegalStateException("Request mismatch");
                    sent+=chunk;
                    calls++;
                    word(cpu,request,0);
                    cpu.writeRegister("R4",0x40);
                    cpu.writeRegister("PC",cpu.readRegister("RP"));
                } else if(pc==0x1a03d8){
                    cleanups++;
                    cpu.writeRegister("PC",cpu.readRegister("RP"));
                } else {
                    if(pc==0x13ad1c && cpu.readRegister("R4").intValue()!=0){
                        int error=limit<107+count?0xfff20006:0xfff20001;
                        if(cpu.readRegister("R4").intValue()!=error) throw new IllegalStateException("Inner error");
                        failures++;
                    }
                    if(!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
                }
            }
            throw new IllegalStateException("Step limit");
        } finally {cpu.dispose();}
    }
    public void run() throws Exception {
        sample(1,108,0x40,1);
        sample(1025,1132,0x40,2);
        sample(1,107,0x40,0);
        sample(1,108,0,0);
        println("ATAM submissions, split, range and flag failures passed");
    }
}
