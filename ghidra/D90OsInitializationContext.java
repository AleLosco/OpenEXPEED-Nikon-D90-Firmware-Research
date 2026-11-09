// AB-101 support: original OS setup windows and table words; static only.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90OsInitializationContext extends GhidraScript {
 public void run()throws Exception{
  var d=new PseudoDisassembler(currentProgram);
  for(long pc=0x408be;pc<0x40a56;){var i=d.disassemble(toAddr(pc));if(i==null)throw new IllegalStateException("Decode "+Long.toHexString(pc));println(String.format("0x%x %s",pc,i));pc+=i.getLength();}
  for(long pc=0x41534;pc<0x41630;){var i=d.disassemble(toAddr(pc));if(i==null)break;println(String.format("helper 0x%x %s",pc,i));pc+=i.getLength();}
  for(long base:new long[]{0x5c40c,0x5c854,0x5cb68,0x5cccc,0x5ccd0,0x5ccd4,0x5ccd8,0x5ccdc})println(String.format("header 0x%x count=%d stride=%d first=%08x",base,Short.toUnsignedInt(currentProgram.getMemory().getShort(toAddr(base))),Short.toUnsignedInt(currentProgram.getMemory().getShort(toAddr(base+2))),currentProgram.getMemory().getInt(toAddr(base+4))));
  for(long p=0x5c400;p<0x5c480;p+=4)println(String.format("table 0x%x = %08x",p,currentProgram.getMemory().getInt(toAddr(p))));
 }
}
