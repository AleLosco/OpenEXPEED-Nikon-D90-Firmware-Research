// AB-80: execute original service -85 through its local return; no scheduler return.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90Service85Queue extends GhidraScript {
 private void put(EmulatorHelper c,long a,long v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt((int)v).array());}
 private long word(EmulatorHelper c,long a){return Integer.toUnsignedLong(ByteBuffer.wrap(c.readMemory(toAddr(a),4)).getInt());}
 private void check(int[] deltas,int position,int residual,int successor)throws Exception{
  long head=0x68000118L,task=0x81002000L,source=0x81002100L,frame=0x81001000L;
  var c=new EmulatorHelper(currentProgram);
  try{
   for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
   c.writeRegister("AC",0);c.writeRegister("S",0);c.writeRegister("I",0);
   c.writeRegister("FP",frame);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0);
   c.writeRegister("R12",-85);c.writeRegister("R4",2);c.writeRegister("PC",0x406b6);
   c.writeMemory(toAddr(frame+0x4d),new byte[]{0x1f});put(c,frame+0x30,0x1234);put(c,0x68000000,task);
   put(c,source,task);put(c,source+4,task);put(c,task,source);put(c,task+4,source);
   c.writeMemory(toAddr(task+12),new byte[]{0,2});put(c,task+8,0);put(c,task+20,99);put(c,task+32,0x55556666);
   put(c,0x68000004,0xffffffffL);c.writeMemory(toAddr(0x68000132),new byte[]{(byte)0xa5});c.writeMemory(toAddr(0x44),new byte[]{0x55});
   long[] nodes=new long[deltas.length+1];nodes[0]=head;
   for(int i=0;i<deltas.length;i++){nodes[i+1]=0x81003000L+0x100*i;put(c,nodes[i+1]+20,deltas[i]);}
   for(int i=0;i<nodes.length;i++){put(c,nodes[i],nodes[(i+1)%nodes.length]);put(c,nodes[i]+4,nodes[(i+nodes.length-1)%nodes.length]);}
   boolean search=false,unlink=false;int steps=0;
   while(c.getExecutionAddress().getOffset()!=0x406c6 && steps++<250){
    long pc=c.getExecutionAddress().getOffset();if(pc==0x40bec)search=true;if(pc==0x418a0)unlink=true;
    if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
   }
   long[] expected=new long[nodes.length+1];expected[0]=head;
   int j=1;for(int i=0;i<=deltas.length;i++){if(i==position)expected[j++]=task;if(i<deltas.length)expected[j++]=nodes[i+1];}
   for(int i=0;i<expected.length;i++)if(word(c,expected[i])!=expected[(i+1)%expected.length] || word(c,expected[i]+4)!=expected[(i+expected.length-1)%expected.length])throw new IllegalStateException("Queue links");
   for(int i=0;i<deltas.length;i++)if(word(c,nodes[i+1]+20)!=(i==position?successor:deltas[i]))throw new IllegalStateException("Delta mismatch");
   if(!search || !unlink || c.getExecutionAddress().getOffset()!=0x406c6 || c.readRegister("R12").intValue()!=0 ||
      word(c,task+20)!=residual || word(c,task+8)!=0x20004 || word(c,task+32)!=0x12346666 ||
      word(c,source)!=source || word(c,source+4)!=source || word(c,0x68000004)!=0xbfffffffL ||
      (c.readMemory(toAddr(0x68000132),1)[0]&255)!=0xb5 || c.readMemory(toAddr(0x44),1)[0]!=1)
    throw new IllegalStateException("Service effects");
   println("Queue="+java.util.Arrays.toString(deltas)+" insert="+position+" residual="+residual+" returned to dispatcher");
  }finally{c.dispose();}
 }
 public void run()throws Exception{
  if(currentProgram.getMemory().getShort(toAddr(0x405c8))!=0x8ba)throw new IllegalStateException("Dispatch table");
  check(new int[]{},0,2,0);check(new int[]{5},0,2,3);check(new int[]{1,5},1,1,4);check(new int[]{1},1,1,0);
  println("Four synthetic service -85 queue scenarios passed");
 }
}
