package messages;

import java.nio.channels.AsynchronousSocketChannel;
import peer.Peer;
import peer.PeerSignature;

public class PredecessorResponseMsg extends Message {

    private PeerSignature initiator = null;

    public PredecessorResponseMsg(PeerSignature initiator) {
        super(TYPE_MSG.PREDECESSOR_RESPONSE);
        this.initiator = initiator;
    }

    public PeerSignature getSender() {
        return this.initiator;
    }

    @Override
    public byte[] getContent() {

        StringBuilder stringBuilder = new StringBuilder();
        if (this.initiator != null)
            stringBuilder.append(type).append(CRLF).append(this.initiator.getAddress().getHostAddress()).append(CRLF)
                    .append(this.initiator.getPort()).append(CRLF).append(this.initiator.getId()).append(CRLF)
                    .append(CRLF);
        else
            stringBuilder.append(type).append(CRLF).append("null").append(CRLF).append("null").append(CRLF)
                    .append("null").append(CRLF).append(CRLF);
        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
    }

}