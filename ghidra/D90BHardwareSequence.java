// Execute the MMIO-writing leaf in synthetic memory; no hardware is modelled.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
import java.util.Arrays;
public class D90BHardwareSequence extends GhidraScript {
    static final long STOP=0x100000, FLAG=0x8f52e35cL;
    static final long[] BASES={0x4000d200L,0x4000d300L,0x4000d400L,0x4000d500L,0x4000d800L,0x4000d900L};
    static final long[] SINGLE={0x4000c010L,0x4000c210L,0x4000cc80L,0x40000010L};
    static final int[] VALUES={0xc000,0x8100,0,0x2200};
    long runTo(EmulatorHelper e,long start,long... stops)throws Exception {
        e.writeRegister("PC",start);
        for(int n=0;n<256;n++) {
            long pc=e.getExecutionAddress().getOffset();
            for(long stop:stops)if(pc==stop)return pc;
            if((currentProgram.getMemory().getShort(toAddr(pc))&65535)==0x1f40)
                throw new IllegalStateException("Unexpected OS boundary");
            if(!e.step(monitor))throw new IllegalStateException(e.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    void sequence(int fill)throws Exception {
        EmulatorHelper e=new EmulatorHelper(currentProgram);
        try {
            byte[] seed=new byte[0x44];Arrays.fill(seed,(byte)fill);
            for(long base:BASES)e.writeMemory(toAddr(base),seed);
            for(long a:SINGLE)e.writeMemory(toAddr(a),Arrays.copyOf(seed,4));
            e.writeRegister("RP",STOP);
            runTo(e,0x108624,STOP);
            for(int i=0;i<BASES.length;i++) {
                byte[] expected=seed.clone();expected[0]=0x40;expected[1]=0;
                if(i<4)Arrays.fill(expected,0x10,0x14,(byte)0);
                expected[0x40]=(byte)(i<4?8:0);expected[0x41]=0;
                if(!Arrays.equals(expected,e.readMemory(toAddr(BASES[i]),seed.length)))
                    throw new IllegalStateException("MMIO window mismatch "+Long.toHexString(BASES[i]));
            }
            for(int i=0;i<SINGLE.length;i++) {
                byte[] expected={(byte)(VALUES[i]>>8),(byte)VALUES[i],(byte)fill,(byte)fill};
                if(!Arrays.equals(expected,e.readMemory(toAddr(SINGLE[i]),4)))
                    throw new IllegalStateException("MMIO register mismatch");
            }
        } finally {e.dispose();}
    }
    void branch(int value)throws Exception {
        EmulatorHelper e=new EmulatorHelper(currentProgram);
        try {
            e.writeMemory(toAddr(FLAG+4),new byte[]{(byte)value});
            e.writeRegister("R8",FLAG);e.writeRegister("RP",STOP);
            boolean active=(value&0x40)!=0;
            long stop=runTo(e,0x10b3ce,0x1077ca,0x1a1e40);
            int after=e.readMemory(toAddr(FLAG+4),1)[0]&255;
            if(stop!=(active?0x1077ca:0x1a1e40) || after!=(active?value&0x3f:value) ||
               (active && e.readRegister("R4").intValue()!=0x15))
                throw new IllegalStateException("Cleanup prefix mismatch");
        } finally {e.dispose();}
    }
    public void run()throws Exception {
        for(int i=0;i<4;i++)if(Integer.toUnsignedLong(currentProgram.getMemory().getInt(toAddr(0x1bf33c+4*i)))!=BASES[i])
            throw new IllegalStateException("Expected B peripheral table");
        sequence(0xa5);sequence(0x5a);
        for(int v:new int[]{0x25,0x65,0xa5,0xe5})branch(v);
        println("B hardware sequence passed=2; secondary branch passed=4; RAM-backed MMIO, no physical effects modelled");
    }
}
