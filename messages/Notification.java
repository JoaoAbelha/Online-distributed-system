package messages;

import java.nio.channels.AsynchronousSocketChannel;

import messages.Message;
import peer.Peer;
import peer.PeerSignature;
import peer.Redistribute;

public class Notification extends Message {

    PeerSignature initiator = null;

    public Notification(PeerSignature initiator) {
        super(TYPE_MSG.NOTIFICATION);
        this.initiator = initiator;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(initiator.getAddress().getHostAddress()).append(CRLF)
                .append(initiator.getPort()).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {

        if (receiver.getPredecessor() == null || receiver.getPredecessor().getId().equals(receiver.getId())
                || Peer.between(this.initiator.getId(), receiver.getPredecessor().getId(), receiver.getId())) {
            receiver.setPredecessor(this.initiator);
            receiver.getThreadPool().execute(new Redistribute(receiver, initiator));
        }

    }

}
