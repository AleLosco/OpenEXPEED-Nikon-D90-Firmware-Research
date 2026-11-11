// AB-142: continuous execution from the third CALL through the original return.
// @category Nikon
import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.util.PseudoDisassembler;
import ghidra.program.database.ProgramDB;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class D90ThirdCallIntegrated extends D90CandidateSystemFix {
    int completed;
    long destination(int i) throws Exception {
        return Integer.toUnsignedLong(currentProgram.getMemory().getInt(toAddr(0x1caabc+8*i)));
    }
    int length(int i) throws Exception {
        return Short.toUnsignedInt(currentProgram.getMemory().getShort(toAddr(0x1caac0+8*i)));
    }
    @Override long[][] extraWindows(){
        long[][] base=super.extraWindows(), w=Arrays.copyOf(base,base.length+7);
        long[][] added={{0x1000da,0x1000e2},{0x13a1dc,0x13a46a},{0x13a802,0x13a826},
            {0x1caab4,0x1cabc2},{0x1ad966,0x1ad97e},{0x1ae4c0,0x1ae4cc},{0x1000e2,0x1000e4}};
        System.arraycopy(added,0,w,base.length,added.length);
        return w;
    }
    @Override long[][] extraSyntheticWindows(){
        long[][] base=super.extraSyntheticWindows(),w=Arrays.copyOf(base,base.length+30);
        int n=base.length;
        w[n++]=new long[]{0x740000,0x748000};
        w[n++]=new long[]{0x760000,0x768000};
        w[n++]=new long[]{0x8f532ec8L,0x8f533042L};
        w[n++]=new long[]{0x8f6e419cL,0x8f6e440cL};
        try {
            for(int i=0;i<26;i++) w[n++]=new long[]{destination(i),destination(i)+length(i)};
        } catch(Exception e){throw new IllegalStateException("Descriptor mapping",e);}
        return w;
    }
    @Override void afterInitialization(EmulatorHelper c,PseudoDisassembler d,ProgramDB p)throws Exception{
        super.afterInitialization(c,d,p);
        var a=p.getLanguage().getDefaultSpace();
        require(reg(c,"PC")==0x1000da,"Candidate boundary");
        long sp=reg(c,"SP");
        String[] names={"R8","R9","R10","R11"};
        long[] regs=new long[4];
        for(int i=0;i<4;i++)regs[i]=reg(c,names[i]);
        byte[][] expected=new byte[26][];
        byte[] flags=new byte[26];
        ByteBuffer[] banks={ByteBuffer.allocate(32768),ByteBuffer.allocate(32768)};
        for(int i=0;i<26;i++){
            byte[] initial=new byte[length(i)];
            Arrays.fill(initial,(byte)0xbb);
            c.writeMemory(a.getAddress(destination(i)),initial);
            expected[i]=initial.clone();
            boolean present=((completed+i)%2)==0;
            if(present){
                for(int j=0;j<initial.length;j++)expected[i][j]=(byte)(i+3*j+completed);
                banks[i%2].putShort((short)i).putShort((short)initial.length).put(expected[i]);
            }else flags[i]=1;
        }
        for(int i=0;i<2;i++){
            banks[i].putShort((short)0xaa55).putShort((short)(3+i)).putShort((short)0xffff).putShort((short)0);
            c.writeMemory(a.getAddress(0x740000+0x20000*i),banks[i].array());
        }
        c.writeMemory(a.getAddress(0x8f532ec8L),new byte[0x17a]);
        if(flags[1]!=0){expected[1][0]&=(byte)0xcf;expected[1][4]=(byte)((expected[1][4]&0x83)|8);}
        expected[1][16]|=(byte)0xf0;expected[1][11]&=(byte)0xfc;
        for(int type=2;type<=3;type++) if(flags[type]!=0){
            byte[] defaults=c.readMemory(a.getAddress(type==2?0x1cabaf:0x1cab8c),type==2?19:35);
            for(int k=0;k<4;k++)System.arraycopy(defaults,0,expected[type],
                type==2?31+58*k:23+56*k,defaults.length);
        }
        for(int i=9;i<=20;i++)if(flags[i]!=0){
            int offset=i==11?40:0;
            Arrays.fill(expected[i],offset,offset+(i<=10?68:i==11?2:64),(byte)0);
        }
        if(flags[23]!=0){Arrays.fill(expected[23],0,8,(byte)0);expected[1][1]&=(byte)0xf7;}
        if(flags[24]!=0)Arrays.fill(expected[24],0,2,(byte)0);
        expected[25][142]=0;
        until(c,d,0x1000e2);
        for(int i=0;i<26;i++) require(Arrays.equals(expected[i],c.readMemory(a.getAddress(destination(i)),length(i))),"Final block "+i);
        require(Arrays.equals(expected[23],c.readMemory(a.getAddress(0x8f6e419cL),624)),"Mirror");
        require(Arrays.equals(flags,c.readMemory(a.getAddress(0x8f53300cL),26)),"Firmware flags");
        require(reg(c,"SP")==sp && reg(c,"RP")==0x1000e2,"Return");
        for(int i=0;i<4;i++)require(reg(c,names[i])==regs[i],"Saved register "+names[i]);
        completed++;
    }
    @Override public void run() throws Exception {
        super.run();require(completed==8,"Scenario count");
        println("AB-142: eight continuous third calls passed with 26 checked blocks and mirror");
    }
}
