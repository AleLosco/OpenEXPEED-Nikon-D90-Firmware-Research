// Replay an isolated one-byte descriptor copy and interrupt states 5/6.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.app.emulator.EmulatorHelper;

public class D90OutputByte extends GhidraScript {
    void executeTo(EmulatorHelper em, long start, long end) throws Exception {
        em.writeRegister("PC", start);
        for (int i = 0; i < 128; i++) {
            if (em.getExecutionAddress().equals(toAddr(end))) return;
            if (!em.step(monitor)) throw new IllegalStateException(em.getLastError());
        }
        throw new IllegalStateException("Instruction limit");
    }
    int byteAt(EmulatorHelper em, long addr) throws Exception {
        return em.readMemory(toAddr(addr), 1)[0] & 255;
    }
    void sample(int value) throws Exception {
        EmulatorHelper em = new EmulatorHelper(currentProgram);
        try {
            long buffer = 0x8f52f6f4L;
            em.writeMemory(toAddr(0x64), new byte[]{0,0});
            em.writeMemory(toAddr(0x50000106), new byte[]{0});
            em.writeMemory(toAddr(0x8f52edeeL), new byte[]{(byte)value});
            em.writeRegister("R0", 0x8f52edeeL);
            em.writeRegister("R10", buffer);
            em.writeRegister("AC", 1);
            executeTo(em, 0x10ffbe, 0x10ffc6);
            em.writeMemory(toAddr(0x8f52f5d4L), new byte[]{(byte)0x8f,0x52,(byte)0xf6,(byte)0xf4});
            em.writeMemory(toAddr(0x8f52f7f7L), new byte[]{1});
            em.writeMemory(toAddr(0x8f52f7f4L), new byte[]{0x47});
            em.writeMemory(toAddr(0x8f52f7feL), new byte[]{5});
            executeTo(em, 0x111f46, 0x111f64);
            int data = byteAt(em, 0x65);
            int state = byteAt(em, 0x8f52f7feL);
            executeTo(em, 0x111f46, 0x111f64);
            int check = byteAt(em, 0x65);
            if (data != value || state != 6 || byteAt(em, 0x8f52f7feL) != 7 ||
                    check != (~(0x47 + value) & 255)) {
                throw new IllegalStateException(String.format("Unexpected output: payload=%02x check=%02x state=%d final=%d buf=%02x sum=%02x", data, check, state, byteAt(em, 0x8f52f7feL), byteAt(em, buffer), byteAt(em,0x8f52f7f4L)));
            }
            println(String.format("payload=%02x trailer=%02x states=5,6,7", data, check));
        } finally {
            em.dispose();
        }
    }
    public void run() throws Exception {
        sample(0x20);
        sample(0x21);
    }
}
