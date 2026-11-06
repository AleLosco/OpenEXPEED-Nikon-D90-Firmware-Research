// AB-89: original dispatcher/service included; explicit INT and post-RETI SSP adapters only.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.model.lang.*;
import ghidra.program.model.pcode.Varnode;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.nio.ByteBuffer;
public class D90QueueInterruptIntegrated extends D90SupervisorStack {
 int inspected=0;
 java.util.Set<Long> visited=new java.util.HashSet<>();
 void inspect(Varnode v){if(v!=null && v.isRegister())require(v.getOffset()+v.getSize()<=8 || v.getOffset()>=16,"Dedicated SSP/USP accessed");}
 void until(EmulatorHelper c,PseudoDisassembler d,long stop)throws Exception{
  int n=0;while(reg(c,"PC")!=stop && n++<400){
   visited.add(reg(c,"PC"));var i=d.disassemble(c.getExecutionAddress());require(i!=null,"Decode failure");
   for(var op:i.getPcode()){inspect(op.getOutput());for(var input:op.getInputs())inspect(input);}
   inspected++;require(c.step(monitor),"Step: "+c.getLastError());
  }
  require(reg(c,"PC")==stop,"Instruction limit");
 }
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language lang=provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"));
  require(currentProgram.getMemory().getInt(toAddr(0xdfefc))==0x4067a,"Original vector");
  var p=new ProgramDB("AB92-service-queue-interrupt",lang,lang.getDefaultCompilerSpec(),this);
  try{
   var a=lang.getDefaultSpace();int tx=p.startTransaction("Copy original instruction windows");
   try{
    for(long[] w:new long[][]{{0x4048a,0x404ae},{0x404fe,0x40500},{0x40a56,0x40bec},{0x4186e,0x418a0},{0xdff9c,0xdffa0},{0x404f4,0x404fa},{0x405c8,0x405ca},{0x4067a,0x406f6},{0x40bec,0x40c26},{0x40f2c,0x40f82},{0x418a0,0x418d4},{0x19e064,0x19e066},{0xdfefc,0xdff00}}){
     byte[] b=new byte[(int)(w[1]-w[0])];currentProgram.getMemory().getBytes(toAddr(w[0]),b);
     p.getMemory().createInitializedBlock("original_"+Long.toHexString(w[0]),a.getAddress(w[0]),b.length,(byte)0,monitor,false);
     p.getMemory().setBytes(a.getAddress(w[0]),b);
    }
    p.getMemory().createInitializedBlock("stack",a.getAddress(0x81001000L),0x8000,(byte)0,monitor,false);
    p.getMemory().createInitializedBlock("synthetic_peripheral",a.getAddress(0x40),0x440,(byte)0,monitor,false);
    p.getMemory().createInitializedBlock("globals",a.getAddress(0x68000000),0x200,(byte)0,monitor,false);
   }finally{p.endTransaction(tx,true);}
   var d=new PseudoDisassembler(p);int cases=0;
   for(int enabled:new int[]{0,1})for(int scenario=0;scenario<4;scenario++){
    int ilm=31,selector=-85;int[][] values={{},{5},{1,5},{1}};int[] deltas=values[scenario];
    int position=new int[]{0,0,1,1}[scenario],residual=new int[]{2,2,1,1}[scenario],successor=new int[]{0,3,4,0}[scenario];
    long head=0x68000118L,task=0x81002000L,source=0x81002100L;
    var c=new EmulatorHelper(p);
    try{
     for(int i=0;i<12;i++)c.writeRegister("R"+i,0x11000000+i);
     c.writeRegister("R4",2);c.writeRegister("R12",selector);String[] special={"AC","FP","RP","MDL","MDH"};
     for(int i=0;i<5;i++)c.writeRegister(special[i],0x22000000+i);
     for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM"})c.writeRegister(f,0);
     c.writeRegister("ILM",ilm);c.writeRegister("I",enabled);c.writeRegister("SP",0x81004000L);c.writeRegister("SSP",0x81004000L);c.writeRegister("TBR",0xdfc00);c.writeRegister("PC",0x19e064);
     put(c,a,0x81003fa4L,0x11223344);put(c,a,0x81004000L,0x55667788);c.writeMemory(a.getAddress(0x46f),new byte[]{0x55});
     put(c,a,0x68000110,0);put(c,a,0x68000114,0);
     for(long sentinel:new long[]{0x68000010L,0x68000120L,0x68000128L}){put(c,a,sentinel,(int)sentinel);put(c,a,sentinel+4,(int)sentinel);}
     put(c,a,0x68000000, (int)task);put(c,a,0x68000004,0xffffffff);
     put(c,a,source,(int)task);put(c,a,source+4,(int)task);put(c,a,task,(int)source);put(c,a,task+4,(int)source);
     c.writeMemory(a.getAddress(task+12),new byte[]{0,2});put(c,a,task+8,0);put(c,a,task+20,99);put(c,a,task+32,0x55556666);
     c.writeMemory(a.getAddress(0x68000132),new byte[]{(byte)0xa5});c.writeMemory(a.getAddress(0x44),new byte[]{0x55});
     long[] nodes=new long[deltas.length+1];nodes[0]=head;
     for(int i=0;i<deltas.length;i++){nodes[i+1]=0x81006000L+0x100*i;put(c,a,nodes[i+1]+20,deltas[i]);put(c,a,nodes[i+1]+8,0x20004);c.writeMemory(a.getAddress(nodes[i+1]+12),new byte[]{0,2});}
     for(int i=0;i<nodes.length;i++){put(c,a,nodes[i],(int)nodes[(i+1)%nodes.length]);put(c,a,nodes[i]+4,(int)nodes[(i+nodes.length-1)%nodes.length]);}
     visited.clear();int ps=(ilm<<16)|(enabled<<4);enter(c,a,false);
     require(reg(c,"PC")==0x4067a,"Original vector target");until(c,d,0x406b6);
     long frame=0x81003fb0L;int saved=(ilm&15)==15?selector&0x7fffffff:selector;
     require(reg(c,"SP")==frame && reg(c,"FP")==frame,"Generated frame base");
     ByteBuffer b=ByteBuffer.wrap(c.readMemory(a.getAddress(frame),72));
     for(int i=0;i<12;i++)require(b.getInt()==(i==4?2:0x11000000+i),"Saved R"+i);
     require(b.getInt()==saved,"Saved selector");for(int i=0;i<5;i++)require(b.getInt()==0x22000000+i,"Saved special");
     require(word(c,a,frame+72)==0x19e066 && word(c,a,frame+76)==ps,"INT frame retained");
     require(reg(c,"ILM")==((ilm&15)==15?30:ilm),"Prefix ILM");
     // No state injection or service bypass after INT entry.
     until(c,d,0x406f4);
     for(long site:new long[]{0x406b6,0x40f2c,0x40bec,0x418a0,0x406c6})require(visited.contains(site),"Missing service site "+Long.toHexString(site));
     long[] expected=new long[nodes.length+1];expected[0]=head;int j=1;
     for(int i=0;i<=deltas.length;i++){if(i==position)expected[j++]=task;if(i<deltas.length)expected[j++]=nodes[i+1];}
     for(int i=0;i<expected.length;i++)require(Integer.toUnsignedLong(word(c,a,expected[i]))==expected[(i+1)%expected.length] && Integer.toUnsignedLong(word(c,a,expected[i]+4))==expected[(i+expected.length-1)%expected.length],"Queue links");
     for(int i=0;i<deltas.length;i++)require(word(c,a,nodes[i+1]+20)==(i==position?successor:deltas[i]),"Queue deltas");
     require(word(c,a,source)==(int)source && word(c,a,source+4)==(int)source,"Source removed");
     require(word(c,a,task+20)==residual && word(c,a,task+8)==0x20004 && word(c,a,task+32)==0xffab6666,"Task fields from generated frame");
     require(word(c,a,0x68000004)==0xbfffffff && (c.readMemory(a.getAddress(0x68000132),1)[0]&255)==0xb5 && c.readMemory(a.getAddress(0x44),1)[0]==1,"Global effects");
     require(reg(c,"SP")==0x81003ff8L && reg(c,"SSP")==0x81003ff8L,"RETI stack boundary");
     returnSupervisor(c,a);
     require(reg(c,"PC")==0x19e066 && reg(c,"SP")==0x81004000L && reg(c,"SSP")==0x81004000L,"Caller return");
     require(reg(c,"PS")==ps && reg(c,"ILM")==ilm && reg(c,"I")==enabled && reg(c,"S")==0,"Restored status");
     for(int i=0;i<12;i++)require(c.readRegister("R"+i).intValue()==(i==4?2:0x11000000+i),"Restored R"+i);
     for(int i=0;i<5;i++)require(c.readRegister(special[i]).intValue()==0x22000000+i,"Restored special");
     require(c.readRegister("R12").intValue()==0,"Signed result");
     require((c.readMemory(a.getAddress(0x46f),1)[0]&255)==(saved<0?0x1e:0x10),"Peripheral RAM byte");
     require(word(c,a,0x81003fa4L)==0x11223344 && word(c,a,0x81004000L)==0x55667788,"Stack sentinels");
     require(word(c,a,0xdff9c)==0x4048a,"Queue interrupt vector");
     java.util.List<Long> waiting=new java.util.ArrayList<>(),runnable=new java.util.ArrayList<>();
     java.util.Map<Long,Integer> delta=new java.util.HashMap<>();
     for(int i=1;i<expected.length;i++){waiting.add(expected[i]);delta.put(expected[i],word(c,a,expected[i]+20));}
     String[] savedRegs={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP","RP","MDL","MDH"};
     long[] before=new long[savedRegs.length];for(int i=0;i<before.length;i++)before[i]=reg(c,savedRegs[i]);
     int calls=0;
     while(!runnable.contains(task) && calls++<8){
      // Synthetic external IRQ entry, not hardware acceptance or the INT adapter.
      require(reg(c,"SP")==0x81004000L && reg(c,"SSP")==0x81004000L,"IRQ stack base");
      put(c,a,0x81003ffcL,ps);put(c,a,0x81003ff8L,0x19e066);
      c.writeRegister("SP",0x81003ff8L);c.writeRegister("SSP",0x81003ff8L);c.writeRegister("I",0);c.writeRegister("S",0);c.writeRegister("PS",ps&~0x30);c.writeRegister("PC",Integer.toUnsignedLong(word(c,a,0xdff9c)));
      visited.clear();until(c,d,0x404aa);require(visited.contains(0x4049eL) && visited.contains(0x40a56L),"IRQ wrapper call path");returnSupervisor(c,a);
      require(java.nio.ByteBuffer.wrap(c.readMemory(a.getAddress(0x4e),2)).getShort()==0x1a,"Wrapper halfword in synthetic RAM");
      while(!waiting.isEmpty()){
       long first=waiting.get(0);int value=delta.get(first)-1;delta.put(first,value);
       if(value>=0)break;waiting.remove(0);runnable.add(first);
      }
      for(int list=0;list<2;list++){
       long sentinel=list==0?head:0x68000010L;var order=list==0?waiting:runnable;long previous=sentinel;
       for(long node:order){require(Integer.toUnsignedLong(word(c,a,previous))==node && Integer.toUnsignedLong(word(c,a,node+4))==previous,"IRQ list links");previous=node;}
       require(Integer.toUnsignedLong(word(c,a,previous))==sentinel && Integer.toUnsignedLong(word(c,a,sentinel+4))==previous,"IRQ list tail");
      }
      for(long node:delta.keySet())require(word(c,a,node+20)==delta.get(node) && word(c,a,node+8)==(runnable.contains(node)?0:0x20004),"IRQ node state");
      require(word(c,a,0x68000110)==0 && word(c,a,0x68000114)==calls,"IRQ counter");
      for(int i=0;i<before.length;i++)require(reg(c,savedRegs[i])==before[i],"IRQ restored "+savedRegs[i]);
      require(reg(c,"PC")==0x19e066 && reg(c,"SP")==0x81004000L && reg(c,"SSP")==0x81004000L && reg(c,"PS")==ps && reg(c,"ILM")==31 && reg(c,"I")==enabled,"IRQ restored caller state");
      require(word(c,a,task+32)==0xffab6666,"IRQ preserves service metadata");
     }
     require(runnable.contains(task),"Task not transferred within bound");
     println("scenario="+scenario+" I="+enabled+" transferred service node after "+calls+" synthetic vector-18 entries");cases++;
    }finally{c.dispose();}
   }
   println(cases+" service-to-vector-18 scenarios passed; no list reset between service and IRQ entries");
   println(inspected+" stepped instruction instances checked: no direct dedicated SSP/USP p-code access");
   println("AB-92 passed; synthetic IRQ entries and explicit SSP synchronization, no physical timing or task execution");
  }finally{p.release(this);}
 }
}
