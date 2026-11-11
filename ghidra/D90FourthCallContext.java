// AB-143: bounded static listing of the candidate's fourth call.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;

public class D90FourthCallContext extends GhidraScript {
    @Override public void run() throws Exception {
        var decoder=new PseudoDisassembler(currentProgram);
        for(long site=0x1000e2;site<0x1000ea;){
            var call=decoder.disassemble(toAddr(site));
            println(String.format("AB-143 candidate %x %s",site,call));
            site+=call.getLength();
        }
        long pc=0x13b7f6;
        for(int count=0;count<320;count++){
            var ins=decoder.disassemble(toAddr(pc));
            if(ins==null) throw new IllegalStateException("Decode at "+Long.toHexString(pc));
            println(String.format("AB-143 %x %s",pc,ins));
            pc+=ins.getLength();
            if(ins.getMnemonicString().equals("RET")) {
                println("AB-143: bounded static listing reached return");
                return;
            }
            if(ins.getMnemonicString().equals("RET:D")){
                var slot=decoder.disassemble(toAddr(pc));
                println(String.format("AB-143 %x %s",pc,slot));
                println("AB-143: bounded static listing reached delayed return");
                return;
            }
        }
        throw new IllegalStateException("Listing bound reached");
    }
}
