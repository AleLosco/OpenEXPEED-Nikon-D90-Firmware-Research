// AB-166: one explicitly injected vector 0x18 after the task 8 user return.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90Record8WaitTick extends D90Record8WaitReturn {
    private int cases;

    @Override void afterInitialization(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p) throws Exception {
        super.afterInitialization(cpu, decoder, p);
        var space = p.getLanguage().getDefaultSpace();
        var expected = ByteBuffer.wrap(cpu.readMemory(space.getAddress(0x68000000),4096));
        require(expected.getInt(0xcd8) == 100, "Node value before IRQ");
        expected.putLong(0x110, expected.getLong(0x110)+1);
        expected.putInt(0xcd8,99);
        injectVector18(cpu, decoder, p, expected.array());
        cases++;
    }

    void injectVector18(EmulatorHelper cpu, PseudoDisassembler decoder, ProgramDB p, byte[] expected) throws Exception {
        var space = p.getLanguage().getDefaultSpace();
        long user = reg(cpu, "SP"), system = reg(cpu, "SSP"), pc = reg(cpu, "PC");
        require(pc == 0x10fdba && reg(cpu, "S") == 1 && system == 0x81004000L, "User boundary");
        require(word(cpu, space, 0xdff9c) == 0x4048a, "Vector 18");
        String[] names = {"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP","RP","MDL","MDH"};
        long[] saved = new long[names.length];
        for (int i=0; i<names.length; i++) saved[i] = reg(cpu,names[i]);
        byte[] userBytes = cpu.readMemory(space.getAddress(user), 32);
        int ps = (int)reg(cpu,"PS") & ~0x001f073f;
        String[] flags = {"C","V","Z","N","I","S","T","D0","D1"};
        int[] bits = {0,1,2,3,4,5,8,9,10};
        for(int i=0;i<bits.length;i++) ps |= (int)(reg(cpu,flags[i])&1)<<bits[i];
        ps |= (int)(reg(cpu,"ILM")&31)<<16;
        put(cpu,space,system-4,ps);
        put(cpu,space,system-8,(int)pc);
        cpu.writeRegister("USP",user);
        cpu.writeRegister("SP",system-8);
        cpu.writeRegister("SSP",system-8);
        cpu.writeRegister("S",0);
        cpu.writeRegister("I",0);
        cpu.writeRegister("PS",ps & ~0x30);
        cpu.writeRegister("PC",0x4048a);
        until(cpu,decoder,0x404aa);
        byte[] actual = cpu.readMemory(space.getAddress(0x68000000),4096);
        for (int i=0;i<expected.length;i++) {
            if (expected[i]!=actual[i]) println(String.format("IRQ OS mismatch +%x expected=%02x actual=%02x",i,expected[i]&255,actual[i]&255));
        }
        require(Arrays.equals(expected,actual), "OS page after single IRQ");
        require(decoder.disassemble(cpu.getExecutionAddress()).getMnemonicString().equals("RETI"), "IRQ return instruction");
        require(cpu.step(monitor), "RETI execution");
        require(reg(cpu,"PC")==pc && reg(cpu,"PS")==Integer.toUnsignedLong(ps) && reg(cpu,"S")==1, "User return state");
        cpu.writeRegister("SSP",system);
        cpu.writeRegister("SP",user);
        for(int i=0;i<names.length;i++) require(reg(cpu,names[i])==saved[i],"Preserved "+names[i]);
        require(Arrays.equals(userBytes,cpu.readMemory(space.getAddress(user),32)),"User stack preserved");
    }

    @Override public void run() throws Exception {
        super.run();
        require(cases==8,"Scenario count");
        println("AB-166: eight single vector 18 entries decremented task 8 node from 100 to 99");
    }
}
