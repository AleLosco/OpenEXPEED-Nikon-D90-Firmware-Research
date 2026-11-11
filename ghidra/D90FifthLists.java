// AB-146: original fifth-call list initialization, stopping before INT.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
import java.nio.ByteBuffer;

public class D90FifthLists extends D90FourthCallIntegrated {
    private int listCases;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), w=Arrays.copyOf(base,base.length+4);
        w[base.length]=new long[]{0x1000ea,0x1000f2};
        w[base.length+1]=new long[]{0x112008,0x112016};
        w[base.length+2]=new long[]{0x11212a,0x112172};
        w[base.length+3]=new long[]{0x105b02,0x105b26};
        return w;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), w=Arrays.copyOf(base,base.length+3);
        w[base.length]=new long[]{0x8f52e4acL,0x8f52e4c4L};
        w[base.length+1]=new long[]{0x8f52e70cL,0x8f52e71cL};
        w[base.length+2]=new long[]{0x8f52e95cL,0x8f52e96cL};
        return w;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        long sp=reg(c,"SP");
        String[] names={"R8","R9","R10"};
        long[] values=new long[3];
        for(int i=0;i<3;i++)values[i]=reg(c,names[i]);
        long[] bases={0x8f52e4acL,0x8f52e70cL,0x8f52e95cL};
        int[] sizes={24,16,16};
        byte[][] expected=new byte[3][];
        for(int i=0;i<3;i++){
            expected[i]=new byte[sizes[i]];
            Arrays.fill(expected[i],(byte)0xcd);
            c.writeMemory(a.getAddress(bases[i]),expected[i]);
        }
        ByteBuffer.wrap(expected[0]).putInt(4,0x8f52e4b0).putInt(8,0x8f52e4b0)
            .putInt(12,0x8f52e960).putInt(16,0x8f52e710);
        ByteBuffer.wrap(expected[1]).putInt(4,0x8f52e4b8).putInt(8,0x8f52e960);
        ByteBuffer.wrap(expected[2]).putInt(4,0x8f52e710).putInt(8,0x8f52e4b8);
        until(c,d,0x112014);
        for(int i=0;i<3;i++)require(Arrays.equals(expected[i],c.readMemory(a.getAddress(bases[i]),sizes[i])),"List and guards "+i);
        require(reg(c,"SP")==sp-4 && word(c,a,sp-4)==0x1000f2,"Outer saved RP");
        for(int i=0;i<3;i++)require(reg(c,names[i])==values[i],"Preserved "+names[i]);
        require(reg(c,"R12")==0xffffffe9L && reg(c,"R4")==28 && reg(c,"R5")==0,"INT arguments");
        require(reg(c,"S")==1 && reg(c,"I")==1 && reg(c,"ILM")==31,"User boundary");
        listCases++;
    }
    @Override public void run()throws Exception{
        super.run();require(listCases==8,"Case count");
        println("AB-146: eight fifth-call list preparations passed before INT 0x40");
    }
}
