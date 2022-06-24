package peer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDeleter {

    private String path;

    public void deleteFile() throws IOException {
        Path path = Paths.get(this.path);
        Files.delete(path);
    }

    public FileDeleter(String path) {
        this.path = path;
    }
}