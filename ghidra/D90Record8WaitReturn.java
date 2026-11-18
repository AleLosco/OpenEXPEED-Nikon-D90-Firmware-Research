// AB-165: follow the dispatcher epilogue and the caller's store without an asynchronous interrupt.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Record8WaitReturn extends D90Record8SecondList {
    private int cases;

    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p) throws Exception {
        super.afterInitialization(cpu, decoder, p);
        var space = p.getLanguage().getDefaultSpace();
        long frame = reg(cpu, "SP");
        long local = Integer.toUnsignedLong(word(cpu, space, frame + 16));
        int savedAc = word(cpu, space, frame + 52);
        byte[] os = cpu.readMemory(space.getAddress(0x68000000), 4096);
        byte[] initial = cpu.readMemory(space.getAddress(local - 4), 12);
        require(word(cpu, space, 0x68000000) == 0x68000270, "Current task 8");
        until(cpu, decoder, 0x404f8);
        require(reg(cpu, "RP") == 0x406d0 && decoder.disassemble(cpu.getExecutionAddress()).getMnemonicString().equals("RET"), "Epilogue call");
        until(cpu, decoder, 0x406d0);
        returnUserService23(cpu, decoder, p, 0x10fdb6);
        until(cpu, decoder, 0x10fdb8);
        require(reg(cpu, "R4") == local && local == reg(cpu, "FP") - 4, "Restored caller local");
        require(reg(cpu, "AC") == Integer.toUnsignedLong(savedAc) && reg(cpu, "R12") == 0, "Restored AC and service result");
        require(Arrays.equals(initial, cpu.readMemory(space.getAddress(local - 4), 12)), "Local and guards before store");
        require(decoder.disassemble(cpu.getExecutionAddress()).toString().equals("ST AC,@R4"), "Caller store");
        until(cpu, decoder, 0x10fdba);
        byte[] expected = initial.clone();
        ByteBuffer.wrap(expected).putInt(4, savedAc);
        require(Arrays.equals(expected, cpu.readMemory(space.getAddress(local - 4), 12)), "Local and guards after store");
        require(Arrays.equals(os, cpu.readMemory(space.getAddress(0x68000000), 4096)), "OS page retained across return");
        println(String.format("AB-165 local=%08x before=%08x stored=%08x", local, ByteBuffer.wrap(initial).getInt(4), savedAc));
        cases++;
    }

    @Override public void run() throws Exception {
        super.run();
        require(cases == 8, "Scenario count");
        println("AB-165: eight task 8 user returns and caller stores verified");
    }
}
