// AB-99: test whether the bounded initialization constructs OS list sentinels.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;
public class D90InitializationQueuePreconditions extends D90InitializationVectorBoundary {
 byte[] expected;
 int checked=0;
 final long[] heads={0x68000010L,0x68000118L,0x68000120L,0x68000128L};
 @Override long[][] extraSyntheticWindows(){return new long[][]{{0x68000000L,0x68000110L},{0x68000118L,0x68000138L}};}
 @Override void beforeInitialization(EmulatorHelper c,ProgramDB p,int pattern)throws Exception{
  var a=p.getLanguage().getDefaultSpace();
  expected=new byte[8];Arrays.fill(expected,(byte)pattern);
  for(long head:heads)c.writeMemory(a.getAddress(head),expected);
  c.writeMemory(a.getAddress(0x68000000),expected);
  c.writeMemory(a.getAddress(0x68000130),expected);
 }
 void checkState(EmulatorHelper c,ProgramDB p){
  var a=p.getLanguage().getDefaultSpace();
  for(long head:heads){
   require(Arrays.equals(expected,c.readMemory(a.getAddress(head),8)),"Changed sentinel "+Long.toHexString(head));
   require(Integer.toUnsignedLong(word(c,a,head))!=head,"Unexpected empty circular list");
  }
  require(Arrays.equals(expected,c.readMemory(a.getAddress(0x68000000),8)),"Task pointer/bitmap changed");
  require(Arrays.equals(expected,c.readMemory(a.getAddress(0x68000130),8)),"Flag region changed");
 }
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  checkState(c,p);
  super.afterInitialization(c,d,p);
  checkState(c,p);checked++;
  println("AB-99: four sentinel pairs, task pointer/bitmap and flag-region poison retained before/after forced wrapper; pattern="+Integer.toHexString(expected[0]&255));
 }
 @Override public void run()throws Exception{
  super.run();require(checked==8,"Precondition case count");
  println("AB-99: eight precondition probes passed; tested initialization does not leave valid empty OS lists from poison inputs; queue not executed");
 }
}
