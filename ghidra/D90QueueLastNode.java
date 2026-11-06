// AB-79: remove a sole synthetic node; test bitmap selection without scheduler effects.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90QueueLastNode extends GhidraScript {
 private void put(EmulatorHelper c,long a,int v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());}
 private int word(EmulatorHelper c,long a){return ByteBuffer.wrap(c.readMemory(toAddr(a),4)).getInt();}
 public void run()throws Exception{
  long node=0x81001000L,head=0x81001100L;
  int count=0;
  for(int index:new int[]{1,2,16,32})for(int bitmap:new int[]{0,-1}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    c.writeRegister("AC",0);c.writeRegister("FP",node);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0x80000000L);
    put(c,node,(int)head);put(c,node+4,(int)head);put(c,head,(int)node);put(c,head+4,(int)node);
    c.writeMemory(toAddr(node+12),new byte[]{0,(byte)index});put(c,0x68000004,bitmap);
    c.writeMemory(toAddr(0x44),new byte[]{0x55});c.writeRegister("PC",0x418a0);
    int steps=0;while(c.getExecutionAddress().getOffset()!=0x80000000L && steps++<60)
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    int expected=bitmap & ~(0x80000000 >>> (index-1));
    if(c.getExecutionAddress().getOffset()!=0x80000000L || word(c,head)!=(int)head || word(c,head+4)!=(int)head ||
       word(c,node)!=(int)head || word(c,node+4)!=(int)head || word(c,0x68000004)!=expected ||
       c.readRegister("FP").longValue()!=node || c.readRegister("AC").intValue()!=1 || c.readMemory(toAddr(0x44),1)[0]!=1)
     throw new IllegalStateException("Last-node mismatch");
    count++;println(String.format("index=%d bitmap=0x%x final=0x%x",index,bitmap,expected));
   }finally{c.dispose();}
  }
  println(count+" sole-node synthetic cases passed");
 }
}
