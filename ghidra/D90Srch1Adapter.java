// AB-116: explicit SRCH1 AC boundary adapter; no native instruction execution.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.model.lang.*;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
public class D90Srch1Adapter extends D90Int40Adapter {
 // Input is all 32 bits of AC. This replaces the instruction at one known site.
 public static void searchOne(EmulatorHelper c,ghidra.program.model.address.AddressSpace a,boolean delaySlot){
  long pc=reg(c,"PC");require(!delaySlot,"Delay slot unsupported");require(pc==0x40e32,"Only verified SRCH1 site supported");
  require(Arrays.equals(c.readMemory(a.getAddress(pc),2),new byte[]{(byte)0x97,(byte)0xdd}),"Expected SRCH1 AC bytes");
  long value=reg(c,"AC");int count=0;
  for(long mask=0x80000000L;mask!=0 && (value&mask)==0;mask>>>=1)count++;
  c.writeRegister("AC",count);c.writeRegister("PC",pc+2);
 }
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated ILM fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language lang=provider.getLanguage(new LanguageID("fr60:BE:16:d90-ilm5"));
  byte[] code=new byte[2];currentProgram.getMemory().getBytes(toAddr(0x40e32),code);require(Arrays.equals(code,new byte[]{(byte)0x97,(byte)0xdd}),"Original word");
  var p=new ProgramDB("AB116-SRCH1-only",lang,lang.getDefaultCompilerSpec(),this);
  try{
   var a=lang.getDefaultSpace();int tx=p.startTransaction("Original instruction");
   try{p.getMemory().createInitializedBlock("original",a.getAddress(0x40e30),8,(byte)0x5a,monitor,false);p.getMemory().setBytes(a.getAddress(0x40e32),code);}finally{p.endTransaction(tx,true);}
   int[] inputs=new int[38];inputs[0]=0;for(int i=0;i<32;i++)inputs[i+1]=1<<i;
   int[] extra={-1,0x7fffffff,0xaaaaaaaa,0x55555555,0x80000001};System.arraycopy(extra,0,inputs,33,5);int cases=0;
   for(int input:inputs)for(int flag:new int[]{0,1}){
    var c=new EmulatorHelper(p);
    try{
     String[] names={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","FP","SP","USP","SSP","RP","MDL","MDH","PS","ILM","C","V","Z","N","I","S","T","D0","D1"};
     for(int i=0;i<20;i++)c.writeRegister(names[i],0x11000000+i*4);
     c.writeRegister("PS",flag==0?0:0x1f073f);c.writeRegister("ILM",flag==0?0:31);
     for(int i=22;i<names.length;i++)c.writeRegister(names[i],flag);
     long[] before=new long[names.length];for(int i=0;i<names.length;i++)before[i]=reg(c,names[i]);
     c.writeRegister("AC",Integer.toUnsignedLong(input));c.writeRegister("PC",0x40e32);
     byte[] memory=c.readMemory(a.getAddress(0x40e30),8);
     searchOne(c,a,false);
     require(reg(c,"PC")==0x40e34 && reg(c,"AC")==Integer.numberOfLeadingZeros(input),"Leading-zero result");
     for(int i=0;i<names.length;i++)require(reg(c,names[i])==before[i],"Preserved "+names[i]);
     require(Arrays.equals(memory,c.readMemory(a.getAddress(0x40e30),8)),"Code/guards retained");
     cases++;println(String.format("AB-116: input=%08x flags=%d result=%d",input,flag,reg(c,"AC")));
    }finally{c.dispose();}
   }
   int rejects=0;
   for(int invalid=0;invalid<4;invalid++){
    var c=new EmulatorHelper(p);
    try{
     String[] names={"PC","AC","R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","FP","SP","USP","SSP","RP","MDL","MDH","PS","ILM","C","V","Z","N","I","S","T","D0","D1"};
     for(String name:names)c.writeRegister(name,0);
     c.writeRegister("PC",0x40e32);c.writeRegister("AC",0x80000000L);
     if(invalid==0){c.writeRegister("PC",0x40e33);c.writeMemory(a.getAddress(0x40e33),code);}
     if(invalid==1){c.writeRegister("PC",0x40e34);c.writeMemory(a.getAddress(0x40e34),code);}
     if(invalid==2)c.writeMemory(a.getAddress(0x40e32),new byte[]{0,0});
     long[] before=new long[names.length];for(int i=0;i<names.length;i++)before[i]=reg(c,names[i]);
     byte[] memory=c.readMemory(a.getAddress(0x40e30),8);boolean refused=false;
     try{searchOne(c,a,invalid==3);}catch(IllegalStateException expected){refused=true;}
     require(refused,"Expected rejection");for(int i=0;i<names.length;i++)require(reg(c,names[i])==before[i],"Rejected input changed "+names[i]);
     require(Arrays.equals(memory,c.readMemory(a.getAddress(0x40e30),8)),"Rejected input changed memory");rejects++;
    }finally{c.dispose();}
   }
   require(rejects==4,"Rejection count");println("AB-116: four unsupported inputs rejected without changing checked registers or code/guard bytes");
   require(cases==76,"Case count");println("AB-116: 76 explicit SRCH1 adapter cases passed; no native clz or hardware validation");
  }finally{p.release(this);}
 }
}
