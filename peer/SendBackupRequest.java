package peer;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

import messages.BackupNACK;
import messages.BackupRequestMsg;
import messages.Message;
import sslengine.SSLUtils;

public class SendBackupRequest implements Runnable {
    private PeerSignature replica_location;
    private BigInteger replica_id;
    private BigInteger file_id;
    private Long file_size;
    private AsynchronousSocketChannel channel;
    private CountDownLatch latch;
    private Message result;

    SendBackupRequest(BigInteger replica_id, BigInteger file_id, Long file_size, Peer peer, CountDownLatch latch) {
        this.file_id = file_id;
        this.file_size = file_size;
        this.replica_location = peer.findSuccessor(replica_id);
        this.replica_id = replica_id;
        this.latch = latch;
    }

    @Override
    public void run() {
        Message message = new BackupRequestMsg(replica_id, file_size, file_id);
        try {
            channel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(replica_location.getAddress(),
                    replica_location.getPort());
            channel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    sendRequestMessage(message);
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    setResult(null);
                    SSLUtils.closeChannel(channel);
                }
            });
        } catch (Exception e) {
            SSLUtils.closeChannel(channel);
            setResult(null);
        }
    }

    private void sendRequestMessage(Message message) {
        SSLUtils.sendMessage(message, channel, null, new CompletionHandler<Void, Void>() {
            @Override
            public void completed(Void result, Void attachment) {
                readBackupRequestResponse();
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                setResult(null);
            }
        });
    }

    private void readBackupRequestResponse() {
        SSLUtils.readMessage(channel, new CompletionHandler<Void, Message>() {
            @Override
            public void completed(Void result, Message attachment) {
                setResult(attachment);
                SSLUtils.closeChannel(channel);
            }

            @Override
            public void failed(Throwable exc, Message attachment) {
                setResult(null);
            }
        });
    }

    public void setResult(Message message) {
        if (message == null) {
            result = new BackupNACK();
        } else
            result = message;

        latch.countDown();
    }

    public Message getResult() {
        return result;
    }
}