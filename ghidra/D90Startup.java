// Verify the B startup handoff and mark the next initialization stage.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;

public class D90Startup extends GhidraScript {
    public void run() throws Exception {
        if (currentProgram.getMemory().getInt(toAddr(0xdfffc)) != 0x40000 ||
            currentProgram.getMemory().getInt(toAddr(0x40464)) != 0x40704)
            throw new IllegalStateException("Unexpected reset vector or startup target");
        Address stage = toAddr(0x40704);
        createLabel(stage, "B_InitStage2", true);
        disassemble(stage);
        if (getFunctionAt(stage) == null) createFunction(stage, "B_InitStage2");
        for (int site = 0x40712; site < 0x4072a; site += 2) {
            Address target = getInstructionAt(toAddr(site)).getFlows()[0];
            disassemble(target);
            if (getFunctionAt(target) == null) createFunction(target, null);
            println("stage2 call " + toAddr(site) + " -> " + target);
        }
        for (int offset : new int[]{0x40000, 0x4000c, 0x40012, 0x40366, 0x403ba,
                                    0x40426, 0x40462, 0x40468, 0x40704, 0x40712, 0x4074e, 0x407bc, 0x407ea, 0x40880, 0x40898}) {
            println(toAddr(offset) + " " + getInstructionAt(toAddr(offset)));
        }
    }
}
