// AB-158: follow the first task call until the nested OS request.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Record8TableInit extends D90Record8Dispatch {
    private static final long HEAD=0x8f52f274L;
    private static final int ROWS=71, STRIDE=12;
    private int cases;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), result=Arrays.copyOf(base,base.length+2);
        result[base.length]=new long[]{0x10fd26,0x10fd40};
        result[base.length+1]=new long[]{0x11031c,0x11034c};
        return result;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), result=Arrays.copyOf(base,base.length+1);
        result[base.length]=new long[]{HEAD-4,HEAD+8+ROWS*STRIDE+4};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p)throws Exception{
        super.afterInitialization(cpu,decoder,p);
        var space=p.getLanguage().getDefaultSpace();
        byte[] expected=new byte[4+8+ROWS*STRIDE+4];
        Arrays.fill(expected,(byte)0xcd);
        cpu.writeMemory(space.getAddress(HEAD-4),expected);
        ByteBuffer.wrap(expected).putInt(4,(int)HEAD).putInt(8,(int)HEAD);
        for(int i=0;i<ROWS;i++) expected[4+8+i*STRIDE+8]=(byte)i;
        until(cpu,decoder,0x11034a);
        require(Arrays.equals(expected,cpu.readMemory(space.getAddress(HEAD-4),expected.length)),"Head, 71 full rows and guards");
        require(reg(cpu,"R4")==3 && reg(cpu,"R5")==128 && reg(cpu,"R12")==0xffffffd0L,"Service -48 arguments");
        require(word(cpu,space,0x68000000)==0x68000270 && reg(cpu,"S")==1,"Record 8 remains current");
        cases++;
    }
    @Override public void run()throws Exception{
        super.run();require(cases==8,"Scenario count");
        println("AB-158: eight table initializations reached service -48 with 71 indexed rows");
    }
}
