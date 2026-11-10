// AB-107 support: branch target after initialization; static evidence only.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90StartupDispatchContext extends GhidraScript {
 public void run()throws Exception{
  var d=new PseudoDisassembler(currentProgram);
  for(long[] w:new long[][]{{0x407bc,0x407f0},{0x40500,0x405c8}})
   for(long pc=w[0];pc<w[1];){var i=d.disassemble(toAddr(pc));if(i==null){println("Undecoded at "+Long.toHexString(pc));break;}println(String.format("0x%x %s",pc,i));pc+=i.getLength();}
 }
}
