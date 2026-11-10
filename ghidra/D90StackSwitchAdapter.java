// AB-109: explicit bounded stack switch; not a global CPU-model change.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.program.model.lang.*;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
public class D90StackSwitchAdapter extends D90SupervisorStack {
 // Only the four non-delay-slot S-changing sites in the startup restore path.
 public static void switchStack(EmulatorHelper c,ghidra.program.model.address.AddressSpace a,ghidra.util.task.TaskMonitor monitor,boolean delaySlot)throws Exception{
  long pc=reg(c,"PC"),sp=reg(c,"SP");
  require(!delaySlot,"Delay slot unsupported");require((pc&1)==0,"Odd PC");
  boolean user=pc==0x407d0 || pc==0x407dc;
  require(user || pc==0x407d6 || pc==0x407e8,"Unsupported site");
  require(reg(c,"S")== (user?0:1),"Unexpected source S");
  require(Arrays.equals(c.readMemory(a.getAddress(pc),2),user?new byte[]{(byte)0x93,0x20}:new byte[]{(byte)0x83,(byte)0xdf}),"Unexpected instruction");
  String source=user?"SSP":"USP",destination=user?"USP":"SSP";
  long target=reg(c,destination);require((sp&3)==0 && (target&3)==0,"Unaligned stack");
  // Validation above performs no writes. A failed emulator step is not rolled back.
  require(c.step(monitor),"S instruction: "+c.getLastError());
  require(reg(c,"PC")==pc+2 && reg(c,"S")== (user?1:0) && reg(c,"SP")==sp,"Unexpected model behavior");
  c.writeRegister(source,sp);c.writeRegister("SP",target);
 }
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language[] languages={currentProgram.getLanguage(),provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"))};int cases=0;
  byte[] code=new byte[28];currentProgram.getMemory().getBytes(toAddr(0x407ce),code);
  for(Language lang:languages){
   var p=new ProgramDB("AB109-stack-characterization",lang,lang.getDefaultCompilerSpec(),this);
   try{
    var a=lang.getDefaultSpace();int tx=p.startTransaction("Original instructions and synthetic stacks");
    try{
     p.getMemory().createInitializedBlock("original",a.getAddress(0x407ce),code.length,(byte)0,monitor,false);p.getMemory().setBytes(a.getAddress(0x407ce),code);
     p.getMemory().createInitializedBlock("stacks",a.getAddress(0x81000000L),0x3000,(byte)0,monitor,false);
    }finally{p.endTransaction(tx,true);}
    for(int ilm:new int[]{0,31})for(int pattern:new int[]{0x11223344,0x55667788}){
     var c=new EmulatorHelper(p);
     try{
      for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM"})c.writeRegister(f,0);
      c.writeRegister("ILM",ilm);c.writeRegister("PC",0x407ce);c.writeRegister("SP",0x81001000L);c.writeRegister("SSP",0x81001000L);c.writeRegister("USP",0x81002080L);c.writeRegister("R0",0x81002000L);c.writeRegister("R1",0);
      put(c,a,0x81001000L,pattern);put(c,a,0x81001004L,~pattern);put(c,a,0x81002000L,0x12345678);put(c,a,0x81002004L,0x23456789);
      byte[] before=c.readMemory(a.getAddress(0x81000000L),0x3000);
      require(c.step(monitor),"MOV USP");require(reg(c,"USP")==0x81002000L && reg(c,"SP")==0x81001000L,"Dedicated USP write does not alias SP");
      switchStack(c,a,monitor,false);require(reg(c,"S")==1 && reg(c,"SP")==0x81002000L,"Explicit user stack switch");
      require(c.step(monitor) && c.step(monitor),"Two pops");require(c.readRegister("R0").intValue()==0x12345678 && c.readRegister("R1").intValue()==0x23456789,"Reads use user stack data");
      switchStack(c,a,monitor,false);require(reg(c,"PC")==0x407d8 && reg(c,"S")==0 && reg(c,"SP")==0x81001000L && reg(c,"SSP")==0x81001000L && reg(c,"USP")==0x81002008L,"Explicit supervisor return and saved user SP");
      require(Arrays.equals(before,c.readMemory(a.getAddress(0x81000000L),0x3000)),"Stack memory retained");
      cases++;println("AB-109: "+lang.getLanguageID()+" ILM input="+ilm+" pattern="+Integer.toHexString(pattern)+": explicit switches select user data and retain advanced USP");
     }finally{c.dispose();}
    }
    int rejected=0,sites=0;
    for(long site:new long[]{0x407d0,0x407d6,0x407dc,0x407e8}){
     boolean user=site==0x407d0 || site==0x407dc;
     for(int invalid=-1;invalid<6;invalid++){
      var c=new EmulatorHelper(p);
      try{
       for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM","R0","R1"})c.writeRegister(f,0);
       c.writeRegister("S",user?0:1);c.writeRegister("PC",site);c.writeRegister("SP",0x81001100L);c.writeRegister("SSP",0x81001000L);c.writeRegister("USP",0x81002000L);
       if(invalid==0)c.writeRegister("PC",site+1);
       if(invalid==1)c.writeRegister("S",user?1:0);
       if(invalid==2)c.writeRegister("SP",0x81001102L);
       if(invalid==3)c.writeRegister(user?"USP":"SSP",0x81002002L);
       if(invalid==4)c.writeMemory(a.getAddress(site),new byte[]{0,0});
       String[] names={"PC","SP","SSP","USP","PS","S","I","N","Z","V","C","T","D0","D1","ILM","R0","R1"};
       long[] before=new long[names.length];for(int k=0;k<names.length;k++)before[k]=reg(c,names[k]);
       byte[] memory=c.readMemory(a.getAddress(0x81000000L),0x3000);
       if(invalid<0){switchStack(c,a,monitor,false);require(reg(c,"SP")== (user?0x81002000L:0x81001000L) && reg(c,user?"SSP":"USP")==0x81001100L,"Save stale source pointer at boundary");sites++;}
       else{boolean refused=false;try{switchStack(c,a,monitor,invalid==5);}catch(IllegalStateException expected){refused=true;}
        require(refused,"Expected rejection");for(int k=0;k<names.length;k++)require(before[k]==reg(c,names[k]),"Rejection changed "+names[k]);
        require(Arrays.equals(memory,c.readMemory(a.getAddress(0x81000000L),0x3000)),"Rejection changed stack");rejected++;
       }
      }finally{c.dispose();}
     }
    }
    require(sites==4 && rejected==24,"Site/rejection count");println("AB-109: "+lang.getLanguageID()+": four site checks and 24 precondition rejections passed");
   }finally{p.release(this);}
  }
  require(cases==8,"Case count");println("AB-109: eight adapted sequences passed; synthetic stack data, no hardware fidelity or full-context claim");
 }
}
