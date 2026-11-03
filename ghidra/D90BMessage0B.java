// Original helper/controller, stopped before notification or at real return.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90BMessage0B extends GhidraScript {
    static final long DATA=0x8f52f1baL, STOP=0x100000;
    void put(EmulatorHelper e,long a,long v,int n)throws Exception {
        byte[] b=new byte[n];for(int i=n-1;i>=0;i--){b[i]=(byte)v;v>>>=8;}e.writeMemory(toAddr(a),b);
    }
    long get(EmulatorHelper e,long a,int n)throws Exception {
        long v=0;for(byte b:e.readMemory(toAddr(a),n))v=v<<8|(b&255);return v;
    }
    long execute(EmulatorHelper e,long pc)throws Exception {
        e.writeRegister("PC",pc);
        for(int i=0;i<256;i++) {
            long at=e.getExecutionAddress().getOffset();
            if(at==STOP || at==0x110412)return at;
            if((currentProgram.getMemory().getShort(toAddr(at))&65535)==0x1f40)
                throw new IllegalStateException("Unexpected OS interrupt");
            if(!e.step(monitor))throw new IllegalStateException(e.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    EmulatorHelper prepare(int value)throws Exception {
        EmulatorHelper e=new EmulatorHelper(currentProgram);
        e.writeRegister("SP",0x8f9f0000L);e.writeRegister("RP",STOP);
        for(String r:new String[]{"FP","R8","R9"})e.writeRegister(r,0);
        e.writeMemory(toAddr(DATA),new byte[]{0x12,(byte)value,0x34,0x56,0x78});return e;
    }
    void verify(EmulatorHelper e,long stop,int value,boolean notify)throws Exception {
        if(stop!=(notify?0x110412:STOP) || get(e,DATA+1,1)!=value || get(e,DATA,1)!=0x12 ||
           get(e,DATA+2,1)!=0x34 || get(e,DATA+3,1)!=0x56 || get(e,DATA+4,1)!=0x78 ||
           (notify && e.readRegister("R4").intValue()!=0xb))throw new IllegalStateException("Message0b mismatch");
    }
    void helper(int value,int requested)throws Exception {
        EmulatorHelper e=prepare(value);
        try {
            e.writeRegister("R4",requested);
            verify(e,execute(e,0x106f14),(value&0xfb)|(requested<<2),((value>>2)&1)!=requested);
        } finally {e.dispose();}
    }
    void controller(int state,int gate,int input,int value)throws Exception {
        EmulatorHelper e=prepare(value);
        try {
            put(e,0x8f52e484L,state,4);put(e,0x8f52e4abL,gate,1);put(e,0x8f52f1aaL,input,1);
            // Stop at the first notification: an initial clear can precede any later set.
            boolean clearFirst=(value&4)!=0;
            boolean set=!clearFirst && (state&0x800)==0 && gate!=0 && (state==1 || (state==0x41 && (input&12)!=0));
            int expected=clearFirst?(value&0xfb):set?(value|4):value;
            verify(e,execute(e,0x106f48),expected,clearFirst||set);
        } finally {e.dispose();}
    }
    public void run()throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0x106f1a))!=(int)DATA)
            throw new IllegalStateException("Expected B helper");
        for(int value=0;value<256;value++)for(int r=0;r<2;r++)helper(value,r);
        for(int state:new int[]{0,1,0x41,0x800,0x801,0x841})for(int gate:new int[]{0,1})
            for(int input:new int[]{0,4,8,12})for(int value:new int[]{0xa1,0xa5})controller(state,gate,input,value);
        println("B message0b helper passed=512; controller passed=96; notification calls not executed");
    }
}
