// AB-129: bounded static listing of the candidate's second call.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;

public class D90SecondCallContext extends GhidraScript {
    @Override public void run() throws Exception {
        var decoder=new PseudoDisassembler(currentProgram);
        long pc=0x18522c;
        for(int count=0;count<160;count++){
            var ins=decoder.disassemble(toAddr(pc));
            if(ins==null) throw new IllegalStateException("Decode at "+Long.toHexString(pc));
            println(String.format("AB-129 %x %s",pc,ins));
            pc+=ins.getLength();
            if(ins.getMnemonicString().equals("RET")) {
                println("AB-129: bounded static listing reached return");
                return;
            }
            if(ins.getMnemonicString().equals("RET:D")){
                var slot=decoder.disassemble(toAddr(pc));
                println(String.format("AB-129 %x %s",pc,slot));
                println("AB-129: bounded static listing reached delayed return");
                return;
            }
        }
        throw new IllegalStateException("Listing bound reached");
    }
}
