package peer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FileReceiver {
    private AsynchronousSocketChannel channel;
    private AsynchronousFileChannel filechannel;
    private AtomicLong position;
    private Peer peer;
    private String filename;
    private Long file_size;

    public FileReceiver(AsynchronousSocketChannel channel, ReplicaInfo info, Peer peer, String path)
            throws IOException {
        this.channel = channel;
        this.file_size = info.getFileSize();
        this.peer = peer;
        this.filename = Peer.getFolder(peer.getPeerRootFolder() + File.separator + path) + File.separator
                + info.getHash();
        this.filechannel = AsynchronousFileChannel.open(Paths.get(this.filename), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
        this.position = new AtomicLong(0l);
    }

    public FileReceiver(AsynchronousSocketChannel channel, String filename, Long file_size, Peer peer, String path)
            throws IOException {
        this.channel = channel;
        this.peer = peer;
        this.file_size = file_size;
        this.filename = Peer.getFolder(peer.getPeerRootFolder() + File.separator + path) + File.separator + filename;
        this.filechannel = AsynchronousFileChannel.open(Paths.get(this.filename), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
        this.position = new AtomicLong(0l);
    }

    public void readFile(CompletionHandler<Void, Void> handler) {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);

        channel.read(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, null, new CompletionHandler<Integer, Void>() {
            CompletionHandler<Integer, Void> current = this;

            @Override
            public void completed(Integer result, Void attachment) {
                if (result >= 0) {
                    if (result > 0) {
                        buffer.flip();

                        filechannel.write(buffer, position.get(), null, new CompletionHandler<Integer, Void>() {
                            @Override
                            public void completed(Integer result, Void a) {
                                position.addAndGet(result.longValue());

                                if (buffer.hasRemaining()) {
                                    filechannel.write(buffer, position.get(), null, this);
                                } else if (position.get() < file_size) {
                                    buffer.compact();
                                    channel.read(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, attachment,
                                            current);
                                } else {
                                    handler.completed(null, null);
                                    close();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, Void a) {
                                handler.failed(null, null);
                                close();
                            }
                        });
                    }

                } else if (result < 0) {
                    handler.failed(null, null);
                    close();
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                handler.failed(null, null);
                close();
            }
        });
    }

    private void close() {
        try {
            filechannel.close();
        } catch (IOException e) {
            // throw new RuntimeException("unable to close channel and FileWriter", e);
        }
    }
}