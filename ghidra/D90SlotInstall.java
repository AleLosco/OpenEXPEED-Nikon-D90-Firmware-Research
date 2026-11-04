// Follow one registration site into the installer, then dispatch on the same RAM.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import java.nio.ByteBuffer;

public class D90SlotInstall extends GhidraScript {
    private void word(EmulatorHelper cpu,long address,int value) {
        cpu.writeMemory(toAddr(address),ByteBuffer.allocate(4).putInt(value).array());
    }
    private void until(EmulatorHelper cpu,long start,long stop) throws Exception {
        cpu.writeRegister("PC",start);
        for(int i=0;i<300;i++){
            if(cpu.getExecutionAddress().getOffset()==stop) return;
            if(!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Step limit");
    }
    public void run() throws Exception {
        EmulatorHelper cpu=new EmulatorHelper(currentProgram);
        long frame=0x81005000L, entry=0x8f6e8f6cL;
        try {
            for(String r:new String[]{"R8","R9","R10","RP"}) cpu.writeRegister(r,0);
            cpu.writeRegister("R11",0x1cd9a0);
            cpu.writeRegister("FP",frame);
            cpu.writeRegister("SP",frame-0x80);
            word(cpu,frame+0x18,0x410000);
            word(cpu,frame+0x1c,0x440000);
            word(cpu,0x410000,0x420000);
            cpu.writeMemory(toAddr(entry+12),new byte[]{0});
            until(cpu,0x1b846e,0x1b84b0);
            if(cpu.readRegister("R4").intValue()!=0 ||
                ByteBuffer.wrap(cpu.readMemory(toAddr(entry+24),4)).getInt()!=0x1b8376)
                throw new IllegalStateException("Registration failed");
            cpu.writeRegister("R4",0);
            cpu.writeRegister("R5",114);
            cpu.writeRegister("R6",1);
            cpu.writeRegister("R7",0x430000);
            until(cpu,0x1b8c0a,0x420000);
            if(cpu.readRegister("R4").intValue()!=114 || cpu.readRegister("R5").intValue()!=1 ||
                cpu.readRegister("R6").intValue()!=0x430000)
                throw new IllegalStateException("Forwarded arguments mismatch");
            println("Slot 0 installed 1b8376 and forwarded to synthetic target");
        } finally {cpu.dispose();}
    }
}
