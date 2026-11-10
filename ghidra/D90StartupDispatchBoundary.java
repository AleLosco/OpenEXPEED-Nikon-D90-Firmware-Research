// AB-107: original startup selection, stopping BEFORE dedicated USP write.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
import java.nio.ByteBuffer;
public class D90StartupDispatchBoundary extends D90OsInitializedInterrupt {
 int selected=0;
 @Override void beforeInitialization(EmulatorHelper c,ProgramDB p,int pattern)throws Exception{super.beforeInitialization(c,p,pattern);c.writeRegister("USP",0x81234560L);}
 @Override long[][] extraWindows(){long[][] b=super.extraWindows(),r=Arrays.copyOf(b,b.length+2);r[b.length]=new long[]{0x40740,0x40750};r[b.length+1]=new long[]{0x407bc,0x407f0};return r;}
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  super.afterInitialization(c,d,p);var a=p.getLanguage().getDefaultSpace();
  byte[] expected=c.readMemory(a.getAddress(0x68000000L),0xf20);
  require(word(c,a,0x5c838)==0x680007c4,"Original descriptor 38");
  require(word(c,a,0x680007dc)==0x68000ed0,"Constructed context pointer");
  long sp=reg(c,"SP"),usp=reg(c,"USP");
  ByteBuffer.wrap(expected).putInt(0,0x680007c4);
  until(c,d,0x407ce);
  require(word(c,a,0x6800cf20)==2,"Startup marker 2");
  require(reg(c,"FP")==0x680007c4L && reg(c,"R0")==0x68000ed0L,"Selected node and context");
  require(Arrays.equals(expected,c.readMemory(a.getAddress(0x68000000L),0xf20)),"Only current-node pointer changed in OS region");
  require(reg(c,"SP")==sp && reg(c,"USP")==usp && reg(c,"S")==0,"Before USP/user switch");
  selected++;println("AB-107: descriptor 38 -> node 680007c4 -> context 68000ed0; stopped BEFORE MOV R0,USP at 407ce");
 }
 @Override public void run()throws Exception{super.run();require(selected==8,"Case count");println("AB-107: eight startup-selection cases passed; user stack switch and RETI not executed");}
}
