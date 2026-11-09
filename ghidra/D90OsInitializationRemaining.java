// AB-103: remaining original table routines on AB-102 memory; not full boot.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class D90OsInitializationRemaining extends D90OsInitializationRecords {
 int scenarios=0;
 @Override void afterRecords(EmulatorHelper c,int pattern)throws Exception{
  var mem=currentProgram.getMemory();
  byte[] expected=c.readMemory(toAddr(0x68000000L),0x1000);
  long[] frames={0x8f991204L,0x68000ed0L};byte[][] saved=new byte[2][];
  for(int i=0;i<2;i++)saved[i]=c.readMemory(toAddr(frames[i]),40);
  byte peripheral=c.readMemory(toAddr(0x44),1)[0];
  long[] entries={0x40906,0x4092c,0x40952,0x40978,0x409c4,0x40a0a,0x40a30};
  long[] tables={0x5c854,0x5cb68,0x5cccc,0x5ccd0,0x5ccd4,0x5ccd8,0x5ccdc};
  int[] counts={49,22,0,0,0,0,0},strides={16,16,12,28,24,8,8};
  for(int routine=0;routine<entries.length;routine++){
   long table=tables[routine];
   check(Short.toUnsignedInt(mem.getShort(toAddr(table)))==counts[routine] && Short.toUnsignedInt(mem.getShort(toAddr(table+2)))==strides[routine],"Original header");
   var b=ByteBuffer.wrap(expected);
   for(int i=0;i<counts[routine];i++){
    long t=table+4+(long)i*strides[routine],d=Integer.toUnsignedLong(mem.getInt(toAddr(t)));
    long min=routine==0?0x680007f0L:0x68000b00L,max=routine==0?0x68000b00L:0x68000c60L;
    check(d>=min && d+16<=max && (d&3)==0,"Destination bounds");
    int offset=(int)(d-0x68000000L);
    b.putInt(offset,(int)d);b.putInt(offset+4,(int)d);b.putInt(offset+12,(int)t);
    if(routine==0)b.putShort(offset+8,mem.getShort(toAddr(t+12)));
    else b.putInt(offset+8,mem.getInt(toAddr(t+12)));
   }
   runRoutine(c,entries[routine]);
   check(Arrays.equals(expected,c.readMemory(toAddr(0x68000000L),expected.length)),"RAM footprint at routine "+Long.toHexString(entries[routine]));
   for(int i=0;i<2;i++)check(Arrays.equals(saved[i],c.readMemory(toAddr(frames[i]),40)),"Prepared context retained");
   check(c.readMemory(toAddr(0x44),1)[0]==peripheral,"Peripheral byte retained");
   check(c.readRegister("SP").longValue()==0x81004000L,"SP restored");
   check(built==39 && activated==2,"No additional record constructor/activation");
   println(String.format("pattern=%02x routine=%x count=%d: expected RAM footprint and prior lists/contexts retained",pattern,entries[routine],counts[routine]));
  }
  scenarios++;
 }
 @Override public void run()throws Exception{
  super.run();check(scenarios==2,"Scenario count");
  println("AB-103: two scenarios, fourteen routine returns; 49 halfword-field and 22 word-field objects per scenario, five zero-count tables; explicit caller transitions");
 }
}
