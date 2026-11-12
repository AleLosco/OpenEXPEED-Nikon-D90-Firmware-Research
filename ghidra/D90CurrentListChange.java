// AB-156: isolated service -27 on the ready lists retained from AB-155.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90CurrentListChange extends D90SeventhCallEntry {
    private int cases;
    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+2);
        windows[base.length]=new long[]{0x4168e,0x416ec};
        windows[base.length+1]=new long[]{0x418a0,0x418d2};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p) throws Exception {
        super.afterInitialization(cpu,decoder,p);
        var space=p.getLanguage().getDefaultSpace();
        int current=word(cpu,space,0x68000000), before=word(cpu,space,0x68000004);
        long frame=0x81003fb0L;
        // Isolate the service from intervening initialization calls and INT entry.
        cpu.writeMemory(space.getAddress(frame+0x4f),new byte[]{0x30});
        cpu.writeRegister("FP",frame);
        cpu.writeRegister("SP",frame);
        cpu.writeRegister("S",0);
        cpu.writeRegister("I",0);
        cpu.writeRegister("R4",0);
        cpu.writeRegister("R5",7);
        cpu.writeRegister("RP",0x406c6);
        cpu.writeRegister("PC",0x4168e);
        until(cpu,decoder,0x406c6);
        require(reg(cpu,"R12")==0 && reg(cpu,"SP")==frame,"Service return");
        require(word(cpu,space,0x68000008)==0x68000008 && word(cpu,space,0x6800000c)==0x68000008,"Empty list 1");
        require(word(cpu,space,0x68000038)==current && word(cpu,space,0x6800003c)==current,"List 7 sentinel");
        require(word(cpu,space,current)==0x68000038 && word(cpu,space,current+4)==0x68000038,"Node links");
        require(word(cpu,space,0x68000000)==current && word(cpu,space,0x68000020)==0x68000270,"Current and list 4");
        require(word(cpu,space,0x68000004)==((before & 0x7fffffff)|0x02000000),"Updated bitmap");
        cases++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(cases==8,"Scenario count");
        println("AB-156: eight isolated service -27 returns moved current control from list 1 to 7");
    }
}
