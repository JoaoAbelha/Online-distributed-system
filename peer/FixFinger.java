package peer;

import java.math.BigInteger;

public class FixFinger implements Runnable {

    private int finger = 0;
    private int bits;

    private Peer initiator;

    public FixFinger(Peer initiator, int bits) {
        this.initiator = initiator;
        this.bits = bits;
    }

    @Override
    public void run() {
        this.finger++;

        if (this.finger > this.bits)
            this.finger = 1;

        // id = (current_id + 2 ^ finger) % nr_possible_peers
        BigInteger id = initiator.getId().add(BigInteger.valueOf(2).pow(this.finger - 1))
                .mod(BigInteger.valueOf(2).pow(bits));
        PeerSignature successor = initiator.findSuccessor(id);
        initiator.getFingerTable().put(this.finger, successor);

    }

}