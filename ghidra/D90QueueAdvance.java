// AB-90: isolated list advancement; caller frequency and physical timing unknown.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90QueueAdvance extends GhidraScript {
 void put(EmulatorHelper c,long p,int v){c.writeMemory(toAddr(p),ByteBuffer.allocate(4).putInt(v).array());}
 int word(EmulatorHelper c,long p){return ByteBuffer.wrap(c.readMemory(toAddr(p),4)).getInt();}
 void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 public void run()throws Exception{
  long head=0x68000118L,node=0x81001000L,ready=0x68000010L;
  for(int initial:new int[]{0,1,2}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String r:new String[]{"AC","FP","RP","S","I","N","Z","V","C"})c.writeRegister(r,0);
    c.writeRegister("SP",0x81004000L);
    put(c,head,(int)node);put(c,head+4,(int)node);put(c,node,(int)head);put(c,node+4,(int)head);
    put(c,node+8,0x20004);c.writeMemory(toAddr(node+12),new byte[]{0,2});put(c,node+20,initial);
    put(c,node+32,0x12345555);put(c,ready,(int)ready);put(c,ready+4,(int)ready);put(c,0x68000004,0);
    c.writeMemory(toAddr(0x68000132),new byte[]{(byte)0xa5});c.writeMemory(toAddr(0x44),new byte[]{0x55});c.writeMemory(toAddr(0x46f),new byte[]{0x55});
    for(int call=1;call<=initial+1;call++){
     // Each invocation is provided by the harness, not by a modeled timer.
     c.writeRegister("PC",0x40a72);boolean inserted=false,hook=false;int n=0;
     while(c.getExecutionAddress().getOffset()!=0x40ae0 && n++<160){
      long pc=c.getExecutionAddress().getOffset();if(pc==0x4186e)inserted=true;if(pc==0x404fe)hook=true;
      if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
     }
     check(c.getExecutionAddress().getOffset()==0x40ae0,"Stop boundary");
     int delta=initial-call;boolean expired=delta<0;
     check(word(c,node+20)==delta,"Decrement");
     check(inserted==expired && hook==expired,"Path selection");
     check(word(c,head)==(int)(expired?head:node) && word(c,head+4)==(int)(expired?head:node),"Source sentinel");
     check(word(c,ready)==(int)(expired?node:ready) && word(c,ready+4)==(int)(expired?node:ready),"Destination sentinel");
     check(word(c,node)==(int)(expired?ready:head) && word(c,node+4)==(int)(expired?ready:head),"Node links");
     check(word(c,node+8)==(expired?0:0x20004) && word(c,0x68000004)==(expired?0x40000000:0),"State/bitmap");
     check(word(c,node+32)==0x12345555 && c.readRegister("SP").longValue()==0x81004000L,"Metadata/stack");
     check((c.readMemory(toAddr(0x68000132),1)[0]&255)==0xb5,"List flag");
     check(c.readMemory(toAddr(0x44),1)[0]==(expired?1:0x55) && c.readMemory(toAddr(0x46f),1)[0]==(expired?0x1e:0x55),"Peripheral RAM bytes");
     println("initial="+initial+" invocation="+call+" delta="+delta+" transferred="+expired);
    }
   }finally{c.dispose();}
  }
  println("Three queue lifecycles passed over six harness invocations; no timer or interrupt modeled");
 }
}
