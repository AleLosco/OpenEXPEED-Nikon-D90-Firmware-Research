// AB-161: test the second peripheral read and dispatch the task 8 request.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90Record8Request170 extends D90Record8Modes {
    private int entries;

    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base, base.length + 3);
        windows[base.length] = new long[]{0x10fd96, 0x10fdba};
        windows[base.length + 1] = new long[]{0x4051e, 0x40520};
        windows[base.length + 2] = new long[]{0x41bfc, 0x41bfe};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p) throws Exception {
        super.afterInitialization(cpu, decoder, p);
        var space = p.getLanguage().getDefaultSpace();
        var input = space.getAddress(0x50000107);
        returnUserService23(cpu, decoder, p, 0x10fd96);
        until(cpu, decoder, 0x10fd9e);
        require(reg(cpu, "R0") == input.getOffset() && cpu.readMemory(input, 1)[0] == 0x40, "Second read address and inherited value");
        long local = reg(cpu, "FP") - 4;
        int initial = word(cpu, space, local);
        cpu.writeMemory(input, new byte[]{0});
        until(cpu, decoder, 0x10fdba);
        // Replay BTSTH and BEQ only. Both precede any argument preparation.
        cpu.writeRegister("PC", 0x10fd9e);
        cpu.writeMemory(input, new byte[]{0x40});
        until(cpu, decoder, 0x10fdb4);
        require(reg(cpu, "R4") == local && reg(cpu, "R5") == 3 && reg(cpu, "R6") == 2
            && reg(cpu, "R7") == 2 && reg(cpu, "AC") == 100 && reg(cpu, "R12") == 0xffffff56L, "User request arguments");
        enterUserOsService(cpu, decoder, p, 0x10fdb4, (int)local, 3, -170, 0x41bfc, 0x4051e);
        require(word(cpu, space, local) == initial, "Local word before service body");
        entries++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(entries == 8, "Request count");
        println("AB-161: eight second-read bypasses and eight service -170 entries verified");
    }
}
