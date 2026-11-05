// Static windows for AB-74; not a complete execution trace.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90StatusSetupContext extends GhidraScript {
 public void run() throws Exception {
 var d=new PseudoDisassembler(currentProgram);
 for(long[] r:new long[][]{{0x19df9c,0x19e0c0},{0x19dc28,0x19dc3c},{0x1a03d8,0x1a0468}}){
 for(long p=r[0];p<r[1];){var i=d.disassemble(toAddr(p)); if(i==null){println("undecoded "+Long.toHexString(p));p+=2;}else{println(String.format("0x%x %s",p,i));p+=i.getLength();}}
 }
 }
}
