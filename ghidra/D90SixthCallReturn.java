// AB-154: complete the sixth call and list the next call boundary.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;

public class D90SixthCallReturn extends D90SixthServiceReturn {
    private int returns;
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p) throws Exception {
        super.afterInitialization(cpu,decoder,p);
        returnUserService23(cpu,decoder,p,0x10fccc);
        long stack=reg(cpu,"SP");
        until(cpu,decoder,0x1000fa);
        require(reg(cpu,"SP")==stack && reg(cpu,"RP")==0x1000fa && reg(cpu,"R12")==0,"Sixth call return");
        returns++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(returns==8,"Return count");
        println("AB-154: eight sixth-call returns reached 0x1000fa with result zero");
        var decoder=new PseudoDisassembler(currentProgram);
        long pc=0x1000fa;
        for(int n=0;n<24;n++) {
            var instruction=decoder.disassemble(toAddr(pc));
            require(instruction!=null,"Next call decode");
            println(String.format("AB-154 continuation %x %s",pc,instruction));
            pc+=instruction.getLength();
            if(instruction.getFlowType().isCall()) return;
        }
        throw new IllegalStateException("Next call listing bound");
    }
}
