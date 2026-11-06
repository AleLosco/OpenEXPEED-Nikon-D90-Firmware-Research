// AB-88: original handler entry/exit with explicit INT/SSP adapters; service bypassed.
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
public class D90FirmwareContext extends D90SupervisorStack {
 int inspected=0;
 void inspect(Varnode v){if(v!=null && v.isRegister())require(v.getOffset()+v.getSize()<=8 || v.getOffset()>=16,"Dedicated SSP/USP accessed");}
 void until(EmulatorHelper c,PseudoDisassembler d,long stop)throws Exception{
  int n=0;while(reg(c,"PC")!=stop && n++<100){
   var i=d.disassemble(c.getExecutionAddress());require(i!=null,"Decode failure");
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
  var p=new ProgramDB("AB88-original-windows",lang,lang.getDefaultCompilerSpec(),this);
  try{
   var a=lang.getDefaultSpace();int tx=p.startTransaction("Copy original instruction windows");
   try{
    for(long[] w:new long[][]{{0x404f4,0x404fa},{0x4067a,0x406b6},{0x406c6,0x406f6},{0x19e064,0x19e066},{0xdfefc,0xdff00}}){
     byte[] b=new byte[(int)(w[1]-w[0])];currentProgram.getMemory().getBytes(toAddr(w[0]),b);
     p.getMemory().createInitializedBlock("original_"+Long.toHexString(w[0]),a.getAddress(w[0]),b.length,(byte)0,monitor,false);
     p.getMemory().setBytes(a.getAddress(w[0]),b);
    }
    p.getMemory().createInitializedBlock("stack",a.getAddress(0x81003000L),0x2000,(byte)0,monitor,false);
    p.getMemory().createInitializedBlock("synthetic_peripheral",a.getAddress(0x440),0x40,(byte)0,monitor,false);
   }finally{p.endTransaction(tx,true);}
   var d=new PseudoDisassembler(p);int cases=0;
   for(int ilm:new int[]{0,15,16,31})for(int enabled:new int[]{0,1})for(int selector:new int[]{0x55,0x80000055})for(int result:new int[]{0,0xdf}){
    var c=new EmulatorHelper(p);
    try{
     for(int i=0;i<12;i++)c.writeRegister("R"+i,0x11000000+i);
     c.writeRegister("R12",selector);String[] special={"AC","FP","RP","MDL","MDH"};
     for(int i=0;i<5;i++)c.writeRegister(special[i],0x22000000+i);
     for(String f:new String[]{"PS","S","I","N","Z","V","C","T","D0","D1","ILM"})c.writeRegister(f,0);
     c.writeRegister("ILM",ilm);c.writeRegister("I",enabled);c.writeRegister("SP",0x81004000L);c.writeRegister("SSP",0x81004000L);c.writeRegister("TBR",0xdfc00);c.writeRegister("PC",0x19e064);
     put(c,a,0x81003fa4L,0x11223344);put(c,a,0x81004000L,0x55667788);c.writeMemory(a.getAddress(0x46f),new byte[]{0x55});
     int ps=(ilm<<16)|(enabled<<4);enter(c,a,false);
     require(reg(c,"PC")==0x4067a,"Original vector target");until(c,d,0x406b6);
     long frame=0x81003fb0L;int saved=(ilm&15)==15?selector&0x7fffffff:selector;
     require(reg(c,"SP")==frame && reg(c,"FP")==frame,"Generated frame base");
     ByteBuffer b=ByteBuffer.wrap(c.readMemory(a.getAddress(frame),72));
     for(int i=0;i<12;i++)require(b.getInt()==0x11000000+i,"Saved R"+i);
     require(b.getInt()==saved,"Saved selector");for(int i=0;i<5;i++)require(b.getInt()==0x22000000+i,"Saved special");
     require(word(c,a,frame+72)==0x19e066 && word(c,a,frame+76)==ps,"INT frame retained");
     require(reg(c,"ILM")==((ilm&15)==15?30:ilm),"Prefix ILM");
     // Explicit service bypass: this is not a service -85 execution.
     c.writeRegister("PC",0x406c6);c.writeRegister("R12",result);until(c,d,0x406f4);
     require(reg(c,"SP")==0x81003ff8L && reg(c,"SSP")==0x81003ff8L,"RETI stack boundary");
     returnSupervisor(c,a);
     require(reg(c,"PC")==0x19e066 && reg(c,"SP")==0x81004000L && reg(c,"SSP")==0x81004000L,"Caller return");
     require(reg(c,"PS")==ps && reg(c,"ILM")==ilm && reg(c,"I")==enabled && reg(c,"S")==0,"Restored status");
     for(int i=0;i<12;i++)require(c.readRegister("R"+i).intValue()==0x11000000+i,"Restored R"+i);
     for(int i=0;i<5;i++)require(c.readRegister(special[i]).intValue()==0x22000000+i,"Restored special");
     require(c.readRegister("R12").intValue()==(byte)result,"Signed result");
     require((c.readMemory(a.getAddress(0x46f),1)[0]&255)==(saved<0?0x1e:0x10),"Peripheral RAM byte");
     require(word(c,a,0x81003fa4L)==0x11223344 && word(c,a,0x81004000L)==0x55667788,"Stack sentinels");cases++;
    }finally{c.dispose();}
   }
   println(cases+" firmware entry/exit cases passed through RETI; dispatcher/service explicitly bypassed");
   println(inspected+" stepped instruction instances checked: no direct dedicated SSP/USP p-code access");
   println("AB-88 passed; original bytes in transient program, explicit INT and post-RETI SSP adapters");
  }finally{p.release(this);}
 }
}
