package messages;

import java.math.BigInteger;
import java.nio.channels.AsynchronousSocketChannel;

import handlers.LastReplyCompletionHandler;
import peer.Peer;
import peer.PeerSignature;
import sslengine.SSLUtils;

/** type address port id */
public class SuccessorRequestMsg extends Message {

  private MessageHandler handler = null;
  private BigInteger key = null;

  public SuccessorRequestMsg(BigInteger key) {
    super(TYPE_MSG.SUCCESSOR_REQUEST);
    this.key = key;
  }

  @Override
  public byte[] getContent() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(type).append(CRLF).append(key.toString(16)).append(CRLF).append(CRLF);
    return stringBuilder.toString().getBytes();
  }

  @Override
  public void handle(Peer receiver, AsynchronousSocketChannel channel) {

    PeerSignature successor = receiver.findSuccessor(key);
    if (successor != null
        && (successor.getId().equals(receiver.getId()) || successor.getId().equals(receiver.getSuccessor().getId()))) {
      Message forwardQuery = new SuccessorMsg(receiver.getPeerSignature());
      SSLUtils.sendMessage(forwardQuery, channel, null, new LastReplyCompletionHandler(channel));
    }
  }
}