// AB-133: bounded original record parser with controlled bank data.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90ThirdCallRecords extends D90CandidateSystemFix {
    int parses;
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+2);
        windows[base.length]=new long[]{0x13a3ac,0x13a46a};
        windows[base.length+1]=new long[]{0x1caab4,0x1cab8c};
        return windows;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), windows=Arrays.copyOf(base,base.length+3);
        windows[base.length]=new long[]{0x740000,0x740400};
        windows[base.length+1]=new long[]{0x760000,0x760400};
        windows[base.length+2]=new long[]{0x8f532ec8L,0x8f533042L};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        long sp=reg(c,"SP"),r10=reg(c,"R10");
        for(int selector=0;selector<2;selector++) for(int marker=0;marker<2;marker++){
            long bank=selector==0?0x740000:0x760000;
            byte[] records=new byte[1024];
            ByteBuffer stream=ByteBuffer.wrap(records);
            stream.putShort((short)0).putShort((short)240);
            stream.position(244);
            stream.putShort((short)1).putShort((short)161);
            stream.position(410); // Round the odd payload length up to an even byte.
            if(marker==1) stream.putShort((short)0xaa55).putShort((short)0x1234);
            int afterMarker=stream.position();
            stream.putShort((short)0xffff).putShort((short)0);
            byte[] dirty=new byte[0x17a];Arrays.fill(dirty,(byte)0xcc);
            c.writeMemory(a.getAddress(0x8f532ec8L),dirty);
            c.writeMemory(a.getAddress(bank),records);
            c.writeRegister("SP",sp);c.writeRegister("R10",r10);
            c.writeRegister("R4",selector);c.writeRegister("RP",0x1000da);c.writeRegister("PC",0x13a3ac);
            until(c,d,0x1000da);
            require(word(c,a,0x8f532ed0L)==(int)(bank+4),"Valid record pointer");
            for(int i=1;i<26;i++) require(word(c,a,0x8f532ed0L+4*i)==0,"Rejected/missing record "+i);
            long output=0x8f532f38L+104*selector;
            for(int i=0;i<26;i++){
                int expected=marker==0?0xcccccccc:i==0?(int)(bank+4):0;
                require(word(c,a,output+4*i)==expected,"Published table");
                require(word(c,a,0x8f532f38L+104*(1-selector)+4*i)==0xcccccccc,"Other table");
            }
            require(word(c,a,0x8f532ec8L+4*selector)==(int)(marker==1?bank+afterMarker:bank),"Bank pointer");
            int sequence=Short.toUnsignedInt(ByteBuffer.wrap(c.readMemory(a.getAddress(0x8f533008L+2*selector),2)).getShort());
            require(sequence==(marker==1?0x1234:0xcccc),"Sequence");
            require((c.readMemory(a.getAddress(0x8f533040L+selector),1)[0]&255)==(marker==1?1:0xcc),"Marker flag");
            require(reg(c,"SP")==sp && reg(c,"R10")==r10,"Restored stack/R10");
            require(Arrays.equals(records,c.readMemory(a.getAddress(bank),1024)),"Bank retained");
            parses++;
        }
    }
    @Override public void run()throws Exception{
        super.run();require(parses==32,"Case count");
        println("AB-133: 32 parser cases passed for both selectors with and without publication");
    }
}
