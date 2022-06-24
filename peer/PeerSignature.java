package peer;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class PeerSignature {

    private BigInteger id;
    private InetAddress address;
    private int port;

    public PeerSignature(InetAddress address, int port) {
        this.address = address;
        this.port = port;

        this.id = SHA256Hasher.hash(address, port).mod(BigInteger.valueOf(2).pow(Constants.CHORD_MOD));
    }

    public PeerSignature(InetSocketAddress address) {
        this.address = address.getAddress();
        this.port = address.getPort();

        this.id = SHA256Hasher.hash(this.address, this.port).mod(BigInteger.valueOf(2).pow(Constants.CHORD_MOD));
    }

    public PeerSignature(String address, String port) {
        try {
            if (address.contains("/"))
                this.address = InetAddress.getByName(address.substring(1));
            else
                this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.port = Integer.parseInt(port);
        this.id = SHA256Hasher.hash(this.address, this.port).mod(BigInteger.valueOf(2).pow(Constants.CHORD_MOD));
    }

    public int getPort() {
        return this.port;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public BigInteger getId() {
        return id;
    }

}