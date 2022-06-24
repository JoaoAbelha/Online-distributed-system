package peer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

import messages.SuccessorRequestMsg;
import messages.Message;
import messages.MessageHandler;
import messages.SuccessorMsg;
import sslengine.SLLAsynchronousSocketChannel;
import sslengine.SSLUtils;

public class SendSuccessorRequest implements Runnable {
    private PeerSignature signature;
    private Peer peer;
    private CountDownLatch latch;
    private PeerSignature successor;
    private BigInteger key;

    SendSuccessorRequest(PeerSignature signature, Peer peer, BigInteger key, CountDownLatch latch) {
        this.peer = peer;
        this.signature = signature;
        this.key = key;
        this.latch = latch;
    }

    @Override
    public void run() {
        Message message = new SuccessorRequestMsg(key);

        try {
            AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
            if (this.signature == null) {
                return;
            }
            InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());
            socketChannel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    sendRequestMessage(socketChannel, message);
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    closeConection(socketChannel);
                    result.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendRequestMessage(AsynchronousSocketChannel socketChannel, Message message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getContent());
        socketChannel.write(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attatchment) {
                readSuccessorMessage(socketChannel);
            }

            @Override
            public void failed(Throwable exc, Void attatchment) {
                closeConection(socketChannel);
                exc.printStackTrace();
            }
        });
    }

    protected void readSuccessorMessage(AsynchronousSocketChannel socketChannel) {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        socketChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attatchment) {
                peer.getThreadPool().submit(() -> {
                    Message message = MessageHandler.handleMessage(buffer);
                    if (message.getType() == Message.TYPE_MSG.SUCCESSOR) {
                        successor = ((SuccessorMsg) message).getPeerSignature();
                        latch.countDown();
                        closeConection(socketChannel);
                    }
                });
            }

            @Override
            public void failed(Throwable exc, Void attatchment) {
                closeConection(socketChannel);
                exc.printStackTrace();
            }
        });
    }

    public PeerSignature getSuccessor() {
        return successor;
    }

    private void closeConection(AsynchronousSocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}