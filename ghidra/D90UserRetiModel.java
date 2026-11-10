// AB-111: characterize original RETI to S=1; no return adapter.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.program.model.lang.*;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
public class D90UserRetiModel extends D90SupervisorStack {
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language[] languages={currentProgram.getLanguage(),provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"))};
  byte[] code=new byte[2];currentProgram.getMemory().getBytes(toAddr(0x407ea),code);require(Arrays.equals(code,new byte[]{(byte)0x97,0x30}),"Original RETI");int cases=0;
  for(int model=0;model<languages.length;model++){
   Language lang=languages[model];var p=new ProgramDB("AB111-user-return",lang,lang.getDefaultCompilerSpec(),this);
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
      require(c.step(monitor),"Original RETI: "+c.getLastError());
      require(reg(c,"PC")==0x407ee && reg(c,"PS")==Integer.toUnsignedLong(ps),"Frame consumed from current SP");
      require(reg(c,"SP")==sp+8 && reg(c,"SSP")==sp && reg(c,"USP")==user,"Separate stack registers retained");
      String[] flags={"C","V","Z","N","I","S","T","D0","D1"};int[] bits={0,1,2,3,4,5,8,9,10};
      for(int i=0;i<flags.length;i++)require(reg(c,flags[i])==((ps>>>bits[i])&1),"Restored "+flags[i]);
      require(reg(c,"ILM")==((ps>>>16)&(model==0?15:31)),"Profile-specific ILM");
      for(int i=0;i<regs.length;i++)require(reg(c,regs[i])==0x11220000+i,"Preserved "+regs[i]);
      require(Arrays.equals(before,c.readMemory(a.getAddress(0x81000000L),0x3000)),"Stack bytes retained");
      cases++;println(String.format("AB-111: %s PS=%08x USP=%08x -> S=1 SP=81001000 SSP=81000ff8; destination not executed",lang.getLanguageID(),ps,user));
     }finally{c.dispose();}
    }
   }finally{p.release(this);}
  }
  require(cases==16,"Case count");println("AB-111: 16 original user-RETI characterizations passed; no SP-to-USP selection or SSP synchronization in either profile");
 }
}
