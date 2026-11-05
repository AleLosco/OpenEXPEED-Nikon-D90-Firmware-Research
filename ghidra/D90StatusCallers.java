// @category Nikon
// Bounded static candidates: references and literal pointers, then local disassembly.
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
import java.util.TreeSet;
public class D90StatusCallers extends GhidraScript {
 public void run() throws Exception {
  long target=0x19d89c; var hits=new TreeSet<Long>();
  var refs=currentProgram.getReferenceManager().getReferencesTo(toAddr(target));
  while(refs.hasNext()){var r=refs.next();println("Reference "+r);hits.add(r.getFromAddress().getOffset());}
  var m=currentProgram.getMemory();
  for(long p=0x198000;p+6<=0x1a4000;p+=2){
   monitor.checkCancelled();
   if((m.getShort(toAddr(p))&0xfff0)==0x9f80 && m.getInt(toAddr(p+2))==target)hits.add(p);
  }
  var d=new PseudoDisassembler(currentProgram);
  for(long h:hits){println(String.format("Candidate 0x%x",h));
   // Start at the matched instruction, not an arbitrary preceding byte boundary.
   for(long p=h,n=0;n<18;n++) {var i=d.disassemble(toAddr(p));if(i==null)break;println(String.format("0x%x %s",p,i));p+=i.getLength();}
  }
  println("Candidate count="+hits.size());
 }
}
