package peer;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

import handlers.LastReplyCompletionHandler;
import messages.DeleteRequestMsg;
import messages.Message;
import messages.Message.TYPE_MSG;
import sslengine.SSLUtils;

public class SendDelete implements Runnable {
    private Peer peer;
    private PeerSignature replica_location;
    private BigInteger replica_id;
    private FileInfo info;
    private AsynchronousSocketChannel channel;
    private AsynchronousServerSocketChannel serverChannel;
    private CountDownLatch latch;

    SendDelete(BigInteger replica_id, Peer peer, FileInfo info, CountDownLatch latch) {
        this.peer = peer;
        this.replica_location = peer.findSuccessor(replica_id);
        this.replica_id = replica_id;
        this.info = info;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            channel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(replica_location.getAddress(), replica_location.getPort());
            channel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    try {
                        serverChannel = SSLUtils.getServerSocketChannel(peer.getGroup());
                        InetSocketAddress socket = new InetSocketAddress(peer.getAddress(), 0);
                        serverChannel.bind(socket);
                        PeerSignature signature = new PeerSignature((InetSocketAddress) serverChannel.getLocalAddress());
                        DeleteRequestMsg message = new DeleteRequestMsg(replica_id, SHA256Hasher.hash(info), signature);
                        readDeleteResponse();
                        sendDeleteMessage(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    SSLUtils.closeChannel(channel);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDeleteMessage(Message message) {
        SSLUtils.sendMessage(message, channel, null, new LastReplyCompletionHandler(channel));
    }

    private void readDeleteResponse() {
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            public void completed(AsynchronousSocketChannel achannel, Void attachment) {
                SSLUtils.readMessage(achannel, new CompletionHandler<Void,Message>() {
                    @Override
                    public void completed(Void result, Message att) {
                        if(att.getType() == TYPE_MSG.DELETED_ACK) {
                            latch.countDown();
                            close();
                        }
                    }
        
                    @Override
                    public void failed(Throwable exc, Message att) {
                    }
                });
            }

            public void failed(Throwable exc, Void attachment) {
                close();
            }
        });
    }

    public void close() {
        SSLUtils.closeChannel(serverChannel);
    }
    
}