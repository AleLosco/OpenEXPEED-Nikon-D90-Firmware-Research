// AB-136: four original helper calls with controlled flag 2.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Type2Defaults extends D90ThirdCallCopies {
    int checked;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(),w=Arrays.copyOf(base,base.length+3);
        w[base.length]=new long[]{0x13a27a,0x13a2b8};
        w[base.length+1]=new long[]{0x13a802,0x13a814};
        w[base.length+2]=new long[]{0x1cabaf,0x1cabc2};
        return w;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        byte[] block=c.readMemory(a.getAddress(0x8f53da52L),234);
        byte[] settings=c.readMemory(a.getAddress(0x8f53c4d4L),160);
        byte[] stack=c.readMemory(a.getAddress(0x8f991204L),80);
        byte[] defaults=c.readMemory(a.getAddress(0x1cabaf),19);
        String[] names={"R8","R9","R10","FP","SP","RP"};
        long[] state=new long[names.length];
        for(int i=0;i<names.length;i++)state[i]=reg(c,names[i]);
        for(int missing=0;missing<2;missing++){
            for(int i=0;i<names.length;i++)c.writeRegister(names[i],state[i]);
            c.writeRegister("PC",0x13a278);
            c.writeMemory(a.getAddress(0x8f53300dL),new byte[]{0,(byte)missing});
            c.writeMemory(a.getAddress(0x8f53da52L),block);
            byte[] expected=block.clone();
            if(missing==1){
                for(int call=0;call<4;call++){
                    until(c,d,0x13a802);
                    require(reg(c,"R4")==0x8f53da71L+58*call && reg(c,"RP")==0x13a2ac,"Call arguments");
                    until(c,d,0x13a2ac);
                    System.arraycopy(defaults,0,expected,31+58*call,19);
                }
            }
            until(c,d,0x13a2b6);
            require(Arrays.equals(expected,c.readMemory(a.getAddress(0x8f53da52L),234)),"Complete type-2 block");
            require(Arrays.equals(settings,c.readMemory(a.getAddress(0x8f53c4d4L),160)),"Type-1 block");
            require(Arrays.equals(stack,c.readMemory(a.getAddress(0x8f991204L),80)),"Stack");
            require(reg(c,"SP")==state[4],"SP retained");
            checked++;
        }
    }
    @Override public void run()throws Exception{
        super.run();require(checked==16,"Case count");
        println("AB-136: 16 type-2 branch cases and 32 helper calls passed");
    }
}
