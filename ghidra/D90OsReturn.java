// AB-81: synthetic saved registers; execute epilogue, stop before RETI.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90OsReturn extends GhidraScript {
 public void run()throws Exception{
  long base=0x81004000L;
  for(int saved:new int[]{0,0x80000000})for(int result:new int[]{0,0xdf}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String flag:new String[]{"N","Z","V","C"})c.writeRegister(flag,0);
    c.writeRegister("S",0);c.writeRegister("I",0);c.writeRegister("AC",0);c.writeRegister("FP",0);c.writeRegister("RP",0);
    c.writeRegister("SP",base);c.writeRegister("PC",0x406c6);c.writeRegister("R12",result);
    ByteBuffer frame=ByteBuffer.allocate(72);for(int i=0;i<18;i++)frame.putInt(i==12?saved:0x11000000+i);
    c.writeMemory(toAddr(base),frame.array());c.writeMemory(toAddr(0x46f),new byte[]{0x55});
    boolean hook=false;int steps=0;
    while(c.getExecutionAddress().getOffset()!=0x406f4 && steps++<50){
     if(c.getExecutionAddress().getOffset()==0x404f8)hook=true;
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    }
    if(!hook || c.getExecutionAddress().getOffset()!=0x406f4 || c.readRegister("SP").longValue()!=base+72 ||
       c.readRegister("R12").intValue()!=(byte)result || c.readMemory(toAddr(0x46f),1)[0]!=(saved==0?0x10:0x55))throw new IllegalStateException("Epilogue mismatch");
    for(int i=0;i<12;i++)if(c.readRegister("R"+i).intValue()!=0x11000000+i)throw new IllegalStateException("Register restore");
    String[] special={"AC","FP","RP","MDL","MDH"};
    for(int i=0;i<special.length;i++)if(c.readRegister(special[i]).intValue()!=0x11000000+13+i)throw new IllegalStateException("Special register restore");
    println(String.format("saved=0x%x result=0x%x: restored 17 registers, stopped before RETI",saved,result));
   }finally{c.dispose();}
  }
  println("Four synthetic OS epilogue cases passed");
 }
}
