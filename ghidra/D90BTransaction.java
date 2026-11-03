// Isolated B transaction replay. Device input and OS boundaries are explicit.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;
public class D90BTransaction extends GhidraScript {
    static final long S=0x8f52f7feL;
    void put(EmulatorHelper em,long a,int v)throws Exception {em.writeMemory(toAddr(a),new byte[]{(byte)v});}
    int get(EmulatorHelper em,long a)throws Exception{return em.readMemory(toAddr(a),1)[0]&255;}
    long run(EmulatorHelper em,long start,long end)throws Exception {
        em.writeRegister("PC",start);
        for(int n=0;n<512;n++) {
            long pc=em.getExecutionAddress().getOffset();
            if(pc==end)return pc;
            // Stop before OS software interrupts; never execute a synthesized OS.
            if((currentProgram.getMemory().getShort(toAddr(pc))&65535)==0x1f40)return pc;
            if(!em.step(monitor))throw new IllegalStateException(em.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    void exact(EmulatorHelper em,long start,long end)throws Exception {
        if(run(em,start,end)!=end)throw new IllegalStateException("Unexpected OS boundary");
    }
    void expect(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    long feed(EmulatorHelper em,int input)throws Exception {
        em.writeMemory(toAddr(0x64),new byte[]{0,(byte)input});
        return run(em,0x111f46,0x111f64);
    }
    void transfer(EmulatorHelper em,int input,int state,int output)throws Exception {
        expect(feed(em,input)==0x111f64,"Unexpected event");
        expect(get(em,S)==state && get(em,0x65)==output,"State/output mismatch");
    }
    void sample(int value,int peer,int length,boolean corrupt)throws Exception {
        EmulatorHelper em=new EmulatorHelper(currentProgram);
        try {
            em.writeRegister("SP",0x8f9f0000L);
            for(String reg:new String[]{"R9","R10","R11"})em.writeRegister(reg,0);
            put(em,0x50000106,0);put(em,0x8f52edeeL,value);put(em,0x8f52f7fcL,0x46);
            exact(em,0x10feba,0x10fedc);
            expect(get(em,0x65)==0x46 && get(em,S)==1,"Initial command");
            expect(feed(em,peer)==0x111934,"Initial reply event");
            expect(get(em,0x8f52f7fbL)==peer && get(em,S)==0,"Reply storage");
            // Omit queue lock/unlock and its flag clear, preserving selected descriptor.
            exact(em,0x10ff08,0x10ff60);
            exact(em,0x10ff84,0x110040);
            expect(get(em,0x8f52f6f4L)==value && get(em,0x8f52f7f4L)==0x46,"Prepared data");
            expect(get(em,0x8f52f7f5L)==peer && get(em,0x8f52f7f7L)==1,"Prepared counts");
            exact(em,0x110046,0x110066); // Stop before clear-event OS call.
            exact(em,0x11006a,0x1100fc);
            int check=(~(0x47+value))&255;
            if(peer==0) {
                expect(get(em,S)==8 && get(em,0x65)==1,"TX-first entry");
                transfer(em,0,9,value);transfer(em,0,10,check);transfer(em,0,11,0);
            } else expect(get(em,S)==2 && get(em,0x65)==0,"RX-first entry");
            transfer(em,length,length==0?(peer==0?13:4):(peer==0?12:3),0);
            int sum=peer+length;
            for(int i=0;i<length;i++) {
                int data=0x30+i;sum+=data;
                transfer(em,data,i+1==length?(peer==0?13:4):(peer==0?12:3),0);
                expect(get(em,0x8f52f5f4L+i)==data,"RX data");
            }
            int trailer=(~sum)&255;if(corrupt)trailer^=1;
            long stop=feed(em,trailer);
            if(peer==0 || corrupt) {
                expect(stop==(peer==0?(corrupt?0x111c94:0x111c88):0x111a16),"Checksum event boundary");
                expect(em.readRegister("R5").intValue()==(corrupt?0x20:0x10),"Event value");
                expect(get(em,S)==0,"Final idle state");
            } else {
                expect(stop==0x111f64 && get(em,S)==5 && get(em,0x65)==1,"Validated RX then TX");
                transfer(em,0,6,value);transfer(em,0,7,check);
                expect(feed(em,0)==0x111af0 && get(em,S)==0,"TX completion event");
            }
        } finally {em.dispose();}
    }
    public void run()throws Exception {
        int cases=0;
        for(int value:new int[]{0,0x20,0x21,255})
            for(int peer:new int[]{0,0x46})
                for(int length:new int[]{0,2})
                    for(boolean corrupt:new boolean[]{false,true}) {sample(value,peer,length,corrupt);cases++;}
        println("B transaction cases passed="+cases+"; OS events inspected, not executed");
    }
}
