// Mark the mirrored block routines; labels describe observed use only.
// @category Nikon
import ghidra.app.script.GhidraScript;
public class D90Settings extends GhidraScript {
    public void run() throws Exception {
        long[] entries = {0x19a542, 0x19a5ec, 0x19a60c};
        String[] names = {"LoadEndmBlock", "RestoreEndmDefaults", "WriteEndmCopies"};
        for (int i = 0; i < entries.length; i++) {
            var a = toAddr(entries[i]);
            disassemble(a);
            createLabel(a, names[i], true);
            if (getFunctionAt(a) == null) createFunction(a, names[i]);
        }
        for (long pc : new long[]{0x19a5e2, 0x19a5ea, 0x100684, 0x100690, 0x168c32}) {
            var a = toAddr(pc);
            disassemble(a);
            println(a + " " + getInstructionAt(a));
        }
    }
}
