package messages;

import java.math.BigInteger;
import java.nio.channels.AsynchronousSocketChannel;

import peer.Peer;
import peer.PeerSignature;

public class RedistributeReplica extends Message {
    private BigInteger replica_id;
    private PeerSignature location;

    public RedistributeReplica(BigInteger replica_id, PeerSignature location) {
        super(TYPE_MSG.REDISTRIBUTE_REPLICA);
        this.replica_id = replica_id;
        this.location = location;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(replica_id.toString(16)).append(CRLF)
                .append(location.getAddress().getHostAddress()).append(CRLF).append(location.getPort()).append(CRLF)
                .append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
        receiver.getPeerInformation().addRemoteReplica(replica_id, location);
    }
}
