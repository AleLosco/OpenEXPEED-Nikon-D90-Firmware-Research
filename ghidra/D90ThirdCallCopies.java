// AB-134: original third call through both parser calls and all 26 selections.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90ThirdCallCopies extends D90CandidateSystemFix {
    int cases;
    static final int[] DIFFERENCES={0,1,0x7ffe,0x7fff,0x8000,0x8001,0xfffe,0xffff};
    static final int[] SIZES={240,160,234};
    static final long[] DEST={0x8f533174L,0x8f53c4d4L,0x8f53da52L};
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), w=Arrays.copyOf(base,base.length+4);
        w[base.length]=new long[]{0x1000da,0x1000e2};
        w[base.length+1]=new long[]{0x13a1dc,0x13a27a};
        w[base.length+2]=new long[]{0x13a3ac,0x13a46a};
        w[base.length+3]=new long[]{0x1caab4,0x1cab8c};
        return w;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), w=Arrays.copyOf(base,base.length+6);
        w[base.length]=new long[]{0x740000,0x740400};
        w[base.length+1]=new long[]{0x760000,0x760400};
        w[base.length+2]=new long[]{0x8f532ec8L,0x8f533042L};
        for(int i=0;i<3;i++) w[base.length+3+i]=new long[]{DEST[i]-1,DEST[i]+SIZES[i]+1};
        return w;
    }
    static byte[] payload(int bank,int index){
        byte[] data=new byte[SIZES[index]];
        for(int i=0;i<data.length;i++) data[i]=(byte)(i+73*bank+index);
        return data;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        require(reg(c,"PC")==0x1000da,"Third call boundary");
        int delta=DIFFERENCES[cases];
        for(int bank=0;bank<2;bank++){
            ByteBuffer records=ByteBuffer.allocate(1024);
            for(int index:new int[]{0,bank+1})
                records.putShort((short)index).putShort((short)SIZES[index]).put(payload(bank,index));
            records.putShort((short)0xaa55).putShort((short)(bank==0?0xfffe+delta:0xfffe));
            records.putShort((short)0xffff).putShort((short)0);
            c.writeMemory(a.getAddress(bank==0?0x740000:0x760000),records.array());
        }
        c.writeMemory(a.getAddress(0x8f532ec8L),new byte[0x17a]);
        for(int i=0;i<3;i++){
            byte[] guard=new byte[SIZES[i]+2]; Arrays.fill(guard,(byte)0xcc);
            c.writeMemory(a.getAddress(DEST[i]-1),guard);
        }
        until(c,d,0x13a3ac);require(reg(c,"R4")==0 && reg(c,"RP")==0x13a1f6,"First parser call");
        until(c,d,0x13a1f6);until(c,d,0x13a3ac);
        require(reg(c,"R4")==1 && reg(c,"RP")==0x13a1fa,"Second parser call");
        until(c,d,0x13a278);
        for(int i=0;i<3;i++){
            byte[] expected=new byte[SIZES[i]+2];
            expected[0]=expected[expected.length-1]=(byte)0xcc;
            int bank=i==0?(delta<0x8000?0:1):i-1;
            System.arraycopy(payload(bank,i),0,expected,1,SIZES[i]);
            require(Arrays.equals(expected,c.readMemory(a.getAddress(DEST[i]-1),expected.length)),"Copy and guards "+i);
        }
        byte[] flags=new byte[26]; Arrays.fill(flags,3,26,(byte)1);
        require(Arrays.equals(flags,c.readMemory(a.getAddress(0x8f53300cL),26)),"Missing flags");
        cases++;
    }
    @Override public void run()throws Exception{
        super.run();require(cases==8,"Case count");
        println("AB-134: eight original double-parser and copy loops passed at 0x13a278");
    }
}
