package peer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import messages.Message;
import messages.MessageHandler;
import messages.PredecessorMsg;
import sslengine.SSLUtils;

/**
 * Check the predecessor peer
 */
public class CheckPredecessor implements Runnable {

    private Peer peerInitiator;
    private AsynchronousSocketChannel socketChannel;

    public CheckPredecessor(Peer initiator) {
        this.peerInitiator = initiator;
    }

    private boolean checkValidPredecessor() {
        return this.peerInitiator.getPredecessor() != null
                && !this.peerInitiator.getPredecessor().getId().equals(this.peerInitiator.getId());
    }

    @Override
    public synchronized void run() {
        if (!checkValidPredecessor()) {
            return;
        }

        PeerSignature signature = this.peerInitiator.getPredecessor();

        Message message = new PredecessorMsg();
        PeerSignature h = this.peerInitiator.getPredecessor();

        try {
            this.socketChannel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());
            socketChannel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    sendPredecessorMessage(socketChannel, message);
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    closeConection(socketChannel);
                    timeout();
                }
            });
        } catch (Exception e) {

        }
    }

    private void sendPredecessorMessage(AsynchronousSocketChannel socketChannel, Message message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getContent());
        socketChannel.write(buffer, 5000, TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attatchment) {
                readSuccessorMessage(socketChannel);
            }

            @Override
            public void failed(Throwable exc, Void attatchment) {
                closeConection(socketChannel);
                timeout();
            }
        });
    }

    protected void readSuccessorMessage(AsynchronousSocketChannel socketChannel) {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        socketChannel.read(buffer, 5000, TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attatchment) {
                peerInitiator.getThreadPool().submit(() -> {
                    Message message = MessageHandler.handleMessage(buffer);

                    if (message.getType() == Message.TYPE_MSG.PREDECESSOR_ALIVE) {
                        closeConection(socketChannel);
                    }
                });
            }

            @Override
            public void failed(Throwable exc, Void attatchment) {
                closeConection(socketChannel);
                timeout();
                exc.printStackTrace();
            }
        });
    }

    private void closeConection(AsynchronousSocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void timeout() {
        if (this.peerInitiator.getPredecessor() == null)
            return;

        if (this.peerInitiator.getPredecessor().getId().equals(this.peerInitiator.getSuccessor().getId())) {
            this.peerInitiator.setSuccessor(null);
        }
        this.peerInitiator.setPredecessor(null);
    }
}