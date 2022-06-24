package messages;

import java.io.File;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

import handlers.LastReplyCompletionHandler;
import peer.FileDeleter;
import peer.Peer;
import peer.PeerSignature;
import sslengine.SSLUtils;

public class DeleteRequestMsg extends Message {

    private BigInteger replicaID;
    private BigInteger fileID;
    private PeerSignature signature;

    public DeleteRequestMsg(BigInteger replicaID, BigInteger fileID, PeerSignature signature) {
        super(TYPE_MSG.DELETE_REQUEST);
        this.replicaID = replicaID;
        this.fileID = fileID;
        this.signature = signature;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(signature.getAddress().getHostAddress()).append(CRLF)
                .append(signature.getPort()).append(CRLF).append(replicaID.toString(16)).append(CRLF)
                .append(fileID.toString(16)).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
        if (receiver.getPeerInformation().hasReplica(fileID, replicaID)) {
            try {
                AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
                InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());

                receiver.getPeerInformation().deleteReplica(fileID);

                receiver.getPeerInformation().removeRemoteReplica(replicaID);
                String path = receiver.getPeerRootFolder() + File.separator + "backup" + File.separator + replicaID;
                FileDeleter deleter = new FileDeleter(path);
                deleter.deleteFile();

                DeletedACK deleted = new DeletedACK(replicaID);
                SSLUtils.sendMessage(deleted, socketChannel, address, null,
                        new LastReplyCompletionHandler(socketChannel));
            } catch (Exception e) {
            }
        } else if (receiver.getPeerInformation().hasRemoteReplica(replicaID)) {
            try {
                PeerSignature signature = receiver.getPeerInformation().getRemoteReplica(replicaID);
                receiver.getPeerInformation().removeRemoteReplica(replicaID);
                AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
                InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());
                SSLUtils.sendMessage(this, socketChannel, address, null, new LastReplyCompletionHandler(socketChannel));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}