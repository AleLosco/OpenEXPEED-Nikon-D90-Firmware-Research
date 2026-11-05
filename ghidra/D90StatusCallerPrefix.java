// Execute two wrapper prefixes through the reset routine; stop before 0x19db40.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90StatusCallerPrefix extends GhidraScript {
 private void put(EmulatorHelper c,long a,int v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());}
 private int word(EmulatorHelper c,long a){return ByteBuffer.wrap(c.readMemory(toAddr(a),4)).getInt();}
 public void run() throws Exception {
  for(long entry:new long[]{0x19d4ac,0x19d52c})
  for(int initial:new int[]{0,-1}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int r=0;r<13;r++)c.writeRegister("R"+r,0);
    c.writeRegister("AC",0);c.writeRegister("FP",0);
    c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0x80000000L);
    c.writeRegister("PC",entry);
    c.writeMemory(toAddr(0x5000000a),new byte[]{(byte)(initial>>8),(byte)initial});
    for(long a:new long[]{0x6300002c,0x63000030,0x63000034,0x63000038})put(c,a,initial);
    c.writeMemory(toAddr(0x6800cf15),new byte[]{0x55,0x66});
    c.writeMemory(toAddr(0x6800cf1f),new byte[]{(byte)0xa5});
    int steps=0;
    while(c.getExecutionAddress().getOffset()!=0x19db40L && steps++<200)
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    if(c.getExecutionAddress().getOffset()!=0x19db40L ||
       c.readRegister("R8").longValue()!=0x6800cf1fL || (c.readMemory(toAddr(0x6800cf1f),1)[0]&255)!=0xa5 ||
       word(c,0x63000034)!=(initial&0x0e00fe00) || word(c,0x63000038)!=(initial&0x0e00fe00) ||
       word(c,0x63000030)!=0xf1ff01ff || word(c,0x6300002c)!=((initial&0xffff00ff)|1) ||
       ByteBuffer.wrap(c.readMemory(toAddr(0x5000000a),2)).getShort()!=(short)(initial&0xfffd) ||
       c.readMemory(toAddr(0x6800cf15),2)[0]!=0 || c.readMemory(toAddr(0x6800cf15),2)[1]!=0)
     throw new IllegalStateException("Unexpected result");
    println(String.format("entry=0x%x initial=0x%x: stopped before 0x19db40, masks=0x%x status=0x%x",entry,initial,word(c,0x63000038),word(c,0x63000030)));
   }finally{c.dispose();}
  }
  println("Four wrapper-prefix cases passed; 0x19db40 not executed");
 }
}
