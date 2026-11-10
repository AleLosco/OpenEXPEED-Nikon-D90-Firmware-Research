// AB-115: isolated installed-source FR81 SRCH1 AC characterization.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.plugin.processors.sleigh.SleighLanguageProvider;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.model.lang.*;
import ghidra.program.database.ProgramDB;
import generic.jar.ResourceFile;
import java.io.File;
import java.util.Arrays;
public class D90Srch1Probe extends D90Int40Adapter {
 public void run()throws Exception{
  String[] args=getScriptArgs();require(args.length==1,"Pass isolated FR81 fr60.ldefs");
  var ctor=SleighLanguageProvider.class.getDeclaredConstructor(ResourceFile.class);ctor.setAccessible(true);
  var provider=ctor.newInstance(new ResourceFile(new File(args[0])));
  Language lang=provider.getLanguage(new LanguageID("fr81:BE:16:default"));
  byte[] code=new byte[2];currentProgram.getMemory().getBytes(toAddr(0x40e32),code);require(Arrays.equals(code,new byte[]{(byte)0x97,(byte)0xdd}),"Original word");
  var p=new ProgramDB("AB115-SRCH1-only",lang,lang.getDefaultCompilerSpec(),this);
  try{
   var a=lang.getDefaultSpace();int tx=p.startTransaction("Original instruction");
   try{p.getMemory().createInitializedBlock("original",a.getAddress(0x40e30),8,(byte)0x5a,monitor,false);p.getMemory().setBytes(a.getAddress(0x40e32),code);}finally{p.endTransaction(tx,true);}
   var d=new PseudoDisassembler(p);require(d.disassemble(a.getAddress(0x40e32)).toString().equals("SRCH1 AC"),"FR81 decode");
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
     if(!c.step(monitor)){
      require(input!=0 && c.getLastError()!=null && c.getLastError().contains("clz"),"Unexpected emulation failure: "+c.getLastError());
      println("AB-115 INCONCLUSIVE grid: "+cases+" cases completed; nonzero input="+Integer.toHexString(input)+" blocked by unimplemented clz. Failed state is not asserted or reused.");return;
     }
     require(reg(c,"PC")==0x40e34 && reg(c,"AC")==Integer.numberOfLeadingZeros(input),"Leading-zero result");
     for(int i=0;i<names.length;i++)require(reg(c,names[i])==before[i],"Preserved "+names[i]);
     require(Arrays.equals(memory,c.readMemory(a.getAddress(0x40e30),8)),"Code/guards retained");
     cases++;println(String.format("AB-115: input=%08x flags=%d result=%d",input,flag,reg(c,"AC")));
    }finally{c.dispose();}
   }
   require(cases==76,"Case count");println("AB-115: 76 isolated SRCH1 cases passed; no firmware-wide FR81 compatibility claim");
  }finally{p.release(this);}
 }
}
