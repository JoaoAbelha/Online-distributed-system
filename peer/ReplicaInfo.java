package peer;

import java.math.BigInteger;

public class ReplicaInfo {
    private BigInteger hash;
    private long file_size;
    private BigInteger file_id;

    public ReplicaInfo(BigInteger hash, long file_size, BigInteger file_id2) {
        this.hash = hash;
        this.file_size = file_size;
        this.file_id = file_id2;
    }

    /**
     * @return the file_size
     */
    public long getFileSize() {
        return file_size;
    }

    /**
     * @return the hash
     */
    public BigInteger getHash() {
        return hash;
    }

    public BigInteger getFileId() {
        return file_id;
    }
}