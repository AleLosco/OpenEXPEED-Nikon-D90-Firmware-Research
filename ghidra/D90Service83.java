// AB-95: service -83 via dispatcher, synthetic descriptor; stop before context epilogue.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class D90Service83 extends D90QueueAdvance {
 public void run()throws Exception{
  check(currentProgram.getMemory().getShort(toAddr(0x405cc))==0x87e,"Service table");
  int cases=0;
  for(int ignored:new int[]{0,0xffff})for(int high:new int[]{0,0x7fff,0x8000,0xffff})for(int low:new int[]{0,0x89abcdef}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String r:new String[]{"AC","FP","RP","S","I","N","Z","V","C"})c.writeRegister(r,0);
    c.writeRegister("PC",0x406b6);c.writeRegister("SP",0x81004000L);c.writeRegister("R4",0x81001000L);c.writeRegister("R12",-83);
    byte[] descriptor=ByteBuffer.allocate(8).putShort((short)ignored).putShort((short)high).putInt(low).array();
    c.writeMemory(toAddr(0x81001000L),descriptor);put(c,0x68000110,0x11112222);put(c,0x68000114,0x33334444);
    boolean selected=false;int n=0;while(c.getExecutionAddress().getOffset()!=0x406c6 && n++<60){
     if(c.getExecutionAddress().getOffset()==0x40ef0)selected=true;
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    }
    boolean valid=high<0x8000;
    check(selected && c.getExecutionAddress().getOffset()==0x406c6 && c.readRegister("SP").longValue()==0x81004000L,"Dispatch/return");
    check(word(c,0x68000110)==(valid?high:0x11112222) && word(c,0x68000114)==(valid?low:0x33334444),"Counter writes");
    check(c.readRegister("R12").intValue()==(valid?0:0xdf),"Local result");
    check(Arrays.equals(descriptor,c.readMemory(toAddr(0x81001000L),8)),"Input modified");
    println(String.format("unused=%04x high=%04x low=%08x accepted=%s",ignored,high,low,valid));cases++;
   }finally{c.dispose();}
  }
  println(cases+" service -83 descriptor cases passed; INT and context epilogue not executed");
 }
}
