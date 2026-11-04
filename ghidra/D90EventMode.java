// Exercise OS dispatch and both ATAM event predicates against synthetic OS objects.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;

public class D90EventMode extends GhidraScript {
    private void block(EmulatorHelper cpu,long start,long stop) throws Exception {
        cpu.writeRegister("PC",start);
        for(int step=0;step<40;step++){
            if(cpu.getExecutionAddress().getOffset()==stop) return;
            if(!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Expected stop "+Long.toHexString(stop));
    }
    private void word(EmulatorHelper cpu,long address,int value){
        cpu.writeMemory(toAddr(address),ByteBuffer.allocate(4).putInt(value).array());
    }
    public void run() throws Exception {
        long obj=0x81001000L, task=0x81002000L;
        int mask=0x30000000;
        EmulatorHelper cpu=new EmulatorHelper(currentProgram);
        try {
            for(int service:new int[]{-170,-48}){
                cpu.writeRegister("R12",service);
                block(cpu,0x406b6,service==-170?0x41bfc:0x41a5a);
            }
            for(int mode:new int[]{0,2}){
                for(int flags:new int[]{0,0x10000000,0x20000000,mask,mask|1}){
                    boolean satisfied= mode==0 ? (flags&mask)==mask : (flags&mask)!=0;
                    cpu.writeRegister("FP",obj);
                    word(cpu,obj+8,flags);
                    cpu.writeRegister("R6",mask);
                    cpu.writeRegister("R7",mode);
                    block(cpu,0x41c46,satisfied?0x41c98:0x41c60);
                    cpu.writeRegister("FP",task);
                    word(cpu,task+0x14,mask);
                    cpu.writeMemory(toAddr(task+14),new byte[]{0,(byte)mode});
                    cpu.writeRegister("R5",flags);
                    block(cpu,0x41a8a,satisfied?0x41aa4:0x41ad6);
                }
            }
            for(int first:new int[]{0x10000000,0x20000000}){
                word(cpu,obj+8,0);
                for(int bit:new int[]{first,mask^first}){
                    cpu.writeRegister("FP",obj);
                    cpu.writeRegister("R5",bit);
                    block(cpu,0x41a6e,0x41a74);
                    int actual=ByteBuffer.wrap(cpu.readMemory(toAddr(obj+8),4)).getInt();
                    if(actual!=(bit==first?first:mask)) throw new IllegalStateException("Flag accumulation");
                }
            }
            println("Dispatch, 20 predicates and both post orders passed");
        } finally {cpu.dispose();}
    }
}
