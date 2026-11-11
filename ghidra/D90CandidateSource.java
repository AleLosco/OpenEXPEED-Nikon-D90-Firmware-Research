// AB-125: compare mapped candidate bytes directly with the digest-gated sample.
// @category Nikon
import ghidra.app.script.GhidraScript;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

public class D90CandidateSource extends GhidraScript {
    @Override public void run() throws Exception {
        String[] args=getScriptArgs();
        if(args.length!=1) throw new IllegalArgumentException("Pass original D90_0101.bin");
        byte[] image=Files.readAllBytes(Path.of(args[0]));
        String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image));
        if(!digest.equals("623906f285c656a7c6e4f3cecd26bc5e2b6da38ba07f0cc76a235f7e524814af"))
            throw new IllegalStateException("Unexpected sample SHA-256");
        // These are the two programmed B spans from prepare.py and AB-15.
        long[][] spans={{0x40000,0xe0000,0x80002},{0x100000,0x740000,0x140002}};
        for(long[] span:spans){
            int count=(int)(span[1]-span[0]);
            byte[] mapped=new byte[count];
            if(currentProgram.getMemory().getBytes(toAddr(span[0]),mapped)!=count)
                throw new IllegalStateException("Short mapped read");
            if(!Arrays.equals(mapped,Arrays.copyOfRange(image,(int)span[2],(int)span[2]+count)))
                throw new IllegalStateException("Span mismatch "+Long.toHexString(span[0]));
            println(String.format("AB-125: CPU [%x,%x) matches file [%x,%x)",span[0],span[1],span[2],span[2]+count));
        }
        println("AB-125: descriptor 5c480 -> file 9c482; entry 1000ac -> file 1400ae");
        println("AB-125: sample digest and both complete programmed B spans verified");
    }
}
