// AB-83: probe stock INT support and consume a synthetic PC/PS frame with RETI.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import java.nio.ByteBuffer;
public class D90InterruptSupport extends GhidraScript {
 void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 void init(EmulatorHelper c){
  for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
  for(String r:new String[]{"AC","FP","RP","MDL","MDH","S","I","N","Z","V","C","T","D0","D1","ILM","PS"})c.writeRegister(r,0);
 }
 public void run()throws Exception{
  var d=new PseudoDisassembler(currentProgram);
  check(d.disassemble(toAddr(0x19e064)).toString().contains("INT #0x40"),"INT instruction");
  check(d.disassemble(toAddr(0x406f4)).toString().equals("RETI"),"RETI instruction");
  var probe=new EmulatorHelper(currentProgram);
  try{
   init(probe);probe.writeRegister("SP",0x81004000L);probe.writeRegister("TBR",0xdfc00);probe.writeRegister("PC",0x19e064);
   boolean ok=probe.step(monitor);String err=probe.getLastError();
   println("Stock INT step success="+ok+" diagnostic="+err);
   check(!ok && err!=null && err.contains("interrupt_prelude"),"Unexpected INT capability; re-evaluate probe");
  }finally{probe.dispose();}
  for(int ps:new int[]{0,0x000f071f})for(int pc:new int[]{0x19e066,0x80000100}){
   var c=new EmulatorHelper(currentProgram);
   try{
    init(c);long sp=0x81004000L;
    byte[] frame=ByteBuffer.allocate(8).putInt(pc).putInt(ps).array();
    c.writeMemory(toAddr(sp),frame);c.writeRegister("SP",sp);c.writeRegister("PC",0x406f4);
    check(c.step(monitor),"RETI step: "+c.getLastError());
    check(c.getExecutionAddress().getOffset()==Integer.toUnsignedLong(pc),"PC restore");
    check(c.readRegister("SP").longValue()==sp+8 && c.readRegister("PS").intValue()==ps,"SP/PS restore");
    String[] flags={"C","V","Z","N","I","S","T","D0","D1"};
    int[] bits={0,1,2,3,4,5,8,9,10};
    for(int i=0;i<bits.length;i++)check(c.readRegister(flags[i]).intValue()==((ps>>>bits[i])&1),"Flag "+flags[i]);
    check(c.readRegister("ILM").intValue()==((ps>>>16)&15),"ILM restore");
    check(java.util.Arrays.equals(frame,c.readMemory(toAddr(sp),8)),"Frame modified");
    println(String.format("RETI pc=%08x ps=%08x: PC/PS/SP and decoded fields verified",pc,ps));
   }finally{c.dispose();}
  }
  println("Stock INT limitation confirmed; four synthetic RETI cases passed");
 }
}
