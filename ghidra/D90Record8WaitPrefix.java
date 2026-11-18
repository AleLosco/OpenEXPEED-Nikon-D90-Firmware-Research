// AB-162: follow service -170 from the task 8 user request to the wait helper.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Record8WaitPrefix extends D90Record8Request170 {
    private int cases;

    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base, base.length + 2);
        windows[base.length] = new long[]{0x41bfe, 0x41c7a};
        windows[base.length + 1] = new long[]{0x40cd0, 0x40d0e};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p) throws Exception {
        super.afterInitialization(cpu, decoder, p);
        var space = p.getLanguage().getDefaultSpace();
        long local = reg(cpu, "R4");
        int original = word(cpu, space, local);
        long task = Integer.toUnsignedLong(word(cpu, space, 0x68000000));
        require(task == 0x68000270, "Current task 8");
        until(cpu, decoder, 0x40cd0);
        require(reg(cpu, "R4") == 100 && reg(cpu, "RP") == 0x41c46, "Helper argument and return");
        until(cpu, decoder, 0x41c46);
        long object = reg(cpu, "FP");
        require(object == 0x68000b20 && word(cpu, space, object + 8) == 0x80, "Object 3 flags");
        until(cpu, decoder, 0x41c60);
        require(reg(cpu, "R9") == 0 && reg(cpu, "R6") == 2 && reg(cpu, "R7") == 2, "Unsatisfied wait predicate");
        until(cpu, decoder, 0x41c76);
        int mode = Short.toUnsignedInt(java.nio.ByteBuffer.wrap(cpu.readMemory(space.getAddress(task + 14), 2)).getShort());
        require(word(cpu, space, task + 0x14) == 2 && mode == 2, "Saved wait mask and mode");
        require(reg(cpu, "FP") == object && reg(cpu, "R8") == 0x100004 && word(cpu, space, local) == original, "Helper boundary and caller word");
        cases++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(cases == 8, "Scenario count");
        println("AB-162: eight unsatisfied task 8 waits reached 0x41c76 with mask and mode saved");
    }
}
