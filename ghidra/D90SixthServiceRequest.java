// AB-152: execute the sixth call up to its INT and inspect the selected descriptor.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90SixthServiceRequest extends D90FifthCallReturn {
    private int requests;
    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base,base.length+2);
        windows[base.length] = new long[]{0x1000f2,0x1000fa};
        windows[base.length+1] = new long[]{0x10fcc2,0x10fcce};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB program) throws Exception {
        super.afterInitialization(cpu,decoder,program);
        long stack = reg(cpu,"SP");
        until(cpu,decoder,0x10fcca);
        require(reg(cpu,"RP")==0x1000fa && reg(cpu,"SP")==stack && reg(cpu,"S")==1,"User call and stack");
        require(reg(cpu,"R4")==8 && reg(cpu,"R5")==0 && reg(cpu,"R12")==0xffffffe9L,"Service arguments");
        requests++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(requests==8,"Scenario count");
        var memory = currentProgram.getMemory();
        int count = Short.toUnsignedInt(memory.getShort(toAddr(0x5c40c)));
        int stride = Short.toUnsignedInt(memory.getShort(toAddr(0x5c40e)));
        require(count==38 && stride==28,"Descriptor header");
        long record = 0x5c410L+7*stride;
        println(String.format("AB-152: record 8 at %x control=%08x entry=%08x context=%08x",
            record,memory.getInt(toAddr(record)),memory.getInt(toAddr(record+16)),memory.getInt(toAddr(record+20))));
        println("AB-152: eight sixth calls reached INT with service -23 and arguments 8/0");
    }
}
