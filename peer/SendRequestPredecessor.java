package peer;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
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
import messages.PredecessorRequestMsg;
import messages.PredecessorResponseMsg;
import messages.SuccessorMsg;
import sslengine.SLLAsynchronousSocketChannel;
import sslengine.SSLUtils;

public class SendRequestPredecessor implements Runnable {

    private PeerSignature signature;
    private Peer peer;
    private CountDownLatch latch;
    private PeerSignature predecessor;

    SendRequestPredecessor(PeerSignature signature, Peer peer, CountDownLatch latch) {
        this.signature = signature;
        this.peer = peer;
        this.latch = latch;
    }

    @Override
    public void run() {
        Message message = new PredecessorRequestMsg(signature.getId());

        try {
            AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());

            socketChannel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    sendRequestMessage(socketChannel, message);
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    MyLogger.print("Successor Went Offline", Level.WARNING);
                    closeConection(socketChannel);
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
                readResponseMessage(socketChannel);
            }

            @Override
            public void failed(Throwable exc, Void attatchment) {
                MyLogger.print("Successor Went Offline", Level.WARNING);
                closeConection(socketChannel);
            }
        });
    }

    protected void readResponseMessage(AsynchronousSocketChannel socketChannel) {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        socketChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attatchment) {
                peer.getThreadPool().submit(() -> {
                    Message message = MessageHandler.handleMessage(buffer);
                    if (message.getType() == Message.TYPE_MSG.PREDECESSOR_RESPONSE) {
                        predecessor = ((PredecessorResponseMsg) message).getSender();
                        latch.countDown();
                        closeConection(socketChannel);
                    }
                });
            }

            @Override
            public void failed(Throwable exc, Void attatchment) {
                MyLogger.print("Successor Went Offline", Level.WARNING);
                closeConection(socketChannel);
            }
        });
    }

    public PeerSignature getPredecessor() {
        return predecessor;
    }

    private void closeConection(AsynchronousSocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}