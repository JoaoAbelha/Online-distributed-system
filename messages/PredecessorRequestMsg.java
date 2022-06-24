package messages;

import java.math.BigInteger;
import java.nio.channels.AsynchronousSocketChannel;

import handlers.LastReplyCompletionHandler;
import peer.Peer;
import sslengine.SSLUtils;

public class PredecessorRequestMsg extends Message {

    BigInteger key = null;

    public PredecessorRequestMsg(BigInteger key) {
        super(Message.TYPE_MSG.PREDECESSOR_REQUEST);
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
        Message msg = new PredecessorResponseMsg(receiver.getPredecessor());
        SSLUtils.sendMessage(msg, channel, null, new LastReplyCompletionHandler(channel));
    }

}