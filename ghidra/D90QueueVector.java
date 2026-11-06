// AB-92 support: inspect original TBR entries and wrapper code, no physical IRQ claim.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90QueueVector extends GhidraScript {
 public void run()throws Exception{
  var mem=currentProgram.getMemory();var d=new PseudoDisassembler(currentProgram);
  for(int n=0;n<256;n++){
   long slot=0xdfc00+0x3fc-4*n;long target=Integer.toUnsignedLong(mem.getInt(toAddr(slot)));
   if(target>=0x40400 && target<0x404ae){
    println(String.format("vector=%02x slot=%x target=%x",n,slot,target));
    for(long pc=target;pc<0x404ae;){var i=d.disassemble(toAddr(pc));if(i==null)break;println(String.format("0x%x %s",pc,i));pc+=i.getLength();}
   }
  }
 }
}
