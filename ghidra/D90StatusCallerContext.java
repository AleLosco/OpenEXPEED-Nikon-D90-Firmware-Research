// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90StatusCallerContext extends GhidraScript {
 public void run() throws Exception {
 var d=new PseudoDisassembler(currentProgram);
 for(long[] r:new long[][]{{0x19d450,0x19d574},{0x19db40,0x19dc00}}){
 for(long p=r[0];p<r[1];){var i=d.disassemble(toAddr(p)); if(i==null){println("undecoded "+Long.toHexString(p));p+=2;}else{println(String.format("0x%x %s",p,i));p+=i.getLength();}}
 }
 }
}
