// AB-93: isolated writes and conditional halfword update, peripherals modeled as RAM.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90QueueSourceControl extends D90QueueAdvance {
 void runToReturn(EmulatorHelper c)throws Exception{
  int n=0;while(c.getExecutionAddress().getOffset()!=0x80000000L && n++<50)if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
  check(c.getExecutionAddress().getOffset()==0x80000000L,"Return boundary");
 }
 public void run()throws Exception{
  for(int initial:new int[]{0,2,0xaaaa,0xffff}){
   var c=new EmulatorHelper(currentProgram);
   try{
    c.writeRegister("RP",0x80000000L);c.writeRegister("PC",0x404ba);c.writeRegister("AC",0x55);
    c.writeMemory(toAddr(0x4e),ByteBuffer.allocate(2).putShort((short)initial).array());runToReturn(c);
    int actual=Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(toAddr(0x4e),2)).getShort());
    check(actual==((initial&2)==0?0x1b:initial),"Conditional 0x4e update");
    println(String.format("0x404ba initial=%04x final=%04x",initial,actual));
   }finally{c.dispose();}
  }
  var c=new EmulatorHelper(currentProgram);
  try{
   c.writeRegister("RP",0x80000000L);c.writeRegister("PC",0x404c8);
   c.writeMemory(toAddr(0x4e),new byte[]{0x55,0x55});put(c,0x40070050,0x55556666);c.writeMemory(toAddr(0x482),new byte[]{0x55});
   runToReturn(c);
   check(ByteBuffer.wrap(c.readMemory(toAddr(0x4e),2)).getShort()==0,"Clear halfword");
   check(word(c,0x40070050)==0x40006666,"Store width");
   check(c.readMemory(toAddr(0x482),1)[0]==0x23 && c.readRegister("R0").intValue()==0x23,"Byte readback");
   println("0x404c8: halfword 0x4e=0, halfword 0x40070050=0x4000, byte 0x482=0x23 verified");
  }finally{c.dispose();}
  c=new EmulatorHelper(currentProgram);
  try{
   c.writeRegister("PC",0x40738);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0x12345678);
   for(long addr:new long[]{0x48,0x4e})c.writeMemory(toAddr(addr),new byte[]{0x55,0x55});c.writeMemory(toAddr(0x448),new byte[]{0x55});
   int steps=0;while(c.getExecutionAddress().getOffset()!=0x40484 && steps++<30)if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
   check(c.getExecutionAddress().getOffset()==0x40484,"Initialization stop before helper");
   check(Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(toAddr(0x48),2)).getShort())==0x9e34 && ByteBuffer.wrap(c.readMemory(toAddr(0x4e),2)).getShort()==0x1b && c.readMemory(toAddr(0x448),1)[0]==0x12,"Startup writes");
   check(c.readRegister("AC").intValue()==0x100936 && c.readRegister("SP").longValue()==0x81003ffcL && word(c,0x81003ffcL)==0x40740,"Startup call frame");
   println("Startup 0x40738 -> 0x4046a writes verified; stopped before call to 0x100936");
  }finally{c.dispose();}
  println("Six source-control/prefix cases passed; peripheral identity and frequency unverified");
 }
}
