package handlers;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import sslengine.SSLUtils;

public class LastReplyCompletionHandler implements CompletionHandler<Void, Void> {
    private AsynchronousSocketChannel channel;

    public LastReplyCompletionHandler(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void completed(Void result, Void attachment) {
        SSLUtils.closeChannel(channel);
    }

    @Override
    public void failed(Throwable exc, Void attachment) {
        SSLUtils.closeChannel(channel);
    }
}