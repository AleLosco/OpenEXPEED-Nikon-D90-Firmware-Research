// AB-117: bounded selection on initialized state; synthetic PC entry, not an IRQ.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
import java.nio.ByteBuffer;
public class D90DispatchCandidate extends D90DispatchReadyState {
 int candidates=0;
 @Override long[][] extraWindows(){long[][] b=super.extraWindows(),r=Arrays.copyOf(b,b.length+2);r[b.length]=new long[]{0x40e2a,0x40e64};r[b.length+1]=new long[]{0x404fa,0x404fc};return r;}
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  super.afterInitialization(c,d,p);var a=p.getLanguage().getDefaultSpace();
  long sp=reg(c,"SP"),ssp=reg(c,"SSP"),usp=reg(c,"USP"),rp=reg(c,"RP");
  require(sp==0x68000f20L && reg(c,"S")==1,"User-loop starting state");
  byte[] expected=c.readMemory(a.getAddress(0x68000000L),0x1000);
  ByteBuffer.wrap(expected).putInt(0xf1c,(int)rp); // Temporary original ST RP,@-SP.
  byte[] external=c.readMemory(a.getAddress(0x8f991204L),40),system=c.readMemory(a.getAddress(0x81003f80L),0x100);
  require(d.disassemble(a.getAddress(0x404fa)).toString().equals("RET"),"Original hook is RET");
  // Explicit harness entry bypasses IRQ entry and the dispatcher prefix.
  c.writeRegister("PC",0x40e2a);
  until(c,d,0x40e32);require(reg(c,"AC")==0x80000000L && reg(c,"R12")==0x68000004L,"Original bitmap load");
  D90Srch1Adapter.searchOne(c,a,false);require(reg(c,"AC")==0 && reg(c,"PC")==0x40e34,"Explicit bit-search result");
  until(c,d,0x40e62);
  require(reg(c,"AC")==0x680001ecL && reg(c,"FP")==0x680007c4L && reg(c,"R12")==0x68000000L && reg(c,"Z")==0,"Different candidate selected");
  require(reg(c,"SP")==sp && reg(c,"SSP")==ssp && reg(c,"USP")==usp && reg(c,"RP")==rp && reg(c,"S")==1,"Stack and RP retained");
  require(Arrays.equals(expected,c.readMemory(a.getAddress(0x68000000L),0x1000)),"OS state plus original scratch push");
  require(Arrays.equals(external,c.readMemory(a.getAddress(0x8f991204L),40)) && Arrays.equals(system,c.readMemory(a.getAddress(0x81003f80L),0x100)),"Other contexts and system stack retained");
  candidates++;println("AB-117: bitmap 80000000 -> adapted index 0 -> candidate 680001ec differs from 680007c4; stopped BEFORE save at 40e62");
 }
 @Override public void run()throws Exception{super.run();require(candidates==8,"Case count");println("AB-117: eight bounded candidate selections passed; synthetic PC entry and explicit SRCH1, no IRQ entry or context switch");}
}
