// Static OS epilogue windows; excludes adjacent tables and data.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90InterruptFrameContext extends GhidraScript {
 public void run()throws Exception{var d=new PseudoDisassembler(currentProgram);
 for(long[] r:new long[][]{{0x404f4,0x404f6},{0x4067a,0x406b6}}){
  println(String.format("Range 0x%x..0x%x",r[0],r[1]));
  for(long p=r[0];p<r[1];){var i=d.disassemble(toAddr(p));if(i==null){println("undecoded "+Long.toHexString(p));p+=2;}else{println(String.format("0x%x %s",p,i));p+=i.getLength();}}
 }
 }
}
