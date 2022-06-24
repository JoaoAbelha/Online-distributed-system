package messages;

import java.math.BigInteger;
import java.nio.channels.AsynchronousSocketChannel;

import peer.Peer;

public class DeletedACK extends Message {

    private BigInteger replicaDeleted;

    public DeletedACK(BigInteger replicaDeleted) {
        super(TYPE_MSG.DELETED_ACK);
        this.replicaDeleted = replicaDeleted;
        // TODO Auto-generated constructor stub
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(replicaDeleted.toString(16)).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
        // TODO Auto-generated method stub

    }
    
}