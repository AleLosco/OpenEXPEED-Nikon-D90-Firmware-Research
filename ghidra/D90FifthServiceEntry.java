// AB-147: explicit user INT entry at the AB-146 boundary, then original dispatcher.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90FifthServiceEntry extends D90FifthLists {
    private int entries;
    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{0x40644,0x40646};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        enterUserService23(c,d,p,0x112014,28);
        entries++;
    }
    void enterUserService23(EmulatorHelper c,PseudoDisassembler d,ProgramDB p,long site,int index)throws Exception{
        enterUserOsService(c,d,p,site,index,0,-23,0x414da,0x40644);
    }
    void enterUserOsService(EmulatorHelper c,PseudoDisassembler d,ProgramDB p,long site,int index,int second,int selector,long target,long slot)throws Exception{
        var a=p.getLanguage().getDefaultSpace();
        long pc=reg(c,"PC"), user=reg(c,"SP"), system=reg(c,"SSP");
        require(pc==site && Arrays.equals(c.readMemory(a.getAddress(pc),2),new byte[]{0x1f,0x40}),"User INT site");
        require(reg(c,"S")==1 && system==0x81004000L && (user&3)==0,"User and supervisor stacks");
        require(reg(c,"TBR")==0xdfc00 && word(c,a,0xdfefc)==0x4067a,"INT vector");
        byte[] userFrame=c.readMemory(a.getAddress(0x8f991204L),80);
        // Separate SLEIGH flag registers are authoritative when packing the saved PS.
        int ps=(int)reg(c,"PS") & ~0x001f073f;
        String[] flags={"C","V","Z","N","I","S","T","D0","D1"};
        int[] bits={0,1,2,3,4,5,8,9,10};
        for(int i=0;i<bits.length;i++) ps|=(int)(reg(c,flags[i])&1)<<bits[i];
        ps|=(int)(reg(c,"ILM")&31)<<16;
        put(c,a,system-4,ps);
        put(c,a,system-8,(int)pc+2);
        c.writeRegister("USP",user);
        c.writeRegister("SSP",system-8);
        c.writeRegister("SP",system-8);
        c.writeRegister("S",0);c.writeRegister("I",0);c.writeRegister("PS",ps&~0x30);
        c.writeRegister("PC",word(c,a,0xdfefc));
        until(c,d,0x406b6);
        require(reg(c,"SP")==system-80 && reg(c,"FP")==system-80 && reg(c,"S")==0,"Software frame");
        require(word(c,a,system-80+16)==index && word(c,a,system-80+20)==second,"Saved R4 and R5");
        require(word(c,a,system-80+48)==(selector & 0x7fffffff) && reg(c,"ILM")==30,"Saved selector and active ILM");
        require(currentProgram.getMemory().getShort(toAddr(slot))==target-0x40672,"Service displacement");
        until(c,d,target);
        require(reg(c,"RP")==0x406c6 && reg(c,"USP")==user,"Service entry and user stack bank");
        require(word(c,a,system-8)==site+2 && word(c,a,system-4)==ps,"Preserved INT frame");
        require(Arrays.equals(userFrame,c.readMemory(a.getAddress(0x8f991204L),80)),"User frame preserved");
    }
    @Override public void run()throws Exception{
        super.run();require(entries==8,"Case count");
        println("AB-147: eight modeled user INT entries dispatched to 0x414da");
    }
}
