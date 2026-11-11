// AB-137: type-3 defaults and intervening type-1 masks.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Type3Defaults extends D90Type2Defaults {
    int checkedType3;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), w=Arrays.copyOf(base,base.length+3);
        w[base.length]=new long[]{0x13a2b8,0x13a2f6};
        w[base.length+1]=new long[]{0x13a814,0x13a826};
        w[base.length+2]=new long[]{0x1cab8c,0x1cabaf};
        return w;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), w=Arrays.copyOf(base,base.length+1);
        // The final byte abuts the previously mapped type-2 leading guard.
        w[base.length]=new long[]{0x8f53d970L,0x8f53da51L};
        return w;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        long r10=reg(c,"R10");
        require(r10==0x8f53c4d4L,"Type-1 base");
        byte[] settings=c.readMemory(a.getAddress(r10),160);
        byte[] stack=c.readMemory(a.getAddress(0x8f991204L),80);
        byte[] defaults=c.readMemory(a.getAddress(0x1cab8c),35);
        for(int flag=0;flag<2;flag++){
            c.writeRegister("PC",0x13a2b6);c.writeRegister("R10",r10);
            c.writeMemory(a.getAddress(0x8f53300fL),new byte[]{(byte)flag});
            c.writeMemory(a.getAddress(r10),settings);
            byte[] input=new byte[226];
            for(int i=0;i<input.length;i++)input[i]=(byte)(i+0x31);
            c.writeMemory(a.getAddress(0x8f53d970L),input);
            byte[] expected=input.clone();
            if(flag==1) for(int call=0;call<4;call++){
                until(c,d,0x13a814);
                require(reg(c,"R4")==0x8f53d987L+56*call && reg(c,"RP")==0x13a2ea,"Type-3 call");
                until(c,d,0x13a2ea);
                System.arraycopy(defaults,0,expected,23+56*call,35);
            }
            until(c,d,0x13a2f4);
            require(Arrays.equals(expected,c.readMemory(a.getAddress(0x8f53d970L),226)),"Complete type-3 block");
            byte[] settingsExpected=settings.clone();
            settingsExpected[16]|=(byte)0xf0;settingsExpected[11]&=(byte)0xfc;
            require(Arrays.equals(settingsExpected,c.readMemory(a.getAddress(r10),160)),"Type-1 masks");
            require(Arrays.equals(stack,c.readMemory(a.getAddress(0x8f991204L),80)),"Stack");
            checkedType3++;
        }
    }
    @Override public void run()throws Exception{
        super.run();require(checkedType3==16,"Case count");
        println("AB-137: 16 type-3 cases and 32 helper calls passed");
    }
}
