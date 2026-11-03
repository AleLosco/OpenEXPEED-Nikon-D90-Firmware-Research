// A port 0x84: isolated builder, receive-copy and checksum probes.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90A84Protocol extends GhidraScript {
    void runTo(EmulatorHelper em,long start,long end)throws Exception {
        em.writeRegister("PC",start);
        for(int n=0;n<2048;n++) {
            if(em.getExecutionAddress().getOffset()==end)return;
            if(!em.step(monitor))throw new IllegalStateException(em.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    void put(EmulatorHelper em,long a,int v)throws Exception{em.writeMemory(toAddr(a),new byte[]{(byte)v});}
    int get(EmulatorHelper em,long a)throws Exception{return em.readMemory(toAddr(a),1)[0]&255;}
    void checksum(int value,int mode)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            int sum=0x47+value, trailer=mode==1?(~sum&255):((sum+1)&255);
            int initial=mode==2?0x10:0;
            em.writeMemory(toAddr(0x39dc5),new byte[]{0x46,1,(byte)value,(byte)trailer});
            put(em,0x36930,initial);em.writeRegister("R4",3);
            runTo(em,0xac59a,0xac5c8);
            int expected=initial | (trailer==((sum+1)&255)?0:0x10);
            if(get(em,0x36930)!=expected)throw new IllegalStateException("Checksum mismatch");
        } finally{em.dispose();}
    }
    void builder(int key,int header,int count)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            em.writeRegister("SP",0x3e000);
            em.writeRegister("R10",0);em.writeRegister("R11",0);
            em.writeMemory(toAddr(0x37476),new byte[]{(byte)(key>>8),(byte)key});
            put(em,0x37474,0);put(em,0x36931,0);put(em,0x36955,0);
            runTo(em,0xab00a,0xab874);
            if(get(em,0x39d93)!=header || get(em,0x39dfa)!=count ||
               get(em,0x39df9)!=0 || (get(em,0x37474)&3)!=1)
                throw new IllegalStateException("Builder mismatch");
        } finally{em.dispose();}
    }
    void receive(int value,int index)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            em.writeRegister("R8",0x39dc5);em.writeRegister("R10",0x39dfb);
            put(em,0x39dfb,index);
            put(em,0x39dc5+index+1,0xa5);
            em.writeMemory(toAddr(0x84),new byte[]{0,(byte)value});
            runTo(em,0xac87e,0xac894);
            if(get(em,0x39dc5+index)!=value || get(em,0x39dc5+index+1)!=0 ||
               get(em,0x39dfb)!=index+1)throw new IllegalStateException("Receive copy mismatch");
        }finally{em.dispose();}
    }
    public void run()throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0xffffc))!=0xfc00e)
            throw new IllegalStateException("Expected A map");
        for(int v=0;v<256;v++)for(int mode=0;mode<3;mode++)checksum(v,mode);
        for(int[] c:new int[][]{{0xa000,0xa0,22},{0xa100,0xa1,18},{0xa200,0xa2,46},
                               {0xa300,0xa3,2},{0x0046,0xa1,18},{0x4600,0xa1,18}})
            builder(c[0],c[1],c[2]);
        for(int v:new int[]{0,0x46,0x98,255})for(int i:new int[]{0,1,45})receive(v,i);
        println("A84 passed: 768 checksum, 6 builder, 12 receive-copy cases; B-style checksum coincides at values 0x38 and 0xb8 only");
    }
}
