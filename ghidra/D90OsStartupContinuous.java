// AB-104: continuous original CALL chain, stopping before peripheral initialization.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
import java.util.*;
public class D90OsStartupContinuous extends D90QueueAdvance {
 public void run()throws Exception{
  var mem=currentProgram.getMemory();
  check(mem.getInt(toAddr(0x5c400))==32 && mem.getInt(toAddr(0x5c40c))==0x0026001c,"Sample headers");
  byte[] expected=new byte[0xf20];var b=ByteBuffer.wrap(expected);
  for(int i=0;i<33;i++){int o=8+8*i;b.putInt(o,0x68000000+o);b.putInt(o+4,0x68000000+o);}
  for(int o:new int[]{0x118,0x120,0x128}){b.putInt(o,0x68000000+o);b.putInt(o+4,0x68000000+o);}b.putInt(0x138,-1);
  Map<Long,byte[]> contexts=new HashMap<>();int aux=0xc60,bitmap=0,active=0;
  List<List<Integer>> lists=new ArrayList<>();for(int i=0;i<33;i++)lists.add(new ArrayList<>());
  for(int i=0;i<39;i++){
   long t=0x5c410L+28*i;int d=mem.getInt(toAddr(t)),o=d-0x68000000;
   int flags=mem.getByte(toAddr(t+12))&255,priority=Short.toUnsignedInt(mem.getShort(toAddr(t+6)));
   check(o>=0x13c && o+44<=0x7f0 && priority>=1 && priority<=33,"Record bounds");
   b.putInt(o,d);b.putInt(o+4,d);b.putInt(o+8,(flags&0x80)!=0?0:16);b.putShort(o+12,(short)priority);
   b.putInt(o+20,-1);b.putInt(o+28,-1);b.putInt(o+36,(int)t);b.putInt(o+40,-1);
   if((flags&0x40)!=0){b.putInt(o+40,0x68000000+aux);b.putInt(aux+12,d);aux+=16;}
   if((flags&0x80)!=0){
    active++;lists.get(priority-1).add(d);if(priority<=32)bitmap|=0x80000000 >>> (priority-1);
    long f=Integer.toUnsignedLong(mem.getInt(toAddr(t+20)));b.putInt(o+24,(int)f);
    byte[] context=new byte[40];var cb=ByteBuffer.wrap(context);
    cb.putInt(0,mem.getInt(toAddr(t+16)));cb.putInt(4,0x1f0030);cb.putInt(8,0x4050a);cb.putInt(36,mem.getInt(toAddr(t+24)));
    contexts.put(f,context);
   }
  }
  check(active==2 && aux==0xec0,"Sample active/aux count");
  for(int i=0;i<33;i++){
   int head=0x68000008+8*i,prev=head;
   for(int d:lists.get(i)){b.putInt(prev-0x68000000,d);b.putInt(d-0x68000000+4,prev);prev=d;}
   b.putInt(prev-0x68000000,head);b.putInt(head-0x68000000+4,prev);
  }
  b.putInt(4,bitmap);
  for(long[] table:new long[][]{{0x5c854,49},{0x5cb68,22}}){
   check(Short.toUnsignedInt(mem.getShort(toAddr(table[0])))==table[1] && mem.getShort(toAddr(table[0]+2))==16,"Table header");
   for(int i=0;i<table[1];i++){
    long t=table[0]+4+16*i;int d=mem.getInt(toAddr(t)),o=d-0x68000000;check(o>=0x7f0 && o+16<=0xc60,"Object bounds");
    b.putInt(o,d);b.putInt(o+4,d);b.putInt(o+12,(int)t);
    if(table[0]==0x5c854)b.putShort(o+8,mem.getShort(toAddr(t+12)));else b.putInt(o+8,mem.getInt(toAddr(t+12)));
   }
  }
  for(long t:new long[]{0x5cccc,0x5ccd0,0x5ccd4,0x5ccd8,0x5ccdc})check(mem.getShort(toAddr(t))==0,"Empty table");
  for(var entry:contexts.entrySet())if(entry.getKey()>=0x68000000L && entry.getKey()+40<=0x68000f20L)
   System.arraycopy(entry.getValue(),0,expected,(int)(entry.getKey()-0x68000000L),40);
  for(int pattern:new int[]{0x5a,0xa5}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String r:new String[]{"AC","FP","RP","MDL","MDH","PS","ILM","N","Z","V","C","S","I","T","D0","D1"})c.writeRegister(r,0);
    c.writeRegister("PC",0x40704);c.writeRegister("SP",0x81004000L);
    byte[] ram=new byte[0x1000],low=new byte[0x408],stack=new byte[0x200];Arrays.fill(ram,(byte)pattern);Arrays.fill(low,(byte)pattern);Arrays.fill(stack,(byte)pattern);
    c.writeMemory(toAddr(0x68000000L),ram);c.writeMemory(toAddr(0x7ffc),low);c.writeMemory(toAddr(0x81003f00L),stack);
    for(long f:contexts.keySet())if(f<0x68000000L || f>=0x68001000L)c.writeMemory(toAddr(f),new byte[40]);
    put(c,0x6800cf20,0x11223344);c.writeMemory(toAddr(0x44),new byte[]{(byte)pattern});c.writeMemory(toAddr(0x46f),new byte[]{(byte)pattern});
    int steps=0,built=0,inserted=0;Set<Long> seen=new HashSet<>();
    while(c.getExecutionAddress().getOffset()!=0x40738 && steps++<30000){
     long pc=c.getExecutionAddress().getOffset();seen.add(pc);if(pc==0x41578)built++;if(pc==0x41534)inserted++;
     check(c.step(monitor),"Step "+Long.toHexString(pc)+": "+c.getLastError());
    }
    check(c.getExecutionAddress().getOffset()==0x40738 && built==39 && inserted==2,"Continuous boundary/counts");
    for(long site:new long[]{0x407f0,0x40878,0x40886,0x408a0,0x408be,0x40906,0x4092c,0x40952,0x40978,0x409c4,0x40a0a,0x40a30})check(seen.contains(site),"Missing call");
    check(Arrays.equals(expected,c.readMemory(toAddr(0x68000000L),expected.length)),"OS memory model");
    for(var entry:contexts.entrySet())check(Arrays.equals(entry.getValue(),c.readMemory(toAddr(entry.getKey()),40)),"Context model");
    check(Arrays.equals(new byte[0x400],c.readMemory(toAddr(0x8000),0x400)),"Low cleared region");
    for(int i=0;i<4;i++){check(c.readMemory(toAddr(0x7ffc+i),1)[0]==(byte)pattern,"Low left guard");check(c.readMemory(toAddr(0x8400+i),1)[0]==(byte)pattern,"Low right guard");}
    byte[] tail=new byte[0xe0];Arrays.fill(tail,(byte)pattern);check(Arrays.equals(tail,c.readMemory(toAddr(0x68000f20L),tail.length)),"OS tail guard");
    check(word(c,0x6800cf20)==1 && c.readRegister("SP").longValue()==0x81004000L,"Marker/SP");
    check(c.readMemory(toAddr(0x44),1)[0]==1 && c.readMemory(toAddr(0x46f),1)[0]==0x10,"Peripheral RAM bytes");
    println("pattern="+Integer.toHexString(pattern)+" steps="+steps+": continuous CALL chain reached 0x40738, OS memory/contexts/clear guards verified");
   }finally{c.dispose();}
  }
  println("AB-104: two continuous OS startup-prefix scenarios passed; no PC/RP changes after start, stopped before AB-97 peripheral initialization");
 }
}
