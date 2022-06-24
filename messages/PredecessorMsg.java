package messages;

import java.nio.channels.AsynchronousSocketChannel;

import handlers.LastReplyCompletionHandler;
import peer.Peer;
import peer.PeerSignature;
import sslengine.SSLUtils;

public class PredecessorMsg extends Message {

    public PredecessorMsg() {
        super(TYPE_MSG.PREDECESSOR);
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {
        Message forwardQuery = new PredecessorResponse();
        SSLUtils.sendMessage(forwardQuery, channel, null, new LastReplyCompletionHandler(channel));
    }
}