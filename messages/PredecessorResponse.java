package messages;

import java.nio.channels.AsynchronousSocketChannel;
import peer.Peer;
import peer.PeerSignature;

public class PredecessorResponse extends Message {

    public PredecessorResponse() {
        super(TYPE_MSG.PREDECESSOR_ALIVE);
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
