// AB-128: controlled ENDM source selection from the candidate call boundary.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.util.Arrays;

public class D90CandidateEndm extends D90CandidatePrologue {
    int copies;
    @Override int stepLimit(){return 200000;}
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), windows=Arrays.copyOf(base,base.length+2);
        windows[base.length]=new long[]{0x19a542,0x19a5ec};
        windows[base.length+1]=new long[]{0x1eb044,0x1f2b64};
        return windows;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), windows=Arrays.copyOf(base,base.length+3);
        windows[base.length]=new long[]{0x7c0000,0x7c7b20};
        windows[base.length+1]=new long[]{0x7e0000,0x7e7b20};
        windows[base.length+2]=new long[]{0x8f95130cL,0x8f958e30L};
        return windows;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        byte[] defaults=c.readMemory(a.getAddress(0x1eb044),0x7b20);
        byte[] signature=Arrays.copyOfRange(defaults,0x7b00,0x7b05);
        require(Arrays.equals(signature,new byte[]{69,78,68,77,0}),"ENDM signature");
        String[] registers={"R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","R10","R11","R12","AC","FP","RP","SP","SSP","USP","PS"};
        long[] initial=new long[registers.length];
        for(int i=0;i<registers.length;i++) initial[i]=reg(c,registers[i]);
        byte[] stack=c.readMemory(a.getAddress(0x8f991204L),80);
        for(int choice=0;choice<3;choice++){
            for(int i=0;i<registers.length;i++) c.writeRegister(registers[i],initial[i]);
            c.writeRegister("PC",0x1000d0);
            byte[] first=new byte[0x7b20],second=new byte[0x7b20],dirty=new byte[0x7b24];
            Arrays.fill(first,(byte)0x51);Arrays.fill(second,(byte)0xa6);Arrays.fill(dirty,(byte)0xcc);
            if(choice==0) {
                System.arraycopy(signature,0,first,0x7b00,5);
                System.arraycopy(signature,0,second,0x7b00,5);
            }
            if(choice==1) System.arraycopy(signature,0,second,0x7b00,5);
            c.writeMemory(a.getAddress(0x7c0000),first);
            c.writeMemory(a.getAddress(0x7e0000),second);
            c.writeMemory(a.getAddress(0x8f95130cL),dirty);
            byte[] expected=(choice==0?first:choice==1?second:defaults).clone();
            System.arraycopy(defaults,0x7b00,expected,0x7b00,32);
            until(c,d,0x1000d2);
            require(Arrays.equals(expected,c.readMemory(a.getAddress(0x8f95130cL),0x7b20)),"Full destination choice "+choice);
            require(word(c,a,0x8f958e2cL)==0x8f95130c,"Published pointer");
            require(Arrays.equals(first,c.readMemory(a.getAddress(0x7c0000),0x7b20)) && Arrays.equals(second,c.readMemory(a.getAddress(0x7e0000),0x7b20)),"Source arrays");
            require(Arrays.equals(stack,c.readMemory(a.getAddress(0x8f991204L),80)),"Candidate stack");
            require(reg(c,"SP")==initial[16] && reg(c,"RP")==0x1000d2,"Call return");
            copies++;
        }
    }
    @Override public void run()throws Exception{
        super.run();
        require(copies==24,"Case count");
        println("AB-128: 24 ENDM copies and published pointers passed across three source choices");
    }
}
