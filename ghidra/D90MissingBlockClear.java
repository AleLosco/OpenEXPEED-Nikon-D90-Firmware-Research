// AB-138: flag-controlled clears with complete neighboring-byte comparisons.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90MissingBlockClear extends D90Type3Defaults {
    private int clearCases;
    private static final long[] TARGETS={0x8f9237acL,0x8f922768L,0x8f94e116L};
    private static final int[] LENGTHS={68,68,2};

    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows();
        long[][] windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{0x13a2f6,0x13a338};
        return windows;
    }

    @Override long[][] extraSyntheticWindows() {
        long[][] base=super.extraSyntheticWindows();
        long[][] windows=Arrays.copyOf(base,base.length+3);
        for(int i=0;i<TARGETS.length;i++)
            windows[base.length+i]=new long[]{TARGETS[i]-4,TARGETS[i]+LENGTHS[i]+4};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var space=p.getLanguage().getDefaultSpace();
        byte[] stack=c.readMemory(space.getAddress(0x8f991204L),80);
        byte[] flags=c.readMemory(space.getAddress(0x8f53300cL),26);
        for(int mask=0;mask<8;mask++) {
            byte[] expectedFlags=flags.clone();
            for(int i=0;i<3;i++) expectedFlags[9+i]=(byte)((mask>>>i)&1);
            c.writeMemory(space.getAddress(0x8f53300cL),expectedFlags);
            byte[][] expected=new byte[3][];
            for(int i=0;i<3;i++) {
                byte[] input=new byte[LENGTHS[i]+8];
                for(int j=0;j<input.length;j++) input[j]=(byte)(j+0x41+i);
                c.writeMemory(space.getAddress(TARGETS[i]-4),input);
                expected[i]=input.clone();
                if((mask&(1<<i))!=0) Arrays.fill(expected[i],4,4+LENGTHS[i],(byte)0);
            }
            c.writeRegister("PC",0x13a2f4);
            until(c,d,0x13a336);
            for(int i=0;i<3;i++)
                require(Arrays.equals(expected[i],c.readMemory(space.getAddress(TARGETS[i]-4),expected[i].length)),"Clear and guards "+i);
            require(Arrays.equals(expectedFlags,c.readMemory(space.getAddress(0x8f53300cL),26)),"Flags retained");
            require(Arrays.equals(stack,c.readMemory(space.getAddress(0x8f991204L),80)),"Stack retained");
            clearCases++;
        }
    }

    @Override public void run() throws Exception {
        super.run();
        require(clearCases==64,"Case count");
        println("AB-138: 64 flag-9/10/11 combinations passed at 0x13a336");
    }
}
