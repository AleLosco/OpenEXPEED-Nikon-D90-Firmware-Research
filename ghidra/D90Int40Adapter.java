// AB-86: explicit Java boundary adapter, not execution of the stock INT p-code.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.program.model.lang.*;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class D90Int40Adapter extends GhidraScript {
 static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static long reg(EmulatorHelper c,String n){return c.readRegister(n).longValue()&0xffffffffL;}
 static int word(EmulatorHelper c,AddressSpace a,long p){return ByteBuffer.wrap(c.readMemory(a.getAddress(p),4)).getInt();}
 static void put(EmulatorHelper c,AddressSpace a,long p,int v){c.writeMemory(a.getAddress(p),ByteBuffer.allocate(4).putInt(v).array());}
 // Caller must supply delay-slot status: this adapter does not infer pipeline state.
 public static void enter(EmulatorHelper c,AddressSpace a,boolean delaySlot){
  long pc=reg(c,"PC"),sp=reg(c,"SP"),tbr=reg(c,"TBR");
  require((pc&1)==0,"Unaligned PC");
  require(!delaySlot,"Delay slot unsupported");require(reg(c,"S")==0,"User stack unsupported");
  require(sp==reg(c,"SSP") && (sp&3)==0 && sp>=8,"SP/SSP precondition");
  require((tbr&0x3ff)==0 && tbr<=0xfffffc00L,"TBR precondition");
  require(Arrays.equals(c.readMemory(a.getAddress(pc),2),new byte[]{0x1f,0x40}),"Only INT 0x40 supported");
  long target=Integer.toUnsignedLong(word(c,a,tbr+0x2fc));
  require(target!=0 && (target&1)==0,"Invalid vector target");
  int ps=(int)reg(c,"PS") & ~0x001f073f;
  String[] flags={"C","V","Z","N","I","S","T","D0","D1"};int[] bits={0,1,2,3,4,5,8,9,10};
  for(int i=0;i<bits.length;i++)ps|=((int)reg(c,flags[i])&1)<<bits[i];
  ps|=((int)reg(c,"ILM")&31)<<16;
  put(c,a,sp-4,ps);put(c,a,sp-8,(int)(pc+2));
  c.writeRegister("SP",sp-8);c.writeRegister("SSP",sp-8);
  c.writeRegister("I",0);c.writeRegister("S",0);c.writeRegister("PS",ps&~0x30);
  c.writeRegister("PC",target);
 }
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language lang=provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"));
  var p=new ProgramDB("AB86-synthetic",lang,lang.getDefaultCompilerSpec(),this);
  try{
   var a=lang.getDefaultSpace();int tx=p.startTransaction("Synthetic memory");
   try{
    p.getMemory().createInitializedBlock("code",a.getAddress(0x80000000L),0x1000,(byte)0,monitor,false);
    p.getMemory().createInitializedBlock("vectors",a.getAddress(0xdfc00),0x400,(byte)0,monitor,false);
    p.getMemory().createInitializedBlock("stack",a.getAddress(0x81003000L),0x2000,(byte)0,monitor,false);
   }finally{p.endTransaction(tx,true);}
   int passed=0;
   for(int ilm:new int[]{0,15,16,30,31})for(int enabled:new int[]{0,1})for(long target:new long[]{0x80000100L,0x80000200L}){
    var c=new EmulatorHelper(p);
    try{
     for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM"})c.writeRegister(f,0);
     c.writeRegister("ILM",ilm);c.writeRegister("I",enabled);c.writeRegister("RP",0x12345678);
     c.writeRegister("SP",0x81004000L);c.writeRegister("SSP",0x81004000L);c.writeRegister("TBR",0xdfc00);c.writeRegister("PC",0x80000000L);
     c.writeMemory(a.getAddress(0x80000000L),new byte[]{0x1f,0x40});
     c.writeMemory(a.getAddress(target),new byte[]{(byte)0x97,0x30});put(c,a,0xdfefc,(int)target);
     put(c,a,0x81003ff4L,0x11223344);put(c,a,0x81004000L,0x55667788);
     int ps=(ilm<<16)|(enabled<<4);
     enter(c,a,false);
     require(reg(c,"PC")==target && reg(c,"SP")==0x81003ff8L && reg(c,"SSP")==0x81003ff8L,"Entry boundary");
     require(word(c,a,0x81003ff8L)==0x80000002 && word(c,a,0x81003ffcL)==ps,"Frame");
     require(reg(c,"I")==0 && reg(c,"S")==0 && reg(c,"PS")==((long)ilm<<16) && reg(c,"RP")==0x12345678,"Entry registers");
     require(c.step(monitor),"RETI failed");
     require(reg(c,"PC")==0x80000002L && reg(c,"SP")==0x81004000L && reg(c,"PS")==ps && reg(c,"ILM")==ilm && reg(c,"I")==enabled,"Return state");
     require(word(c,a,0x81003ff4L)==0x11223344 && word(c,a,0x81004000L)==0x55667788 && reg(c,"RP")==0x12345678,"Sentinels/RP");
     // Record the model's separate SP/SSP registers; do not silently repair the alias.
     require(reg(c,"SSP")==0x81003ff8L,"Re-evaluate SP/SSP model");
     passed++;
    }finally{c.dispose();}
   }
   println(passed+" adapter entry/original RETI cases passed; SP restored, SSP remains separate/stale");
   int rejected=0;
   for(int invalid=0;invalid<6;invalid++){
    var c=new EmulatorHelper(p);
    try{
     for(String f:new String[]{"PS","ILM","I","C","V","Z","N","T","D0","D1","RP"})c.writeRegister(f,0);
     c.writeRegister("PC",0x80000000L);c.writeRegister("SP",0x81004000L);c.writeRegister("SSP",0x81004000L);c.writeRegister("S",0);c.writeRegister("TBR",0xdfc00);
     c.writeMemory(a.getAddress(0x80000000L),new byte[]{0x1f,0x40});put(c,a,0xdfefc,0x80000100);put(c,a,0x81003ff8L,0x11223344);
     if(invalid==0)c.writeRegister("S",1);
     if(invalid==1)c.writeRegister("SSP",0x81004004L);
     if(invalid==2)c.writeMemory(a.getAddress(0x80000000L),new byte[]{0x1f,0x41});
     if(invalid==3)put(c,a,0xdfefc,0x80000101);
     if(invalid==5){
      c.writeRegister("PC",0x80000001L);
      c.writeMemory(a.getAddress(0x80000001L),new byte[]{0x1f,0x40});
     }
     put(c,a,0x81003ffcL,0x55667788);
     String[] watched={"PC","SP","SSP","TBR","PS","ILM","I","S","C","V","Z","N","T","D0","D1","RP"};
     long[] before=new long[watched.length];
     for(int j=0;j<watched.length;j++)before[j]=reg(c,watched[j]);
     byte[] frameBefore=c.readMemory(a.getAddress(0x81003ff4L),16);
     boolean rejectedCase=false;
     try{enter(c,a,invalid==4);}catch(IllegalStateException expected){rejectedCase=true;}
     require(rejectedCase,"Unsupported input accepted: "+invalid);
     for(int j=0;j<watched.length;j++)require(reg(c,watched[j])==before[j],"Rejection changed "+watched[j]);
     require(Arrays.equals(frameBefore,c.readMemory(a.getAddress(0x81003ff4L),16)),"Rejection changed frame/guards");
     rejected++;
    }finally{c.dispose();}
   }
   println(rejected+" unsupported inputs rejected before frame/PC/SP mutation");
   println("AB-86 adapter probes passed; INT replaced explicitly; no firmware service or scheduler executed");
  }finally{p.release(this);}
 }
}
