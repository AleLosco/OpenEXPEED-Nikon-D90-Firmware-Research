// AB-108: characterize S/SP/USP behavior, without a stack-switch adapter.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.program.model.lang.*;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
public class D90UserStackModel extends D90SupervisorStack {
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language[] languages={currentProgram.getLanguage(),provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"))};int cases=0;
  byte[] code=new byte[10];currentProgram.getMemory().getBytes(toAddr(0x407ce),code);
  for(Language lang:languages){
   var p=new ProgramDB("AB108-stack-characterization",lang,lang.getDefaultCompilerSpec(),this);
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
      require(c.step(monitor),"ORCCR");require(reg(c,"S")==1 && reg(c,"SP")==0x81001000L,"S changes without switching SP");
      require(c.step(monitor) && c.step(monitor),"Two pops");require(c.readRegister("R0").intValue()==pattern && c.readRegister("R1").intValue()==~pattern,"Reads use unchanged SP region");
      require(c.step(monitor),"ANDCCR");require(reg(c,"PC")==0x407d8 && reg(c,"S")==0 && reg(c,"SP")==0x81001008L && reg(c,"SSP")==0x81001000L && reg(c,"USP")==0x81002000L,"No bank switch or dedicated stack synchronization");
      require(Arrays.equals(before,c.readMemory(a.getAddress(0x81000000L),0x3000)),"Stack memory retained");
      cases++;println("AB-108: "+lang.getLanguageID()+" ILM input="+ilm+" pattern="+Integer.toHexString(pattern)+": S toggles; SP independent; pops read supervisor-positioned synthetic data");
     }finally{c.dispose();}
    }
   }finally{p.release(this);}
  }
  require(cases==8,"Case count");println("AB-108: eight model-characterization cases passed; missing stack alias observed, no hardware fidelity claim or adapter");
 }
}
