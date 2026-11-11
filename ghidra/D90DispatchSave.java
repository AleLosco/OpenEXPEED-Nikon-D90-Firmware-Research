// AB-119: continue from the vector prefix through the outgoing context store.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
import java.nio.ByteBuffer;

public class D90DispatchSave extends D90DispatchPrefix {
    int saves;
    @Override long[][] extraWindows() {
        long[][] previous = super.extraWindows();
        long[][] windows = Arrays.copyOf(previous, previous.length + 2);
        windows[previous.length] = new long[]{0x40e2a, 0x40e7c};
        windows[previous.length+1] = new long[]{0x404fa, 0x404fc};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c, PseudoDisassembler d, ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var a = p.getLanguage().getDefaultSpace();
        byte[] os = c.readMemory(a.getAddress(0x68000000L),4096);
        byte[] candidate = c.readMemory(a.getAddress(0x8f991204L),40);
        var expected = ByteBuffer.wrap(os);
        expected.putInt(0xed0,0x407ee);
        expected.putInt(0xed4,0x1f0030);
        String[] registers = {"RP","MDL","MDH","R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11"};
        for(int i=0;i<registers.length;i++)
            expected.putInt(0xed8+4*i,(int)reg(c,registers[i]));
        expected.putInt(0x7dc,0x68000ed0);
        until(c,d,0x40e32);
        require(reg(c,"AC")==0x80000000L,"Ready bitmap");
        D90Srch1Adapter.searchOne(c,a,false);
        until(c,d,0x40e62);
        require(reg(c,"AC")==0x680001ecL && reg(c,"FP")==0x680007c4L,"Candidate/current");
        until(c,d,0x40e6c);
        long user = reg(c,"SP"), system = reg(c,"SSP");
        require(user==0x68000ed8L && system==0x81003ff8L,"Before supervisor switch");
        until(c,d,0x40e6e);
        require(reg(c,"S")==0 && reg(c,"SP")==user,"Supervisor mode entered");
        c.writeRegister("USP",user);
        c.writeRegister("SP",system);
        until(c,d,0x40e74);
        require(reg(c,"S")==1 && reg(c,"SP")==system+8,"User mode entered");
        require(reg(c,"R0")==0x407ee && reg(c,"R1")==0x1f0030,"Frame loaded");
        c.writeRegister("SSP",system+8);
        c.writeRegister("SP",user);
        until(c,d,0x40e7a);
        require(reg(c,"SP")==0x68000ed0L,"Outgoing stack pointer");
        require(Arrays.equals(os,c.readMemory(a.getAddress(0x68000000L),4096)),"OS memory with outgoing frame and pointer");
        require(Arrays.equals(candidate,c.readMemory(a.getAddress(0x8f991204L),40)),"Candidate frame preserved");
        saves++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(saves==8,"Scenario count");
        println("AB-119: eight outgoing context saves passed at 0x40e7a");
    }
}
