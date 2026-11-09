// AB-98: same synthetic memory from initialization to forced vector-18 prefix.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class D90InitializationVectorBoundary extends D90SourceInitializationReturn {
 int boundaries=0;
 @Override long[][] extraWindows(){return new long[][]{{0x4048a,0x404ae},{0xdff9c,0xdffa0}};}
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  var a=p.getLanguage().getDefaultSpace();
  require(word(c,a,0xdff9c)==0x4048a,"Original vector 18");
  int config=Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(a.getAddress(0x48),2)).getShort());
  int control=Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(a.getAddress(0x4e),2)).getShort());
  println(String.format("Initialization boundary: 48=%04x 4e=%04x",config,control));
  require(config==0x9e34 && control==0x1b,"Final initialization configuration");
  byte[] guards=c.readMemory(a.getAddress(0x4c),6);
  long sp=reg(c,"SP"),pc=reg(c,"PC"),ps=reg(c,"PS");
  require(sp==0x81004000L && pc==0x40740 && reg(c,"S")==0,"Supervisor boundary");
  // Explicit synthetic IRQ frame and stack synchronization; no acceptance model.
  put(c,a,sp-4,(int)ps);put(c,a,sp-8,(int)pc);
  c.writeRegister("SP",sp-8);c.writeRegister("SSP",sp-8);
  c.writeRegister("I",0);c.writeRegister("S",0);c.writeRegister("PS",ps&~0x30L);
  c.writeRegister("PC",Integer.toUnsignedLong(word(c,a,0xdff9c)));
  until(c,d,0x4049e); // Stop BEFORE calling the OS queue routine.
  require(reg(c,"R12")==0x40a56 && reg(c,"SP")==sp-80,"Wrapper call boundary");
  require(word(c,a,sp-8)==(int)pc && word(c,a,sp-4)==(int)ps,"Synthetic frame retained");
  byte[] expected=guards.clone();expected[2]=0;expected[3]=0x1a;
  require(Arrays.equals(expected,c.readMemory(a.getAddress(0x4c),6)),"Halfword and adjacent guards");
  require(Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(a.getAddress(0x48),2)).getShort())==config,"Configuration retained");
  require(word(c,a,0x68000110)==0 && word(c,a,0x68000114)==0,"Queue not yet executed");
  boundaries++;
  println("AB-98 boundary passed: 4e 001b -> 001a; stopped before CALL 0x4049e; IRQ entry forced");
 }
 @Override public void run()throws Exception{
  super.run();require(boundaries==8,"Case count");
  println("AB-98: eight initialization/vector-prefix cases passed; no IRQ acceptance or queue execution");
 }
}
