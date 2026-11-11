// AB-132: bounded static listing of the third-call helper and descriptor table.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;

public class D90ThirdCallHelper extends GhidraScript {
    @Override public void run() throws Exception {
        var decoder=new PseudoDisassembler(currentProgram);
        var memory=currentProgram.getMemory();
        for(int index=0;index<26;index++){
            long row=0x1caabcL+8*index;
            println(String.format("row %02d %08x %d %04x",index,memory.getInt(toAddr(row)),
                Short.toUnsignedInt(memory.getShort(toAddr(row+4))),
                Short.toUnsignedInt(memory.getShort(toAddr(row+6)))));
        }
        long pc=0x13a3ac;
        for(int count=0;count<320;count++){
            var ins=decoder.disassemble(toAddr(pc));
            if(ins==null) throw new IllegalStateException("Decode at "+Long.toHexString(pc));
            println(String.format("AB-132 %x %s",pc,ins));
            pc+=ins.getLength();
            if(ins.getMnemonicString().equals("RET")) {
                println("AB-132: bounded static listing reached return");
                return;
            }
            if(ins.getMnemonicString().equals("RET:D")){
                var slot=decoder.disassemble(toAddr(pc));
                println(String.format("AB-132 %x %s",pc,slot));
                println("AB-132: bounded static listing reached delayed return");
                return;
            }
        }
        throw new IllegalStateException("Listing bound reached");
    }
}
