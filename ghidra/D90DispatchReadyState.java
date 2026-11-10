// AB-114: inspect dispatcher inputs on the state produced by AB-113; no IRQ injection.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
public class D90DispatchReadyState extends D90StartupUserLoop {
 int ready=0;
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  super.afterInitialization(c,d,p);var a=p.getLanguage().getDefaultSpace();
  require(currentProgram.getMemory().getInt(toAddr(0xdff00))==0x40d44,"Vector 3f / table index 192");
  require(word(c,a,0x68000000)==0x680007c4 && word(c,a,0x68000004)==0x80000000,"Current control and ready bitmap");
  require(word(c,a,0x68000008)==0x680001ec && word(c,a,0x6800000c)==0x680001ec,"First ready-list sentinel");
  require(word(c,a,0x680001ec)==0x68000008 && word(c,a,0x680001f0)==0x68000008,"Candidate links");
  require(word(c,a,0x68000204)==0x8f991204 && word(c,a,0x8f991204L)==0x1000ac && word(c,a,0x8f991208L)==0x1f0030,"Candidate context and frame");
  require((c.readMemory(a.getAddress(0x68000130),1)[0]&255)==0 && (c.readMemory(a.getAddress(0x68000131),1)[0]&255)==0,"Dispatcher flag bytes zero");
  require(reg(c,"PC")==0x407ee && reg(c,"S")==1,"User loop boundary retained");
  ready++;println("AB-114: ready-state candidate 680001ec -> 8f991204 -> PC 1000ac differs from current 680007c4; dispatcher NOT executed");
 }
 @Override public void run()throws Exception{super.run();require(ready==8,"Case count");println("AB-114: eight live ready-state inspections passed; vector 3f identity only, no trigger/acceptance claim");}
}
