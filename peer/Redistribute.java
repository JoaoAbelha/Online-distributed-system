package peer;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Map;
import java.util.logging.Level;

import handlers.LastReplyCompletionHandler;
import messages.Message;
import messages.RedistributeReplica;
import sslengine.SSLUtils;

public class Redistribute implements Runnable {
    private Peer peer;
    private PeerSignature signature;

    public Redistribute(Peer peer, PeerSignature signature) {
        this.peer = peer;
        this.signature = signature;
    }

    @Override
    public void run() {
        for (Map.Entry<BigInteger, PeerSignature> entry : peer.getPeerInformation().getRemoteReplicas().entrySet()) {
            BigInteger replica_id = entry.getKey();
            if (Peer.between(replica_id, peer.getId(), signature.getId())) {

                try {
                    RedistributeReplica message = new RedistributeReplica(replica_id, entry.getValue());
                    AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
                    InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());
                    MyLogger.print("Redestributing Replica " + replica_id + " to peer " + signature.getId(),
                            Level.INFO);
                    SSLUtils.sendMessage(message, socketChannel, address, null,
                            new LastReplyCompletionHandler(socketChannel));
                    peer.getPeerInformation().removeRemoteReplica(replica_id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
