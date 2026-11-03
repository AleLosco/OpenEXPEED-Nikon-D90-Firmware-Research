// Replay the A two-byte command lookup, stopping before its RET.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;

public class D90ACommands extends GhidraScript {
    void runTo(EmulatorHelper em, long start, long end) throws Exception {
        em.writeRegister("PC", start);
        for (int n=0; n<2048; n++) {
            if (em.getExecutionAddress().equals(toAddr(end))) return;
            if (!em.step(monitor)) throw new IllegalStateException(em.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    void put(EmulatorHelper em, long address, int value) throws Exception {
        em.writeMemory(toAddr(address), new byte[]{(byte)value});
    }
    int get(EmulatorHelper em, long address) throws Exception {
        return em.readMemory(toAddr(address),1)[0]&255;
    }
    void lookup(int key, int index, int descriptor, int flags) throws Exception {
        EmulatorHelper em = new EmulatorHelper(currentProgram);
        try {
            em.writeRegister("SP", 0x3e000);
            em.writeRegister("R10",0);em.writeRegister("R11",0);
            em.writeRegister("R4", key);
            put(em, 0x36db8, flags);
            put(em, 0x366d2, 0xa5);
            runTo(em, 0x85076, 0x85102);
            int expectedFlags = flags, count = 0xa5;
            if (index == 255) expectedFlags |= 2;
            else if ((flags&0x20)!=0) {
                count = descriptor&255;
                if ((descriptor&0x8000)!=0) expectedFlags = (flags&63)|64;
                else if (count>0) expectedFlags = (flags&63)|128;
            }
            if (em.readRegister("R4").intValue()!=index ||
                get(em,0x36db8)!=expectedFlags || get(em,0x366d2)!=count)
                throw new IllegalStateException(String.format("Lookup mismatch key=%04x flags=%02x", key,flags));
        } finally { em.dispose(); }
    }
    void selection(int flags) throws Exception {
        EmulatorHelper em = new EmulatorHelper(currentProgram);
        try {
            em.writeRegister("R8",0x36db8);
            em.writeRegister("SP",0x3e000);
            put(em,0x36db8,flags);
            put(em,0x60,0);
            em.writeMemory(toAddr(0x36db0),new byte[4]); // Equal settings: helper performs no I/O.
            em.writeMemory(toAddr(0x3609c),new byte[4]);
            runTo(em,0x84dbc,0x84de8);
            byte[] pointer=em.readMemory(toAddr(0x3609c),4);
            long value=0;
            for(byte b:pointer) value=(value<<8)|(b&255);
            if(value!=((flags&32)!=0?0x851b6:0))
                throw new IllegalStateException("Conditional installation mismatch");
        } finally { em.dispose(); }
    }
    void handshake(int second) throws Exception {
        EmulatorHelper em = new EmulatorHelper(currentProgram);
        try {
            em.writeRegister("R6",0x365c0);
            em.writeMemory(toAddr(0x365c0),new byte[]{0x53,(byte)second});
            put(em,0x36db8,0);
            put(em,0x60,0);
            runTo(em,0x8514e,second==0x53?0x8516c:0x8517a);
            if(get(em,0x36db8)!=(second==0x53?32:0))
                throw new IllegalStateException("Handshake flag mismatch");
        } finally { em.dispose(); }
    }
    public void run() throws Exception {
        if(currentProgram.getMemory().getInt(toAddr(0xffffc))!=0xfc00e)
            throw new IllegalStateException("Expected A program");
        int count=0;
        for(int index=0;index<52;index++) {
            int key=currentProgram.getMemory().getShort(toAddr(0xe1fd4+index*12))&65535;
            int descriptor=currentProgram.getMemory().getShort(toAddr(0xe1fd6+index*12))&65535;
            if(key==0) throw new IllegalStateException("Early table terminator");
            for(int flags:new int[]{0,32}) {lookup(key,index,descriptor,flags);count++;}
        }
        if(currentProgram.getMemory().getShort(toAddr(0xe1fd4+52*12))!=0)
            throw new IllegalStateException("Missing table terminator");
        for(int key:new int[]{0x0046,0x4601,0x4620,0xffff})
            for(int flags:new int[]{0,32}) {lookup(key,255,0,flags);count++;}
        selection(0); selection(32);
        handshake(0x53); handshake(0x52);
        println("A command lookup cases passed="+count+"; conditional installations passed=2; handshake cases passed=2");
    }
}
