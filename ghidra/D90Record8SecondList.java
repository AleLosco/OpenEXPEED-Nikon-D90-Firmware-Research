// AB-164: follow the second list insertion through the service return.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Record8SecondList extends D90Record8WaitList {
    private int cases;

    @Override long[][] extraWindows() {
        long[][] base = super.extraWindows();
        long[][] windows = Arrays.copyOf(base, base.length + 1);
        windows[base.length] = new long[]{0x41c7a, 0x41cac};
        return windows;
    }

    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p) throws Exception {
        super.afterInitialization(cpu, decoder, p);
        var space = p.getLanguage().getDefaultSpace();
        int os = 0x68000000, task = 0x68000270, head = 0x68000128;
        int node = word(cpu, space, task + 0x28);
        require(node >= os && node <= os + 4096 - 12 && node != task && node != head, "Distinct OS node");
        require(reg(cpu, "R10") == node && reg(cpu, "R11") == head, "Insertion registers");
        require(word(cpu, space, head) == head && word(cpu, space, head + 4) == head, "Empty second list");
        require(word(cpu, space, node + 8) == 100, "Previously prepared node value");
        long sp = reg(cpu, "SP");
        require(word(cpu, space, sp) == 0x406c6, "Service return address");
        var expected = ByteBuffer.wrap(cpu.readMemory(space.getAddress(os), 4096));
        expected.putInt(node-os, head).putInt(node-os+4, head);
        expected.putInt(head-os, node).putInt(head-os+4, node);
        until(cpu, decoder, 0x41c88);
        until(cpu, decoder, 0x41ca4);
        require(Arrays.equals(expected.array(), cpu.readMemory(space.getAddress(os), 4096)), "Both lists after insertion");
        until(cpu, decoder, 0x406c6);
        require(reg(cpu, "SP") == sp + 4 && reg(cpu, "R12") == 0, "Service result and stack");
        require(Arrays.equals(expected.array(), cpu.readMemory(space.getAddress(os), 4096)), "OS page after service return");
        println(String.format("AB-164 node=%08x head=%08x value=%d", node, head, word(cpu, space, node+8)));
        cases++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(cases == 8, "Scenario count");
        println("AB-164: eight second-list insertions and service -170 returns verified");
    }
}
