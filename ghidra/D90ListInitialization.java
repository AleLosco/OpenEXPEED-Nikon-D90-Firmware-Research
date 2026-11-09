// AB-100: original sentinel stores, bounded execution; full boot not executed.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import java.util.Arrays;
import java.nio.ByteBuffer;
public class D90ListInitialization extends D90QueueAdvance {
 public void run()throws Exception{
  var d=new PseudoDisassembler(currentProgram);
  for(long[] range:new long[][]{{0x40704,0x40750},{0x40834,0x408be}}){
   for(long pc=range[0];pc<range[1];){var i=d.disassemble(toAddr(pc));check(i!=null,"Decode");println(String.format("0x%x %s",pc,i));pc+=i.getLength();}
  }
  int count=currentProgram.getMemory().getInt(toAddr(0x5c400));
  println("Original list count at 0x5c400="+count);check(count>=2 && count<=32,"Bounded list count");
  for(int fill:new int[]{0x5a,0xa5}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(String reg:new String[]{"AC","FP","N","Z","V","C","S","I"})c.writeRegister(reg,0);
    c.writeRegister("R0",0);c.writeRegister("PC",0x40886);c.writeRegister("RP",0x80000000L);c.writeRegister("SP",0x81004000L);
    byte[] initial=new byte[0x140];Arrays.fill(initial,(byte)fill);c.writeMemory(toAddr(0x68000000L),initial);
    byte[] expected=initial.clone();var buffer=ByteBuffer.wrap(expected);
    for(int i=0;i<=count;i++){
     int offset=8+8*i;buffer.putInt(offset,0x68000000+offset);buffer.putInt(offset+4,0x68000000+offset);
    }
    for(long head:new long[]{0x68000118L,0x68000120L,0x68000128L}){buffer.putInt((int)(head-0x68000000L),(int)head);buffer.putInt((int)(head+4-0x68000000L),(int)head);}
    buffer.putInt(0x138,-1);int steps=0;
    while(c.getExecutionAddress().getOffset()!=0x80000000L && steps++<250)check(c.step(monitor),"List-array step: "+c.getLastError());
    check(c.getExecutionAddress().getOffset()==0x80000000L,"List-array return");
    c.writeRegister("PC",0x408a0);steps=0;
    while(c.getExecutionAddress().getOffset()!=0x80000000L && steps++<30)check(c.step(monitor),"Step: "+c.getLastError());
    check(c.getExecutionAddress().getOffset()==0x80000000L,"Return");
    byte[] actual=c.readMemory(toAddr(0x68000000L),initial.length);
    for(int j=0;j<actual.length;j++)if(actual[j]!=expected[j])println(String.format("Mismatch 0x%x expected=%02x actual=%02x",0x68000000L+j,expected[j]&255,actual[j]&255));
    check(Arrays.equals(expected,actual),"Stores/guards");
    check(c.readRegister("SP").longValue()==0x81004000L && c.readRegister("RP").longValue()==0x80000000L,"Stack/RP");
    println("fill="+Integer.toHexString(fill)+": list array plus three self-linked sentinels and delay-slot word 0x68000138=-1 verified; surrounding bytes retained");
   }finally{c.dispose();}
  }
  println("AB-100: two sentinel-initialization cases passed; isolated 0x40886 then 0x408a0, not reset");
 }
}
