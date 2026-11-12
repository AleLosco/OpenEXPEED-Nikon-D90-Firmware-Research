// AB-150: follow the original service epilogue and model the user stack selection.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Service23UserReturn extends D90Service23Return {
    private int successes, rejections;
    @Override void afterInitialization(EmulatorHelper c, PseudoDisassembler d, ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        require(word(c,p.getLanguage().getDefaultSpace(),reg(c,"USP"))==0x1000f2,"Saved outer return");
        boolean success = reg(c,"R12")==0;
        returnUserService23(c,d,p,0x112016);
        if (success) successes++; else rejections++;
    }
    void returnUserService23(EmulatorHelper c,PseudoDisassembler d,ProgramDB p,long site) throws Exception {
        var a = p.getLanguage().getDefaultSpace();
        long user = reg(c,"USP"), base = reg(c,"SP");
        long expectedResult = reg(c,"R12")==0 ? 0 : 0xffffffc1L;
        byte[] userStack = c.readMemory(a.getAddress(0x8f991204L),80);
        long savedStatus = Integer.toUnsignedLong(word(c,a,base+76));
        require(word(c,a,base+72)==site,"Saved return addresses");
        until(c,d,0x406f4);
        require(reg(c,"SP")==base+72 && reg(c,"SSP")==base+72 && reg(c,"S")==0,"Supervisor stack before RETI");
        require(d.disassemble(c.getExecutionAddress()).toString().equals("RETI"),"Original RETI");
        require(c.step(monitor),"RETI: "+c.getLastError());
        require(reg(c,"PC")==site && reg(c,"PS")==savedStatus && reg(c,"S")==1 && reg(c,"I")==1 && reg(c,"ILM")==31,"User return status");
        require(reg(c,"SP")==base+80 && reg(c,"SSP")==base+72 && reg(c,"USP")==user,"Raw modeled stacks");
        c.writeRegister("SSP",base+80);
        c.writeRegister("SP",user);
        require(reg(c,"R12")==expectedResult && reg(c,"SP")==user && reg(c,"SSP")==0x81004000L,"Caller result and stack selection");
        require(Arrays.equals(userStack,c.readMemory(a.getAddress(0x8f991204L),80)),"User stack preserved");

    }
    @Override public void run() throws Exception {
        super.run();
        require(successes==4 && rejections==4,"User result counts");
        println("AB-150: eight user returns at 0x112016 with results 0 or -63");
    }
}
