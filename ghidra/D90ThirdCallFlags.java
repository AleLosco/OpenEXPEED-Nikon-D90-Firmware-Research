// AB-135: bounded flag-controlled branch experiment after AB-134.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90ThirdCallFlags extends D90ThirdCallCopies {
    int branches;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(),w=Arrays.copyOf(base,base.length+1);
        w[base.length]=new long[]{0x13a27a,0x13a2b8};
        return w;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        byte[] block=c.readMemory(a.getAddress(0x8f53c4d4L),160);
        byte[] stack=c.readMemory(a.getAddress(0x8f991204L),80);
        String[] names={"R8","R9","R10","FP","SP","RP"};
        long[] state=new long[names.length];
        for(int i=0;i<names.length;i++)state[i]=reg(c,names[i]);
        for(int missing1=0;missing1<2;missing1++)for(int missing2=0;missing2<2;missing2++){
            for(int i=0;i<names.length;i++)c.writeRegister(names[i],state[i]);
            c.writeRegister("PC",0x13a278);
            c.writeMemory(a.getAddress(0x8f53300dL),new byte[]{(byte)missing1,(byte)missing2});
            c.writeMemory(a.getAddress(0x8f53c4d4L),block);
            byte[] expected=block.clone();
            if(missing1==1){
                expected[0]&=(byte)0xcf;
                expected[4]=(byte)((expected[4]&0x83)|8);
            }
            until(c,d,missing2==0?0x13a2b6:0x13a802);
            require(Arrays.equals(expected,c.readMemory(a.getAddress(0x8f53c4d4L),160)),"Flag-one controlled bytes");
            require(Arrays.equals(stack,c.readMemory(a.getAddress(0x8f991204L),80)),"Stack retained");
            if(missing2==1)
                require(reg(c,"R4")==0x8f53da71L && reg(c,"RP")==0x13a2ac,"First helper call arguments");
            branches++;
        }
    }
    @Override public void run()throws Exception{
        super.run();require(branches==32,"Case count");
        println("AB-135: 32 flag-controlled branches passed before helper execution");
    }
}
