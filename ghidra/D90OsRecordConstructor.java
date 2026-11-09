// AB-101: isolated 0x41578 using original descriptors, synthetic destination RAM.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class D90OsRecordConstructor extends D90QueueAdvance {
 public void run()throws Exception{
  var mem=currentProgram.getMemory();int count=Short.toUnsignedInt(mem.getShort(toAddr(0x5c40c)))+1;
  int stride=Short.toUnsignedInt(mem.getShort(toAddr(0x5c40e)));check(count==39 && stride==28,"Sample table header");
  int cases=0;
  for(int index=0;index<count;index++)for(int pattern:new int[]{0x5a,0xa5}){
   long descriptor=0x5c410L+(long)index*stride;
   long destination=Integer.toUnsignedLong(mem.getInt(toAddr(descriptor)));
   check(destination>=0x6800013cL && destination+0x2c<=0x680007f0L,"Destination range");
   byte[] bytes=new byte[0x30];Arrays.fill(bytes,(byte)pattern);byte[] expected=bytes.clone();var b=ByteBuffer.wrap(expected);
   b.putInt(4,(int)destination);b.putInt(8,(int)destination);b.putInt(12,0x10);
   b.putShort(16,mem.getShort(toAddr(descriptor+6)));b.putInt(20,0);b.putInt(24,-1);b.putInt(32,-1);b.putShort(38,(short)0);b.putInt(40,(int)descriptor);
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String reg:new String[]{"AC","FP","N","Z","V","C","S","I"})c.writeRegister(reg,0);
    c.writeRegister("R2",descriptor);c.writeRegister("PC",0x41578);c.writeRegister("RP",0x80000000L);c.writeRegister("SP",0x81004000L);
    c.writeMemory(toAddr(destination-4),bytes);int steps=0;
    while(c.getExecutionAddress().getOffset()!=0x80000000L && steps++<40)check(c.step(monitor),"Step: "+c.getLastError());
    check(c.getExecutionAddress().getOffset()==0x80000000L,"Return");
    check(Arrays.equals(expected,c.readMemory(toAddr(destination-4),bytes.length)),"Record/guards index="+index);
    check(c.readRegister("FP").longValue()==destination && c.readRegister("SP").longValue()==0x81004000L,"FP/SP");
    for(int[] pair:new int[][]{{5,24},{6,16},{7,20}})check(c.readRegister("R"+pair[0]).intValue()==mem.getInt(toAddr(descriptor+pair[1])),"Descriptor output register");
    cases++;
   }finally{c.dispose();}
  }
  println("AB-101: "+cases+" constructor cases passed across 39 original descriptors and two destination patterns; activation/insertion not executed");
 }
}
