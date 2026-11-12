// AB-159: original OS service -48 entered from the task's first nested request.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Record8Service48 extends D90Record8TableInit {
    private int cases;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), result=Arrays.copyOf(base,base.length+2);
        result[base.length]=new long[]{0x40612,0x40614};
        result[base.length+1]=new long[]{0x41a5a,0x41af4};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p)throws Exception{
        super.afterInitialization(cpu,decoder,p);
        var space=p.getLanguage().getDefaultSpace();
        long table=0x5cb68;
        int count=Short.toUnsignedInt(currentProgram.getMemory().getShort(toAddr(table)));
        int stride=Short.toUnsignedInt(currentProgram.getMemory().getShort(toAddr(table+2)));
        long descriptor=table+4+2*stride;
        long object=Integer.toUnsignedLong(word(cpu,space,descriptor));
        int before=word(cpu,space,object+8);
        require(word(cpu,space,object)==object && word(cpu,space,object+4)==object,"Empty object 3 waiting list");
        byte[] os=cpu.readMemory(space.getAddress(0x68000000),4096);
        java.nio.ByteBuffer.wrap(os).putInt((int)(object-0x68000000)+8,before|128);
        enterUserOsService(cpu,decoder,p,0x11034a,3,128,-48,0x41a5a,0x40612);
        until(cpu,decoder,0x406c6);
        require(reg(cpu,"R12")==0,"Service result");
        // The service clears the shared flag before processing and sets it at exit.
        os[0x132]|=(byte)0x80;
        byte[] actual=cpu.readMemory(space.getAddress(0x68000000),4096);
        for(int i=0;i<4096;i++) if(os[i]!=actual[i]) println(String.format("AB-159 mismatch +%x expected=%02x actual=%02x",i,os[i]&255,actual[i]&255));
        require(Arrays.equals(os,actual),"OS state and object flags");
        println(String.format("AB-159 object=%x header=%d/%d flags=%x->%x",object,count,stride,before,word(cpu,space,object+8)));
        cases++;
    }
    @Override public void run()throws Exception{
        super.run();require(cases==8,"Scenario count");
        println("AB-159: eight service -48 returns verified on the inherited empty object list");
    }
}
