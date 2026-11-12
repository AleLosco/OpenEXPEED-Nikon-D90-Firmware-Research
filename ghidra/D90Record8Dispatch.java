// AB-157: isolated dispatcher entry on the AB-156 ready lists.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Record8Dispatch extends D90CurrentListChange {
    private static final long FRAME=0x8f993e04L;
    private int cases;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), result=Arrays.copyOf(base,base.length+1);
        result[base.length]=new long[]{0x10fcce,0x10fcda};
        return result;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), result=Arrays.copyOf(base,base.length+2);
        result[base.length]=new long[]{FRAME+44,FRAME+80};
        result[base.length+1]=new long[]{0x82000000L,0x82000100L};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p)throws Exception{
        super.afterInitialization(cpu,decoder,p);
        var space=p.getLanguage().getDefaultSpace();
        // Stand in for the interrupted task and its already-saved prefix.
        long system=0x81003ff8L, outgoing=0x820000c0L;
        put(cpu,space,system,0x100166);
        put(cpu,space,system+4,0x1f0030);
        for(int offset=40;offset<80;offset+=4) put(cpu,space,FRAME+offset,0x12340000+offset);
        cpu.writeRegister("SP",outgoing);
        cpu.writeRegister("USP",outgoing);
        cpu.writeRegister("SSP",system);
        cpu.writeRegister("S",1);
        cpu.writeRegister("PC",0x40e2a);
        until(cpu,decoder,0x40e32);
        D90Srch1Adapter.searchOne(cpu,space,false);
        require(reg(cpu,"AC")==3,"List 4 index");
        until(cpu,decoder,0x40e62);
        require(reg(cpu,"AC")==0x68000270 && reg(cpu,"FP")==0x680001ec,"Candidate and outgoing control");
        until(cpu,decoder,0x40e6e);
        long saved=reg(cpu,"SP");
        require(reg(cpu,"S")==0,"Supervisor transition");
        cpu.writeRegister("USP",saved);
        cpu.writeRegister("SP",system);
        until(cpu,decoder,0x40e74);
        require(reg(cpu,"S")==1 && reg(cpu,"SP")==system+8,"Outgoing frame consumed");
        cpu.writeRegister("SSP",system+8);
        cpu.writeRegister("SP",saved);
        until(cpu,decoder,0x40e8c);
        require(reg(cpu,"SP")==FRAME,"Record 8 context load");
        until(cpu,decoder,0x40ea2);
        require(reg(cpu,"SP")==FRAME+8 && reg(cpu,"S")==0,"Candidate PC and PS popped");
        cpu.writeRegister("USP",FRAME+8);
        cpu.writeRegister("SP",system+8);
        until(cpu,decoder,0x40ea8);
        require(reg(cpu,"SP")==system && reg(cpu,"S")==1,"Candidate supervisor frame");
        cpu.writeRegister("SSP",system);
        cpu.writeRegister("SP",FRAME+8);
        until(cpu,decoder,0x40ec8);
        require(reg(cpu,"SP")==FRAME+80 && reg(cpu,"S")==0 && reg(cpu,"RP")==0x4050a,"Restored context");
        require(word(cpu,space,0x68000000)==0x68000270 && word(cpu,space,system)==0x10fcce && word(cpu,space,system+4)==0x1f0030,"Current control and return frame");
        cpu.writeRegister("USP",FRAME+80);
        cpu.writeRegister("SP",system);
        require(cpu.step(monitor),"RETI");
        require(reg(cpu,"PC")==0x10fcce && reg(cpu,"PS")==0x1f0030 && reg(cpu,"S")==1 && reg(cpu,"SP")==system+8,"User entry");
        cpu.writeRegister("SSP",system+8);
        cpu.writeRegister("SP",FRAME+80);
        until(cpu,decoder,0x10fd26);
        require(reg(cpu,"RP")==0x10fcda && reg(cpu,"R8")==0x60,"First task call");
        cases++;
    }
    @Override public void run()throws Exception{
        super.run();require(cases==8,"Scenario count");
        println("AB-157: eight selections restored record 8 and reached its first call");
    }
}
