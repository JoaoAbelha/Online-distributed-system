package peer;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Restore {
    private FileInfo fileInfo;
    private ArrayList<BigInteger> hashes;
    private Peer peer;

    public Restore(Peer peer, FileInfo info) {
        this.peer = peer;
        this.fileInfo = info;
        this.hashes = new ArrayList<>();
    }

    public void restore() {
        BigInteger file_id = SHA256Hasher.hash(fileInfo);

        if (peer.getPeerInformation().hasFile(file_id)) {
            this.restoreOwnFile(file_id);
        } else {
            generateReplicatorPeers();
            this.restoreFile();
        }
    }

    private void restoreOwnFile(BigInteger file_id) {
        try {
            // read
            ReplicaInfo info = this.peer.getPeerInformation().getReplicaInfo(file_id);
            String replica_path = this.peer.getPeerRootFolder() + File.separator + "backup" + File.separator
                    + info.getHash();
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get(replica_path),
                    StandardOpenOption.READ);
            ByteBuffer buffer = ByteBuffer.allocate((int) info.getFileSize());
            fileChannel.read(buffer, 0).get();

            // write
            String restore_path = Peer.getFolder(peer.getPeerRootFolder() + File.separator + "restore") + File.separator
                    + fileInfo.getFilename();
            fileChannel = AsynchronousFileChannel.open(Paths.get(restore_path), StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);
            buffer.flip();
            fileChannel.write(buffer, 0).get();
            MyLogger.print("Could Restore File " + fileInfo.getFilename(), Level.INFO);
        } catch (IOException | InterruptedException | ExecutionException e) {
            MyLogger.print("Failed to restore own file", Level.WARNING);
            e.printStackTrace();
        }
    }

    private void restoreFile() {
        for (BigInteger hash : hashes) {
            CountDownLatch latch = new CountDownLatch(1);
            SendRestoreRequest request = new SendRestoreRequest(hash, this, latch);
            try {
                this.peer.getThreadPool().execute(request);
                latch.await(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
                MyLogger.print("Could Restore Replica " + hash, Level.INFO);
                break;
            } catch (InterruptedException e) {
                request.close();
                MyLogger.print("Replica not found", Level.WARNING);
                continue;
            }
        }
    }

    private void generateReplicatorPeers() {
        for (int replication_number = 0; replication_number < fileInfo.getReplicationDegree(); replication_number++) {
            hashes.add(SHA256Hasher.hash(fileInfo, replication_number).mod(BigInteger.valueOf(2).pow(Constants.CHORD_MOD)));
        }
    }

    public Peer getPeer() {
        return peer;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }
}