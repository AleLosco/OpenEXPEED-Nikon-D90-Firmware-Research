// Synthetic state/MMIO. Start after OS acquisition; never execute OS or callees.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90BMaskConsumers extends GhidraScript {
    static final long STATE=0x8f52e484L, ENABLE=0x8f52e480L, PORT=0x50000106L, FLAG=0x8f52e35cL;
    void put(EmulatorHelper e,long a,long v,int size)throws Exception {
        byte[] b=new byte[size];for(int i=size-1;i>=0;i--){b[i]=(byte)v;v>>>=8;}
        e.writeMemory(toAddr(a),b);
    }
    long get(EmulatorHelper e,long a,int size)throws Exception {
        long v=0;for(byte b:e.readMemory(toAddr(a),size))v=(v<<8)|(b&255);return v;
    }
    long runTo(EmulatorHelper e,long start,long... stops)throws Exception {
        e.writeRegister("PC",start);
        for(int i=0;i<96;i++) {
            long pc=e.getExecutionAddress().getOffset();
            for(long stop:stops)if(pc==stop)return pc;
            if((currentProgram.getMemory().getShort(toAddr(pc))&65535)==0x1f40)
                throw new IllegalStateException("Unexpected INT");
            if(!e.step(monitor))throw new IllegalStateException(e.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    void masks(boolean set,long initial,long enabled,int port)throws Exception {
        EmulatorHelper e=new EmulatorHelper(currentProgram);
        try {
            put(e,STATE,initial,4);put(e,ENABLE,enabled,4);put(e,PORT,port,1);
            e.writeRegister("R5",STATE);e.writeRegister("R6",set?0x800:0xfffff7ffL);
            e.writeRegister("RP",0);
            long value=set?(initial|0x800):(initial&0xfffff7ffL);
            boolean overlap=(value&enabled)!=0;
            long expected=set?(overlap?0x106f48:0x106ed8):(overlap?0x106f48:0x106f14);
            int expectedPort=set?(overlap?port|2:port):(overlap?port:port&0xfd);
            long stop=runTo(e,set?0x106eb6:0x106e28,0x106f48,0x106f14,0x106ed8);
            if(stop!=expected || get(e,STATE,4)!=value || get(e,ENABLE,4)!=enabled || get(e,PORT,1)!=expectedPort)
                throw new IllegalStateException("Mask/port mismatch");
            if(!set && !overlap && e.readRegister("R4").intValue()!=0)
                throw new IllegalStateException("Clear helper argument");
            if(set && !overlap && (e.readRegister("R4").intValue()!=0x16 || e.readRegister("R12").intValue()!=-55))
                throw new IllegalStateException("OS release argument");
        } finally {e.dispose();}
    }
    void reader(int flag,boolean event)throws Exception {
        EmulatorHelper e=new EmulatorHelper(currentProgram);
        try {
            put(e,FLAG,flag,1);e.writeRegister("R8",FLAG);e.writeRegister("RP",0);
            long expected;
            if(event)expected=(flag&0x80)!=0?0x10aee2:0x10aed4;
            else expected=(flag&0x80)==0?0x10b0d4:(flag&0x40)!=0?0x10b3ce:0x108624;
            long stop=event?runTo(e,0x10aec6,0x10aee2,0x10aed4):
                runTo(e,0x10b062,0x10b0d4,0x10b3ce,0x108624);
            if(stop!=expected || get(e,FLAG,1)!=flag)throw new IllegalStateException("Reader branch mismatch");
            if(event && (flag&0x80)==0 && (e.readRegister("R4").intValue()!=5 || e.readRegister("R5").intValue()!=0x2000))
                throw new IllegalStateException("Reader event arguments");
        } finally {e.dispose();}
    }
    public void run()throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0x106ea8))!=(int)STATE ||
           currentProgram.getMemory().getInt(toAddr(0x10a808))!=(int)FLAG)
            throw new IllegalStateException("Expected B code");
        for(boolean set:new boolean[]{false,true})
            for(long state:new long[]{0,0x800,0x100,0x900,0xffffffffL})
                for(long enable:new long[]{0,0x800,0x100,0x900})
                    for(int port:new int[]{0x55,0x57})masks(set,state,enable,port);
        for(int flag:new int[]{0x25,0x65,0xa5,0xe5})for(boolean event:new boolean[]{false,true})reader(flag,event);
        println("B masks passed=80; bit80 readers passed=8; OS acquisition omitted; no INT or downstream callee executed");
    }
}
