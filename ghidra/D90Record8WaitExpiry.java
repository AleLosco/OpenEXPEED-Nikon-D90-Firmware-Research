// AB-167: controlled expiry of the task 8 node after the AB-166 tick.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Record8WaitExpiry extends D90Record8WaitTick {
    private int cases;

    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base, base.length+2);
        windows[base.length] = new long[]{0x40500,0x40502};
        windows[base.length+1] = new long[]{0x41eac,0x41ecc};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p) throws Exception {
        super.afterInitialization(cpu, decoder, p);
        var space = p.getLanguage().getDefaultSpace();
        require(word(cpu,space,0x68000cd8)==99,"Inherited countdown");
        // Isolate the expiry boundary without claiming 99 consecutive IRQs.
        put(cpu,space,0x68000cd8,1);
        var expected=ByteBuffer.wrap(cpu.readMemory(space.getAddress(0x68000000),4096));
        expected.putLong(0x110,expected.getLong(0x110)+1);
        expected.putInt(0xcd8,0);
        injectVector18(cpu,decoder,p,expected.array());
        // BP tests N==0, so zero remains linked. The next decrement becomes -1.
        expected.putLong(0x110,expected.getLong(0x110)+1);
        expected.putInt(0xcd8,-1);
        expected.putInt(0x128,0x68000128).putInt(0x12c,0x68000128);
        expected.putInt(0xb20,0x68000b20).putInt(0xb24,0x68000b20);
        expected.putInt(0x270,0x68000020).putInt(0x274,0x68000020);
        expected.putInt(0x20,0x68000270).putInt(0x24,0x68000270);
        expected.putInt(0x278,0);
        expected.putShort(0x292,(short)0xab);
        expected.putInt(4,expected.getInt(4)|0x10000000);
        injectVector18(cpu,decoder,p,expected.array());
        cases++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(cases==8,"Expiry count");
        println("AB-167: eight zero boundaries and subsequent expiries returned task 8 to ready list 4");
    }
}
