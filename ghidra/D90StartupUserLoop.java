// AB-113: return to the constructed user context and execute a bounded self-loop.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
public class D90StartupUserLoop extends D90StartupContextRestore {
 int entered=0,loopSteps=0;
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  super.afterInitialization(c,d,p);var a=p.getLanguage().getDefaultSpace();
  require(reg(c,"PC")==0x407ea && word(c,a,0x81003ff8L)==0x407ee && word(c,a,0x81003ffcL)==0x1f0030,"Firmware-produced user frame");
  String[] general={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP","RP","MDL","MDH"};
  long[] values=new long[general.length];for(int i=0;i<general.length;i++)values[i]=reg(c,general[i]);
  long[] bases={0x68000000L,0x8f991204L,0x81003f80L,0,0x6800cf20L};int[] lengths={0x1000,40,0x100,0x1000,4};
  byte[][] memory=new byte[bases.length][];for(int i=0;i<bases.length;i++)memory[i]=c.readMemory(a.getAddress(bases[i]),lengths[i]);
  D90UserRetiAdapter.returnUser(c,a,monitor,false);
  require(reg(c,"PC")==0x407ee && reg(c,"PS")==0x1f0030 && reg(c,"S")==1 && reg(c,"I")==1 && reg(c,"ILM")==31,"Restored user state");
  require(reg(c,"SP")==0x68000f20L && reg(c,"USP")==0x68000f20L && reg(c,"SSP")==0x81004000L,"User and supervisor stacks");
  for(int i=0;i<general.length;i++)require(values[i]==reg(c,general[i]),"RETI preserved "+general[i]);
  String[] control={"PC","PS","S","I","ILM","SP","SSP","USP","C","V","Z","N","T","D0","D1"};
  long[] state=new long[control.length];for(int i=0;i<control.length;i++)state[i]=reg(c,control[i]);
  for(int n=0;n<16;n++){
   var instruction=d.disassemble(c.getExecutionAddress());require(instruction!=null && instruction.toString().equals("BRA 0x000407ee"),"Original self-loop");
   for(var op:instruction.getPcode()){inspect(op.getOutput());for(var v:op.getInputs())inspect(v);}
   require(c.step(monitor),"Loop step: "+c.getLastError());inspected++;loopSteps++;
   for(int i=0;i<control.length;i++)require(state[i]==reg(c,control[i]),"Loop preserved "+control[i]);
   for(int i=0;i<general.length;i++)require(values[i]==reg(c,general[i]),"Loop preserved "+general[i]);
  }
  for(int i=0;i<bases.length;i++)require(Arrays.equals(memory[i],c.readMemory(a.getAddress(bases[i]),lengths[i])),"Compared memory retained");
  entered++;println("AB-113: firmware-produced frame reached 0x407ee with S=1 I=1 ILM=31; 16 self-loop steps retained checked state");
 }
 @Override public void run()throws Exception{super.run();require(entered==8 && loopSteps==128,"Scenario/loop count");println("AB-113: eight user-context entries and 128 self-loop steps passed; explicit adapters, no hardware interrupts or task scheduling");}
}
