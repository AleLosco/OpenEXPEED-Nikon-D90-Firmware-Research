// AB-120: load the candidate stack and its first two words.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
import java.nio.ByteBuffer;

public class D90DispatchLoad extends D90DispatchSave {
    int loads;

    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base, base.length + 2);
        windows[base.length] = new long[]{0x40e7c, 0x40e90};
        windows[base.length+1] = new long[]{0x404fc, 0x404fe};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper c, PseudoDisassembler d, ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var a = p.getLanguage().getDefaultSpace();
        long rp = reg(c,"RP");
        byte[] expected = c.readMemory(a.getAddress(0x68000000L),4096);
        ByteBuffer.wrap(expected).putInt(0xecc,(int)rp);
        byte[] frame = c.readMemory(a.getAddress(0x8f991204L),40);
        until(c,d,0x40e8a);
        require(reg(c,"SP")==0x68000ed0L && reg(c,"FP")==0x680001ecL && reg(c,"RP")==rp,"Hook returned to candidate load");
        until(c,d,0x40e8c);
        require(reg(c,"SP")==0x8f991204L,"Stack loaded from candidate");
        until(c,d,0x40e90);
        require(reg(c,"R0")==0x1000ac && reg(c,"R1")==0x1f0030 && reg(c,"SP")==0x8f99120cL,"First two candidate words");
        require(Arrays.equals(frame,c.readMemory(a.getAddress(0x8f991204L),40)),"Candidate memory");
        require(Arrays.equals(expected,c.readMemory(a.getAddress(0x68000000L),4096)),"Outgoing frame and OS state");
        loads++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(loads==8,"Scenario count");
        println("AB-120: eight candidate stack loads passed at 0x40e90");
    }
}
