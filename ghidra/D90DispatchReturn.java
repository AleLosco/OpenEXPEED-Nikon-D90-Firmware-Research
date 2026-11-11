// AB-123: bounded dispatcher epilogue and user return.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90DispatchReturn extends D90DispatchRegisters {
    int returns;
    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{0x40eb6,0x40eca};
        return windows;
    }
    @Override long[][] extraSyntheticWindows() {
        long[][] base=super.extraSyntheticWindows(), windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{0x8f991248L,0x8f991254L};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        long user=reg(c,"SP"), system=reg(c,"SSP");
        require(user==0x8f991248L && system==0x81003ff8L,"Initial stacks");
        require(reg(c,"FP")==0x680001ecL && c.readMemory(a.getAddress(0x6800020fL),1)[0]==0,"Zero candidate field low byte");
        String[] names={"R12","AC","FP"};
        int tag=0x12300000+16*returns;
        for(int i=0;i<3;i++) put(c,a,user+4*i,tag+i);
        until(c,d,0x40ebc);
        until(c,d,0x40ec6);
        for(int i=0;i<3;i++) require(reg(c,names[i])==tag+i,"Restore "+names[i]);
        require(reg(c,"SP")==user+12,"Final candidate pop");
        until(c,d,0x40ec8);
        require(reg(c,"S")==0 && reg(c,"SP")==user+12,"Supervisor transition");
        c.writeRegister("USP",user+12);
        c.writeRegister("SP",system);
        require(word(c,a,system)==0x1000ac && word(c,a,system+4)==0x1f0030,"Supervisor return frame");
        require(d.disassemble(c.getExecutionAddress()).toString().equals("RETI"),"Original RETI");
        require(c.step(monitor),"Dispatcher RETI: "+c.getLastError());
        require(reg(c,"PC")==0x1000ac && reg(c,"PS")==0x1f0030 && reg(c,"S")==1 && reg(c,"I")==1 && reg(c,"ILM")==31,"Returned PC and status");
        require(reg(c,"SP")==system+8 && reg(c,"SSP")==system && reg(c,"USP")==user+12,"Stack model after RETI");
        c.writeRegister("SSP",system+8);
        c.writeRegister("SP",user+12);
        require(word(c,a,0x68000000L)==0x680001ec,"Current candidate");
        returns++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(returns==8,"Scenario count");
        println("AB-123: eight dispatcher returns reached 0x1000ac before candidate execution");
    }
}
