// AB-87: explicit SP/SSP synchronization at supervisor RETI boundary only.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.program.model.lang.*;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
public class D90SupervisorStack extends D90Int40Adapter {
 // Not a general alias implementation. Nested interrupts and S=1 remain unsupported.
 public void returnSupervisor(EmulatorHelper c,AddressSpace a)throws Exception{
  long pc=reg(c,"PC"),sp=reg(c,"SP");
  require(reg(c,"S")==0 && sp==reg(c,"SSP") && (sp&3)==0 && sp<=0xfffffff7L,"Supervisor return precondition");
  require(Arrays.equals(c.readMemory(a.getAddress(pc),2),new byte[]{(byte)0x97,0x30}),"Expected RETI boundary");
  int ps=word(c,a,sp+4);long target=Integer.toUnsignedLong(word(c,a,sp));
  require((ps&0x20)==0 && (target&1)==0,"Unsupported return state");
  require(c.step(monitor),"RETI step: "+c.getLastError());
  require(reg(c,"SP")==sp+8 && reg(c,"PC")==target && reg(c,"S")==0 && reg(c,"PS")==Integer.toUnsignedLong(ps),"RETI effects");
  // Explicit harness effect, after validating the modeled instruction's effects.
  c.writeRegister("SSP",sp+8);
 }
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language lang=provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"));
  var p=new ProgramDB("AB87-synthetic",lang,lang.getDefaultCompilerSpec(),this);
  try{
   var a=lang.getDefaultSpace();int tx=p.startTransaction("Synthetic memory");
   try{
    p.getMemory().createInitializedBlock("code",a.getAddress(0x80000000L),0x1000,(byte)0,monitor,false);
    p.getMemory().createInitializedBlock("vectors",a.getAddress(0xdfc00),0x400,(byte)0,monitor,false);
    p.getMemory().createInitializedBlock("stack",a.getAddress(0x81003000L),0x2000,(byte)0,monitor,false);
   }finally{p.endTransaction(tx,true);}
   int pairs=0;
   for(int ilm:new int[]{0,15,16,30,31})for(int enabled:new int[]{0,1})for(long target:new long[]{0x80000100L,0x80000200L}){
    var c=new EmulatorHelper(p);
    try{
     for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM"})c.writeRegister(f,0);
     c.writeRegister("ILM",ilm);c.writeRegister("I",enabled);c.writeRegister("RP",0x12345678);
     c.writeRegister("SP",0x81004000L);c.writeRegister("SSP",0x81004000L);c.writeRegister("TBR",0xdfc00);c.writeRegister("PC",0x80000000L);
     c.writeMemory(a.getAddress(0x80000000L),new byte[]{0x1f,0x40,0x1f,0x40});
     c.writeMemory(a.getAddress(target),new byte[]{(byte)0x97,0x30});put(c,a,0xdfefc,(int)target);
     put(c,a,0x81003ff4L,0x11223344);put(c,a,0x81004000L,0x55667788);
     int ps=(ilm<<16)|(enabled<<4);
     for(int cycle=0;cycle<2;cycle++){
      require(reg(c,"PC")==0x80000000L+2*cycle,"Consecutive INT site");
      enter(c,a,false);
      require(word(c,a,0x81003ff8L)==(int)(0x80000002L+2*cycle) && word(c,a,0x81003ffcL)==ps,"Reused frame");
      returnSupervisor(c,a);
      require(reg(c,"SP")==0x81004000L && reg(c,"SSP")==0x81004000L && reg(c,"ILM")==ilm && reg(c,"I")==enabled,"Synchronized return");
      require(reg(c,"RP")==0x12345678 && word(c,a,0x81003ff4L)==0x11223344 && word(c,a,0x81004000L)==0x55667788,"Sentinels/RP");
     }
     require(reg(c,"PC")==0x80000004L,"Final PC");pairs++;
    }finally{c.dispose();}
   }
   println(pairs+" consecutive INT pairs passed: 40 explicit RETI-boundary SSP synchronizations");
   int rejects=0;
   for(int invalid=0;invalid<3;invalid++){
    var c=new EmulatorHelper(p);
    try{
     c.writeRegister("PC",0x80000100L);c.writeRegister("SP",0x81003ff8L);c.writeRegister("SSP",0x81003ff8L);c.writeRegister("S",0);
     c.writeMemory(a.getAddress(0x80000100L),new byte[]{(byte)0x97,0x30});put(c,a,0x81003ff8L,0x80000002);put(c,a,0x81003ffcL,0);
     if(invalid==0)put(c,a,0x81003ffcL,0x20);
     if(invalid==1)c.writeRegister("SSP",0x81004000L);
     if(invalid==2)c.writeMemory(a.getAddress(0x80000100L),new byte[]{0x1f,0x40});
     long oldSsp=reg(c,"SSP");byte[] frame=c.readMemory(a.getAddress(0x81003ff8L),8);boolean rejected=false;
     try{returnSupervisor(c,a);}catch(IllegalStateException expected){rejected=true;}
     require(rejected && reg(c,"PC")==0x80000100L && reg(c,"SP")==0x81003ff8L && reg(c,"SSP")==oldSsp && Arrays.equals(frame,c.readMemory(a.getAddress(0x81003ff8L),8)),"Rejection mutated return");rejects++;
    }finally{c.dispose();}
   }
   println(rejects+" unsupported return inputs rejected before stepping/synchronizing");
   println("AB-87 passed; supervisor boundary convention only, no global SP/SSP alias or firmware execution");
  }finally{p.release(this);}
 }
}
