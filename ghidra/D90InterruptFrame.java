// AB-82: firmware-generated register frame, synthetic hardware frame; no INT or RETI.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class D90InterruptFrame extends GhidraScript {
 void check(boolean ok,String msg){if(!ok)throw new IllegalStateException(msg);}
 void until(EmulatorHelper c,long pc)throws Exception{
  int n=0;while(c.getExecutionAddress().getOffset()!=pc && n++<100)
   if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
  check(c.getExecutionAddress().getOffset()==pc,"Step limit");
 }
 public void run()throws Exception{
  long sp=0x81004000L;
  for(int nibble:new int[]{0,15})for(int selector:new int[]{0x55,0x80000055}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<12;i++)c.writeRegister("R"+i,0x11000000+i);
    c.writeRegister("R12",selector);
    String[] special={"AC","FP","RP","MDL","MDH"};
    for(int i=0;i<5;i++)c.writeRegister(special[i],0x22000000+i);
    for(String f:new String[]{"S","I","N","Z","V","C","ILM"})c.writeRegister(f,0);
    byte[] hardware=ByteBuffer.allocate(8).putInt(0x80000100).putInt(nibble<<16).array();
    c.writeMemory(toAddr(sp),hardware);c.writeMemory(toAddr(sp-80),new byte[80]);
    c.writeMemory(toAddr(0x46f),new byte[]{0x55});
    c.writeRegister("SP",sp);c.writeRegister("PC",0x4067a);
    until(c,0x406b6);
    long frame=sp-72;int saved=nibble==15?selector&0x7fffffff:selector;
    check(c.readRegister("SP").longValue()==frame && c.readRegister("FP").longValue()==frame,"Frame address");
    ByteBuffer b=ByteBuffer.wrap(c.readMemory(toAddr(frame),72));
    for(int i=0;i<12;i++)check(b.getInt()==0x11000000+i,"Saved R"+i);
    check(b.getInt()==saved,"Saved selector");
    for(int i=0;i<5;i++)check(b.getInt()==0x22000000+i,"Saved special");
    check(Arrays.equals(hardware,c.readMemory(toAddr(sp),8)),"Hardware bytes changed");
    check((c.readMemory(toAddr(0x46f),1)[0]&255)==0x1e,"Entry byte");
    // Explicitly bypass service dispatch to isolate entry/exit frame compatibility.
    c.writeRegister("PC",0x406c6);c.writeRegister("R12",0);
    until(c,0x406f4);
    check(c.readRegister("SP").longValue()==sp,"Final SP");
    for(int i=0;i<12;i++)check(c.readRegister("R"+i).intValue()==0x11000000+i,"Restored R"+i);
    for(int i=0;i<5;i++)check(c.readRegister(special[i]).intValue()==0x22000000+i,"Restored special");
    check(Arrays.equals(hardware,c.readMemory(toAddr(sp),8)),"Final hardware bytes");
    check((c.readMemory(toAddr(0x46f),1)[0]&255)==(saved<0?0x1e:0x10),"Exit byte");
    println(String.format("nibble=%x selector=%08x saved=%08x: 72-byte frame and 17 registers verified before RETI",nibble,selector,saved));
   }finally{c.dispose();}
  }
  println("Four interrupt entry/exit frame cases passed; dispatcher bypassed; INT/RETI not executed");
 }
}
