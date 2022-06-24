package messages;

import java.nio.channels.AsynchronousSocketChannel;

import peer.Peer;

public class BackupNACK extends Message {

    public BackupNACK() {
        super(TYPE_MSG.BACKUP_NACK);
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {

    }

}