// B state gate 0x1011ce: synthetic RAM, stop before mask helpers or any INT.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90BStateGate extends GhidraScript {
    void put(EmulatorHelper e,long a,long v,int size)throws Exception {
        byte[] b=new byte[size];for(int i=size-1;i>=0;i--){b[i]=(byte)v;v>>>=8;}
        e.writeMemory(toAddr(a),b);
    }
    int get(EmulatorHelper e,long a)throws Exception{return e.readMemory(toAddr(a),1)[0]&255;}
    void sample(int mode,int low,int bit)throws Exception {
        EmulatorHelper e=new EmulatorHelper(currentProgram);
        try {
            for(String r:new String[]{"R8","R9","FP","RP"})e.writeRegister(r,0);
            e.writeRegister("SP",0x8f9f0000L);
            int a=0xa0|bit|low;
            put(e,0x8f52edf6L,a,1);put(e,0x8f52e0ecL,0x55,1);
            put(e,0x8f549e1eL,mode==0?1:0,2);
            put(e,0x8f52e348L,mode>=2?0x80:0,1);
            put(e,0x8f534191L,0,1);
            put(e,0x8f87156cL,7,4);put(e,0x8f871570L,mode==3?7:8,4);
            put(e,0x8f871538L,2,4);
            e.writeRegister("PC",0x1011ce);
            long stop=0;
            for(int i=0;i<128;i++) {
                long pc=e.getExecutionAddress().getOffset();
                if(pc==0x106ea2 || pc==0x106e14){stop=pc;break;}
                if((currentProgram.getMemory().getShort(toAddr(pc))&65535)==0x1f40)
                    throw new IllegalStateException("Unmodelled OS boundary");
                if(!e.step(monitor))throw new IllegalStateException(e.getLastError());
            }
            boolean clear=mode==3 || (mode==1 && low==0);
            int expectedA=clear?(a&0xfb):a;
            if(mode!=0 && !clear && low==1)expectedA|=4;
            if(stop!=(clear?0x106e14:0x106ea2) || get(e,0x8f52edf6L)!=expectedA ||
               get(e,0x8f52e0ecL)!=(clear?0:0x55) ||
               e.readRegister("R4").intValue()!=(clear?0xfffff7ff:0x800))
                throw new IllegalStateException("Gate mismatch mode="+mode+" low="+low);
        } finally {e.dispose();}
    }
    public void run()throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0x1011d6))!=0x8f52edf6)
            throw new IllegalStateException("Expected B state gate");
        for(int mode=0;mode<4;mode++)for(int low=0;low<4;low++)for(int bit:new int[]{0,4})sample(mode,low,bit);
        println("B state gate passed=32; stops before mask helpers; alternate OS-query branch untested");
    }
}
