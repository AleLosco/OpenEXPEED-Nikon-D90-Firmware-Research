// AB-106: forced vector 18 on OS state built by original initialization.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
public class D90OsInitializedInterrupt extends D90OsPeripheralInitialization {
 int interrupts=0;
 @Override long[][] extraWindows(){
  long[][] base=super.extraWindows(),all=Arrays.copyOf(base,base.length+4);
  all[base.length]=new long[]{0x4048a,0x404ae};
  all[base.length+1]=new long[]{0x404fe,0x40500};
  all[base.length+2]=new long[]{0x40a56,0x40bec};
  all[base.length+3]=new long[]{0xdff9c,0xdffa0};return all;
 }
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  super.afterInitialization(c,d,p);
  var a=p.getLanguage().getDefaultSpace();
  require(word(c,a,0xdff9c)==0x4048a,"Original vector 18");
  long sp=reg(c,"SP"),pc=reg(c,"PC"),ps=reg(c,"PS");
  require(sp==0x81004000L && pc==0x40740 && reg(c,"S")==0,"Supervisor boundary");
  String[] names={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP","RP","MDL","MDH"};
  long[] values=new long[names.length];for(int i=0;i<names.length;i++)values[i]=reg(c,names[i]);
  byte[] expected=c.readMemory(a.getAddress(0x68000000L),0xf20);
  require(word(c,a,0x68000110)==0 && word(c,a,0x68000114)==0,"Initial counter");
  expected[0x117]=1; // Big-endian low counter word: 0 -> 1.
  byte[] context=c.readMemory(a.getAddress(0x8f991204L),40);
  // Explicit external IRQ entry; no hardware acceptance or timing model.
  put(c,a,sp-4,(int)ps);put(c,a,sp-8,(int)pc);
  c.writeRegister("SP",sp-8);c.writeRegister("SSP",sp-8);
  c.writeRegister("I",0);c.writeRegister("S",0);c.writeRegister("PS",ps&~0x30L);c.writeRegister("PC",0x4048a);
  until(c,d,0x404aa);returnSupervisor(c,a);
  require(Arrays.equals(expected,c.readMemory(a.getAddress(0x68000000L),0xf20)),"OS state: counter only");
  require(Arrays.equals(context,c.readMemory(a.getAddress(0x8f991204L),40)),"External context retained");
  for(int i=0;i<names.length;i++)require(reg(c,names[i])==values[i],"Restored "+names[i]);
  require(reg(c,"PC")==pc && reg(c,"SP")==sp && reg(c,"SSP")==sp && reg(c,"PS")==ps,"IRQ return state");
  require(Arrays.equals(c.readMemory(a.getAddress(0x4e),2),new byte[]{0,0x1a}),"Wrapper control halfword");
  interrupts++;println("AB-106: initialized OS lists retained; counter 0 -> 1; forced vector 18 returned to 0x40740");
 }
 @Override public void run()throws Exception{super.run();require(interrupts==8,"IRQ case count");println("AB-106: eight initialized-state IRQ scenarios passed; no scheduler or hardware acceptance model");}
}
