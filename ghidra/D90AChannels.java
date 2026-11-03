// Inspect A's mapped reset and replay only its callback-selection instructions.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;

public class D90AChannels extends GhidraScript {
    public void run() throws Exception {
        if (currentProgram.getMemory().getInt(toAddr(0xffffc)) != 0xfc00e)
            throw new IllegalStateException("Expected D90 A map");
        for (long entry : new long[]{0xfc00e, 0x80042, 0x8139e, 0x813ca,
                0x83ee8, 0x83f72, 0x85104, 0x851b6, 0x87ce8, 0xac89a}) {
            if (getInstructionAt(toAddr(entry)) == null && !disassemble(toAddr(entry)))
                throw new IllegalStateException("Cannot disassemble A entry");
            println(String.format("entry=%x %s", entry, getInstructionAt(toAddr(entry))));
        }
        // Begin after interrupt register saves. No interrupt or device is modeled.
        for (long target : new long[]{0x83f72, 0x85104, 0x851b6, 0x87ce8}) {
            EmulatorHelper em = new EmulatorHelper(currentProgram);
            try {
                em.writeMemory(toAddr(0x3609c), new byte[]{0, (byte)(target >> 16),
                    (byte)(target >> 8), (byte)target});
                em.writeRegister("R4", 0x3609c);
                em.writeRegister("PC", 0x813ac);
                for (int n = 0; n < 5; n++)
                    if (!em.step(monitor)) throw new IllegalStateException(em.getLastError());
                if (!em.getExecutionAddress().equals(toAddr(target)))
                    throw new IllegalStateException("Callback dispatch mismatch");
                println(String.format("A RX callback selected=%x", target));
            } finally { em.dispose(); }
        }
    }
}
