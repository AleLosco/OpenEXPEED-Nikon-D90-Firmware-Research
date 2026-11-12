// AB-160: compare two peripheral-selected preparations through service -47.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Record8Modes extends D90Record8HardwareBranch {
    private int cases;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), result=Arrays.copyOf(base,base.length+4);
        result[base.length]=new long[]{0x10fd66,0x10fd96};
        result[base.length+1]=new long[]{0x10fdba,0x10fdf6};
        result[base.length+2]=new long[]{0x40614,0x40616};
        result[base.length+3]=new long[]{0x41af4,0x41b1c};
        return result;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), result=Arrays.copyOf(base,base.length+1);
        result[base.length]=new long[]{0x8f52f7fcL,0x8f52f7ffL};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper cpu,PseudoDisassembler decoder,ProgramDB p)throws Exception{
        super.afterInitialization(cpu,decoder,p);
        var space=p.getLanguage().getDefaultSpace();
        String[] names={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","AC","FP","RP","MDL","MDH","PS","C","V","Z","N","I","S","T","D0","D1","ILM","SP","SSP","USP"};
        long[] saved=new long[names.length];
        for(int i=0;i<names.length;i++) saved[i]=reg(cpu,names[i]);
        byte[] initialLow=cpu.readMemory(space.getAddress(0),4096);
        byte[] os=cpu.readMemory(space.getAddress(0x68000000),4096);
        for(int value:new int[]{0,0x40}){
            for(int i=0;i<names.length;i++) cpu.writeRegister(names[i],saved[i]);
            cpu.writeMemory(space.getAddress(0),initialLow);
            cpu.writeMemory(space.getAddress(0x8f52f7fcL),new byte[]{0x55,0x55,0x55});
            cpu.writeMemory(space.getAddress(0x50000107),new byte[]{(byte)value});
            cpu.writeRegister("PC",0x10fd5c);
            long site=value==0 ? 0x10fdf4 : 0x10fd94;
            int mask=value==0 ? -2 : -3;
            until(cpu,decoder,site);
            byte[] expected=initialLow.clone();
            expected[0x41]&=(byte)0xbf;
            expected[0x40]=(byte)0xbf;
            int half=Short.toUnsignedInt(ByteBuffer.wrap(initialLow).getShort(0x42));
            int next=value==0 ? (half&0xcfff)|0x2000 : half|0x3000;
            ByteBuffer.wrap(expected).putShort(0x42,(short)next);
            require(Arrays.equals(expected,cpu.readMemory(space.getAddress(0),4096)),"Low peripheral RAM");
            require(Arrays.equals(cpu.readMemory(space.getAddress(0x8f52f7fcL),3),new byte[]{0x55,(byte)(value==0?1:3),0x55}),"Mode byte and guards");
            require(reg(cpu,"R4")==3 && reg(cpu,"R5")==Integer.toUnsignedLong(mask) && reg(cpu,"R12")==0xffffffd1L,"Service -47 arguments");
            enterUserOsService(cpu,decoder,p,site,3,mask,-47,0x41af4,0x40614);
            until(cpu,decoder,0x406c6);
            require(reg(cpu,"R12")==0 && Arrays.equals(os,cpu.readMemory(space.getAddress(0x68000000),4096)),"Object flags remain 0x80");
            cases++;
        }
    }
    @Override public void run()throws Exception{
        super.run();require(cases==16,"Case count");
        println("AB-160: 16 mode preparations and service -47 returns verified");
    }
}
