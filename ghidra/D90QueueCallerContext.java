import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90QueueCallerContext extends GhidraScript {
 public void run()throws Exception{var d=new PseudoDisassembler(currentProgram);
 for(var ref: getReferencesTo(toAddr(0x40a56)))println("Existing reference to 0x40a56: "+ref);
 for(var block:currentProgram.getMemory().getBlocks())if(block.isInitialized()){
  var start=block.getStart();while(start.compareTo(block.getEnd())<=0){
   var hit=currentProgram.getMemory().findBytes(start,block.getEnd(),new byte[]{0,4,0x0a,0x56},null,true,monitor);if(hit==null)break;
   println("Literal 0x40a56 at "+hit);start=hit.add(1);
  }
 }
 for(long[] r:new long[][]{{0x40498,0x404ae},{0x40a30,0x40a72},{0x40b72,0x40bec}}){
  println(String.format("Range 0x%x..0x%x",r[0],r[1]));
  for(long p=r[0];p<r[1];){var i=d.disassemble(toAddr(p));if(i==null){println("undecoded "+Long.toHexString(p));p+=2;}else{println(String.format("0x%x %s",p,i));p+=i.getLength();}}
 }
 }
}
