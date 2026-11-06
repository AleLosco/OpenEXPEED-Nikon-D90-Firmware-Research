// AB-85: isolated language correction; synthetic instruction sequences, no firmware execution.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.program.model.lang.*;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.nio.ByteBuffer;
public class D90IlmCorrection extends GhidraScript {
 void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 public void run()throws Exception{
  String[] args=getScriptArgs();check(args.length==1,"Pass isolated fr60.ldefs path");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language fixed=provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"));
  check(fixed!=null,"Isolated language unavailable");
  for(boolean corrected:new boolean[]{false,true}){
   Language lang=corrected?fixed:currentProgram.getLanguage();
   var p=new ProgramDB("AB85-synthetic",lang,lang.getDefaultCompilerSpec(),this);
   try{
    var space=lang.getDefaultSpace();int tx=p.startTransaction("Synthetic test memory");
    try{p.getMemory().createInitializedBlock("code",space.getAddress(0x80000000L),0x100,(byte)0,monitor,false);
        p.getMemory().createInitializedBlock("stack",space.getAddress(0x81003000L),0x2000,(byte)0,monitor,false);
    }finally{p.endTransaction(tx,true);}
    for(int ilm=0;ilm<32;ilm++)for(int flags:new int[]{0,0x70f}){
     var c=new EmulatorHelper(p);
     try{
      for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM","R0","R1","R2"})c.writeRegister(f,0);
      c.writeRegister("PS",flags);
      String[] names={"C","V","Z","N","T","D0","D1"};int[] bits={0,1,2,3,8,9,10};
      for(int i=0;i<bits.length;i++)c.writeRegister(names[i],(flags>>>bits[i])&1);
      long base=0x80000000L,sp=0x81004000L;
      c.writeMemory(space.getAddress(base),new byte[]{(byte)0x87,(byte)ilm,0x17,0x10,0x17,(byte)0x90,0x07,0x11,0x07,(byte)0x90,0x17,0x12,(byte)0x97,0x30});
      c.writeMemory(space.getAddress(sp),ByteBuffer.allocate(8).putInt(0x80000080).putInt((ilm<<16)|flags).array());
      c.writeRegister("SP",sp);c.writeRegister("PC",base);
      int expected=corrected?ilm:ilm&15, ps=(expected<<16)|flags;
      for(int i=0;i<6;i++){
       check(c.step(monitor),"Step "+i+": "+c.getLastError());
       if(i==0)check(c.readRegister("ILM").intValue()==expected,"STILM");
       if(i==2)check(ByteBuffer.wrap(c.readMemory(space.getAddress(sp-4),4)).getInt()==ps,"ST PS");
       if(i==3)check(c.readRegister("ILM").intValue()==0,"MOV R1 PS clear");
       if(i==4)check(c.readRegister("ILM").intValue()==expected,"LD PS");
      }
      check(c.readRegister("R0").intValue()==ps && c.readRegister("R2").intValue()==ps,"MOV PS");
      check(c.readRegister("SP").longValue()==sp,"Stack roundtrip");
      check(c.step(monitor),"RETI");
      check(c.readRegister("ILM").intValue()==expected && c.readRegister("PS").intValue()==((ilm<<16)|flags),"RETI state");
      check(c.readRegister("SP").longValue()==sp+8 && c.getExecutionAddress().getOffset()==0x80000080L,"RETI boundary");
     }finally{c.dispose();}
    }
    println((corrected?"Isolated five-bit":"Installed four-bit")+" model: 64 STILM/MOV/ST/LD/RETI sequences passed");
   }finally{p.release(this);}
  }
  println("128 synthetic sequences passed; installed language and firmware database unchanged");
 }
}
