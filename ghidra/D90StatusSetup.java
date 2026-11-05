// AB-74: original initializer prefix and isolated gate; no invented callee returns.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class D90StatusSetup extends GhidraScript {
 private void put(EmulatorHelper c,long a,int v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());}
 private int word(EmulatorHelper c,long a){return ByteBuffer.wrap(c.readMemory(toAddr(a),4)).getInt();}
 private EmulatorHelper cpu(long pc){var c=new EmulatorHelper(currentProgram);for(int i=0;i<13;i++)c.writeRegister("R"+i,0);c.writeRegister("AC",0);c.writeRegister("FP",0);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0);c.writeRegister("PC",pc);return c;}
 private void step(EmulatorHelper c)throws Exception{if(!c.step(monitor))throw new IllegalStateException(c.getLastError());}
 public void run()throws Exception{
  for(int fill:new int[]{0x55,0xaa}){
   var c=cpu(0x19db40);
   try{
    byte[] initial=new byte[0x2e0];Arrays.fill(initial,(byte)fill);c.writeMemory(toAddr(0x6800cc40L),initial);
    byte[] expected=initial.clone();
    for(long a:new long[]{0x6800cd10L,0x6800cd08L,0x6800cd0cL,0x6800cd04L,0x6800cc50L,0x6800cc44L,0x6800cd00L,0x6800cc40L,0x6800cc4cL,0x6800cc48L})Arrays.fill(expected,(int)(a-0x6800cc40L),(int)(a-0x6800cc40L)+4,(byte)0);
    for(long a:new long[]{0x6800cf1fL,0x6800cf1eL,0x6800cf1dL,0x6800cf1bL,0x6800cf1cL,0x6800cf17L,0x6800cf14L})expected[(int)(a-0x6800cc40L)]=0;
    int n=0;while(c.getExecutionAddress().getOffset()!=0x19e020 && n++<150)step(c);
    if(c.getExecutionAddress().getOffset()!=0x19e020 || !Arrays.equals(expected,c.readMemory(toAddr(0x6800cc40L),expected.length)))throw new IllegalStateException("Initializer mismatch");
    println(String.format("Initializer fill=0x%x passed to 0x19e020",fill));
   }finally{c.dispose();}
  }
  for(int status:new int[]{0,0x40000,0x80000,0xc0000}){
   var c=cpu(0x19db56);
   try{
    put(c,0x63000024,status);c.writeMemory(toAddr(0x63000028),new byte[]{(byte)0xa4,0x55,0x66,0x77});
    long stop=(status&0x40000)==0?0x1a03d8:0x19e68e;
    int n=0;while(c.getExecutionAddress().getOffset()!=stop && n++<50)step(c);
    int expected=(status&0x40000)==0?0xa4556677:0xa5556677;
    if(c.getExecutionAddress().getOffset()!=stop || word(c,0x63000028)!=expected || word(c,0x63000024)!=status || ((status&0x40000)==0 && c.readRegister("R4").intValue()!=0))throw new IllegalStateException("Gate mismatch");
    println(String.format("Gate status=0x%x stopped before 0x%x",status,stop));
   }finally{c.dispose();}
  }
  println("Two initializer and four isolated gate cases passed");
 }
}
