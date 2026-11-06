import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90SourceTailContext extends GhidraScript {
 public void run()throws Exception{var d=new PseudoDisassembler(currentProgram);
 for(long[] r:new long[][]{{0x100d24,0x100d3e},{0x100d3e,0x100df2},{0x100f00,0x100fc4}}){
  println(String.format("Range 0x%x..0x%x",r[0],r[1]));
  for(long p=r[0];p<r[1];){var i=d.disassemble(toAddr(p));if(i==null){println("undecoded "+Long.toHexString(p));p+=2;}else{println(String.format("0x%x %s",p,i));p+=i.getLength();}}
 }
 }
}
