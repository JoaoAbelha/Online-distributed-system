package peer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import messages.BackupACK;
import messages.BackupConfirmMsg;
import messages.Message.TYPE_MSG;

public class Backup {
    private FileInfo fileInfo;
    private ArrayList<BigInteger> hashes;
    private Peer peer;
    private int current_replication;

    Backup(FileInfo fileInfo, Peer peer) {
        this.fileInfo = fileInfo;
        this.peer = peer;
        this.hashes = new ArrayList<>();
        this.current_replication = 0;
    }

    public void backup() {
        if (!this.peer.getPeerFiles().addBackupRequest(fileInfo)) {
            MyLogger.print("File " + fileInfo.getFilename() + " already backed up. Delete to backup again!",
                    Level.WARNING);
            return;
        }

        this.generateReplicatorPeers();
        this.sendBackupRequests();
    }

    private void generateReplicatorPeers() {
        for (int replication_number = 0; replication_number < fileInfo.getReplicationDegree(); replication_number++) {
            hashes.add(SHA256Hasher.hash(fileInfo, replication_number).mod(BigInteger.valueOf(2).pow(Constants.CHORD_MOD)));
        }
    }

    private void sendBackupRequests() {
        for (BigInteger hash : hashes) {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                SendBackupRequest request = new SendBackupRequest(hash, SHA256Hasher.hash(fileInfo),
                        fileInfo.getFileSize(), peer, latch);
                this.peer.getThreadPool().execute(request);
                latch.await();
                if (request.getResult().getType() == TYPE_MSG.BACKUP_ACK) {
                    sendBackupContent(hash, ((BackupACK) request.getResult()).getSignature());
                } else if (request.getResult().getType() == TYPE_MSG.BACKUP_CONFIRM) {
                    notify(true, hash, ((BackupConfirmMsg) request.getResult()).getSignature());
                } else
                    notify(false, hash, null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendBackupContent(BigInteger hash, PeerSignature signature) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            SendBackupContent sendBackupContent = new SendBackupContent(hash, SHA256Hasher.hash(fileInfo),
                    fileInfo.getFileSize(), fileInfo.getPath(), signature, latch);
            this.peer.getThreadPool().execute(sendBackupContent);
            latch.await();
            if (sendBackupContent.result.getType() == TYPE_MSG.BACKUP_CONFIRM) {
                notify(true, hash, ((BackupConfirmMsg) sendBackupContent.result).getSignature());
            } else
                notify(false, hash, null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public Peer getPeer() {
        return peer;
    }

    public synchronized void notify(boolean success, BigInteger hash, PeerSignature signature) {
        String log = success ? "Peer " + signature.getId() + " saved replica " + hash + " successfully "
                : "Replica " + hash + " backup failed";

        MyLogger.print(log, Level.INFO);

        if (success) {
            current_replication++;
            if (current_replication == fileInfo.getReplicationDegree()) {
                MyLogger.print("File successfully backed up", Level.INFO);
            }
        }
    }
}
