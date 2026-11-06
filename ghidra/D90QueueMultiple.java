// AB-91: whole 0x40a56 routine, synthetic lists and counter; timing source unknown.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import java.util.Arrays;
public class D90QueueMultiple extends D90QueueAdvance {
 public void run()throws Exception{
  int[][] initial={{0,1},{0,0},{1,1},{0,2},{0,0,0},{}};
  int[][] finalValues={{-1,0},{-1,-1},{0,1},{-1,1},{-1,-1,-1},{}};
  int[] removed={1,2,0,1,3,0};
  long head=0x68000118L,ready=0x68000010L;
  for(int test=0;test<initial.length;test++){
   var c=new EmulatorHelper(currentProgram);
   try{
    for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
    for(String r:new String[]{"AC","FP","RP","S","I","N","Z","V","C"})c.writeRegister(r,0);
    c.writeRegister("SP",0x81004000L);c.writeRegister("FP",0x12345678);c.writeRegister("RP",0x80000000L);c.writeRegister("PC",0x40a56);
    put(c,0x68000110,0x1234);put(c,0x68000114,0xffffffff);
    for(long s:new long[]{head,ready,0x68000120L,0x68000128L}){put(c,s,(int)s);put(c,s+4,(int)s);}
    put(c,0x68000004,0);c.writeMemory(toAddr(0x68000132),new byte[]{(byte)0xa5});c.writeMemory(toAddr(0x44),new byte[]{0x55});c.writeMemory(toAddr(0x46f),new byte[]{0x55});
    long[] nodes=new long[initial[test].length];for(int i=0;i<nodes.length;i++)nodes[i]=0x81001000L+0x100*i;
    for(int i=0;i<nodes.length;i++){
     put(c,nodes[i],(int)(i+1<nodes.length?nodes[i+1]:head));put(c,nodes[i]+4,(int)(i>0?nodes[i-1]:head));
     put(c,nodes[i]+8,0x20004);c.writeMemory(toAddr(nodes[i]+12),new byte[]{0,2});put(c,nodes[i]+20,initial[test][i]);put(c,nodes[i]+32,0x12345555);
    }
    if(nodes.length>0){put(c,head,(int)nodes[0]);put(c,head+4,(int)nodes[nodes.length-1]);}
    int inserts=0,n=0;while(c.getExecutionAddress().getOffset()!=0x80000000L && n++<600){
     if(c.getExecutionAddress().getOffset()==0x4186e)inserts++;
     if(!c.step(monitor))throw new IllegalStateException(c.getLastError());
    }
    check(c.getExecutionAddress().getOffset()==0x80000000L && inserts==removed[test],"Return/insertion count");
    check(word(c,0x68000110)==0x1235 && word(c,0x68000114)==0,"64-bit counter carry");
    for(int i=0;i<nodes.length;i++){check(word(c,nodes[i]+20)==finalValues[test][i],"Final delta");check(word(c,nodes[i]+8)==(i<removed[test]?0:0x20004),"Node state");check(word(c,nodes[i]+32)==0x12345555,"Metadata");}
    for(int list=0;list<2;list++){
     long sentinel=list==0?head:ready;int first=list==0?removed[test]:0,last=list==0?nodes.length:removed[test];
     long prev=sentinel;
     for(int i=first;i<last;i++){check(word(c,prev)==(int)nodes[i] && word(c,nodes[i]+4)==(int)prev,"List forward/previous");prev=nodes[i];}
     check(word(c,prev)==(int)sentinel && word(c,sentinel+4)==(int)prev,"List tail");
    }
    check(word(c,0x68000004)==(removed[test]>0?0x40000000:0),"Bitmap");
    check(c.readRegister("SP").longValue()==0x81004000L && c.readRegister("FP").intValue()==0x12345678 && c.readRegister("RP").longValue()==0x80000000L,"Saved context");
    println("initial="+Arrays.toString(initial[test])+" final="+Arrays.toString(finalValues[test])+" moved="+removed[test]+" counter carry verified");
   }finally{c.dispose();}
  }
  println("Six complete 0x40a56 routine scenarios passed; secondary lists empty, no hardware timer modeled");
 }
}
