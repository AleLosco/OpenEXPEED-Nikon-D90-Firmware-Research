// AB-156: inspect the candidate continuation and its seventh callee in the B database.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;

public class D90CandidateSchedulingSites extends GhidraScript {
    private void list(PseudoDisassembler decoder,long start,int limit) throws Exception {
        long pc=start;
        for(int n=0;n<limit;n++){
            var ins=decoder.disassemble(toAddr(pc));
            if(ins==null) throw new IllegalStateException("Decode at "+Long.toHexString(pc));
            println(String.format("AB-156 %x %x %s",start,pc,ins));
            pc+=ins.getLength();
            if(ins.getMnemonicString().startsWith("RET") || ins.getMnemonicString().startsWith("BRA")){
                if(ins.getDelaySlotDepth()>0) println(String.format("AB-156 slot %x %s",pc,decoder.disassemble(toAddr(pc))));
                return;
            }
        }
        println("AB-156 listing bound "+Long.toHexString(pc));
    }
    @Override public void run() throws Exception {
        var decoder=new PseudoDisassembler(currentProgram);
        list(decoder,0x14ca9a,160);
        list(decoder,0x100102,320);
        list(decoder,0x416b4,128);
        list(decoder,0x416cc,128);
        list(decoder,0x416d8,128);
        for (int selector : new int[]{-48,-27,-47}) {
            long slot = 0x40672L + 2*selector;
            int offset = currentProgram.getMemory().getShort(toAddr(slot));
            long target = 0x40672L + offset;
            println(String.format("AB-156 service %d table %x offset %x target %x",selector,slot,offset,target));
            list(decoder,target,96);
        }
    }
}
