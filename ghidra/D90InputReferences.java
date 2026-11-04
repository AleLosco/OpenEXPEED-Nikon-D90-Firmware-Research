import ghidra.app.script.GhidraScript;
import ghidra.program.model.scalar.Scalar;
public class D90InputReferences extends GhidraScript {
 public void run() throws Exception {
  long[] targets={0x8f52e4abL,0x8f52f1aaL,0x4000d200L,0x4000d300L,0x4000d400L,0x4000d500L};
  var it=currentProgram.getListing().getInstructions(true);
  while(it.hasNext()&&!monitor.isCancelled()) {
   var ins=it.next(); boolean match=false;
   for(int op=0;op<ins.getNumOperands();op++) for(Object obj:ins.getOpObjects(op))
    if(obj instanceof Scalar) for(long target:targets) if((((Scalar)obj).getUnsignedValue()&0xffffffffL)==target) match=true;
   if(match){println("MATCH "+ins.getAddress()+" FUNCTION "+getFunctionContaining(ins.getAddress())); var p=ins; for(int n=0;n<12&&p!=null;n++,p=p.getNext()) println(p.getAddress()+" "+p);}
  }
 }
}
