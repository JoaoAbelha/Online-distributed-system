package peer;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import messages.BackupACK;
import messages.Message.TYPE_MSG;

public class Reclaim {
    private Peer peer;

    public Reclaim(Peer peer) {
        this.peer = peer;
    }

    public void reclaim() {
        Set<Map.Entry<BigInteger, ReplicaInfo>> entrySet = peer.getPeerInformation().getBackupFiles().entrySet();
        Iterator<Map.Entry<BigInteger, ReplicaInfo>> itr = entrySet.iterator();

        while (peer.getPeerInformation().isOversize()) {
            if (!itr.hasNext())
                return;

            Map.Entry<BigInteger, ReplicaInfo> entry = itr.next();
            ReplicaInfo info = entry.getValue();
            String path = Peer.getFolder(peer.getPeerRootFolder() + File.separator + "backup") + File.separator
                    + info.getHash();

            peer.getPeerInformation().removeBackup(entry.getKey());
            this.requestSave(info, path);

            try {
                FileDeleter fileDeleter = new FileDeleter(path);
                fileDeleter.deleteFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestSave(ReplicaInfo replicaInfo, String path) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            SendBackupRequest request = new SendBackupRequest(replicaInfo.getHash(), replicaInfo.getFileId(),
                    replicaInfo.getFileSize(), peer, latch);
            this.peer.getThreadPool().execute(request);
            latch.await();

            if (request.getResult().getType() == TYPE_MSG.BACKUP_ACK) {
                sendBackupContent(replicaInfo, ((BackupACK) request.getResult()).getSignature(), path);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendBackupContent(ReplicaInfo info, PeerSignature signature, String path) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            SendBackupContent sendBackupContent = new SendBackupContent(info.getHash(), info.getFileId(),
                    info.getFileSize(), path, signature, latch);
            this.peer.getThreadPool().execute(sendBackupContent);
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}