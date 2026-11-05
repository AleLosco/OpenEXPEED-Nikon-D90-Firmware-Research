// Candidate scan only: literal base in R4, a textual #0x30 match and at most 12 instructions.
// Does not establish reachability, complete access coverage or hardware semantics.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
public class D90StatusCandidates extends GhidraScript {
 public void run() throws Exception {
  var m=currentProgram.getMemory(); var d=new PseudoDisassembler(currentProgram);
  for(long p=0x198000;p+6<=0x1a4000;p+=2){
   monitor.checkCancelled();
   if((m.getShort(toAddr(p))&0xffff)!=0x9f84 || m.getInt(toAddr(p+2))!=0x63000000) continue;
   StringBuilder s=new StringBuilder(); boolean hit=false; long q=p;
   for(int n=0;n<12;n++){
    var i=d.disassemble(toAddr(q)); if(i==null)break;
    String t=i.toString(); s.append(String.format("0x%x %s%n",q,t));
    if(t.contains("#0x30,"))hit=true;
    if(!i.getFlowType().isFallthrough()) {
     if(i.getDelaySlotDepth()>0){var delay=d.disassemble(toAddr(q+i.getLength()));s.append("delay: "+delay+"\n");}
     break;
    }
    q+=i.getLength();
   }
   if(hit)println(s.toString());
  }
  for(long p=0x1a011a;p<0x1a012a;p+=2)println(String.format("Helper 0x%x %s",p,d.disassemble(toAddr(p))));
 }
}
