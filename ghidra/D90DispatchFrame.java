// AB-121: stage candidate PC and PS on the supervisor stack.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90DispatchFrame extends D90DispatchLoad {
    int frames;

    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base,base.length+1);
        windows[base.length] = new long[]{0x40e90,0x40ea8};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper c, PseudoDisassembler d, ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var a = p.getLanguage().getDefaultSpace();
        long user = reg(c,"SP"), system = reg(c,"SSP"), rp = reg(c,"RP"), fp = reg(c,"FP");
        long pc = reg(c,"R0"), ps = reg(c,"R1");
        require(system==0x81004000L && user==0x8f99120cL,"Starting stacks");
        byte[] os = c.readMemory(a.getAddress(0x68000000L),4096);
        byte[] candidate = c.readMemory(a.getAddress(user-8),40);
        ByteBuffer.wrap(candidate).putInt((int)fp).putInt((int)rp);
        byte[] supervisor = c.readMemory(a.getAddress(system-128),256);
        ByteBuffer.wrap(supervisor).putInt(120,(int)pc).putInt(124,(int)ps);
        until(c,d,0x404f6);
        require(reg(c,"RP")==0x40e9c && reg(c,"SP")==user-8,"Hook entry");
        until(c,d,0x40ea2);
        require(reg(c,"S")==0 && reg(c,"SP")==user && reg(c,"FP")==fp && reg(c,"RP")==rp,"Hook return and S transition");
        require(reg(c,"R0")==pc && reg(c,"R1")==ps,"Frame values survived hook");
        c.writeRegister("USP",user);
        c.writeRegister("SP",system);
        until(c,d,0x40ea4);
        require(reg(c,"SP")==system-4 && word(c,a,system-4)==(int)ps,"PS push");
        until(c,d,0x40ea6);
        require(reg(c,"SP")==system-8 && word(c,a,system-8)==(int)pc,"PC push");
        require(Arrays.equals(supervisor,c.readMemory(a.getAddress(system-128),256)),"Supervisor frame and surrounding bytes");
        require(Arrays.equals(candidate,c.readMemory(a.getAddress(user-8),40)),"Candidate scratch stores");
        require(Arrays.equals(os,c.readMemory(a.getAddress(0x68000000L),4096)),"Outgoing OS state");
        frames++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(frames==8,"Scenario count");
        println("AB-121: eight supervisor frames passed at 0x40ea6");
    }
}
