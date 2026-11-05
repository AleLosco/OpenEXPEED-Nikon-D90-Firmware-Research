// AB-77: dispatch and bounded gates with a synthetic saved frame; no scheduler run.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90Service85 extends GhidraScript {
 private void put(EmulatorHelper c,long a,int v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());}
 public void run()throws Exception {
  if(currentProgram.getMemory().getShort(toAddr(0x405c8))!=0x8ba)throw new IllegalStateException("Service table mismatch");
  for(int[] test:new int[][]{{0x1f,2},{0x1f,0},{0x1f,-1},{0,2}}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    c.writeRegister("S",0);c.writeRegister("I",0);
    c.writeRegister("AC",0);c.writeRegister("FP",0x81001000L);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0);
    c.writeRegister("R12",-85);c.writeRegister("R4",test[1]);c.writeRegister("PC",0x406b6);
    c.writeMemory(toAddr(0x8100104dL),new byte[]{(byte)test[0]});put(c,0x81001030L,0x1234);put(c,0x68000000,0x81002000);
    long stop=test[0]!=0x1f?0x41000:test[1]<0?0x40f82:test[1]==0?0x40f7c:0x40bec;
    boolean entered=false;int n=0;
    while(c.getExecutionAddress().getOffset()!=stop && n++<60){
     if(c.getExecutionAddress().getOffset()==0x40f2c)entered=true;
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    }
    if(!entered || c.getExecutionAddress().getOffset()!=stop ||
       (stop==0x40bec && (c.readRegister("FP").longValue()!=0x81002000L || c.readRegister("R12").intValue()!=0x1234)))throw new IllegalStateException("Gate mismatch");
    println(String.format("frameByte=0x%x argument=%d dispatch=0x40f2c stop=0x%x",test[0],test[1],stop));
   }finally{c.dispose();}
  }
  println("Four service -85 dispatch/gate cases passed");
 }
}
