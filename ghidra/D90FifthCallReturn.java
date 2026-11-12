// AB-151: complete the fifth candidate call and inspect its continuation.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90FifthCallReturn extends D90Service23UserReturn {
    private int returns;
    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base,base.length+1);
        windows[base.length] = new long[]{0x112016,0x11201a};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper emulator, PseudoDisassembler decoder, ProgramDB program) throws Exception {
        super.afterInitialization(emulator,decoder,program);
        var space = program.getLanguage().getDefaultSpace();
        long stack = reg(emulator,"SP");
        long result = reg(emulator,"R12");
        byte[] userStack = emulator.readMemory(space.getAddress(0x8f991204L),80);
        require(word(emulator,space,stack)==0x1000f2,"Saved candidate return");
        until(emulator,decoder,0x1000f2);
        require(reg(emulator,"SP")==stack+4 && reg(emulator,"RP")==0x1000f2,"Fifth call return");
        require(reg(emulator,"R12")==result,"Service result preserved");
        require(reg(emulator,"S")==1 && reg(emulator,"I")==1 && reg(emulator,"ILM")==31,"User status preserved");
        require(Arrays.equals(userStack,emulator.readMemory(space.getAddress(0x8f991204L),80)),"Stack memory preserved");
        returns++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(returns==8,"Return count");
        println("AB-151: eight fifth-call returns at 0x1000f2 preserve 0 or -63");
        var decoder = new PseudoDisassembler(currentProgram);
        long pc = 0x1000f2;
        for(int count=0;count<24;count++) {
            var instruction = decoder.disassemble(toAddr(pc));
            require(instruction!=null,"Continuation decode");
            println(String.format("AB-151 continuation %x %s",pc,instruction));
            pc += instruction.getLength();
            if(instruction.getFlowType().isCall()) return;
        }
        throw new IllegalStateException("Continuation bound reached before call");
    }
}
