// Bounded literal candidates; not exhaustive computed-address or reachability analysis.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
import java.nio.ByteBuffer;
public class D90ListInitializationCandidates extends GhidraScript {
 public void run()throws Exception{
  var mem=currentProgram.getMemory();var d=new PseudoDisassembler(currentProgram);
  for(int value:new int[]{0x68000000,0x68000010,0x68000110,0x68000118,0x68000120,0x68000128}){
   byte[] pattern=ByteBuffer.allocate(4).putInt(value).array();int hits=0;
   for(var block:mem.getBlocks())if(block.isInitialized()){
    var start=block.getStart();while(start.compareTo(block.getEnd())<=0){
     monitor.checkCancelled();var found=mem.findBytes(start,block.getEnd(),pattern,null,true,monitor);if(found==null)break;
     println(String.format("literal %08x at %s",value,found));hits++;
     if(found.getOffset()>=block.getStart().getOffset()+2){
      var site=found.subtract(2);int opcode=mem.getShort(site)&0xffff;
      if((site.getOffset()&1)==0 && (opcode&0xfff0)==0x9f80){
       for(int n=0;n<22;n++){
        var i=d.disassemble(site);if(i==null){println("undecoded "+site);break;}
        println(site+" "+i);site=site.add(i.getLength());
        if(!block.contains(site))break;
       }
      }
     }
     if(found.equals(block.getEnd()))break;start=found.add(1);
    }
   }
   println(String.format("literal %08x hits=%d; aligned LDI:32 candidates only",value,hits));
  }
 }
}
