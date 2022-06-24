package peer;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

import handlers.LastReplyCompletionHandler;
import messages.BackupNACK;
import messages.BackupSuccessorMsg;
import messages.Message;
import sslengine.SSLUtils;

public class SendBackupSucessor implements Runnable {
    private Peer peer;
    private CountDownLatch latch;
    private AsynchronousSocketChannel channel;
    private AsynchronousServerSocketChannel serverChannel;
    private BigInteger file_id;
    private BigInteger replica_id;
    private long file_size;
    private Message result = null;

    public SendBackupSucessor(Peer peer, CountDownLatch latch, BigInteger file_id, BigInteger replica_id,
            long file_size) {
        this.peer = peer;
        this.latch = latch;
        this.file_id = file_id;
        this.replica_id = replica_id;
        this.file_size = file_size;
    }

    @Override
    public void run() {
        try {
            channel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(peer.getSuccessor().getAddress(), peer.getSuccessor().getPort());
            channel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    try {
                        serverChannel = SSLUtils.getServerSocketChannel(peer.getGroup());
                        InetSocketAddress socket = new InetSocketAddress(peer.getAddress(), 0);
                        serverChannel.bind(socket);
                        PeerSignature signature = new PeerSignature((InetSocketAddress) serverChannel.getLocalAddress());
                        Message message = new BackupSuccessorMsg(replica_id, file_size, file_id, signature, peer.getId());
                        readBackupResponse();
                        sendRequestMessage(message);
                    } catch (Exception e) {
                        setResult(null);
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    setResult(null);
                }
            });
        } catch (Exception e) {
            setResult(null);
            e.printStackTrace();
        }

    }

    private void sendRequestMessage(Message message) {
        SSLUtils.sendMessage(message, channel, null, new LastReplyCompletionHandler(channel));
    }

    private void readBackupResponse() {
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            public void completed(AsynchronousSocketChannel achannel, Void attachment) {
                SSLUtils.readMessage(achannel, new CompletionHandler<Void,Message>() {
                    @Override
                    public void completed(Void result, Message att) {
                        setResult(att);
                    }
        
                    @Override
                    public void failed(Throwable exc, Message att) {
                        setResult(null);
                    }
                });
            }

            public void failed(Throwable exc, Void attachment) {
                setResult(null);
            }
        });
    }

    private void setResult(Message message) {
        if(message == null) {
            result = new BackupNACK();
        } else result = message;

        latch.countDown();
        close();
    }

    public Message getResult() {
		return result;
	}

	public void close() {
        SSLUtils.closeChannel(serverChannel);
	}
}