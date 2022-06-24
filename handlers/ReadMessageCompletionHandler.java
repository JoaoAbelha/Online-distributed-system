package handlers;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import messages.Message;
import peer.Peer;

public class ReadMessageCompletionHandler implements CompletionHandler<Void, Message> {
    private AsynchronousSocketChannel channel;
    private Peer peer;

    public ReadMessageCompletionHandler(Peer peer, AsynchronousSocketChannel channel) {
        this.peer = peer;
        this.channel = channel;
    }

    @Override
    public void completed(Void result, Message attachment) {
        if (attachment != null) {
            attachment.handle(peer, channel);
        } else {
            closeConnection();
        }
    }

    @Override
    public void failed(Throwable exc, Message attachment) {
        closeConnection();
    }

    private void closeConnection() {
        try {
            channel.close();
        } catch (IOException e) {
        }
    }
}