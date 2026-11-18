// AB-163: compare task 8's ready and object lists across the original wait helper.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Record8WaitList extends D90Record8WaitPrefix {
    private static final int OS = 0x68000000, READY = OS + 0x20;
    private static final int TASK = OS + 0x270, OBJECT = OS + 0xb20;
    private int cases;

    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base, base.length + 1);
        windows[base.length] = new long[]{0x41ecc, 0x41eee};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p) throws Exception {
        super.afterInitialization(cpu, decoder, p);
        var space = p.getLanguage().getDefaultSpace();
        long sp = reg(cpu, "SP");
        int slot = word(cpu, space, sp + 0x34);
        byte[] before = cpu.readMemory(space.getAddress(OS), 4096);
        var expected = ByteBuffer.wrap(before.clone());
        require(word(cpu, space, READY) == TASK && word(cpu, space, READY+4) == TASK
            && word(cpu, space, TASK) == READY && word(cpu, space, TASK+4) == READY, "Only task 8 on ready list 4");
        require(word(cpu, space, OBJECT) == OBJECT && word(cpu, space, OBJECT+4) == OBJECT, "Empty object list");
        int bitmap = expected.getInt(4);
        require((bitmap & 0x10000000) != 0, "Ready bit set");

        until(cpu, decoder, 0x41ecc);
        require(reg(cpu, "RP") == 0x41c7a && reg(cpu, "AC") == Integer.toUnsignedLong(slot), "Delayed call argument");
        until(cpu, decoder, 0x41c7a);
        expected.putInt(4, bitmap & ~0x10000000);
        expected.putInt(READY-OS, READY).putInt(READY-OS+4, READY);
        expected.putInt(TASK-OS, OBJECT).putInt(TASK-OS+4, OBJECT);
        expected.putInt(TASK-OS+8, 0x100004).putShort(TASK-OS+0x20, (short)slot);
        expected.putInt(OBJECT-OS, TASK).putInt(OBJECT-OS+4, TASK);
        require(Arrays.equals(expected.array(), cpu.readMemory(space.getAddress(OS), 4096)), "OS page after wait helper");
        require(reg(cpu, "SP") == sp && reg(cpu, "RP") == 0x41c7a, "Returned to caller");
        println(String.format("AB-163 saved halfword=%04x bitmap=%08x->%08x", slot & 0xffff, bitmap, expected.getInt(4)));
        cases++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(cases == 8, "Scenario count");
        println("AB-163: eight task 8 transfers from ready list 4 to object 3 verified");
    }
}
