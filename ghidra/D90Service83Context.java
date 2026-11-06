// Static service -85 lookup; the class name is not a recovered OS symbol.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90Service83Context extends GhidraScript {
 public void run()throws Exception{
 long slot=0x40672-83*2; long target=0x40672+currentProgram.getMemory().getShort(toAddr(slot));
 println(String.format("slot=0x%x target=0x%x",slot,target));var d=new PseudoDisassembler(currentProgram);
 for(long[] w:new long[][]{{target,0x40f0e},{0x40f84,0x40f88}})
 for(long p=w[0];p<w[1];){var i=d.disassemble(toAddr(p));if(i==null)break;println(String.format("0x%x %s",p,i));p+=i.getLength();}
 }
}
