// AB-97: initialization call returns after explicit INT/service/RETI and original tail callees.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.model.lang.*;
import ghidra.program.model.pcode.Varnode;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
public class D90SourceInitializationReturn extends D90SupervisorStack {
 HashSet<Long> seen=new HashSet<>();int inspected=0;
 void inspect(Varnode v){if(v!=null && v.isRegister())require(v.getOffset()+v.getSize()<=8 || v.getOffset()>=16,"Direct SSP/USP access");}
 int stepLimit(){return 1200;}
 long startAddress(){return 0x40738;}
 int counterBeforeService(boolean high){return high?0x11112222:0x33334444;}
 void until(EmulatorHelper c,PseudoDisassembler d,long stop)throws Exception{
  int n=0;while(reg(c,"PC")!=stop && n++<stepLimit()){
   seen.add(reg(c,"PC"));var i=d.disassemble(c.getExecutionAddress());require(i!=null,"Decode");
   for(var op:i.getPcode()){inspect(op.getOutput());for(var v:op.getInputs())inspect(v);}
   inspected++;require(c.step(monitor),"Step: "+c.getLastError());
  }
  require(reg(c,"PC")==stop,"Step bound");
 }
 void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{}
 void beforeInitialization(EmulatorHelper c,ProgramDB p,int pattern)throws Exception{}
 long[][] extraSyntheticWindows(){return new long[0][];}
 long[][] extraWindows(){return new long[0][];}
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language lang=provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"));
  require(currentProgram.getMemory().getInt(toAddr(0xdfefc))==0x4067a && currentProgram.getMemory().getShort(toAddr(0x405cc))==0x87e,"Original vector/table");
  var p=new ProgramDB("AB97-source-return",lang,lang.getDefaultCompilerSpec(),this);
  try{
   var a=lang.getDefaultSpace();int tx=p.startTransaction("Original windows and synthetic memory");
   try{
    for(long[] w:new long[][]{{0x40738,0x40740},{0x4046a,0x4048a},{0x100936,0x100fc4},{0x404f4,0x404fa},{0x405cc,0x405ce},{0x4067a,0x406f6},{0x40ef0,0x40f0e},{0xdfefc,0xdff00}}){
     byte[] b=new byte[(int)(w[1]-w[0])];currentProgram.getMemory().getBytes(toAddr(w[0]),b);
     p.getMemory().createInitializedBlock("original_"+Long.toHexString(w[0]),a.getAddress(w[0]),b.length,(byte)0,monitor,false);p.getMemory().setBytes(a.getAddress(w[0]),b);
    }
    for(long[] w:extraWindows()){
     byte[] b=new byte[(int)(w[1]-w[0])];currentProgram.getMemory().getBytes(toAddr(w[0]),b);
     p.getMemory().createInitializedBlock("extra_"+Long.toHexString(w[0]),a.getAddress(w[0]),b.length,(byte)0,monitor,false);
     p.getMemory().setBytes(a.getAddress(w[0]),b);
    }
    for(long[] w:new long[][]{{0x40000000L,0x40200000L},{0x50000000L,0x50000100L},{0x6f000000L,0x6f010000L},{0,0x1000},{0x6b000000L,0x6b000100L},{0x81003000L,0x81005000L},{0x68000110L,0x68000118L}})
     p.getMemory().createInitializedBlock("synthetic_"+Long.toHexString(w[0]),a.getAddress(w[0]),w[1]-w[0],(byte)0,monitor,false);
    for(long[] w:extraSyntheticWindows())
     p.getMemory().createInitializedBlock("extra_ram_"+Long.toHexString(w[0]),a.getAddress(w[0]),w[1]-w[0],(byte)0,monitor,false);
   }finally{p.endTransaction(tx,true);}
   int cases=0;var d=new PseudoDisassembler(p);
   for(int seed:new int[]{0,255})for(int stackSeed:new int[]{0x5a,0xa5})for(int ilm:new int[]{0,31}){
    var c=new EmulatorHelper(p);
    try{
     for(int i=0;i<13;i++)c.writeRegister("R"+i,0);
     for(String r:new String[]{"AC","FP","RP","MDL","MDH","PS","S","I","N","Z","V","C","T","D0","D1","ILM"})c.writeRegister(r,0);
     c.writeRegister("ILM",ilm);c.writeRegister("PC",startAddress());c.writeRegister("SP",0x81004000L);c.writeRegister("SSP",0x81004000L);c.writeRegister("TBR",0xdfc00);
     byte[] low=new byte[0x1000],high=new byte[0x100],stack=new byte[0x2000];Arrays.fill(low,(byte)seed);Arrays.fill(high,(byte)seed);Arrays.fill(stack,(byte)stackSeed);
     c.writeMemory(a.getAddress(0),low);c.writeMemory(a.getAddress(0x6b000000),high);c.writeMemory(a.getAddress(0x81003000L),stack);
     byte[] bank=new byte[0x200000],small=new byte[0x100],aux=new byte[0x10000];Arrays.fill(bank,(byte)seed);Arrays.fill(small,(byte)seed);Arrays.fill(aux,(byte)seed);
     c.writeMemory(a.getAddress(0x40000000L),bank);c.writeMemory(a.getAddress(0x50000000L),small);c.writeMemory(a.getAddress(0x6f000000L),aux);
     put(c,a,0x68000110,0x11112222);put(c,a,0x68000114,0x33334444);
     beforeInitialization(c,p,stackSeed);
     seen.clear();until(c,d,0x10094e);
     require(c.readRegister("R12").intValue()==-83 && reg(c,"SP")==0x81003fecL && reg(c,"R4")==0x81003fecL,"Prefix service arguments");
     require(word(c,a,0x68000110)==counterBeforeService(true) && word(c,a,0x68000114)==counterBeforeService(false),"Counter sentinel before service");
     byte[] descriptor=c.readMemory(a.getAddress(0x81003fecL),8);
     require(Arrays.equals(descriptor,new byte[]{(byte)stackSeed,(byte)stackSeed,0,0,0,0,0,0}),"Prefix descriptor");
     String[] regs={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","AC","FP","RP","MDL","MDH"};
     long[] before=new long[regs.length];for(int i=0;i<regs.length;i++)before[i]=reg(c,regs[i]);
     // Explicit SP/SSP boundary synchronization before entering the INT adapter.
     c.writeRegister("SSP",reg(c,"SP"));enter(c,a,false);
     int savedPs=word(c,a,0x81003fe8L);
     until(c,d,0x406f4);returnSupervisor(c,a);
     for(long site:new long[]{0x100936,0x10095e,0x100cec,0x100d18,0x406b6,0x40ef0,0x406c6})require(seen.contains(site),"Missing path "+Long.toHexString(site));
     require(reg(c,"PC")==0x100950 && reg(c,"SP")==0x81003fecL && reg(c,"SSP")==0x81003fecL && c.readRegister("R12").intValue()==0,"Service return");
     require(reg(c,"PS")==Integer.toUnsignedLong(savedPs) && reg(c,"ILM")==ilm,"Status restore");
     for(int i=0;i<regs.length;i++)require(reg(c,regs[i])==before[i],"Restored "+regs[i]);
     require(word(c,a,0x68000110)==0 && word(c,a,0x68000114)==0,"Counter reset");
     require(Arrays.equals(descriptor,c.readMemory(a.getAddress(0x81003fecL),8)),"Descriptor preserved");
     until(c,d,0x40740);
     for(long site:new long[]{0x100d24,0x100d3e,0x100f00,0x100958,0x40486})require(seen.contains(site),"Missing tail site");
     require(reg(c,"SP")==0x81004000L && reg(c,"RP")==0x40740 && reg(c,"FP")==0x440,"Initialization return context");
     require(reg(c,"SSP")==0x81003fecL,"SSP remains separate after ordinary stack instructions");
     require(word(c,a,0x68000110)==0 && word(c,a,0x68000114)==0,"Counter retained");
     int half=seed==0?0:0xffff,next=seed==0?0:0xffff;
     require(Short.toUnsignedInt(java.nio.ByteBuffer.wrap(c.readMemory(a.getAddress(0x50000008),2)).getShort())==(half|0x4000),"50000008 OR");
     require(word(c,a,0x5000000c)==(0x53000000|next),"5000000c width");
     for(long addr:new long[]{0x40080002L,0x40080006L})require(Short.toUnsignedInt(java.nio.ByteBuffer.wrap(c.readMemory(a.getAddress(addr),2)).getShort())==((half&0xff0f)|0x30),"800xx RMW");
     for(long addr:new long[]{0x40090000L,0x400a0000L,0x40050000L,0x4003040aL,0x4013040aL,0x40020000L,0x400f0000L,0x400b0000L,0x40040000L,0x400e0000L,0x400c0000L,0x40010000L,0x4011a000L})require(word(c,a,addr)==next,"Halfword clear width");
     for(long addr:new long[]{0x6f004008L,0x6f00c008L,0x4011c000L})require(word(c,a,addr)==0,"Word clear");
     require(Short.toUnsignedInt(java.nio.ByteBuffer.wrap(c.readMemory(a.getAddress(0x40040800),2)).getShort())==0x8000,"40040800");
     require(Short.toUnsignedInt(java.nio.ByteBuffer.wrap(c.readMemory(a.getAddress(0x400c0004),2)).getShort())==0xc000,"400c0004");
     for(long addr:new long[]{0x4000d240L,0x4000d340L,0x4000d440L,0x4000d540L})require(Short.toUnsignedInt(java.nio.ByteBuffer.wrap(c.readMemory(a.getAddress(addr),2)).getShort())==8,"Channel halfword");
     require(c.readMemory(a.getAddress(0x4001ff00),2)[0]==0 && c.readMemory(a.getAddress(0x4001ff00),2)[1]==0,"Byte clears");
     afterInitialization(c,d,p);
     println("seed="+seed+" stack="+stackSeed+" ILM="+ilm+": initialization returned to 0x40740; tail writes verified; raw SSP remains 0x81003fec");cases++;
    }finally{c.dispose();}
   }
   println(cases+" complete initialization-call scenarios passed; "+inspected+" steps checked for direct SSP/USP accesses");
   println("AB-97 passed; original calls through 0x100f00 returned; RAM peripherals and explicit INT/SSP boundaries");
  }finally{p.release(this);}
 }
}
