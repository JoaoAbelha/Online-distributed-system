package peer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import messages.BackupConfirmMsg;
import messages.BackupContentMsg;
import messages.BackupNACK;
import messages.Message;
import messages.Message.TYPE_MSG;
import sslengine.SSLUtils;

public class SendBackupContent implements Runnable {
    private PeerSignature signature;
    private AsynchronousSocketChannel channel;
    private CountDownLatch latch;
    private BigInteger replica_id;
    private BigInteger file_id;
    private String path;
    private Long file_size;
    public Message result;

    public SendBackupContent(BigInteger replica_id, BigInteger file_id, Long file_size, String path, PeerSignature signature, CountDownLatch latch) {
        this.signature = signature;
        this.file_id = file_id;
        this.file_size = file_size;
        this.path = path;
        this.latch = latch;
        this.replica_id = replica_id;
    }

    @Override
    public void run() {
        try {
            channel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(signature.getAddress(), signature.getPort());
            channel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    try {
                        Message message = new BackupContentMsg(replica_id, file_size, file_id);
                        FileSender sender = new FileSender(channel, path, file_size, message);
                        sender.init_tranfer(new CompletionHandler<Void,Void>() {
                            @Override
                            public void completed(Void arg0, Void arg1) {
                                readReceptionResponse();
                            }
    
                            @Override
                            public void failed(Throwable arg0, Void arg1) {
                                setResult(null);
                            }
                            
                        });
                    } catch (IOException e) {
                        setResult(null);
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                    SSLUtils.closeChannel(channel);
                    setResult(null);
                }
            });
        } catch (Exception e) {
            setResult(null);
            SSLUtils.closeChannel(channel);
        }

    }

    private void readReceptionResponse() {
        SSLUtils.readMessage(channel, new CompletionHandler<Void,Message>() {
            @Override
            public void completed(Void arg0, Message arg1) {
                if(arg1.getType() == TYPE_MSG.BACKUP_CONFIRM) {
                    setResult(arg1);
                    SSLUtils.closeChannel(channel);
                } else if (arg1.getType() == Message.TYPE_MSG.BACKUP_NACK) {
                    setResult(null);
                    SSLUtils.closeChannel(channel);
                }
            }

            @Override
            public void failed(Throwable arg0, Message arg1) {
                setResult(null);
            }
        });
    }
    
    public void setResult(Message message) {
        if(message == null) {
            result = new BackupNACK();
        } else result = message;

        latch.countDown();
    }
}