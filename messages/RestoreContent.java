package messages;

import java.nio.channels.AsynchronousSocketChannel;

import peer.Peer;

public class RestoreContent extends Message {
    private Long file_size;
    public RestoreContent(Long file_size) {
        super(TYPE_MSG.RESTORE_CONTENT);
        this.file_size = file_size;
    }

    @Override
    public byte[] getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type).append(CRLF).append(file_size).append(CRLF).append(CRLF);

        return stringBuilder.toString().getBytes();
    }

    @Override
    public void handle(Peer receiver, AsynchronousSocketChannel channel) {

    }

    public Long getFileSize() {
        return file_size;
    }
    
}