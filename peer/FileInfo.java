package peer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

public class FileInfo {
    private Integer replication_degree;
    private String filename;
    private long file_size;
    private FileTime creation_date;
    private String path;

    public FileInfo(String path, int replication_degree) {
        File file = new File(path);
        this.path = path;
        
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);    
            this.file_size = attributes.size();
            this.creation_date = attributes.creationTime();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.filename = file.getName();
        this.replication_degree = replication_degree;
    }

    public FileInfo(String path) {
        File file = new File(path);
        this.path = path;
        
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);    
            this.file_size = attributes.size();
            this.creation_date = attributes.creationTime();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.filename = file.getName();
        this.replication_degree = null;
    }

    public String getFilename() {
        return filename;
    }

    public int getReplicationDegree() {
        return replication_degree;
    }

    public long getFileSize() {
        return file_size;
    }

    public FileTime getCreationTime() {
        return creation_date;
    }

    public String getPath() {
        return path;
    }
}
