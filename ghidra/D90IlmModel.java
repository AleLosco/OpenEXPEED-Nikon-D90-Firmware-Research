// AB-84: characterize the installed FR model's ILM width; no CPU-model patch.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import java.nio.ByteBuffer;
public class D90IlmModel extends GhidraScript {
 void check(boolean ok,String msg){if(!ok)throw new IllegalStateException(msg);}
 public void run()throws Exception{
  var d=new PseudoDisassembler(currentProgram);
  check(d.disassemble(toAddr(0x40692)).toString().contains("STILM #0x1e"),"STILM site");
  var c=new EmulatorHelper(currentProgram);
  try{
   c.writeRegister("PC",0x40692);c.writeRegister("ILM",0);
   check(c.step(monitor),"STILM step");
   check(c.readRegister("ILM").intValue()==14,"Installed STILM model changed");
   println("Original STILM 0x1e at 0x40692: installed model ILM=0x0e");
  }finally{c.dispose();}
  for(int ilm:new int[]{15,30,31}){
   c=new EmulatorHelper(currentProgram);
   try{
    for(String f:new String[]{"S","I","N","Z","V","C","T","D0","D1","ILM","PS"})c.writeRegister(f,0);
    long sp=0x81004000L;int ps=ilm<<16;
    c.writeRegister("SP",sp);c.writeRegister("PC",0x406f4);
    c.writeMemory(toAddr(sp),ByteBuffer.allocate(8).putInt(0x19e066).putInt(ps).array());
    check(c.step(monitor),"RETI step");
    check(c.readRegister("PS").intValue()==ps && c.readRegister("ILM").intValue()==(ilm&15),"Installed RETI model changed");
    check(c.readRegister("SP").longValue()==sp+8 && c.getExecutionAddress().getOffset()==0x19e066,"RETI boundary");
    println(String.format("RETI PS=%08x: PS preserved, installed model ILM=%02x",ps,ilm&15));
   }finally{c.dispose();}
  }
  println("Four ILM model characterization cases passed; 5-bit fidelity NOT established");
 }
}
