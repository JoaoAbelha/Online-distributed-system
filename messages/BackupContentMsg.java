package messages;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import handlers.LastReplyCompletionHandler;
import peer.FileDeleter;
import peer.FileReceiver;
import peer.Peer;
import peer.ReplicaInfo;
import sslengine.SSLUtils;

public class BackupContentMsg extends Message {
    private BigInteger replica_id;
    private BigInteger file_id;
    private long file_size;

    public BackupContentMsg(BigInteger hash, Long file_size, BigInteger file_id) {
        super(TYPE_MSG.BACKUP_CONTENT);
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
            SSLUtils.sendMessage(confirmMsg, channel, null, new LastReplyCompletionHandler(channel));
            return;
        }

        if (!receiver.getPeerInformation().hasFile(file_id) && receiver.getPeerInformation().canStore(file_size)) {
            try {
                ReplicaInfo replicaInfo = new ReplicaInfo(replica_id, file_size, file_id);
                receiver.getPeerInformation().addReplica(file_id, replicaInfo);
                FileReceiver fileReceiver = new FileReceiver(channel, replicaInfo, receiver, "backup");
                fileReceiver.readFile(new CompletionHandler<Void,Void>() {
                    @Override
                    public void completed(Void arg0, Void arg1) {
                        SSLUtils.sendMessage(new BackupConfirmMsg(receiver.getPeerSignature()), channel, null, new LastReplyCompletionHandler(channel));
                    }

                    @Override
                    public void failed(Throwable arg0, Void arg1) {
                        backupFail(receiver);
                        SSLUtils.closeChannel(channel);
                    }
                });
            } catch (IOException e) {
                backupFail(receiver);
                SSLUtils.closeChannel(channel);
            }
        } else {
            BackupNACK nack = new BackupNACK();
            SSLUtils.sendMessage(nack, channel, null, new LastReplyCompletionHandler(channel));
        }
    }

    private void backupFail(Peer receiver) {
        try {
            receiver.getPeerInformation().deleteReplica(file_id);
            FileDeleter fileDeleter = new FileDeleter(receiver.getPeerRootFolder() + File.separator + "backup" + File.separator + replica_id);
            fileDeleter.deleteFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}