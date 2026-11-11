// AB-140: flag-23 initialization followed by the original 624-byte copy.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Type23Copy extends D90MissingArrayClear {
    private int copies23;
    private static final long SOURCE=0x8f6e3f2cL;
    @Override long[][] extraWindows() {
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+2);
        windows[base.length]=new long[]{0x13a366,0x13a38e};
        windows[base.length+1]=new long[]{0x1ad966,0x1ad97e};
        return windows;
    }
    @Override long[][] extraSyntheticWindows() {
        long[][] base=super.extraSyntheticWindows(), windows=Arrays.copyOf(base,base.length+1);
        windows[base.length]=new long[]{SOURCE-4,SOURCE+1248+4};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var space=p.getLanguage().getDefaultSpace();
        byte[] settings=c.readMemory(space.getAddress(0x8f53c4d4L),160);
        byte[] stack=c.readMemory(space.getAddress(0x8f991204L),80);
        for(int flag=0;flag<2;flag++) {
            byte[] data=new byte[1256];
            for(int i=0;i<data.length;i++) data[i]=(byte)(1+i%251);
            byte[] expected=data.clone();
            byte[] expectedSettings=settings.clone();
            if(flag==1) {
                Arrays.fill(expected,4,12,(byte)0);
                expectedSettings[1]&=(byte)0xf7;
            }
            System.arraycopy(expected,4,expected,628,624);
            c.writeMemory(space.getAddress(SOURCE-4),data);
            c.writeMemory(space.getAddress(0x8f53c4d4L),settings);
            c.writeMemory(space.getAddress(0x8f533023L),new byte[]{(byte)flag});
            c.writeRegister("PC",0x13a364);
            until(c,d,0x13a38c);
            require(Arrays.equals(expected,c.readMemory(space.getAddress(SOURCE-4),1256)),"Source, destination and guards");
            require(Arrays.equals(expectedSettings,c.readMemory(space.getAddress(0x8f53c4d4L),160)),"Settings mask");
            require(Arrays.equals(stack,c.readMemory(space.getAddress(0x8f991204L),80)),"Stack");
            copies23++;
        }
    }
    @Override public void run() throws Exception {
        super.run();
        require(copies23==16,"Case count");
        println("AB-140: 16 flag-23 initializations and full 624-byte copies passed");
    }
}
