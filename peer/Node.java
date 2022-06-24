package peer;

import java.math.BigInteger;
import java.net.InetAddress;

public abstract class Node {
    public abstract void listen();

    public abstract void setPredecessor(PeerSignature id);

    public abstract BigInteger getId();

    public abstract PeerSignature getPredecessor();

    public abstract int getPort();

    public abstract InetAddress getAddress();

    public abstract void stabilize();

    public abstract void notify(PeerSignature id);

    public abstract void addFinger(PeerSignature signature, int index);

    public abstract PeerSignature getCloserPreviousNode(BigInteger id);

    public abstract boolean joinChord(PeerSignature node);
}
