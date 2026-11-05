// AB-76: continuous prefix through configuration, stopping before the first OS INT.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90SetupConfig extends GhidraScript {
 private void put(EmulatorHelper c,long a,int v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());}
 private int word(EmulatorHelper c,long a){return ByteBuffer.wrap(c.readMemory(toAddr(a),4)).getInt();}
 public void run()throws Exception{
  for(int mode:new int[]{0xab01,0xab04,0xab00})
  for(boolean advance:new boolean[]{false,true}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    c.writeRegister("AC",0);c.writeRegister("FP",0);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0);
    c.writeRegister("PC",0x19e020);put(c,0x63000100,0x80000000);put(c,0x68000114,0);
    c.writeMemory(toAddr(0x50000004),new byte[]{(byte)(mode>>8),(byte)mode});
    c.writeMemory(toAddr(0x50000105),new byte[]{0x52});
    c.writeMemory(toAddr(0x6800cf1f),new byte[]{(byte)0xa5});
    put(c,0x63000024,0);put(c,0x63000028,0xaaaaaaaa);put(c,0x6300002c,0xffffffff);
    int modeCalls=0,secondReads=0;
    int calls=0,reads=0;boolean injected=false;int n=0;
    while(c.getExecutionAddress().getOffset()!=0x19e064 && n++<800){
     long pc=c.getExecutionAddress().getOffset();
     if(pc==0x1a0322)modeCalls++;
     if(pc==0x1a0398)secondReads++;
     if(pc==0x19e144){
      calls++;
      if(word(c,0x63000100)!=0x80000100)throw new IllegalStateException("Missing set bit");
      if(advance && !injected){put(c,0x68000114,1000);injected=true;}
     }
     if(pc==0x19e128){reads++;if(!advance){put(c,0x63000100,0x80000000);injected=true;}}
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    }
    if(c.getExecutionAddress().getOffset()!=0x19e064 || !injected || calls!=(advance?3:1) || reads!=(advance?2:1) ||
       word(c,0x63000100)!=(advance?0x80000100:0x80000000) || word(c,0x68000114)!=(advance?1000:0))throw new IllegalStateException("Unexpected wait path");
    if(ByteBuffer.wrap(c.readMemory(toAddr(0x50000004),2)).getShort()!=(short)0xab01 ||
       word(c,0x63000028)!=((0xaaaaaaaa|0xf00)&~2) || word(c,0x6300002c)!=0xfffffffb ||
       word(c,0x63000024)!=0 || (c.readMemory(toAddr(0x50000105),1)[0]&255)!=0x5a ||
       (c.readMemory(toAddr(0x6800cf1f),1)[0]&255)!=0xa5 ||
       c.readRegister("R4").intValue()!=2 || c.readRegister("R12").intValue()!=-85 ||
       modeCalls!=((mode&15)==1?0:1) || secondReads!=1)throw new IllegalStateException("Configuration mismatch");
    println("mode="+Integer.toHexString(mode)+" advance="+advance+" helper calls="+calls+" status reads="+reads+" stopped before INT at 0x19e064");
   }finally{c.dispose();}
  }
  println("Six configuration scenarios passed; OS service not executed");
 }
}
