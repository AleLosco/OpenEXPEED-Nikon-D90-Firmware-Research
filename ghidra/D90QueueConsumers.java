// AB-90 support: bounded literal search, not a complete reference analysis.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
import java.nio.ByteBuffer;
public class D90QueueConsumers extends GhidraScript {
 public void run()throws Exception{
  var mem=currentProgram.getMemory();var d=new PseudoDisassembler(currentProgram);
  for(int value:new int[]{0x68000118,0x68000114}){
   byte[] pattern=ByteBuffer.allocate(4).putInt(value).array();int hits=0;
   for(var block:mem.getBlocks())if(block.isInitialized()){
    var start=block.getStart();while(start.compareTo(block.getEnd())<=0){
     var found=mem.findBytes(start,block.getEnd(),pattern,null,true,monitor);if(found==null)break;
     var site=found.subtract(2);var i=d.disassemble(site);
     println(String.format("literal %08x at %s candidate %s: %s",value,found,site,i));hits++;
     start=found.add(1);
    }
   }
   println(String.format("literal %08x total hits=%d (initialized blocks only)",value,hits));
  }
 }
}
