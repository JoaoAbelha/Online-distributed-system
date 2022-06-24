package handlers;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class BufferFlusherCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {
    private CompletionHandler<Void, Void> completionHandler;

    private AsynchronousSocketChannel channel;
    int written = 0;

    public BufferFlusherCompletionHandler(AsynchronousSocketChannel channel, CompletionHandler<Void, Void> completionHandler) {
        this.channel = channel;
        this.completionHandler = completionHandler;
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
        written += result;
        if (attachment.hasRemaining()) {
            channel.write(attachment, attachment, this);
        } else {
            completionHandler.completed(null, null);
        }
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        completionHandler.failed(exc, null);
    }
}