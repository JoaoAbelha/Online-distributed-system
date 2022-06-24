package messages;

import java.nio.channels.AsynchronousSocketChannel;

import peer.Peer;
import peer.PeerSignature;

public class BackupACK extends Message {
    private PeerSignature signature;

    public BackupACK(PeerSignature signature) {
        super(TYPE_MSG.BACKUP_ACK);
        this.signature = signature;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(signature.getAddress().getHostAddress()).append(CRLF)
                .append(signature.getPort()).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
    }

    public PeerSignature getSignature() {
        return signature;
    }

}