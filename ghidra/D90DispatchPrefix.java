// AB-118: synthetic vector entry after the AB-113 user loop.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90DispatchPrefix extends D90DispatchReadyState {
    int prefixes;
    @Override long[][] extraWindows() {
        long[][] previous = super.extraWindows();
        long[][] windows = Arrays.copyOf(previous, previous.length + 1);
        windows[previous.length] = new long[]{0x40d44, 0x40d58};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c, PseudoDisassembler d, ProgramDB p) throws Exception {
        super.afterInitialization(c, d, p);
        var a = p.getLanguage().getDefaultSpace();
        long user = reg(c,"SP"), system = reg(c,"SSP"), pc = reg(c,"PC"), ps = reg(c,"PS");
        int target = currentProgram.getMemory().getInt(toAddr(0xdff00));
        require(target == 0x40d44 && ps == 0x1f0030 && pc == 0x407ee, "Entry state");
        long[] saved = {reg(c,"R12"), reg(c,"AC"), reg(c,"FP")};
        byte[] os = c.readMemory(a.getAddress(0x68000000L), 0x1000);
        // Impose a supervisor frame, retaining the loop PC and starting ILM.
        put(c,a,system-8,(int)pc);
        put(c,a,system-4,(int)ps);
        c.writeRegister("SSP",system-8);
        c.writeRegister("SP",system-8);
        c.writeRegister("PS",ps & ~0x30);
        c.writeRegister("S",0);
        c.writeRegister("I",0);
        c.writeRegister("PC",target);
        until(c,d,0x40d48);
        require(reg(c,"S")==1 && reg(c,"I")==0 && reg(c,"SP")==system-8,"Before modeled stack switch");
        c.writeRegister("SP",user); // ORCCR selected USP. Ghidra leaves SP unchanged.
        until(c,d,0x40e2a);
        require(reg(c,"SP")==user-12 && reg(c,"SSP")==system-8,"Prefix stack pointers");
        var expected = java.nio.ByteBuffer.wrap(os);
        for(int i=0;i<saved.length;i++)
            expected.putInt((int)(user-12-0x68000000L)+4*i,(int)saved[i]);
        require(Arrays.equals(os,c.readMemory(a.getAddress(0x68000000L),0x1000)),"Saved register order and OS memory");
        require(word(c,a,system-8)==(int)pc && word(c,a,system-4)==(int)ps,"Supervisor frame");
        prefixes++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(prefixes==8,"Scenario count");
        println("AB-118: eight synthetic vector prefixes reached bitmap entry with checked stacks");
    }
}
