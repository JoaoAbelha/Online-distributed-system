package peer;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class PeerInformation {
    private static final int MAX_DISK_SPACE = 10000000;
    private int max_disk_space;
    private int current_occupied_space;
    private ConcurrentHashMap<BigInteger, ReplicaInfo> backup_files;
    private ConcurrentHashMap<BigInteger, PeerSignature> remote_replicas;

    public PeerInformation() {
        max_disk_space = MAX_DISK_SPACE;
        current_occupied_space = 0;
        backup_files = new ConcurrentHashMap<>();
        remote_replicas = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<BigInteger, ReplicaInfo> getBackupFiles() {
        return backup_files;
    }

    public synchronized void setMaxSpace(int space) {
        this.max_disk_space = space;
    }

    public synchronized boolean isOversize() {
        return current_occupied_space > max_disk_space * 1000;
    }

    public synchronized boolean canStore(long size) {
        return current_occupied_space + size <= max_disk_space * 1000;
    }

    public boolean hasFile(BigInteger file_id) {
        return backup_files.keySet().contains(file_id);
    }

    public synchronized void addReplica(BigInteger file_id, ReplicaInfo replicaInfo) {
        backup_files.put(file_id, replicaInfo);
        current_occupied_space += replicaInfo.getFileSize();
    }

    public boolean hasReplica(BigInteger file_id, BigInteger replicaID) {
        if (this.hasFile(file_id)) {
            return this.backup_files.get(file_id).getHash().equals(replicaID);
        }
        return false;
    }

    public ReplicaInfo getReplicaInfo(BigInteger file_id) {
        return backup_files.get(file_id);
    }

    public synchronized void deleteReplica(BigInteger fileID) {
        if (this.hasFile(fileID)) {
            int file_size = (int) this.backup_files.get(fileID).getFileSize();
            current_occupied_space -= file_size;
            this.backup_files.remove(fileID);
        }
    }

    public void addRemoteReplica(BigInteger replica_id, PeerSignature signature) {
        remote_replicas.put(replica_id, signature);
    }

    public boolean hasRemoteReplica(BigInteger replica_id) {
        return remote_replicas.keySet().contains(replica_id);
    }

    public PeerSignature getRemoteReplica(BigInteger replica_id) {
        return remote_replicas.get(replica_id);
    }

    public void removeBackup(BigInteger file_id) {
        this.backup_files.remove(file_id);
    }

    public void removeRemoteReplica(BigInteger replicaID) {
        if(hasRemoteReplica(replicaID)) {
            this.remote_replicas.remove(replicaID);
        }
	}

	public ConcurrentHashMap<BigInteger,PeerSignature> getRemoteReplicas() {
		return this.remote_replicas;
	}


    @Override
    public String toString() {
        String state = "Backed Up Files:\n";
        for (ReplicaInfo replica : this.backup_files.values()) {
            state += "\t" + replica.getFileId() + "\n";
        }

        state += "Replica Locations:\n";
        for (PeerSignature peer : this.remote_replicas.values()) {
            state += "\t" + peer.getId() + "\n";
        }

        state += "\n Occupied Space (B): " + this.current_occupied_space;
        state += "\n Maximum Space (B): " + this.max_disk_space;

        return state;
    }

}