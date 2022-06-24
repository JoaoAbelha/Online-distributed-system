package peer;

import java.util.concurrent.CountDownLatch;

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
import messages.Notification;
import messages.PredecessorRequestMsg;
import messages.PredecessorResponseMsg;
import messages.SuccessorMsg;
import sslengine.SLLAsynchronousSocketChannel;
import sslengine.SSLUtils;

public class SendNotification implements Runnable {

    private PeerSignature signature;
    private Peer peer;

    SendNotification(PeerSignature signature, Peer peer) {
        this.signature = signature;
        this.peer = peer;
    }

    @Override
    public void run() {

        Message notification = new Notification(peer.getPeerSignature());

        try {
            AsynchronousSocketChannel socketChannel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());
            socketChannel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    sendMessage(socketChannel, notification);
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    peer.setSuccessor(null);
                    closeConection(socketChannel);
                    result.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(AsynchronousSocketChannel socketChannel, Message message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getContent());
        socketChannel.write(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attatchment) {

            }

            @Override
            public void failed(Throwable exc, Void attatchment) {
                peer.setSuccessor(null);
                closeConection(socketChannel);
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

}