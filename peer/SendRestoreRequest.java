package peer;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

import handlers.LastReplyCompletionHandler;
import messages.Message;
import messages.RestoreContent;
import messages.RestoreRequest;
import messages.Message.TYPE_MSG;
import sslengine.SSLUtils;

public class SendRestoreRequest implements Runnable {
    private BigInteger replica_id;
    private Restore restore;
    private AsynchronousSocketChannel channel;
    private AsynchronousServerSocketChannel serverChannel;
    private PeerSignature replica_location;
    private CountDownLatch latch;

    public SendRestoreRequest(BigInteger replica_id, Restore restore, CountDownLatch latch) {
        this.replica_id = replica_id;
        this.restore = restore;
        this.replica_location = this.restore.getPeer().findSuccessor(replica_id);
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            channel = SSLUtils.getSocketChannel();
            InetSocketAddress address = new InetSocketAddress(replica_location.getAddress(),
                    replica_location.getPort());
            channel.connect(address, null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attatchment) {
                    try {
                        serverChannel = SSLUtils.getServerSocketChannel(restore.getPeer().getGroup());
                        InetSocketAddress socket = new InetSocketAddress(restore.getPeer().getAddress(), 0);
                        serverChannel.bind(socket);
                        PeerSignature signature = new PeerSignature((InetSocketAddress) serverChannel.getLocalAddress());
                        Message message = new RestoreRequest(replica_id, SHA256Hasher.hash(restore.getFileInfo()), signature);
                        readRestoreResponse();
                        sendRequestMessage(message);
                    } catch (Exception e) {
                    }
                }

                @Override
                public void failed(Throwable result, Void attatchment) {
                }
            });
        } catch (Exception e) {
        }
    }

    private void readRestoreResponse() {
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            public void completed(AsynchronousSocketChannel achannel, Void attachment) {
                SSLUtils.readMessage(achannel, new CompletionHandler<Void,Message>() {
                    @Override
                    public void completed(Void result, Message att) {
                        if(att.getType() == TYPE_MSG.RESTORE_CONTENT) {
                            try {
                                FileReceiver fileReceiver = new FileReceiver(achannel, restore.getFileInfo().getFilename(), ((RestoreContent)att).getFileSize(), restore.getPeer(), "restore");
                                fileReceiver.readFile(new CompletionHandler<Void,Void>() {
                                    @Override
                                    public void completed(Void arg0, Void arg1) {
                                        latch.countDown();
                                        SSLUtils.closeChannel(achannel);
                                        close();
                                    }
            
                                    @Override
                                    public void failed(Throwable arg0, Void arg1) {
                                        restoreFail(restore.getPeer());
                                        SSLUtils.closeChannel(achannel);
                                        close();
                                    }
                                });
                            } catch (IOException e) {
                                restoreFail(restore.getPeer());
                                SSLUtils.closeChannel(achannel);
                                close();
                            }
                        }
                    }
        
                    @Override
                    public void failed(Throwable exc, Message att) {
                        restoreFail(restore.getPeer());
                        SSLUtils.closeChannel(achannel);
                        close();
                    }
                });
            }

            public void failed(Throwable exc, Void attachment) {
                close();
            }
        });
    }

    private void sendRequestMessage(Message message) {
        SSLUtils.sendMessage(message, channel, null, new LastReplyCompletionHandler(channel));
    }

	public void close() {
        SSLUtils.closeChannel(serverChannel);
    }
    
    private void restoreFail(Peer peer) {
        try {
            FileDeleter fileDeleter = new FileDeleter(peer.getPeerRootFolder() + File.separator + "restore" + File.separator + restore.getFileInfo().getFilename());
            fileDeleter.deleteFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
