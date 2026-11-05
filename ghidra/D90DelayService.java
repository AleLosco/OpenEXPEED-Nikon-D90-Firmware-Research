// Static service -85 lookup; the class name is not a recovered OS symbol.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90DelayService extends GhidraScript {
 public void run()throws Exception{
 long slot=0x40672-85*2; long target=0x40672+currentProgram.getMemory().getShort(toAddr(slot));
 println(String.format("slot=0x%x target=0x%x",slot,target));var d=new PseudoDisassembler(currentProgram);
 for(long p=target;p<target+160;){var i=d.disassemble(toAddr(p));if(i==null)break;println(String.format("0x%x %s",p,i));p+=i.getLength();}
 }
}
