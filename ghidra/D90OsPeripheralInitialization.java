// AB-105: continuous OS prefix -> original peripheral initialization with explicit INT/SSP.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import ghidra.program.model.address.AddressSpace;
import java.util.Arrays;
public class D90OsPeripheralInitialization extends D90SourceInitializationReturn {
 AddressSpace space;byte[] osBefore,externalBefore;int completed=0;
 @Override int stepLimit(){return 30000;}
 @Override long startAddress(){return 0x40704;}
 @Override int counterBeforeService(boolean high){return 0;}
 @Override long[][] extraWindows(){return new long[][]{{0x40704,0x40738},{0x407f0,0x40a56},{0x41534,0x415a2},{0x4186e,0x418a0},{0x5c400,0x5cce4}};}
 @Override long[][] extraSyntheticWindows(){return new long[][]{{0x68000000L,0x68000110L},{0x68000118L,0x68001000L},{0x8000,0x8400},{0x6800cf20L,0x6800cf24L},{0x8f991204L,0x8f99122cL}};}
 @Override void beforeInitialization(EmulatorHelper c,ProgramDB p,int pattern)throws Exception{
  space=p.getLanguage().getDefaultSpace();byte[] ram=new byte[0x1000];Arrays.fill(ram,(byte)pattern);c.writeMemory(space.getAddress(0x68000000L),ram);
  byte[] low=new byte[0x400];Arrays.fill(low,(byte)pattern);c.writeMemory(space.getAddress(0x8000),low);
  c.writeMemory(space.getAddress(0x8f991204L),new byte[40]);put(c,space,0x6800cf20,0x11223344);
  osBefore=null;externalBefore=null;
 }
 @Override void until(EmulatorHelper c,PseudoDisassembler d,long stop)throws Exception{
  if(reg(c,"PC")==0x40704){
   super.until(c,d,0x40738);
   require(word(c,space,0x6800cf20)==1 && word(c,space,0x68000004)==0x80000000,"OS marker/bitmap");
   for(int i=0;i<33;i++){
    long head=0x68000008L+8*i,node=i==0?0x680001ecL:i==32?0x680007c4L:head;
    require(word(c,space,head)==(int)node && word(c,space,head+4)==(int)node,"OS sentinel");
    if(node!=head)require(word(c,space,node)==(int)head && word(c,space,node+4)==(int)head && word(c,space,node+8)==0,"Inserted node");
   }
   for(long head:new long[]{0x68000118L,0x68000120L,0x68000128L})require(word(c,space,head)==(int)head && word(c,space,head+4)==(int)head,"Other sentinels");
   require(word(c,space,0x68000110)==0 && word(c,space,0x68000114)==0,"OS counter zero");
   for(long site:new long[]{0x407f0,0x40878,0x40886,0x408a0,0x408be,0x40906,0x4092c,0x40952,0x40978,0x409c4,0x40a0a,0x40a30})require(seen.contains(site),"OS path");
   osBefore=c.readMemory(space.getAddress(0x68000000L),0xf20);
   externalBefore=c.readMemory(space.getAddress(0x8f991204L),40);
  }
  super.until(c,d,stop);
 }
 @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
  require(osBefore!=null && Arrays.equals(osBefore,c.readMemory(space.getAddress(0x68000000L),0xf20)),"OS region retained through peripheral initialization");
  require(Arrays.equals(externalBefore,c.readMemory(space.getAddress(0x8f991204L),40)),"External context retained");
  require(word(c,space,0x6800cf20)==1,"Marker before 0x40740 instruction");completed++;
  println("AB-105: OS-produced lists/contexts retained to 0x40740; no list reinjection, explicit INT/SSP only");
 }
 @Override public void run()throws Exception{
  super.run();require(completed==8,"Scenario count");
  println("AB-105: eight continuous OS/peripheral initialization scenarios passed with synthetic RAM and explicit INT/SSP boundaries");
 }
}
