package messages;

import java.nio.channels.AsynchronousSocketChannel;
import peer.Peer;
import peer.PeerSignature;

public class SuccessorMsg extends Message {

  private String port = null;
  private String address = null;
  private PeerSignature initiator = null;

  public SuccessorMsg(PeerSignature initiator) {
    super(TYPE_MSG.SUCCESSOR);
    this.initiator = initiator;
  }

  public PeerSignature getPeerSignature() {
    return initiator;
  }

  @Override
  public byte[] getContent() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(type).append(CRLF).append(this.initiator.getAddress().getHostAddress()).append(CRLF)
        .append(this.initiator.getPort()).append(CRLF).append(this.initiator.getId()).append(CRLF).append(CRLF);
    return stringBuilder.toString().getBytes();
  }

  @Override
  public void handle(Peer receiver, AsynchronousSocketChannel channel) {
  }
}