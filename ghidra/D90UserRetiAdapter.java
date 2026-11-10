// AB-112: explicit user RETI boundary adapter on the isolated ILM profile.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.program.model.lang.*;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
public class D90UserRetiAdapter extends D90SupervisorStack {
 public static void returnUser(EmulatorHelper c,ghidra.program.model.address.AddressSpace a,ghidra.util.task.TaskMonitor monitor,boolean delaySlot)throws Exception{
  long pc=reg(c,"PC"),sp=reg(c,"SP"),user=reg(c,"USP");
  require(!delaySlot,"Delay slot unsupported");require(pc==0x407ea,"Only startup RETI supported");
  require(reg(c,"S")==0 && sp==reg(c,"SSP"),"Supervisor entry and synchronized SSP required");
  require((sp&3)==0 && sp<=0xfffffff7L && (user&3)==0,"Stack alignment/range");
  require(Arrays.equals(c.readMemory(a.getAddress(pc),2),new byte[]{(byte)0x97,0x30}),"Original RETI required");
  long target=Integer.toUnsignedLong(word(c,a,sp));int ps=word(c,a,sp+4);
  require(target!=0 && (target&1)==0 && (ps&0x20)!=0,"Valid user return frame required");
  // Only precondition failures are guaranteed not to mutate the emulator.
  require(c.step(monitor),"Original RETI: "+c.getLastError());
  require(reg(c,"PC")==target && reg(c,"PS")==Integer.toUnsignedLong(ps) && reg(c,"S")==1 && reg(c,"ILM")==((ps>>>16)&31),"Unexpected restored state / ILM profile");
  require(reg(c,"SP")==sp+8 && reg(c,"SSP")==sp && reg(c,"USP")==user,"Unexpected stack model");
  c.writeRegister("SSP",sp+8);c.writeRegister("SP",user);
 }
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language[] languages={provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"))};
  byte[] code=new byte[2];currentProgram.getMemory().getBytes(toAddr(0x407ea),code);require(Arrays.equals(code,new byte[]{(byte)0x97,0x30}),"Original RETI");int cases=0;
  for(int model=0;model<languages.length;model++){
   Language lang=languages[model];var p=new ProgramDB("AB112-user-return",lang,lang.getDefaultCompilerSpec(),this);
   try{
    var a=lang.getDefaultSpace();int tx=p.startTransaction("Original RETI and synthetic stacks");
    try{
     p.getMemory().createInitializedBlock("original",a.getAddress(0x407ea),2,(byte)0,monitor,false);p.getMemory().setBytes(a.getAddress(0x407ea),code);
     p.getMemory().createInitializedBlock("stacks",a.getAddress(0x81000000L),0x3000,(byte)0,monitor,false);
    }finally{p.endTransaction(tx,true);}
    for(long user:new long[]{0x81002000L,0x81002100L})for(int ps:new int[]{0x30,0x1f0030,0x100020,0xf073f}){
     var c=new EmulatorHelper(p);
     try{
      for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM"})c.writeRegister(f,0);
      String[] regs={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP","RP","MDL","MDH"};
      for(int i=0;i<regs.length;i++)c.writeRegister(regs[i],0x11220000+i);
      long sp=0x81000ff8L;c.writeRegister("PC",0x407ea);c.writeRegister("SP",sp);c.writeRegister("SSP",sp);c.writeRegister("USP",user);
      put(c,a,sp,0x407ee);put(c,a,sp+4,ps);put(c,a,user,0x80000100);put(c,a,user+4,0);
      put(c,a,sp-4,0x55667788);put(c,a,sp+8,0x99aabbcc);
      byte[] before=c.readMemory(a.getAddress(0x81000000L),0x3000);
      returnUser(c,a,monitor,false);
      require(reg(c,"PC")==0x407ee && reg(c,"PS")==Integer.toUnsignedLong(ps),"Frame consumed from current SP");
      require(reg(c,"SP")==user && reg(c,"SSP")==sp+8 && reg(c,"USP")==user,"Explicit user return stack selection");
      String[] flags={"C","V","Z","N","I","S","T","D0","D1"};int[] bits={0,1,2,3,4,5,8,9,10};
      for(int i=0;i<flags.length;i++)require(reg(c,flags[i])==((ps>>>bits[i])&1),"Restored "+flags[i]);
      require(reg(c,"ILM")==((ps>>>16)&31),"Profile-specific ILM");
      for(int i=0;i<regs.length;i++)require(reg(c,regs[i])==0x11220000+i,"Preserved "+regs[i]);
      require(Arrays.equals(before,c.readMemory(a.getAddress(0x81000000L),0x3000)),"Stack bytes retained");
      cases++;println(String.format("AB-112: %s PS=%08x USP=%08x -> S=1 SP=USP SSP=81001000; destination not executed",lang.getLanguageID(),ps,user));
     }finally{c.dispose();}
    }
    int rejected=0;
    for(int invalid=0;invalid<11;invalid++){
     var c=new EmulatorHelper(p);
     try{
      String[] names={"PC","SP","SSP","USP","PS","S","I","N","Z","V","C","T","D0","D1","ILM","R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP","RP","MDL","MDH"};
      for(String name:names)c.writeRegister(name,0);
      c.writeRegister("PC",0x407ea);c.writeRegister("SP",0x81000ff8L);c.writeRegister("SSP",0x81000ff8L);c.writeRegister("USP",0x81002000L);
      put(c,a,0x81000ff8L,0x407ee);put(c,a,0x81000ffcL,0x1f0030);
      if(invalid==0)c.writeRegister("PC",0x407eb);
      if(invalid==1)c.writeRegister("PC",0x407ec);
      if(invalid==2)c.writeRegister("S",1);
      if(invalid==3)c.writeRegister("SSP",0x81001000L);
      if(invalid==4){c.writeRegister("SP",0x81000ffaL);c.writeRegister("SSP",0x81000ffaL);}
      if(invalid==5)c.writeRegister("USP",0x81002002L);
      if(invalid==6)c.writeMemory(a.getAddress(0x407ea),new byte[]{0,0});
      if(invalid==7)put(c,a,0x81000ff8L,0x407ef);
      if(invalid==8)put(c,a,0x81000ffcL,0x1f0010);
      if(invalid==9){c.writeRegister("SP",0xfffffffcL);c.writeRegister("SSP",0xfffffffcL);}
      long[] before=new long[names.length];for(int i=0;i<names.length;i++)before[i]=reg(c,names[i]);
      byte[] memory=c.readMemory(a.getAddress(0x81000000L),0x3000);boolean refused=false;
      try{returnUser(c,a,monitor,invalid==10);}catch(IllegalStateException expected){refused=true;}
      require(refused,"Expected rejection");for(int i=0;i<names.length;i++)require(before[i]==reg(c,names[i]),"Rejection changed "+names[i]);
      require(Arrays.equals(memory,c.readMemory(a.getAddress(0x81000000L),0x3000)),"Rejection changed stack");rejected++;
     }finally{c.dispose();}
    }
    require(rejected==11,"Rejection count");println("AB-112: eleven invalid boundaries rejected without changing checked registers or stack memory");
   }finally{p.release(this);}
  }
  require(cases==8,"Case count");println("AB-112: eight adapted user returns passed; explicit stack correction after original RETI");
 }
}
