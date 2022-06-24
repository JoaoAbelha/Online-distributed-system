package peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import messages.Message;
import sslengine.SSLUtils;

public class FileSender {
    private AsynchronousSocketChannel channel;
    private AsynchronousFileChannel fileChannel;
    private String path;
    private long file_size;
    private Message initial_message;

    public FileSender(AsynchronousSocketChannel channel, FileInfo info, Message initial_message) throws IOException {
        this.channel = channel;
        this.path = info.getPath();
        this.file_size = info.getFileSize();
        this.fileChannel = AsynchronousFileChannel.open(Paths.get(this.path), StandardOpenOption.READ);
        this.initial_message = initial_message;
    }

    public FileSender(AsynchronousSocketChannel channel, String path, Long file_size, Message initial_message) throws IOException {
        this.channel = channel;
        this.path = path;
        
        File file = new File(path);
        if(!file.exists()) {
            throw new FileNotFoundException();
        }

        this.file_size = file_size;
        this.fileChannel = AsynchronousFileChannel.open(Paths.get(path), StandardOpenOption.READ);
        this.initial_message = initial_message;
    }

    public void init_tranfer(CompletionHandler<Void, Void> handler) {
        SSLUtils.sendMessage(initial_message, channel, null, new CompletionHandler<Void,Void>() {
            @Override
            public void completed(Void arg0, Void arg1) {
                transfer(handler);
            }

            @Override
            public void failed(Throwable arg0, Void arg1) {
                handler.failed(arg0, arg1);
            }
        });
    }

    public void init_tranfer(CompletionHandler<Void, Void> handler, InetSocketAddress address) {
        SSLUtils.sendMessage(initial_message, channel, address, null, new CompletionHandler<Void,Void>() {
            @Override
            public void completed(Void arg0, Void arg1) {
                transfer(handler);
            }

            @Override
            public void failed(Throwable arg0, Void arg1) {
                handler.failed(arg0, arg1);
            }
        });
	}

    private void transfer(CompletionHandler<Void, Void> handler) {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);

        fileChannel.read(buffer, 0, null, new CompletionHandler<Integer,Void>() {
            private long pos = 0;
            private CompletionHandler<Integer,Void> current = this;

            @Override
            public void completed(Integer result, Void a) {
                if (result < 0) {
                    handler.failed(null, null);
                    close();
                    return;
                }
                
                pos += result;
                buffer.flip();
                
                channel.write(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, null, new CompletionHandler<Integer,Void>() {
                    @Override
                    public void completed(Integer result, Void attachment) {
                        if (result < 0) {
                            handler.failed(null, null);
                            close();
                            return;
                        }

                        if(buffer.hasRemaining()) {
                            channel.write(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, null, this);
                        } else if(pos < file_size) {
                            buffer.clear();
                            fileChannel.read(buffer, pos, null, current);
                        } else {
                            handler.completed(null, null);
                            close();
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        handler.failed(null, null);
                        close();
                        exc.printStackTrace();
                    }  
                });
            }

            @Override
            public void failed(Throwable exc, Void a) {
                handler.failed(null, null);
                close();
                exc.printStackTrace();
            }
        });
    } 

    private void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
