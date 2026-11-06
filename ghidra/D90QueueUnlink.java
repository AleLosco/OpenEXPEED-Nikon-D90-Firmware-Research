// AB-78: execute the original doubly-linked-list unlink block on synthetic nodes.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90QueueUnlink extends GhidraScript {
 private void word(EmulatorHelper c,long a,int v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());}
 private int word(EmulatorHelper c,long a){return ByteBuffer.wrap(c.readMemory(toAddr(a),4)).getInt();}
 private void check(long[] nodes,int remove)throws Exception{
  var c=new EmulatorHelper(currentProgram);
  try{
   for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
   c.writeRegister("AC",0);c.writeRegister("FP",nodes[remove]);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0x80000000L);
   for(int i=0;i<nodes.length;i++){
    long prev=nodes[(i+nodes.length-1)%nodes.length],next=nodes[(i+1)%nodes.length];
    word(c,nodes[i],(int)next);word(c,nodes[i]+4,(int)prev);
   }
   c.writeRegister("PC",0x418a0);
   int n=0;while(c.getExecutionAddress().getOffset()!=0x80000000L && n++<40)if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
   long prev=nodes[(remove+nodes.length-1)%nodes.length],next=nodes[(remove+1)%nodes.length];
   if(c.getExecutionAddress().getOffset()!=0x80000000L || word(c,prev)!=(int)next || word(c,next+4)!=(int)prev ||
      word(c,nodes[remove])!=(int)next || word(c,nodes[remove]+4)!=(int)prev || c.readRegister("AC").intValue()!=1)
    throw new IllegalStateException("Unlink mismatch");
  }finally{c.dispose();}
 }
 public void run()throws Exception{
  check(new long[]{0x81001000L,0x81001100L,0x81001200L},1);
  check(new long[]{0x81002000L,0x81002100L,0x81002200L,0x81002300L},2);
  println("Original 0x418a0 unlinked middle nodes from two synthetic circular lists");
 }
}
