// Apply D90 B labels after importing the prepared Intel HEX.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.program.model.mem.Memory;

public class D90Labels extends GhidraScript {
    @Override
    public void run() throws Exception {
        Memory memory = currentProgram.getMemory();
        if (memory.getByte(toAddr(0x40000)) != (byte)0x9f ||
            memory.getByte(toAddr(0x8f52d79eL)) != (byte)0x8f ||
            memory.getByte(toAddr(0xdfc00)) != 0) {
            throw new IllegalStateException("Expected mapped D90 B ROM and updater RAM");
        }
        createLabel(toAddr(0x40000), "B_ResetCandidate", true);
        createLabel(toAddr(0xdfc00), "B_VectorTable", true);
        createLabel(toAddr(0x16b8fe), "UpdateFileDispatch", true);
        createLabel(toAddr(0x16ca7c), "CombinedSizeGuard", true);
        createLabel(toAddr(0x8f52d79eL), "UpdateController", true);
        createLabel(toAddr(0x8f52da8cL), "EraseRange", true);
        createLabel(toAddr(0x8f52dad2L), "ProgramAndVerifyRange", true);
    }
}
