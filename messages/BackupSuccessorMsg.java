package messages;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.KeyManagementException;

import handlers.LastReplyCompletionHandler;
import peer.Peer;
import peer.PeerSignature;
import peer.ReplicaInfo;
import sslengine.SSLUtils;

public class BackupSuccessorMsg extends Message {
    private BigInteger replica_id;
    private BigInteger file_id;
    private Long file_size;
    private PeerSignature signature;
    private BigInteger origin_peer;

    public BackupSuccessorMsg(BigInteger hash, Long file_size, BigInteger file_id, PeerSignature signature,
            BigInteger origin) {
        super(TYPE_MSG.BACKUP_SUCCESSOR);
        this.replica_id = hash;
        this.file_id = file_id;
        this.file_size = file_size;
        this.signature = signature;
        this.origin_peer = origin;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(signature.getAddress().getHostAddress()).append(CRLF)
                .append(signature.getPort()).append(CRLF).append(origin_peer.toString(16)).append(CRLF)
                .append(replica_id.toString(16)).append(CRLF).append(file_id.toString(16)).append(CRLF)
                .append(file_size).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
        try {
            AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());

            if (origin_peer.equals(receiver.getId())) {
                BackupNACK nack = new BackupNACK();
                SSLUtils.sendMessage(nack, socketChannel, address, null, new LastReplyCompletionHandler(socketChannel));
                return;
            }

            if (receiver.getPeerInformation().hasReplica(file_id, replica_id)) {
                BackupConfirmMsg confirmMsg = new BackupConfirmMsg(receiver.getPeerSignature());
                SSLUtils.sendMessage(confirmMsg, socketChannel, address, null,
                        new LastReplyCompletionHandler(socketChannel));
                return;
            }

            if (!receiver.getPeerInformation().hasFile(file_id) && receiver.getPeerInformation().canStore(file_size)) {
                BackupACK ack = new BackupACK(receiver.getPeerSignature());
                SSLUtils.sendMessage(ack, socketChannel, address, null, new LastReplyCompletionHandler(socketChannel));
            } else {
                InetSocketAddress sucessor_address = new InetSocketAddress(receiver.getSuccessor().getAddress(),
                        receiver.getSuccessor().getPort());
                SSLUtils.sendMessage(this, socketChannel, sucessor_address, null,
                        new LastReplyCompletionHandler(socketChannel));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}