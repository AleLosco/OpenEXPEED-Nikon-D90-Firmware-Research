// AB-126: first eight original instructions after the dispatcher return.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90CandidatePrologue extends D90DispatchReturn {
    int prologues;
    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{0x1000ac,0x1000d2};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        long sp=reg(c,"SP");
        require(reg(c,"PC")==0x1000ac && sp==0x8f991254L && reg(c,"S")==1,"Returned user context");
        byte[] stack=c.readMemory(a.getAddress(sp-80),80);
        ByteBuffer expected=ByteBuffer.wrap(stack);
        expected.position(56);
        for(String name:new String[]{"FP","RP","R8","R9","R10","R11"})
            expected.putInt((int)reg(c,name));
        until(c,d,0x1000b2);
        require(reg(c,"SP")==sp-28 && reg(c,"FP")==sp-24,"Prologue frame");
        until(c,d,0x1000d0);
        long[] targets={0x8f52edf9L,0x8f52edfcL,0x8f52f1baL,0x8f549e28L,0x19a542L};
        for(int i=0;i<targets.length;i++)
            require(reg(c,"R"+(8+i))==targets[i],"Loaded R"+(8+i));
        require(Arrays.equals(stack,c.readMemory(a.getAddress(sp-80),80)),"Saved words and surrounding candidate stack");
        require(d.disassemble(c.getExecutionAddress()).toString().equals("CALL @R12"),"First call boundary");
        prologues++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(prologues==8,"Scenario count");
        println("AB-126: eight candidate prologues reached the first CALL at 0x1000d0");
    }
}
