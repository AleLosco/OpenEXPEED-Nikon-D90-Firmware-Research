// AB-139: nine flag-indexed clears with full stride and guard checks.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90MissingArrayClear extends D90MissingBlockClear {
    private int arrayCases;
    private static final long BASE=0x8f94cc9cL;
    private static final int STRIDE=578;
    private static final int[] MASKS={0,511,0x155,0xaa,1,2,4,8,16,32,64,128,256};

    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows();
        long[][] windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{0x13a338,0x13a366};
        return windows;
    }
    @Override long[][] extraSyntheticWindows() {
        long[][] base=super.extraSyntheticWindows();
        long[][] windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{BASE-4,BASE+9*STRIDE+4};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var space=p.getLanguage().getDefaultSpace();
        byte[] originalFlags=c.readMemory(space.getAddress(0x8f53300cL),26);
        byte[] stack=c.readMemory(space.getAddress(0x8f991204L),80);
        for(int mask:MASKS) {
            byte[] flags=originalFlags.clone();
            for(int i=0;i<9;i++) flags[12+i]=(byte)((mask>>>i)&1);
            c.writeMemory(space.getAddress(0x8f53300cL),flags);
            byte[] data=new byte[9*STRIDE+8];
            for(int i=0;i<data.length;i++) data[i]=(byte)(1+i%251);
            c.writeMemory(space.getAddress(BASE-4),data);
            byte[] expected=data.clone();
            for(int i=0;i<9;i++) if((mask&(1<<i))!=0)
                Arrays.fill(expected,4+i*STRIDE,4+i*STRIDE+64,(byte)0);
            c.writeRegister("PC",0x13a336);
            until(c,d,0x13a364);
            require(reg(c,"R4")==0x1452 && reg(c,"R5")==21,"Loop termination");
            require(Arrays.equals(expected,c.readMemory(space.getAddress(BASE-4),expected.length)),"Array and guards");
            require(Arrays.equals(flags,c.readMemory(space.getAddress(0x8f53300cL),26)),"Flags");
            require(Arrays.equals(stack,c.readMemory(space.getAddress(0x8f991204L),80)),"Stack");
            arrayCases++;
        }
    }
    @Override public void run() throws Exception {
        super.run();
        require(arrayCases==104,"Case count");
        println("AB-139: 104 nine-record clear cases passed at 0x13a364");
    }
}
