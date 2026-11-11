// AB-144: fourth candidate call with two controlled record banks.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90FourthCallIntegrated extends D90ThirdCallIntegrated {
    static final int SLOT=0x3600;
    static final long OUTPUT=0x89f958c0L;
    int fourthCases;

    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), w=Arrays.copyOf(base,base.length+3);
        w[base.length]=new long[]{0x1000e4,0x1000ea};
        w[base.length+1]=new long[]{0x13b7f6,0x13b942};
        w[base.length+2]=new long[]{0x1cac80,0x1cac88};
        return w;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(), w=Arrays.copyOf(base,base.length+5);
        w[base.length]=new long[]{0x780000,0x788000};
        w[base.length+1]=new long[]{0x7a0000,0x7a8000};
        w[base.length+2]=new long[]{0x8f5330f0L,0x8f533174L};
        w[base.length+3]=new long[]{0x8f533264L,0x8f533270L};
        w[base.length+4]=new long[]{OUTPUT-4,OUTPUT+5*SLOT+4};
        return w;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        require(reg(c,"PC")==0x1000e2,"Fourth call entry");
        long sp=reg(c,"SP");
        int delta=new int[]{0,1,0x7fff,0x8000,0x8001,0xffff,2,0x8000}[fourthCases];
        byte[][][] payload=new byte[2][5][];
        for(int bank=0;bank<2;bank++){
            long address=0x780000+bank*0x20000;
            require(word(c,a,0x1cac80+4*bank)==address,"Original bank pointer");
            ByteBuffer stream=ByteBuffer.allocate(32768);
            int[] lengths=bank==0?new int[]{3,9,0,-1,-1}:new int[]{7,-1,5,-1,SLOT};
            for(int i=0;i<5;i++)if(lengths[i]>=0){
                byte[] bytes=new byte[lengths[i]];
                for(int j=0;j<bytes.length;j++) bytes[j]=(byte)(11*i+71*bank+3*j+fourthCases);
                stream.putShort((short)i).putShort((short)bytes.length).put(bytes);
                if((bytes.length&1)!=0)stream.put((byte)0);
                payload[bank][i]=bytes;
            }
            stream.putShort((short)0xaa55).putShort((short)(bank==0?delta:0));
            if(fourthCases>=4){
                // A second marker would replace the published sequence if parsing continued.
                int invalidLength=bank==0?SLOT+1:0;
                stream.putShort((short)(bank==0?4:5)).putShort((short)invalidLength);
                stream.position(stream.position()+invalidLength+(invalidLength&1));
                stream.putShort((short)0xaa55).putShort((short)0xabcd);
            }
            stream.putInt(0xffff0000);
            c.writeMemory(a.getAddress(address),stream.array());
        }
        byte[] output=new byte[5*SLOT+8], meta=new byte[12];
        Arrays.fill(output,(byte)0xcc);Arrays.fill(meta,(byte)0xcc);
        c.writeMemory(a.getAddress(OUTPUT-4),output);
        c.writeMemory(a.getAddress(0x8f533264L),meta);
        int preferred=delta<0x8000?0:1;
        for(int i=0;i<5;i++){
            byte[] chosen=payload[preferred][i];
            if(chosen==null || chosen.length==0)chosen=payload[1-preferred][i];
            if(chosen!=null && chosen.length!=0){
                System.arraycopy(chosen,0,output,4+i*SLOT,chosen.length);
                ByteBuffer.wrap(meta).putShort(2*i,(short)chosen.length);
            }
        }
        meta[10]=1;meta[11]=1;
        until(c,d,0x1000ea);
        require(Arrays.equals(output,c.readMemory(a.getAddress(OUTPUT-4),output.length)),"Five slots and guards");
        require(Arrays.equals(meta,c.readMemory(a.getAddress(0x8f533264L),12)),"Lengths and marker flags");
        require(word(c,a,0x8f533170L)==(delta<<16),"First markers retained");
        require(reg(c,"SP")==sp && reg(c,"RP")==0x1000ea,"Return");
        fourthCases++;
    }
    @Override public void run()throws Exception{
        super.run();require(fourthCases==8,"Case count");
        println("AB-144: eight complete fourth calls passed at 0x1000ea");
    }
}
