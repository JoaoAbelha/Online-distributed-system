package peer;

import java.math.BigInteger;
import java.util.logging.Level;

/**
 * n asks its successor for its predecessor p and decides whether p should be
 * n‘s successor instead (this is the case if p recently joined the system).
 */
public class Stabilize implements Runnable {

    private Peer peerInitiator;

    public Stabilize(Peer initiator) {
        this.peerInitiator = initiator;
    }

    @Override
    public synchronized void run() {
        PeerSignature identifier = null;
        PeerSignature successor = this.peerInitiator.getSuccessor();
        PeerSignature predecessor = this.peerInitiator.getPredecessor();

        if (successor.getId().equals(this.peerInitiator.getId())) {
            if (predecessor == null) {
                return;
            }
            identifier = predecessor;
        } else {
            try {
                identifier = this.peerInitiator.requestPredecessor(this.peerInitiator.getSuccessor());
            } catch (Exception e) {
                MyLogger.print("Fault Tolerance (not fully implemented)", Level.SEVERE);
                return;
            }
        }

        if (identifier != null) {
            if (!identifier.getId().equals(this.peerInitiator.getSuccessor().getId()) && Peer.between(
                    identifier.getId(), this.peerInitiator.getId(), this.peerInitiator.getSuccessor().getId())) {
                this.peerInitiator.setSuccessor(identifier);
            } else if (this.peerInitiator.getSuccessor().getId().equals(this.peerInitiator.getId())
                    && this.peerInitiator.getPredecessor() != null) {
                this.peerInitiator.setSuccessor(this.peerInitiator.getPredecessor());
            }
        }

        try {
            successor = this.peerInitiator.getSuccessor();
            predecessor = this.peerInitiator.getPredecessor();
            if (!successor.getId().equals(this.peerInitiator.getId()))
                this.peerInitiator.notify(this.peerInitiator.getSuccessor());
        } catch (Exception e) {

        }
    }

}