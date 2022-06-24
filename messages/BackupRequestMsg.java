package messages;

import java.math.BigInteger;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import handlers.LastReplyCompletionHandler;
import peer.Constants;
import peer.FileInfo;
import peer.Peer;
import peer.ReplicaInfo;
import peer.SHA256Hasher;
import peer.SendBackupSucessor;
import sslengine.SSLUtils;

public class BackupRequestMsg extends Message {
    private BigInteger replica_id;
    private BigInteger file_id;
    private long file_size;

    public BackupRequestMsg(BigInteger hash, Long file_size, BigInteger file_id) {
        super(TYPE_MSG.BACKUP_REQUEST);
        this.replica_id = hash;
        this.file_size = file_size;
        this.file_id = file_id;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(replica_id.toString(16)).append(CRLF)
                .append(file_id.toString(16)).append(CRLF).append(file_size).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
        if (receiver.getPeerInformation().hasReplica(file_id, replica_id)) {
            BackupConfirmMsg confirmMsg = new BackupConfirmMsg(receiver.getPeerSignature());
            receiver.getPeerInformation().addRemoteReplica(replica_id, receiver.getPeerSignature());
            SSLUtils.sendMessage(confirmMsg, channel, null, new LastReplyCompletionHandler(channel));
            return;
        }

        if (!receiver.getPeerInformation().hasFile(file_id) && receiver.getPeerInformation().canStore(file_size)) {
            BackupACK ack = new BackupACK(receiver.getPeerSignature());
            receiver.getPeerInformation().addRemoteReplica(replica_id, receiver.getPeerSignature());
            SSLUtils.sendMessage(ack, channel, null, new LastReplyCompletionHandler(channel));
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            SendBackupSucessor backupSucessor = new SendBackupSucessor(receiver, latch, file_id, replica_id, file_size);

            try {
                receiver.getThreadPool().execute(backupSucessor);
                latch.await(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Message result = backupSucessor.getResult();
                if(result.getType() == TYPE_MSG.BACKUP_ACK) {
                    receiver.getPeerInformation().addRemoteReplica(replica_id, ((BackupACK)result).getSignature());
                } else if(result.getType() == TYPE_MSG.BACKUP_CONFIRM) {
                    receiver.getPeerInformation().addRemoteReplica(replica_id, ((BackupConfirmMsg)result).getSignature());
                }
                SSLUtils.sendMessage(backupSucessor.getResult(), channel, null, new LastReplyCompletionHandler(channel));
            } catch (InterruptedException e) {
                backupSucessor.close();
            }
        }
    }
}