// Isolated A port 0xd4 receive handler; no device or OS execution.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90AD4Receive extends GhidraScript {
    void put(EmulatorHelper em,long a,int v)throws Exception {em.writeMemory(toAddr(a),new byte[]{(byte)v});}
    int get(EmulatorHelper em,long a)throws Exception{return em.readMemory(toAddr(a),1)[0]&255;}
    long number(EmulatorHelper em,long a,int size)throws Exception {
        long result=0;for(byte b:em.readMemory(toAddr(a),size))result=(result<<8)|(b&255);return result;
    }
    void sample(int value,int scenario)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            em.writeMemory(toAddr(0x37380),new byte[]{0,3,(byte)0xd0,0});
            em.writeMemory(toAddr(0xd4),new byte[]{0,(byte)value});
            em.writeMemory(toAddr(0x37388),new byte[]{0,0x55});
            em.writeMemory(toAddr(0x162),new byte[]{0,0});
            put(em,0x3d000,0xa5);put(em,0x368cc,scenario==0?0:1);
            put(em,0x368cb,2);put(em,0x368cd,3);
            put(em,0x39d34,scenario==2?0x32:0);
            put(em,0xd0,0);put(em,0xd2,scenario==3?8:0);
            put(em,0x368ce,0);put(em,0x111,0);
            em.writeRegister("PC",0x89b32);
            long stop=scenario==3?0x89bd8:0x89bdc;
            int steps=0;
            while(em.getExecutionAddress().getOffset()!=stop && steps++<128)
                if(!em.step(monitor))throw new IllegalStateException(em.getLastError());
            if(steps>=128)throw new IllegalStateException("Instruction limit");
            if(scenario==3) {
                if(get(em,0x3d000)!=0xa5 || number(em,0x37380,4)!=0x3d000 ||
                   get(em,0x368cc)!=1 || get(em,0x368ce)!=2 ||
                   em.readRegister("R5").intValue()!=0x8000)
                    throw new IllegalStateException("Error path mismatch");
            } else if(get(em,0x3d000)!=value || number(em,0x37380,4)!=0x3d001 ||
                      get(em,0x368cc)!=(scenario==0?1:2) ||
                      number(em,0x37388,2)!=(scenario==2?0x1388:1))
                throw new IllegalStateException("Receive path mismatch");
        } finally {em.dispose();}
    }
    public void run()throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0xffffc))!=0xfc00e)
            throw new IllegalStateException("Expected A program");
        for(int value:new int[]{0,0x46,0x98,255})
            for(int scenario=0;scenario<4;scenario++)sample(value,scenario);
        println("A port d4 cases passed=16; first/subsequent/special-count/error paths");
    }
}
