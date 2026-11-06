// AB-93 support: bounded aligned direct-call scan; candidates may include decoded data.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90QueueSourceContext extends GhidraScript {
 public void run()throws Exception{
  var d=new PseudoDisassembler(currentProgram);
  for(long pc=0x40000;pc<0x42000;pc+=2){
   var i=d.disassemble(toAddr(pc));if(i==null || !i.getFlowType().isCall())continue;
   for(var target:i.getFlows())if(target.getOffset()==0x404c8 || target.getOffset()==0x404ba)println("Aligned candidate call "+i.getAddress()+" "+i);
  }
  for(long[] w:new long[][]{{0x40738,0x40740},{0x4046a,0x4048a},{0x16398e,0x16399a},{0x163a2a,0x163a36},{0x165c52,0x165c5e},{0x166248,0x166254}}){
   for(long pc=w[0];pc<w[1];){var i=d.disassemble(toAddr(pc));if(i==null)break;println(String.format("0x%x %s",pc,i));pc+=i.getLength();}
  }
 }
}
