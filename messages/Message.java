package messages;

import java.nio.channels.AsynchronousSocketChannel;

import peer.Peer;
import peer.PeerSignature;

public abstract class Message {
    public final static String CRLF = "\r\n";

    public enum TYPE_MSG {
        PREDECESSOR, NOTIFICATION, BACKUP_REQUEST, BACKUP_ACK, BACKUP_NACK, BACKUP_CONTENT, BACKUP_SUCCESSOR, BACKUP_CONFIRM, SUCCESSOR_REQUEST,
        SUCCESSOR, PREDECESSOR_RESPONSE, PREDECESSOR_REQUEST, PREDECESSOR_ALIVE, DELETE_REQUEST, DELETED_ACK,
        RESTORE_REQUEST, RESTORE_CONTENT, REDISTRIBUTE_REPLICA
    }

    protected TYPE_MSG type;

    public Message(TYPE_MSG type) {
        this.type = type;
    };

    public TYPE_MSG getType() {
        return type;
    }

    public abstract byte[] getContent();

    public abstract void handle(Peer receiver, AsynchronousSocketChannel channel);
}