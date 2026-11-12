// AB-148: inspect the original OS service selected by the AB-147 dispatcher.
// @category Nikon
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.data.*;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.model.listing.Instruction;
import java.util.Arrays;

public class D90Service23Context extends GhidraScript {
    private void dump(PseudoDisassembler pd,long start,int limit) throws Exception {
        println("AB-148 BLOCK "+Long.toHexString(start));
        long pc=start;
        for(int n=0;n<limit;n++) {
            Instruction ins=pd.disassemble(toAddr(pc));
            if(ins==null) { println("AB-148 UNDECODED "+Long.toHexString(pc)); return; }
            StringBuilder line=new StringBuilder(String.format("AB-148 %s  %s",ins.getAddress(),ins.getMnemonicString()));
            for(int i=0;i<ins.getNumOperands();i++) line.append(i==0?" ":", ").append(ins.getDefaultOperandRepresentation(i));
            line.append("  [").append(ins.getFlowType()).append("]");
            Address[] flows=ins.getFlows();
            if(flows.length>0) line.append(" -> ").append(Arrays.toString(flows));
            println(line.toString());
            pc+=ins.getLength();
            if(ins.getMnemonicString().startsWith("RET")) {
                if(ins.getMnemonicString().equals("RET:D")) {
                    Instruction delay=pd.disassemble(toAddr(pc));
                    if(delay!=null) println(String.format("AB-148 %s  %s",delay.getAddress(),delay));
                }
                println("AB-148 block "+Long.toHexString(start)+" reaches "+ins.getMnemonicString()); return;
            }
        }
        println("AB-148 block "+Long.toHexString(start)+" reached listing bound");
    }
    @Override public void run() throws Exception {
        Address entry=toAddr(0x414da);
        FunctionManager fm=currentProgram.getFunctionManager();
        Function f=fm.getFunctionContaining(entry);
        if(f==null) {
            println("AB-148: no function contains 0x414da in the existing analyzed listing; using bounded pseudo-disassembly");
            PseudoDisassembler pd=new PseudoDisassembler(currentProgram);
            long pc=0x414da;
            for(int n=0;n<240;n++) {
                Instruction ins=pd.disassemble(toAddr(pc));
                if(ins==null) { println("AB-148 UNDECODED "+Long.toHexString(pc)); break; }
                StringBuilder line=new StringBuilder(String.format("AB-148 %s  %s",ins.getAddress(),ins.getMnemonicString()));
                for(int i=0;i<ins.getNumOperands();i++) line.append(i==0?" ":", ").append(ins.getDefaultOperandRepresentation(i));
                line.append("  [").append(ins.getFlowType()).append("]");
                Address[] flows=ins.getFlows();
                if(flows.length>0) line.append(" -> ").append(Arrays.toString(flows));
                println(line.toString());
                pc+=ins.getLength();
                if(ins.getMnemonicString().startsWith("RET")) {
                    if(ins.getMnemonicString().equals("RET:D")) {
                        Instruction delay=pd.disassemble(toAddr(pc));
                        if(delay!=null) println(String.format("AB-148 %s  %s",delay.getAddress(),delay));
                    }
                    println("AB-148: bounded linear listing reached "+ins.getMnemonicString());
                    for(long target:new long[]{0x41534,0x415a2,0x415aa,0x41852}) dump(pd,target,120);
                    dumpTableRecord(28);
                    return;
                }
            }
            println("AB-148: pseudo-disassembly bound ended without a decoded return");
            for(long target:new long[]{0x41534,0x415a2,0x415aa,0x41852}) dump(pd,target,120);
            return;
        }
        AddressSetView body=f.getBody();
        println(String.format("AB-148 FUNCTION %s entry=%s body=%s..%s signature=%s",
            f.getName(),f.getEntryPoint(),body.getMinAddress(),body.getMaxAddress(),f.getSignature()));
        ReferenceIterator inbound=currentProgram.getReferenceManager().getReferencesTo(entry);
        while(inbound.hasNext()) {
            Reference r=inbound.next();
            println(String.format("AB-148 XREF %s %s %s",r.getFromAddress(),r.getReferenceType(),r.getSource()));
        }
        InstructionIterator it=currentProgram.getListing().getInstructions(body,true);
        int count=0;
        while(it.hasNext() && count<600) {
            Instruction ins=it.next();
            if(ins.getAddress().getOffset()>0x41700) break;
            StringBuilder line=new StringBuilder(String.format("AB-148 %s  %s",ins.getAddress(),ins.getMnemonicString()));
            for(int i=0;i<ins.getNumOperands();i++) line.append(i==0?" ":", ").append(ins.getDefaultOperandRepresentation(i));
            line.append("  [").append(ins.getFlowType()).append("]");
            Address[] flows=ins.getFlows();
            if(flows.length>0) line.append(" -> ").append(Arrays.toString(flows));
            if(ins.getFlowType().isCall()) {
                for(Address target:flows) {
                    Function callee=fm.getFunctionAt(target);
                    line.append(" {callee=").append(callee==null?"?":callee.getName()+" "+callee.getSignature()).append("}");
                }
            }
            println(line.toString()); count++;
        }
        println("AB-148 listed "+count+" instructions; this is static database evidence, not a fresh firmware-byte comparison.");
    }

    private void dumpTableRecord(int oneBasedIndex) throws Exception {
        var mem=currentProgram.getMemory();
        int count=Short.toUnsignedInt(mem.getShort(toAddr(0x5c40c)));
        int stride=Short.toUnsignedInt(mem.getShort(toAddr(0x5c40e)));
        long descriptor=0x5c410L+(long)(oneBasedIndex-1)*stride;
        println(String.format("AB-148 TABLE count-header=%d stride=%d service-R4=%d descriptor=%08x",count,stride,oneBasedIndex,descriptor));
        StringBuilder fields=new StringBuilder("AB-148 DESCRIPTOR");
        for(int offset=0;offset<stride;offset+=4) fields.append(String.format(" +%02x=%08x",offset,mem.getInt(toAddr(descriptor+offset))));
        println(fields.toString());
    }
}
