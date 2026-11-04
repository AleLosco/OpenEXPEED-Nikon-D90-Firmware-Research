// Bounded straight-line search for stores overlapping the ATAM selector in component B.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.Varnode;
import ghidra.program.model.symbol.RefType;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class D90AtamStoreScan extends GhidraScript {
    private static final long TARGET = 0x6800cf1eL;
    private final HashMap<Varnode, Long> known = new HashMap<>();
    private final Set<Long> resolved = new TreeSet<>();
    private final Set<Long> unknown = new TreeSet<>();
    private final Set<Long> hits = new TreeSet<>();

    private Long value(Varnode node) {
        return node.isConstant() ? Long.valueOf(node.getOffset()) : known.get(node);
    }

    private void evaluate(PcodeOp op, long pc) {
        if (op.getOpcode() == PcodeOp.STORE) {
            Long address = value(op.getInput(1));
            if (address == null) {
                unknown.add(pc);
            } else {
                resolved.add(pc);
                if (address <= TARGET && TARGET - address < op.getInput(2).getSize()) hits.add(pc);
            }
        }
        Varnode out = op.getOutput();
        if (out == null) return;
        Long result = null;
        Long a = op.getNumInputs() > 0 ? value(op.getInput(0)) : null;
        Long b = op.getNumInputs() > 1 ? value(op.getInput(1)) : null;
        if (a != null) {
            switch (op.getOpcode()) {
                case PcodeOp.COPY: case PcodeOp.INT_ZEXT: result = a; break;
                case PcodeOp.INT_ADD: if (b != null) result = a + b; break;
                case PcodeOp.INT_SUB: if (b != null) result = a - b; break;
                case PcodeOp.INT_AND: if (b != null) result = a & b; break;
                case PcodeOp.INT_OR: if (b != null) result = a | b; break;
            }
        }
        known.keySet().removeIf(n -> n.intersects(out));
        if (result != null && out.getSize() <= 4)
            known.put(out, result & ((1L << (8 * out.getSize())) - 1));
    }

    public void run() throws Exception {
        var decoder = new PseudoDisassembler(currentProgram);
        var memory = currentProgram.getMemory();
        int seeds = 0;
        for (long start = 0x198000; start + 6 <= 0x1a4000; start += 2) {
            monitor.checkCancelled();
            if ((memory.getShort(toAddr(start)) & 0xfff0) != 0x9f80) continue;
            long base = Integer.toUnsignedLong(memory.getInt(toAddr(start + 2)));
            if (base < 0x6800cc00L || base >= 0x6800d000L) continue;
            seeds++;
            known.clear();
            long pc = start;
            for (int i = 0; i < 32 && pc < 0x1a4000; i++) {
                var instruction = decoder.disassemble(toAddr(pc));
                if (instruction == null || instruction.getFlowType() != RefType.FALL_THROUGH) break;
                known.keySet().removeIf(Varnode::isUnique);
                boolean branch = false;
                for (PcodeOp op : instruction.getPcode()) {
                    int code = op.getOpcode();
                    if (code == PcodeOp.BRANCH || code == PcodeOp.CBRANCH ||
                        code == PcodeOp.BRANCHIND) { branch = true; break; }
                    evaluate(op, pc);
                }
                if (branch) break;
                pc += instruction.getLength();
            }
        }
        println("Seeds=" + seeds + " resolved store sites=" + resolved.size() +
            " unknown store sites=" + unknown.size());
        Set<Long> unresolvedOnly = new TreeSet<>(unknown);
        unresolvedOnly.removeAll(resolved);
        println("Sites without any resolved destination=" + unresolvedOnly.size());
        for (long pc : unresolvedOnly)
            println(String.format("Unresolved 0x%x %s", pc, decoder.disassemble(toAddr(pc))));
        for (long pc : hits) println(String.format("Overlapping store 0x%x", pc));
        if (!hits.contains(0x19dfbcL) || !hits.contains(0x19f772L))
            throw new IllegalStateException("Missing AB-63 positive control");
    }
}
