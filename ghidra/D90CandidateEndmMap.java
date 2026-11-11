// AB-127: compare ENDM dependencies against the initialized AB-126 program.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import ghidra.program.model.mem.Memory;

public class D90CandidateEndmMap extends D90CandidatePrologue {
    private int cases;
    private long coverage(Memory memory,long start,long length) {
        long bytes=0;
        for(var block:memory.getBlocks()) {
            if(block.isInitialized())
                bytes+=Math.max(0,Math.min(start+length,block.getEnd().getOffset()+1)
                    -Math.max(start,block.getStart().getOffset()));
        }
        return bytes;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p) throws Exception {
        super.afterInitialization(c,d,p);
        long[][] ranges={{0x19a542,0xaa},{0x1f2b44,4},{0x1eb044,0x7b20},
            {0x7c0000,0x7b20},{0x7e0000,0x7b20},{0x8f95130cL,0x7b20},{0x8f958e2cL,4}};
        for(long[] range:ranges) {
            long original=coverage(currentProgram.getMemory(),range[0],range[1]);
            long harness=coverage(p.getMemory(),range[0],range[1]);
            if(cases==0) println(String.format("AB-127 %x length=%x database=%x harness=%x",range[0],range[1],original,harness));
            require(harness==0,"Unexpected AB-126 coverage");
        }
        if(cases++==0){
            var decoder=new PseudoDisassembler(currentProgram);
            for(long pc=0x19a542;pc<0x19a5ec;){
                var ins=decoder.disassemble(toAddr(pc));
                println(String.format("AB-127 %x %s",pc,ins));
                pc+=ins.getLength();
            }
        }
    }
    @Override public void run() throws Exception {
        super.run();
        require(cases==8,"Scenario count");
        println("AB-127: eight pre-call map inspections passed");
    }
}
