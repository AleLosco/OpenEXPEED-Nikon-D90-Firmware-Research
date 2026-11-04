// Exercise the transfer setup and the separate residual-count block.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;

public class D90AtamTransfer extends GhidraScript {
    private void word(EmulatorHelper cpu,long a,int v) {
        cpu.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());
    }
    private int word(EmulatorHelper cpu,long a) {
        return ByteBuffer.wrap(cpu.readMemory(toAddr(a),4)).getInt();
    }
    private void block(EmulatorHelper cpu,long start,long stop) throws Exception {
        cpu.writeRegister("PC",start);
        for(int i=0;i<90;i++){
            if(cpu.getExecutionAddress().getOffset()==stop) return;
            if(!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Step limit");
    }
    private void sample(int offset,int control,int residual) throws Exception {
        EmulatorHelper cpu=new EmulatorHelper(currentProgram);
        try {
            long request=0x6800cd08L;
            int dest=0x410000+offset;
            word(cpu,request,3);
            word(cpu,request+4,107);
            word(cpu,request+8,dest);
            cpu.writeMemory(toAddr(0xdc0),new byte[26]);
            cpu.writeRegister("RP",0x40000);
            block(cpu,0x19afee,0x40000);
            if(word(cpu,0xdc8)!=0x63000020 || word(cpu,0xdcc)!=dest || word(cpu,0xdd8)!=1536 ||
               (ByteBuffer.wrap(cpu.readMemory(toAddr(0xdc4),2)).getShort()&65535)!=control)
                throw new IllegalStateException("Transfer configuration");
            cpu.writeRegister("R10",request);
            cpu.writeRegister("R8",0x6800cf1fL);
            cpu.writeMemory(toAddr(0x6800cf1fL),new byte[]{0x40});
            cpu.writeMemory(toAddr(0x6800cf1cL),new byte[]{1});
            word(cpu,0x63000004,(residual<<16)|0x1234);
            block(cpu,0x19ddb2,residual==0?0x19de02:0x19ddea);
            if(word(cpu,request)!=residual || word(cpu,request+4)!=110-residual ||
               cpu.readMemory(toAddr(0x6800cf1cL),1)[0]!=(residual==0?0:1))
                throw new IllegalStateException("Residual count mismatch");
        } finally {cpu.dispose();}
    }
    public void run() throws Exception {
        sample(0,0x40ad,0); sample(4,0x404a,1);
        sample(2,0x4049,2); sample(1,0x4048,3);
        println("ATAM transfer setup and residual accounting passed: 4 cases");
    }
}
