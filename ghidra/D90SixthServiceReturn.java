// AB-153: execute the second user service -23 against the inherited OS lists.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90SixthServiceReturn extends D90SixthServiceRequest {
    private static final long CONTEXT = 0x8f993e04L;
    private int cases;
    @Override long[][] extraSyntheticWindows() {
        long[][] base = super.extraSyntheticWindows();
        long[][] result = Arrays.copyOf(base,base.length+1);
        result[base.length] = new long[]{CONTEXT-4,CONTEXT+44};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p) throws Exception {
        super.afterInitialization(cpu,decoder,p);
        var space = p.getLanguage().getDefaultSpace();
        int node=0x68000270, head=0x68000020;
        byte[] os=cpu.readMemory(space.getAddress(0x68000000),4096);
        ByteBuffer expected=ByteBuffer.wrap(os);
        require(expected.getInt(0)!=node && expected.getInt(0x278)==16 && expected.getShort(0x27c)==4,"Record 8 state");
        require(expected.getInt(0x20)==head && expected.getInt(0x24)==head,"Empty list 4");
        byte[] frame=new byte[48];
        Arrays.fill(frame,(byte)0xa5);
        cpu.writeMemory(space.getAddress(CONTEXT-4),frame);
        byte[] previousContext=cpu.readMemory(space.getAddress(0x8f9b0204L),40);
        enterUserService23(cpu,decoder,p,0x10fcca,8);
        long sp=reg(cpu,"SP");
        until(cpu,decoder,0x406c6);
        expected.putInt(4,expected.getInt(4)|0x10000000);
        expected.putInt(0x20,node).putInt(0x24,node);
        expected.putInt(0x270,head).putInt(0x274,head).putInt(0x278,0).putInt(0x288,(int)CONTEXT);
        ByteBuffer.wrap(frame).putInt(4,0x10fcce).putInt(8,0x1f0030).putInt(12,0x4050a).putInt(40,0);
        require(Arrays.equals(os,cpu.readMemory(space.getAddress(0x68000000),4096)),"OS region including previous list");
        require(Arrays.equals(frame,cpu.readMemory(space.getAddress(CONTEXT-4),48)),"Record 8 context and guards");
        require(Arrays.equals(previousContext,cpu.readMemory(space.getAddress(0x8f9b0204L),40)),"Previous context");
        require(reg(cpu,"R12")==0 && reg(cpu,"SP")==sp,"Local return");
        cases++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(cases==8,"Scenario count");
        println("AB-153: eight second service returns inserted record 8 in list 4");
    }
}
