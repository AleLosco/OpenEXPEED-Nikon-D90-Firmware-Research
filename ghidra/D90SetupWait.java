// AB-75: synthetic bit-clear and counter-advance scenarios; no real clock/MMIO model.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.nio.ByteBuffer;
public class D90SetupWait extends GhidraScript {
 private void put(EmulatorHelper c,long a,int v){c.writeMemory(toAddr(a),ByteBuffer.allocate(4).putInt(v).array());}
 private int word(EmulatorHelper c,long a){return ByteBuffer.wrap(c.readMemory(toAddr(a),4)).getInt();}
 public void run()throws Exception{
  for(boolean advance:new boolean[]{false,true}){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    c.writeRegister("AC",0);c.writeRegister("FP",0);c.writeRegister("SP",0x81004000L);c.writeRegister("RP",0);
    c.writeRegister("PC",0x19e020);put(c,0x63000100,0x80000000);put(c,0x68000114,0);
    int calls=0,reads=0;boolean injected=false;int n=0;
    while(c.getExecutionAddress().getOffset()!=0x19e028 && n++<500){
     long pc=c.getExecutionAddress().getOffset();
     if(pc==0x19e144){
      calls++;
      if(word(c,0x63000100)!=0x80000100)throw new IllegalStateException("Missing set bit");
      if(advance && !injected){put(c,0x68000114,1000);injected=true;}
     }
     if(pc==0x19e128){reads++;if(!advance){put(c,0x63000100,0x80000000);injected=true;}}
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    }
    if(c.getExecutionAddress().getOffset()!=0x19e028 || !injected || calls!=(advance?3:1) || reads!=(advance?2:1) ||
       word(c,0x63000100)!=(advance?0x80000100:0x80000000) || word(c,0x68000114)!=(advance?1000:0))throw new IllegalStateException("Unexpected wait path");
    println("advance="+advance+" helper calls="+calls+" status reads="+reads+" returned to 0x19e028");
   }finally{c.dispose();}
  }
  println("Two synthetic wait scenarios passed");
 }
}
