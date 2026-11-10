// AB-110: restore the constructed initial context, stopping before RETI.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
import java.nio.ByteBuffer;
public class D90StartupContextRestore extends D90StartupDispatchBoundary {
 int restored=0;
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  super.afterInitialization(c,d,p);var a=p.getLanguage().getDefaultSpace();
  require(reg(c,"PC")==0x407ce && reg(c,"R0")==0x68000ed0L,"Selected context boundary");
  byte[] os=c.readMemory(a.getAddress(0x68000000L),0xf20);
  byte[] frame=c.readMemory(a.getAddress(0x68000ed0L),80);
  byte[] external=c.readMemory(a.getAddress(0x8f991204L),40);
  long supervisor=reg(c,"SP");
  require(supervisor==0x81004000L,"Supervisor stack base");
  byte[] guardLow=c.readMemory(a.getAddress(supervisor-12),4),guardHigh=c.readMemory(a.getAddress(supervisor),4);
  require(d.disassemble(c.getExecutionAddress()).toString().equals("MOV R0,USP"),"Dedicated instruction boundary");
  // This original MOV explicitly accesses USP; keep it outside the ordinary-step guard.
  require(c.step(monitor),"MOV USP");require(reg(c,"PC")==0x407d0 && reg(c,"USP")==0x68000ed0L,"Original USP load");
  D90StackSwitchAdapter.switchStack(c,a,monitor,false);
  until(c,d,0x407d6);
  require(c.readRegister("R0").intValue()==ByteBuffer.wrap(frame).getInt(0) && c.readRegister("R1").intValue()==ByteBuffer.wrap(frame).getInt(4),"PC/PS read from constructed context");
  D90StackSwitchAdapter.switchStack(c,a,monitor,false);
  until(c,d,0x407dc);
  require(Arrays.equals(Arrays.copyOf(frame,8),c.readMemory(a.getAddress(supervisor-8),8)),"Original pushes transfer PC/PS");
  D90StackSwitchAdapter.switchStack(c,a,monitor,false);
  until(c,d,0x407e8);
  String[] names={"RP","MDL","MDH","R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP"};
  for(int i=0;i<names.length;i++)require(c.readRegister(names[i]).intValue()==ByteBuffer.wrap(frame).getInt(8+4*i),"Restored "+names[i]);
  require(reg(c,"SP")==0x68000f20L,"Consumed user context");
  D90StackSwitchAdapter.switchStack(c,a,monitor,false);
  require(reg(c,"PC")==0x407ea && reg(c,"S")==0 && reg(c,"SP")==supervisor-8 && reg(c,"SSP")==supervisor-8 && reg(c,"USP")==0x68000f20L,"Before RETI boundary");
  require(Arrays.equals(os,c.readMemory(a.getAddress(0x68000000L),0xf20)) && Arrays.equals(external,c.readMemory(a.getAddress(0x8f991204L),40)),"OS and context memory unchanged");
  require(Arrays.equals(guardLow,c.readMemory(a.getAddress(supervisor-12),4)) && Arrays.equals(guardHigh,c.readMemory(a.getAddress(supervisor),4)),"Frame guards retained");
  require(Arrays.equals(Arrays.copyOf(frame,8),c.readMemory(a.getAddress(supervisor-8),8)),"Final PC/PS frame retained");
  restored++;println(String.format("AB-110: restored 18 registers; frame PC=%08x PS=%08x; USP=68000f20 SSP=81003ff8; stopped BEFORE RETI",ByteBuffer.wrap(frame).getInt(0),ByteBuffer.wrap(frame).getInt(4)));
 }
 @Override public void run()throws Exception{super.run();require(restored==8,"Case count");println("AB-110: eight context restores passed; four explicit stack switches per case; user RETI not executed");}
}
