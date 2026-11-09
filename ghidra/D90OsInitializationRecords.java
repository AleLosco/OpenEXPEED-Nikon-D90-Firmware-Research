// AB-102: original list constructors followed by full 0x408be; synthetic RAM.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.util.*;
public class D90OsInitializationRecords extends D90QueueAdvance {
 void runRoutine(EmulatorHelper c,long entry)throws Exception{
  c.writeRegister("PC",entry);c.writeRegister("RP",0x80000000L);int steps=0;
  while(c.getExecutionAddress().getOffset()!=0x80000000L && steps++<10000){
   long pc=c.getExecutionAddress().getOffset();if(pc==0x41578)built++;if(pc==0x41534)activated++;
   check(c.step(monitor),"Step "+Long.toHexString(pc)+": "+c.getLastError());
  }
  check(c.getExecutionAddress().getOffset()==0x80000000L,"Bounded return");
 }
 void afterRecords(EmulatorHelper c,int pattern)throws Exception{}
 int built,activated;
 public void run()throws Exception{
  var mem=currentProgram.getMemory();check(mem.getInt(toAddr(0x5c40c))==0x0026001c && mem.getInt(toAddr(0x5c400))==32,"Original headers");
  int[] flags=new int[39],priority=new int[39];long[] node=new long[39],frame=new long[39];
  int active=0,auxCount=0,bitmap=0;
  List<List<Long>> lists=new ArrayList<>();for(int i=0;i<33;i++)lists.add(new ArrayList<>());
  for(int i=0;i<39;i++){
   long t=0x5c410L+28*i;node[i]=Integer.toUnsignedLong(mem.getInt(toAddr(t)));frame[i]=Integer.toUnsignedLong(mem.getInt(toAddr(t+20)));
   flags[i]=mem.getByte(toAddr(t+12))&255;priority[i]=Short.toUnsignedInt(mem.getShort(toAddr(t+6)));
   check(priority[i]>=1 && priority[i]<=33,"Priority range");
   if((flags[i]&0x40)!=0)auxCount++;
   if((flags[i]&0x80)!=0){active++;lists.get(priority[i]-1).add(node[i]);bitmap|=priority[i]<=32 ? 0x80000000 >>> (priority[i]-1) : 0;}
   println(String.format("descriptor=%d T=%x D=%x flags=%02x priority=%d frame=%x",i,t,node[i],flags[i],priority[i],frame[i]));
  }
  for(int pattern:new int[]{0x5a,0xa5}){
   var c=new EmulatorHelper(currentProgram);built=activated=0;
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String r:new String[]{"AC","FP","MDL","MDH","N","Z","V","C","S","I","PS","ILM"})c.writeRegister(r,0);
    c.writeRegister("SP",0x81004000L);
    byte[] ram=new byte[0x1000];Arrays.fill(ram,(byte)pattern);c.writeMemory(toAddr(0x68000000L),ram);
    byte[] stack=new byte[0x200];Arrays.fill(stack,(byte)pattern);c.writeMemory(toAddr(0x81003f00L),stack);
    put(c,0x68000004,0);c.writeMemory(toAddr(0x44),new byte[]{(byte)pattern});
    for(int i=0;i<39;i++)if((flags[i]&0x80)!=0){byte[] f=new byte[40];Arrays.fill(f,(byte)pattern);c.writeMemory(toAddr(frame[i]),f);}
    runRoutine(c,0x40886);runRoutine(c,0x408a0);runRoutine(c,0x408be);
    check(built==39 && activated==active,"Constructor/activation count");
    long aux=0x68000c60L;
    for(int i=0;i<39;i++){
     long t=0x5c410L+28*i,d=node[i];boolean enabled=(flags[i]&0x80)!=0;
     check(word(c,d+8)==(enabled?0:16) && word(c,d+36)==(int)t,"Record state/config");
     if(!enabled)check(word(c,d)==(int)d && word(c,d+4)==(int)d,"Dormant self-links");
     if((flags[i]&0x40)!=0){check(word(c,d+40)==(int)aux && word(c,aux+12)==(int)d,"Auxiliary linkage");aux+=16;}
     else check(word(c,d+40)==-1,"No auxiliary block");
     if(enabled){
      check(word(c,d+24)==(int)frame[i],"Saved context pointer");
      check(word(c,frame[i])==mem.getInt(toAddr(t+16)) && word(c,frame[i]+4)==0x1f0030 && word(c,frame[i]+8)==0x4050a && word(c,frame[i]+36)==mem.getInt(toAddr(t+24)),"Prepared context fields");
     }
    }
    for(int i=0;i<33;i++){
     long head=0x68000008L+8*i,prev=head;
     for(long d:lists.get(i)){check(word(c,prev)==(int)d && word(c,d+4)==(int)prev,"List links");prev=d;}
     check(word(c,prev)==(int)head && word(c,head+4)==(int)prev,"List tail");
    }
    for(long head:new long[]{0x68000118L,0x68000120L,0x68000128L})check(word(c,head)==(int)head && word(c,head+4)==(int)head,"Other lists retained");
    check(word(c,0x68000004)==bitmap,"Bitmap");check(c.readRegister("SP").longValue()==0x81004000L,"SP restored");
    check((c.readMemory(toAddr(0x44),1)[0]&255)==(active>0?1:pattern),"Synthetic peripheral byte");
    afterRecords(c,pattern);
    println(String.format("pattern=%02x built=%d activated=%d aux=%d bitmap=%08x verified",pattern,built,activated,auxCount,bitmap));
   }finally{c.dispose();}
  }
  println("AB-102: two full 0x408be scenarios passed after original list construction; caller transitions imposed, no scheduler/IRQ acceptance");
 }
}
