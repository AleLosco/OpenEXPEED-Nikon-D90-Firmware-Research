// AB-94: continuous initialization prefix, stop before service -83 INT.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.util.Arrays;
import java.nio.ByteBuffer;
public class D90SourceHelper extends D90QueueAdvance {
 public void run()throws Exception{
  for(int seed:new int[]{0,255}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String r:new String[]{"AC","FP","RP","MDL","MDH","S","I","N","Z","V","C"})c.writeRegister(r,0);
    byte[] low=new byte[0x1000],high=new byte[0x100];Arrays.fill(low,(byte)seed);Arrays.fill(high,(byte)seed);
    c.writeMemory(toAddr(0),low);c.writeMemory(toAddr(0x6b000000),high);
    byte[] stack=new byte[0x100];Arrays.fill(stack,(byte)0x5a);c.writeMemory(toAddr(0x81003f00L),stack);
    c.writeRegister("PC",0x40738);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0x12345678);
    int n=0;java.util.Set<Long> seen=new java.util.HashSet<>();
    while(c.getExecutionAddress().getOffset()!=0x10094e && n++<1000){
     seen.add(c.getExecutionAddress().getOffset());if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    }
    check(c.getExecutionAddress().getOffset()==0x10094e,"INT boundary");
    for(long site:new long[]{0x100936,0x10095e,0x100cec,0x100d18})check(seen.contains(site),"Missing helper");
    check(c.readRegister("R12").intValue()==-83,"Service selector");
    long frame=c.readRegister("FP").longValue(),arg=c.readRegister("R4").longValue();
    check(arg==frame-8 && c.readRegister("SP").longValue()==0x81003fecL,"Argument frame");
    byte[] descriptor=c.readMemory(toAddr(arg),8);
    check(Arrays.equals(descriptor,new byte[]{0x5a,0x5a,0,0,0,0,0,0}),"Descriptor bytes and preserved input");
    check(Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(toAddr(0x48),2)).getShort())==0x9e34 && ByteBuffer.wrap(c.readMemory(toAddr(0x4e),2)).getShort()==0x1b && c.readMemory(toAddr(0x448),1)[0]==0x12,"Startup writes retained");
    check(c.readMemory(toAddr(0x40),1)[0]==0 && c.readMemory(toAddr(0xa0),1)[0]==0 && c.readMemory(toAddr(0x41),1)[0]==0x41 && c.readMemory(toAddr(0xa1),1)[0]==4,"Control bytes");
    int initial=seed==0?0:0xffffffff;
    check(word(c,0x6b000008)==0xdfffffff && word(c,0x6b000000)==(initial&0xfeffffff) && word(c,0x6b000018)==(initial|0x08000000),"High-bank operations");
    check(word(c,0x130)==0xffffffff && Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(toAddr(0x13e),2)).getShort())==0xa13 && ByteBuffer.wrap(c.readMemory(toAddr(0x4f8),2)).getShort()==0x7e && ByteBuffer.wrap(c.readMemory(toAddr(0x4fe),2)).getShort()==0x873 && c.readMemory(toAddr(0x7ef),1)[0]==0x14,"Later helper writes");
    println("seed="+seed+" prefix completed in "+n+" steps; stopped before service -83; descriptor=5a5a000000000000");
   }finally{c.dispose();}
  }
  println("Two continuous initialization-prefix cases passed; INT and later helpers not executed");
 }
}
