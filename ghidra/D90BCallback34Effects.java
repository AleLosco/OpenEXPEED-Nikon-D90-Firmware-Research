// Extend callback replay through the real 0x10a806 leaf and to OS entry in 0x1011b0.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90BCallback34Effects extends GhidraScript {
    static final long A=0x8f52edf6L, B=0x8f52edf5L, FLAG=0x8f530e1eL;
    void put(EmulatorHelper em,long addr,int value)throws Exception {
        em.writeMemory(toAddr(addr),new byte[]{(byte)value});
    }
    int get(EmulatorHelper em,long addr)throws Exception {
        return em.readMemory(toAddr(addr),1)[0]&255;
    }
    long execute(EmulatorHelper em)throws Exception {
        em.writeRegister("PC",0x11151c);
        for(int n=0;n<96;n++) {
            long pc=em.getExecutionAddress().getOffset();
            if(pc==0x1011b8 || pc==0x110412 || pc==0x1115b8)return pc;
            if((currentProgram.getMemory().getShort(toAddr(pc))&65535)==0x1f40)
                throw new IllegalStateException("Unexpected OS interrupt");
            if(!em.step(monitor))throw new IllegalStateException(em.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    int sample(int a,int b)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            em.writeRegister("SP",0x8f9f0000L);em.writeRegister("RP",0);
            put(em,A,a);put(em,A+1,0x5a);put(em,A+2,0xc3);
            put(em,B,b);put(em,FLAG,0x85);put(em,0x8f52e35cL,0x25);
            int al=a&3,bl=b&3,changed=a,which=3;
            long expected=0x1115b8;
            if(al==0 && bl==1) {expected=0x1011b8;changed=(a&0xfc)|1;which=0;}
            else if(al==0 && bl==3) {expected=0x110412;which=1;}
            else if(al==1 && bl==0) {expected=0x1011b8;changed=a&0xf8;which=0;}
            else if(al>=2 && bl!=3) {expected=0x1115b8;which=2;}
            long stop=execute(em);
            boolean valid=stop==expected && get(em,A)==changed && get(em,B)==b &&
                get(em,A+1)==0x5a && get(em,A+2)==0xc3 &&
                get(em,FLAG)==(which>=2?0xa5:0x85);
            if(which==1)valid &= em.readRegister("R4").intValue()==0x33;
            valid &= get(em,0x8f52e35cL)==(which==2?0xa5:0x25);
            if(which==0)valid &= em.readRegister("R4").intValue()==0x15 && em.readRegister("R12").intValue()==-53;
            if(which>=2)valid &= em.readRegister("R4").intValue()==0xc &&
                em.readRegister("R5").intValue()==0x800;
            if(!valid)throw new IllegalStateException(String.format(
                "a=%02x b=%02x stop=%x expected=%x aAfter=%02x",a,b,stop,expected,get(em,A)));
            return which;
        } finally {em.dispose();}
    }
    public void run()throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0x1c42f0))!=0x11151c)
            throw new IllegalStateException("Expected B descriptor 0x34 callback");
        int[] counts=new int[4];
        for(int upperA:new int[]{0,0x54,0xa8,0xfc})
            for(int upperB:new int[]{0,0x54,0xa8,0xfc})
                for(int a=0;a<4;a++)for(int b=0;b<4;b++)counts[sample(upperA|a,upperB|b)]++;
        println(String.format("B callback34 effects passed=256; OSentry=%d notify33=%d leafAndEvent=%d directEvent=%d; real leaf executed, no INT executed",
            counts[0],counts[1],counts[2],counts[3]));
    }
}
