// Differential checksum test on A's original routine, with synthetic buffers.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90AD4Checksum extends GhidraScript {
    void runTo(EmulatorHelper em,long start,long end)throws Exception {
        em.writeRegister("PC",start);
        for(int n=0;n<2048;n++) {
            if(em.getExecutionAddress().getOffset()==end)return;
            if(!em.step(monitor))throw new IllegalStateException(em.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    long read(EmulatorHelper em,long address,int size)throws Exception {
        long v=0;for(byte b:em.readMemory(toAddr(address),size))v=(v<<8)|(b&255);return v;
    }
    void check(int value,int mode)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            int sum=0x46+1+value;
            int trailer=mode==0?0xaa:mode==1?(-sum&255):(~sum&255);
            em.writeMemory(toAddr(0x3d000),new byte[]{0x46,1,(byte)value,(byte)trailer});
            em.writeMemory(toAddr(0x368ce),new byte[]{(byte)0xa3});
            em.writeRegister("R4",mode==0?0:1);
            em.writeRegister("R5",0x3d000);em.writeRegister("R6",4);
            runTo(em,0x8988a,0x898c4);
            if(mode==0) {
                if(read(em,0x3d003,1)!=(-sum&255))throw new IllegalStateException("Generated checksum");
            } else if(read(em,0x368ce,1)!=(mode==1?0xa3:0xa7))
                throw new IllegalStateException("Validation mismatch");
        } finally {em.dispose();}
    }
    void buffers(int mode)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            byte[] dirty=new byte[96];java.util.Arrays.fill(dirty,(byte)0xa5);
            em.writeMemory(toAddr(0x3738a),dirty);
            em.writeMemory(toAddr(0x39d34),new byte[]{(byte)mode});
            runTo(em,0x89430,0x8947c);
            long tx=mode==0x32?0x365c0:0x3738a,rx=mode==0x32?0x36648:0x373ba;
            if(read(em,0x3737c,4)!=tx || read(em,0x37380,4)!=rx)
                throw new IllegalStateException("Buffer map mismatch");
            for(byte b:em.readMemory(toAddr(0x3738a),96))
                if((b&255)!=(mode==0x32?0xa5:0))throw new IllegalStateException("Buffer clearing mismatch");
        } finally {em.dispose();}
    }
    public void run()throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0xffffc))!=0xfc00e)
            throw new IllegalStateException("Expected A program");
        for(int value=0;value<256;value++)for(int mode=0;mode<3;mode++)check(value,mode);
        buffers(1);buffers(0x32);
        println("A d4 checksum cases passed=768; buffer preparation cases passed=2");
    }
}
