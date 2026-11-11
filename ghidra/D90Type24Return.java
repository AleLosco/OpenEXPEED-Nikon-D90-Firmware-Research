// AB-141: flag-24 delayed store and original third-call return.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Type24Return extends D90Type23Copy {
    private int returns24;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), w=Arrays.copyOf(base,base.length+2);
        w[base.length]=new long[]{0x13a38e,0x13a3ac};
        w[base.length+1]=new long[]{0x1ae4c0,0x1ae4cc};
        return w;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), w=Arrays.copyOf(base,base.length+2);
        w[base.length]=new long[]{0x8f6e1162L,0x8f6e1168L};
        w[base.length+1]=new long[]{0x8f549bedL,0x8f549bf0L};
        return w;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        long sp=reg(c,"SP"), flagsBase=reg(c,"R9");
        ByteBuffer saved=ByteBuffer.wrap(c.readMemory(a.getAddress(sp),20));
        String[] names={"RP","R8","R9","R10","R11"};
        long[] values=new long[5];
        for(int i=0;i<5;i++) values[i]=Integer.toUnsignedLong(saved.getInt());
        require(values[0]==0x1000e2,"Third-call return address");
        byte[] stack=c.readMemory(a.getAddress(0x8f991204L),80);
        for(int flag=0;flag<2;flag++){
            c.writeRegister("PC",0x13a38c);c.writeRegister("SP",sp);c.writeRegister("R9",flagsBase);
            c.writeMemory(a.getAddress(0x8f533024L),new byte[]{(byte)flag});
            byte[] target={0x11,0x22,0x33,0x44,0x55,0x66};
            c.writeMemory(a.getAddress(0x8f6e1162L),target);
            c.writeMemory(a.getAddress(0x8f549bedL),new byte[]{0x55,0x66,0x77});
            until(c,d,0x1000e2);
            if(flag==1){target[2]=0;target[3]=0;}
            require(Arrays.equals(target,c.readMemory(a.getAddress(0x8f6e1162L),6)),"Flag-24 halfword and guards");
            require(Arrays.equals(new byte[]{0x55,0,0x77},c.readMemory(a.getAddress(0x8f549bedL),3)),"Final byte and guards");
            for(int i=0;i<5;i++)require(reg(c,names[i])==values[i],"Restored "+names[i]);
            require(reg(c,"SP")==sp+20,"Returned stack pointer");
            require(Arrays.equals(stack,c.readMemory(a.getAddress(0x8f991204L),80)),"Stack bytes preserved");
            returns24++;
        }
    }
    @Override public void run()throws Exception{
        super.run();require(returns24==16,"Case count");
        println("AB-141: 16 flag-24 branches and third-call returns passed at 0x1000e2");
    }
}
