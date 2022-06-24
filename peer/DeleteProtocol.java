package peer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DeleteProtocol {

    private Peer initiator;
    private FileInfo fileInfo;

    DeleteProtocol(FileInfo fileInfo, Peer peer) {
        this.initiator = peer;
        this.fileInfo = fileInfo;
    }

    private ArrayList<BigInteger> generateReplicatorPeers(int replication) {
        ArrayList<BigInteger> hashes = new ArrayList<>();
        for (int replication_number = 0; replication_number < replication; replication_number++) {
            hashes.add(SHA256Hasher.hash(fileInfo, replication_number).mod(BigInteger.valueOf(2).pow(Constants.CHORD_MOD)));
        }
        return hashes;
    }

    public void delete() {
        ArrayList<BigInteger> replicatorIDS = generateReplicatorPeers(fileInfo.getReplicationDegree());

        for (BigInteger replica : replicatorIDS) {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                this.initiator.getThreadPool().submit(new SendDelete(replica, initiator, fileInfo, latch));
                latch.await(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
                MyLogger.print("Deleted replica " + replica + " successfully", Level.INFO);
            } catch (InterruptedException e) {
                MyLogger.print("Unable to delete replica " + replica, Level.WARNING);
            }
        }
    }

}