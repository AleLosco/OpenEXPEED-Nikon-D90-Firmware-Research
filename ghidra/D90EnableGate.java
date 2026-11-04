// Isolated B block 0x101ce6; stop before 0x101d08, no calls or hardware.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90EnableGate extends GhidraScript {
 private void conditionalWrite(int packed, long r4, long r8) throws Exception {
  EmulatorHelper cpu = new EmulatorHelper(currentProgram);
  try {
   cpu.writeMemory(toAddr(0x8f52f1e9L), new byte[]{(byte)packed});
   cpu.writeMemory(toAddr(0x8f52e4aaL), new byte[]{0x55, 0x33, 0x66});
   cpu.writeRegister("R4", r4);
   cpu.writeRegister("R8", r8);
   cpu.writeRegister("R9", 0x5678);
   cpu.writeRegister("PC", 0x101ca2);
   long stop = (packed & 15) == 0 ? 0x101cc6 : 0x101d84;
   for (int i = 0; i < 30 && cpu.getExecutionAddress().getOffset() != stop; i++) {
    if (!cpu.step(monitor)) throw new IllegalStateException(cpu.getLastError());
   }
   boolean writes = (packed & 15) == 0 && r4 == 0 && r8 != 0;
   byte[] bytes = cpu.readMemory(toAddr(0x8f52e4aaL), 3);
   if (cpu.getExecutionAddress().getOffset() != stop || bytes[0] != 0x55 ||
       bytes[1] != (writes ? 1 : 0x33) || bytes[2] != 0x66)
    throw new IllegalStateException("Conditional gate mismatch: " + packed + "/" + r4 + "/" + r8);
  } finally { cpu.dispose(); }
 }
 public void run() throws Exception {
  int cases=0;
  for(int a:new int[]{0,0x20})for(int b:new int[]{0,0x40})for(int initial:new int[]{0,255}) {
   EmulatorHelper e=new EmulatorHelper(currentProgram);
   try {
    e.writeMemory(toAddr(0x8f52f212L),new byte[]{(byte)a});
    e.writeMemory(toAddr(0x8f52f1bbL),new byte[]{(byte)b});
    e.writeMemory(toAddr(0x8f52e4aaL),new byte[]{0x55,(byte)initial,0x66});
    e.writeRegister("R8",0x1234);e.writeRegister("R9",0x5678);
    e.writeRegister("PC",0x101ce6);
    int steps=0;
    while(e.getExecutionAddress().getOffset()!=0x101d08 && steps++<20)
     if(!e.step(monitor))throw new IllegalStateException(e.getLastError());
    byte[] mem=e.readMemory(toAddr(0x8f52e4aaL),3);
    boolean keep=a==0x20 && b==0;
    if(e.getExecutionAddress().getOffset()!=0x101d08 || mem[0]!=0x55 || mem[1]!=1 || mem[2]!=0x66 ||
       e.readRegister("R8").intValue()!=(keep?0x1234:0) || e.readRegister("R9").intValue()!=(keep?0x5678:0))
     throw new IllegalStateException("Gate mismatch");
    cases++;
   } finally {e.dispose();}
  }
  int conditionalCases = 0;
  for (int packed : new int[]{0, 0xf0, 1, 0x11})
   for (long r4 : new long[]{0, 1, 0xffffffffL})
    for (long r8 : new long[]{0, 1, 0x80000000L}) {
     conditionalWrite(packed, r4, r8);
     conditionalCases++;
    }
  println("Conditional gate passed=" + conditionalCases);
  println("Enable gate passed="+cases+"; direct block entry, stop=0x101d08; no full caller execution");
 }
}
