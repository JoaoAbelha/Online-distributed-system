package messages;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import peer.MyLogger;
import peer.PeerSignature;

public class MessageHandler {

  public static Message handleMessage(ByteBuffer buffer) {
    String message = new String(buffer.array());

    String[] argumentsArray = message.split("\\s+");
    String type = argumentsArray[0];

    switch (type) {
      case "SUCCESSOR_REQUEST":
        SuccessorRequestMsg msg = new SuccessorRequestMsg(new BigInteger(argumentsArray[1], 16));
        return msg;
      case "PREDECESSOR":
        PredecessorMsg pre = new PredecessorMsg();
        return pre;
      case "PREDECESSOR_ALIVE":
        PredecessorResponse prer = new PredecessorResponse();
        return prer;
      case "SUCCESSOR":
        SuccessorMsg suc = new SuccessorMsg(new PeerSignature(argumentsArray[1], argumentsArray[2]));
        return suc;
      case "NOTIFICATION":
        Notification not = new Notification(new PeerSignature(argumentsArray[1], argumentsArray[2]));
        return not;
      case "PREDECESSOR_REQUEST":
        PredecessorRequestMsg pred = new PredecessorRequestMsg(new BigInteger(argumentsArray[1], 16));
        return pred;
      case "PREDECESSOR_RESPONSE":
        PredecessorResponseMsg predR = null;
        if (argumentsArray[1].equals("null")) {
          predR = new PredecessorResponseMsg(null);
        } else {
          predR = new PredecessorResponseMsg(new PeerSignature(argumentsArray[1], argumentsArray[2]));
        }
        return predR;
      case "BACKUP_REQUEST":
        BackupRequestMsg backupRequestMsg = new BackupRequestMsg(new BigInteger(argumentsArray[1], 16),
            Long.parseLong(argumentsArray[3]), new BigInteger(argumentsArray[2], 16));
        return backupRequestMsg;
      case "BACKUP_CONTENT":
        BackupContentMsg backupContentMsg = new BackupContentMsg(new BigInteger(argumentsArray[1], 16),
            Long.parseLong(argumentsArray[3]), new BigInteger(argumentsArray[2], 16));
        return backupContentMsg;
      case "BACKUP_SUCCESSOR":
        BackupSuccessorMsg backupSuccessorMsg = new BackupSuccessorMsg(new BigInteger(argumentsArray[4], 16),
            Long.parseLong(argumentsArray[6]), new BigInteger(argumentsArray[5], 16),
            new PeerSignature(argumentsArray[1], argumentsArray[2]), new BigInteger(argumentsArray[3], 16));
        return backupSuccessorMsg;
      case "BACKUP_ACK":
        BackupACK backupACK = new BackupACK(new PeerSignature(argumentsArray[1], argumentsArray[2]));
        return backupACK;
      case "BACKUP_CONFIRM":
        BackupConfirmMsg backupcConfirmMsg = new BackupConfirmMsg(
            new PeerSignature(argumentsArray[1], argumentsArray[2]));
        return backupcConfirmMsg;
      case "BACKUP_NACK":
        BackupNACK backupNACK = new BackupNACK();
        return backupNACK;
      case "DELETE_REQUEST":
        BigInteger replicaID = new BigInteger(argumentsArray[3], 16);
        BigInteger fileID = new BigInteger(argumentsArray[4], 16);
        DeleteRequestMsg deleteRequest = new DeleteRequestMsg(replicaID, fileID,
            new PeerSignature(argumentsArray[1], argumentsArray[2]));
        return deleteRequest;
      case "DELETED_ACK":
        DeletedACK ack = new DeletedACK(new BigInteger(argumentsArray[1], 16));
        return ack;
      case "RESTORE_REQUEST":
        RestoreRequest request = new RestoreRequest(new BigInteger(argumentsArray[3], 16),
            new BigInteger(argumentsArray[4], 16), new PeerSignature(argumentsArray[1], argumentsArray[2]));
        return request;
      case "RESTORE_CONTENT":
        RestoreContent restoreContent = new RestoreContent(Long.parseLong(argumentsArray[1]));
        return restoreContent;
      case "REDISTRIBUTE_REPLICA":
        RedistributeReplica redistributeReplica = new RedistributeReplica(new BigInteger(argumentsArray[1], 16), new PeerSignature(argumentsArray[2], argumentsArray[3]));
        return redistributeReplica;
      default:
        MyLogger.print("Unknown message of type " + type, Level.WARNING);
        break;
    }

    return null;
  }
}