// AB-155: inspect each straight-line prefix up to its first transfer.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;

public class D90PreparedTaskEntries extends GhidraScript {
    @Override public void run() throws Exception {
        var decoder = new PseudoDisassembler(currentProgram);
        for (long entry : new long[]{0x14ca90,0x10fcce,0x11201a}) {
            long pc = entry;
            for (int i=0; i<80; i++) {
                var ins = decoder.disassemble(toAddr(pc));
                if (ins==null) throw new IllegalStateException("Decode "+Long.toHexString(pc));
                println(String.format("AB-155 %x %x %s",entry,pc,ins));
                pc += ins.getLength();
                if (!ins.getFlowType().isFallthrough()) {
                    if (ins.getDelaySlotDepth()>0) println(String.format("AB-155 slot %x %s",pc,decoder.disassemble(toAddr(pc))));
                    break;
                }
                if (i==79) throw new IllegalStateException("Prefix bound");
            }
        }
        println("AB-155: three entry prefixes listed");
    }
}
