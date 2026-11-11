// AB-130: both SYSTEMFIXADJUST choices after the final AB-128 ENDM case.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90CandidateSystemFix extends D90CandidateEndm {
    int systemCopies;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+3);
        windows[base.length]=new long[]{0x1000d2,0x1000da};
        windows[base.length+1]=new long[]{0x18522c,0x18527a};
        windows[base.length+2]=new long[]{0x1d610c,0x1d6200};
        return windows;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), windows=Arrays.copyOf(base,base.length+2);
        windows[base.length]=new long[]{0x880000,0x8800f4};
        windows[base.length+1]=new long[]{0x8f933210L,0x8f93330cL};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        require(reg(c,"PC")==0x1000d2,"After ENDM");
        byte[] defaults=c.readMemory(a.getAddress(0x1d610c),244);
        byte[] signature=Arrays.copyOfRange(defaults,228,244);
        require(Arrays.equals(signature,"SYSTEMFIXADJUST\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII)),"Original signature");
        byte[] endm=c.readMemory(a.getAddress(0x8f95130cL),0x7b24);
        byte[] stack=c.readMemory(a.getAddress(0x8f991204L),80);
        long sp=reg(c,"SP");
        for(int valid=0;valid<2;valid++){
            c.writeRegister("PC",0x1000d2);
            byte[] source=new byte[244];
            Arrays.fill(source,(byte)0x6b);
            if(valid==1) System.arraycopy(signature,0,source,228,16);
            byte[] dirty=new byte[252];
            Arrays.fill(dirty,(byte)0xcc);
            c.writeMemory(a.getAddress(0x880000),source);
            c.writeMemory(a.getAddress(0x8f933210L),dirty);
            byte[] expected=dirty.clone();
            System.arraycopy(valid==1?source:defaults,0,expected,4,244);
            until(c,d,0x1000da);
            require(Arrays.equals(expected,c.readMemory(a.getAddress(0x8f933210L),252)),"System block and guards");
            require(Arrays.equals(source,c.readMemory(a.getAddress(0x880000),244)),"External source");
            require(Arrays.equals(endm,c.readMemory(a.getAddress(0x8f95130cL),0x7b24)),"ENDM and pointer");
            require(Arrays.equals(stack,c.readMemory(a.getAddress(0x8f991204L),80)),"Stack bytes");
            require(reg(c,"SP")==sp && reg(c,"RP")==0x1000da,"Return");
            systemCopies++;
        }
    }
    @Override public void run()throws Exception{
        super.run();
        require(systemCopies==16,"Case count");
        println("AB-130: 16 SYSTEMFIXADJUST copies and ENDM preservation checks passed");
    }
}
