// AB-149: execute service -23 from the AB-147 boundary.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Service23Return extends D90FifthServiceEntry {
    private int cases;
    private static final long FRAME = 0x8f9b0204L;
    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows(), result = Arrays.copyOf(base, base.length+3);
        result[base.length] = new long[]{0x414da,0x41534};
        result[base.length+1] = new long[]{0x415a2,0x415b2};
        result[base.length+2] = new long[]{0x41852,0x4186e};
        return result;
    }
    @Override long[][] extraSyntheticWindows() {
        long[][] base = super.extraSyntheticWindows(), result = Arrays.copyOf(base, base.length+1);
        result[base.length] = new long[]{FRAME-4, FRAME+44};
        return result;
    }
    @Override void afterInitialization(EmulatorHelper c, PseudoDisassembler d, ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        var a = p.getLanguage().getDefaultSpace();
        int node = 0x680005e0, head = 0x68000068;
        require(reg(c,"R4")==28 && reg(c,"R5")==0 && word(c,a,node+8)==16,"Inherited service inputs");
        require(word(c,a,head)==head && word(c,a,head+4)==head && word(c,a,0x68000000)!=node,"Inherited list");
        boolean eligible = cases%2==0;
        if (!eligible) put(c,a,node+8,0);
        byte[] frame = new byte[48];
        Arrays.fill(frame,(byte)0xa5);
        c.writeMemory(a.getAddress(FRAME-4),frame);
        long osBase = 0x68000000L, sp = reg(c,"SP");
        byte[] os = c.readMemory(a.getAddress(osBase),4096);
        byte signal = c.readMemory(a.getAddress(0x44),1)[0];
        if (eligible) {
            ByteBuffer state = ByteBuffer.wrap(os);
            state.putInt(4,state.getInt(4)|0x80000);
            state.putInt(0x68,node).putInt(0x6c,node);
            state.putInt(0x5e0,head).putInt(0x5e4,head).putInt(0x5e8,0).putInt(0x5f8,(int)FRAME);
            ByteBuffer.wrap(frame).putInt(4,0x11201a).putInt(8,0x1f0030).putInt(12,0x4050a).putInt(40,0);
        }
        until(c,d,0x406c6);
        require(reg(c,"SP")==sp && reg(c,"R12")== (eligible ? 0 : 0xc1),"Local return");
        require(Arrays.equals(os,c.readMemory(a.getAddress(osBase),os.length)),"OS memory");
        require(Arrays.equals(frame,c.readMemory(a.getAddress(FRAME-4),frame.length)),"Frame and guards");
        require(c.readMemory(a.getAddress(0x44),1)[0]==(eligible ? 1 : signal),"Peripheral RAM");
        cases++;
    }
    @Override public void run() throws Exception {
        super.run();
        require(cases==8,"Scenario count");
        println("AB-149: four eligible and four ineligible service returns verified");
    }
}
