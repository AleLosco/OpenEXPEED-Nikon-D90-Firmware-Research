// AB-158: bounded static listing of the candidate's record 8 first callee.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;

public class D90Record8FirstCallee extends GhidraScript {
    @Override public void run() throws Exception {
        list(0x10fd26);
        list(0x11031c);
    }
    private void list(long pc) throws Exception {
        var decoder=new PseudoDisassembler(currentProgram);
        for(int count=0;count<320;count++){
            var ins=decoder.disassemble(toAddr(pc));
            if(ins==null) throw new IllegalStateException("Decode at "+Long.toHexString(pc));
            println(String.format("AB-158 %x %s",pc,ins));
            pc+=ins.getLength();
            if(ins.getMnemonicString().equals("RET")) {
                println("AB-158: bounded static listing reached return");
                return;
            }
            if(ins.getMnemonicString().equals("RET:D")){
                var slot=decoder.disassemble(toAddr(pc));
                println(String.format("AB-158 %x %s",pc,slot));
                println("AB-158: bounded static listing reached delayed return");
                return;
            }
        }
        throw new IllegalStateException("Listing bound reached");
    }
}
