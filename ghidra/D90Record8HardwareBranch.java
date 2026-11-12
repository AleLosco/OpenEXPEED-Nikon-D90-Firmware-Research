// AB-159: return from the OS request and probe the first peripheral-dependent branch.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Record8HardwareBranch extends D90Record8Service48 {
    private int branches;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), result=Arrays.copyOf(base,base.length+2);
        result[base.length]=new long[]{0x11034c,0x110350};
        result[base.length+1]=new long[]{0x10fd40,0x10fd66};
        return result;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), result=Arrays.copyOf(base,base.length+1);
        result[base.length]=new long[]{0x50000104,0x50000108};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p)throws Exception{
        super.afterInitialization(cpu,decoder,p);
        var space=p.getLanguage().getDefaultSpace();
        returnUserService23(cpu,decoder,p,0x11034c);
        until(cpu,decoder,0x10fd5c);
        require(cpu.readMemory(space.getAddress(0x60),1)[0]==0x40 && cpu.readMemory(space.getAddress(0x61),1)[0]==0x41,"Control bytes");
        require(cpu.readMemory(space.getAddress(0x63),1)[0]==0 && word(cpu,space,0x100)==0x278cf,"Additional control values");
        require(cpu.readMemory(space.getAddress(0x10e),2)[0]==0 && cpu.readMemory(space.getAddress(0x10e),2)[1]==0,"Halfword clear");
        for(int value:new int[]{0,0x40}){
            cpu.writeRegister("PC",0x10fd5c);
            cpu.writeMemory(space.getAddress(0x50000107),new byte[]{(byte)value});
            until(cpu,decoder,value==0 ? 0x10fdba : 0x10fd66);
            require(cpu.readMemory(space.getAddress(0x50000107),1)[0]==(byte)value,"Input byte preserved");
            branches++;
        }
    }
    @Override public void run()throws Exception{
        super.run();require(branches==16,"Branch count");
        println("AB-159: 16 controlled peripheral branches passed after the original service return");
    }
}
