// Verify the task entry and the ready-list lookup in the B dispatcher.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;

public class D90FirstTask extends GhidraScript {
    public void run() throws Exception {
        long[] entries = {0x1000ac, 0x40d44};
        String[] names = {"B_StartupTask", "B_DispatchVectorCandidate"};
        for (int i = 0; i < entries.length; i++) {
            Address a = toAddr(entries[i]);
            createLabel(a, names[i], true);
            disassemble(a);
            if (getFunctionAt(a) == null) createFunction(a, names[i]);
        }
        for (int pc = 0x40e2a; pc < 0x40e3a; ) {
            if (pc == 0x40e32) {
                if ((currentProgram.getMemory().getShort(toAddr(pc)) & 0xffff) != 0x97dd)
                    throw new IllegalStateException("Unexpected bit-search encoding");
                println(toAddr(pc) + " 97dd: SRCH1 AC (FR81 extension)");
                pc += 2;
                disassemble(toAddr(pc));
                continue;
            }
            disassemble(toAddr(pc));
            var ins = getInstructionAt(toAddr(pc));
            if (ins == null) throw new IllegalStateException("Missing instruction at " + pc);
            println(ins.getAddress() + " " + ins);
            pc += ins.getLength();
        }
    }
}
