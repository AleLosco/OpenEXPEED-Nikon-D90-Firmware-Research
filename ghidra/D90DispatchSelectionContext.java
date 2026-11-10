// AB-114 support: dispatcher windows and vector identity; static, no program edits.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90DispatchSelectionContext extends GhidraScript {
 public void run()throws Exception{
  var m=currentProgram.getMemory();var d=new PseudoDisassembler(currentProgram);
  for(int n=0;n<256;n++){long slot=0xdfc00+0x3fc-4*n;if(m.getInt(toAddr(slot))==0x40d44)println(String.format("vector=%02x slot=%x target=40d44",n,slot));}
  for(long pc=0x40d44;pc<0x40ef0;){
   if(pc==0x40e32){if(Short.toUnsignedInt(m.getShort(toAddr(pc)))!=0x97dd)throw new IllegalStateException("SRCH1 word changed");println("0x40e32 raw=97dd; unsupported FR60 instruction, no emulation");pc+=2;continue;}
   var i=d.disassemble(toAddr(pc));if(i==null){println("Undecoded at "+Long.toHexString(pc));break;}
   println(String.format("0x%x %s",pc,i));pc+=i.getLength();
  }
 }
}
