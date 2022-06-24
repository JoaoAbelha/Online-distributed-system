package messages;

import java.io.File;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

import handlers.LastReplyCompletionHandler;
import peer.FileSender;

import peer.Peer;
import peer.PeerSignature;
import peer.ReplicaInfo;
import sslengine.SSLUtils;

public class RestoreRequest extends Message {
    private BigInteger replica_id;
    private BigInteger file_id;
    private PeerSignature signature;

    public RestoreRequest(BigInteger replica_id, BigInteger file_id, PeerSignature signature) {
        super(TYPE_MSG.RESTORE_REQUEST);
        this.replica_id = replica_id;
        this.file_id = file_id;
        this.signature = signature;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(signature.getAddress().getHostAddress()).append(CRLF)
                .append(signature.getPort()).append(CRLF).append(replica_id.toString(16)).append(CRLF)
                .append(file_id.toString(16)).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
        if (receiver.getPeerInformation().hasReplica(file_id, replica_id)) {
            try {
                ReplicaInfo info = receiver.getPeerInformation().getReplicaInfo(file_id);
                RestoreContent restoreContent = new RestoreContent(info.getFileSize());
                AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
                InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());

                String path = receiver.getPeerRootFolder() + File.separator + "backup" + File.separator + replica_id;
                FileSender fileSender = new FileSender(socketChannel, path, info.getFileSize(), restoreContent);
                fileSender.init_tranfer(new LastReplyCompletionHandler(channel), address);
            } catch (Exception e) {
            }
        } else if (receiver.getPeerInformation().hasRemoteReplica(replica_id)) {
            try {
                PeerSignature signature = receiver.getPeerInformation().getRemoteReplica(replica_id);
                AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
                InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());
                SSLUtils.sendMessage(this, socketChannel, address, null, new LastReplyCompletionHandler(socketChannel));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}