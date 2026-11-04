// Observe OS call arguments at the boundary, without dispatching INT 0x40.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;

public class D90AtamWake extends GhidraScript {
    private void trace(EmulatorHelper cpu, long start, long end) throws Exception {
        cpu.writeRegister("PC", start);
        for (int i=0; i<50; i++) {
            if (cpu.getExecutionAddress().getOffset()==end) return;
            if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
        }
        throw new IllegalStateException("Expected stop "+Long.toHexString(end));
    }
    private void reg(EmulatorHelper cpu,String name,int expected) {
        if (cpu.readRegister(name).intValue()!=expected) throw new IllegalStateException(name);
    }
    public void run() throws Exception {
        EmulatorHelper cpu=new EmulatorHelper(currentProgram);
        try {
            cpu.writeRegister("FP",0x81004000L);
            trace(cpu,0x19dce4,0x19dcfc);
            reg(cpu,"R4",0x81003ffc); reg(cpu,"R5",7); reg(cpu,"R6",0x30000000);
            reg(cpu,"R7",0); reg(cpu,"AC",3000); reg(cpu,"R12",-170);
            for(int status:new int[]{0,0x100,0x8101,0x300}){
                cpu.writeMemory(toAddr(0xdc2),new byte[]{(byte)(status>>8),(byte)status});
                boolean post=(status&0x3f00)==0x100;
                trace(cpu,0x19b15e,post?0x19b184:0x19b188);
                if(post){reg(cpu,"R4",7); reg(cpu,"R5",0x10000000); reg(cpu,"R12",-48);}
            }
            cpu.writeRegister("R8",0x6800cf1eL);
            for(int value:new int[]{0x11,0x12,0x19,0x22,0x23}){
                cpu.writeMemory(toAddr(0x6800cf1eL),new byte[]{(byte)value});
                boolean post=value==0x12 || value==0x19 || value==0x22;
                trace(cpu,0x19a75c,post?0x19a780:0x19a784);
                if(post){reg(cpu,"R4",7); reg(cpu,"R5",0x20000000); reg(cpu,"R12",-48);}
            }
            println("Wait arguments and 9 producer paths passed");
        } finally {cpu.dispose();}
    }
}
