// Static dependency windows for AB-75.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90SetupDependencies extends GhidraScript {
 public void run() throws Exception {
 var d=new PseudoDisassembler(currentProgram);
 for(long[] r:new long[][]{{0x404ae,0x404e0},{0x19e0fa,0x19e162},{0x1a0322,0x1a034e},{0x1a0156,0x1a017c},{0x1a0382,0x1a03d8}}){
 for(long p=r[0];p<r[1];){var i=d.disassemble(toAddr(p)); if(i==null){println("undecoded "+Long.toHexString(p));p+=2;}else{println(String.format("0x%x %s",p,i));p+=i.getLength();}}
 }
 }
}
