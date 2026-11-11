// AB-122: bound the candidate restore through the current-control store.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
import java.nio.ByteBuffer;

public class D90DispatchRegisters extends D90DispatchFrame {
    int restores;
    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base,base.length+1);
        windows[base.length] = new long[]{0x40ea8,0x40eb6};
        return windows;
    }
    @Override long[][] extraSyntheticWindows() {
        long[][] base = super.extraSyntheticWindows();
        long[][] windows = Arrays.copyOf(base,base.length+1);
        windows[base.length] = new long[]{0x8f99122cL,0x8f991248L};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c, PseudoDisassembler d, ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var a = p.getLanguage().getDefaultSpace();
        long user = reg(c,"USP"), system = reg(c,"SP");
        require(user==0x8f99120cL && system==0x81003ff8L,"Stacks at frame boundary");
        byte[] os = c.readMemory(a.getAddress(0x68000000L),4096);
        byte[] supervisor = c.readMemory(a.getAddress(system),8);
        // The previous fixture provided 40 bytes. Tag only the seven additional words.
        for(int i=0;i<7;i++)
            put(c,a,0x8f99122cL+4*i,0x12200000+16*restores+i);
        byte[] source = c.readMemory(a.getAddress(user),60);
        until(c,d,0x40ea8);
        require(reg(c,"S")==1 && reg(c,"SP")==system,"Mode transition");
        c.writeRegister("SSP",system);
        c.writeRegister("SP",user);
        until(c,d,0x40eb2);
        String[] registers={"RP","MDL","MDH","R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11"};
        var words=ByteBuffer.wrap(source);
        for(String name:registers)
            require(reg(c,name)==Integer.toUnsignedLong(words.getInt()),"Restored "+name);
        require(reg(c,"SP")==user+60 && reg(c,"AC")==0x680001ecL && reg(c,"R12")==0x68000000L,"Store inputs");
        require(Arrays.equals(os,c.readMemory(a.getAddress(0x68000000L),4096)) && word(c,a,0x68000000L)==0x680007c4,"Before store");
        until(c,d,0x40eb4);
        ByteBuffer.wrap(os).putInt(0x680001ec);
        require(Arrays.equals(os,c.readMemory(a.getAddress(0x68000000L),4096)),"Current control updated");
        require(Arrays.equals(source,c.readMemory(a.getAddress(user),60)) && Arrays.equals(supervisor,c.readMemory(a.getAddress(system),8)),"Frames preserved");
        restores++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(restores==8,"Scenario count");
        println("AB-122: eight register restores and current-control stores passed at 0x40eb4");
    }
}
