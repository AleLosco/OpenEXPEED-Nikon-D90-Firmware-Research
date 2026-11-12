// AB-155: observe ready-list state as initialization reaches the AB-72 routine.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90SeventhCallEntry extends D90SixthCallReturn {
    private int withBoth, withRecord8;
    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows(), result=Arrays.copyOf(base,base.length+2);
        result[base.length]=new long[]{0x1000fa,0x100102};
        result[base.length+1]=new long[]{0x14ca90,0x14ca9a};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p) throws Exception {
        super.afterInitialization(cpu,decoder,p);
        var space=p.getLanguage().getDefaultSpace();
        long stack=reg(cpu,"SP");
        until(cpu,decoder,0x19d89c);
        require(reg(cpu,"SP")==stack-4 && word(cpu,space,stack-4)==0x100102 && reg(cpu,"RP")==0x14ca9a,"Nested calls");
        int current=word(cpu,space,0x68000000), bitmap=word(cpu,space,0x68000004);
        require(current==0x680001ec && word(cpu,space,0x68000008)==current,"Current control and list 1");
        require(word(cpu,space,0x68000020)==0x68000270,"Record 8 in list 4");
        if(word(cpu,space,0x68000068)==0x680005e0) {
            require(bitmap==0x90080000,"Bitmap with record 28");
            withBoth++;
        } else {
            require(word(cpu,space,0x68000068)==0x68000068 && bitmap==0x90000000,"Bitmap without record 28");
            withRecord8++;
        }
    }
    @Override public void run() throws Exception {
        super.run();
        require(withBoth==4 && withRecord8==4,"Ready list cases");
        println("AB-155: eight entries at 0x19d89c retain current control at head of list 1");
    }
}
