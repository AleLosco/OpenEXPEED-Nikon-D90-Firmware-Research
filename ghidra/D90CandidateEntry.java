// AB-124: inspect the original candidate descriptor and mapped entry bytes.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;

public class D90CandidateEntry extends GhidraScript {
    @Override public void run() throws Exception {
        var memory=currentProgram.getMemory();
        int matches=0;
        for(int i=0;i<39;i++){
            long descriptor=0x5c410L+28*i;
            if(memory.getInt(toAddr(descriptor))==0x680001ec){
                int entry=memory.getInt(toAddr(descriptor+16));
                int frame=memory.getInt(toAddr(descriptor+20));
                if(entry!=0x1000ac || frame!=0x8f991204)
                    throw new IllegalStateException("Descriptor mismatch");
                println(String.format("AB-124: descriptor=%x entry=%x frame=%x",descriptor,entry,frame));
                matches++;
            }
        }
        if(matches!=1) throw new IllegalStateException("Descriptor count");
        var block=memory.getBlock(toAddr(0x1000ac));
        if(block==null || !block.isInitialized()) throw new IllegalStateException("Entry not initialized");
        println("AB-124: block="+block.getName()+" start="+block.getStart()+" end="+block.getEnd());
        byte[] bytes=new byte[32];
        memory.getBytes(toAddr(0x1000ac),bytes);
        println("AB-124: entry32="+java.util.HexFormat.of().formatHex(bytes));
        var disassembler=new PseudoDisassembler(currentProgram);
        long pc=0x1000ac;
        for(int i=0;i<12;i++){
            var instruction=disassembler.disassemble(toAddr(pc));
            if(instruction==null) throw new IllegalStateException("Decode at "+Long.toHexString(pc));
            println(String.format("AB-124: %x %s",pc,instruction));
            pc+=instruction.getLength();
        }
        println("AB-124: descriptor and entry mapping inspected; static decoding only");
    }
}
