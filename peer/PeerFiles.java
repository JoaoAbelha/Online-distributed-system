package peer;

import java.util.concurrent.ConcurrentHashMap;

public class PeerFiles {
    private ConcurrentHashMap<String, FileInfo> requested_backup;

    public PeerFiles() {
        requested_backup = new ConcurrentHashMap<>();
    }

    public boolean addBackupRequest(FileInfo fileInfo) {
        if (hasBackupRequest(fileInfo.getFilename())) {
            return false;
        }

        requested_backup.put(fileInfo.getFilename(), fileInfo);
        return true;
    }

    public boolean hasBackupRequest(String filename) {
        return requested_backup.keySet().contains(filename);
    }

    public FileInfo getFile(String filename) {
        if (hasBackupRequest(filename)) {
            return this.requested_backup.get(filename);
        }

        return null;
    }

    public void deleteBackupRequest(String filename) {
        if (getFile(filename) != null)
            requested_backup.remove(filename);
    }

}