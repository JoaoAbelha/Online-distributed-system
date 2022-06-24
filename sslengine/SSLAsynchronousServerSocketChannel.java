package sslengine;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class SSLAsynchronousServerSocketChannel extends AsynchronousServerSocketChannel {
    private SSLContext context;
    private AsynchronousServerSocketChannel socketChannel;

    public SSLAsynchronousServerSocketChannel(AsynchronousServerSocketChannel channel, SSLContext context) {
        super(channel.provider());
        this.context = context;
        this.socketChannel = channel;
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }

    @Override
    public boolean isOpen() {
        return socketChannel.isOpen();
    }

    @Override
    public <T> T getOption(SocketOption<T> arg0) throws IOException {
        return this.getOption(arg0);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return this.socketChannel.supportedOptions();
    }

    @Override
    public Future<AsynchronousSocketChannel> accept() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler) {
        socketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
            @Override
            public void completed(AsynchronousSocketChannel arg0, Void arg1) {
                try {
                    SSLEngine engine = SSLAsynchronousServerSocketChannel.this.context.createSSLEngine();
                    engine.setUseClientMode(false);
                    engine.setNeedClientAuth(true);
                    String[] cipher_suites = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"};
                    engine.setEnabledCipherSuites(cipher_suites);
                    
                    AsynchronousSocketChannel ssl_channel = new SLLAsynchronousSocketChannel(arg0, engine);
                    engine.beginHandshake();
                    ((SLLAsynchronousSocketChannel) ssl_channel).doHandshake(attachment, new CompletionHandler<Void,A>() {
                        @Override
                        public void completed(Void arg0, A arg1) {
                            handler.completed(ssl_channel, attachment);
                        }

                        @Override
                        public void failed(Throwable arg0, A arg1) {
                            handler.failed(arg0, attachment);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable arg0, Void arg1) {
                handler.failed(arg0, attachment);
            }
        });
    }

    @Override
    public AsynchronousServerSocketChannel bind(SocketAddress arg0, int arg1) throws IOException {
        return this.socketChannel.bind(arg0, arg1);
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return this.socketChannel.getLocalAddress();
    }

    @Override
    public <T> AsynchronousServerSocketChannel setOption(SocketOption<T> arg0, T arg1) throws IOException {
        return this.socketChannel.setOption(arg0, arg1);
    }
    
}