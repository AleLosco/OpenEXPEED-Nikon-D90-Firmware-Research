// Disassemble the verified entry points in the D90 B map.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;

public class D90Functions extends GhidraScript {
    public void run() throws Exception {
        int reset = currentProgram.getMemory().getInt(toAddr(0xdfffc));
        if (reset != 0x40000) {
            throw new IllegalStateException("Unexpected B reset vector");
        }
        for (long entry : new long[]{reset, 0x16b8fe, 0x16ca7c, 0x8f52d79eL,
                                      0x8f52da8cL, 0x8f52dad2L}) {
            Address addr = toAddr(entry);
            if (getInstructionAt(addr) == null && !disassemble(addr)) {
                throw new IllegalStateException("Cannot disassemble " + addr);
            }
            if (getFunctionAt(addr) == null && createFunction(addr, null) == null) {
                throw new IllegalStateException("Cannot create function at " + addr);
            }
            println(addr + " " + getFunctionAt(addr).getName() + " " + getInstructionAt(addr));
        }
    }
}
