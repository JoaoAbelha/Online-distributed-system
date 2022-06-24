package peer;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * class to achieve some tolerance to denial-of-service
 */
public class SHA256Hasher {

    public static BigInteger hash(InetAddress inetAddress, int port) {

        String address = inetAddress.getHostAddress();
        MessageDigest md;

        try {
            // Create new SHA-1 digest
            md = MessageDigest.getInstance("SHA-256");

            // Hash address
            StringBuffer plainID = new StringBuffer(address).append(Integer.toString(port));
            byte[] addressBytes = md.digest(plainID.toString().getBytes());

            return new BigInteger(1, addressBytes);

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: SHA-256 DNE");
            e.printStackTrace();
        }

        return null;

    }

    public static BigInteger hash(FileInfo fileInfo, int replication_number) {
        MessageDigest md;

        try {
            // Create new SHA-1 digest
            md = MessageDigest.getInstance("SHA-256");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(fileInfo.getFilename()).append(fileInfo.getFileSize())
                    .append(fileInfo.getCreationTime()).append(replication_number);

            // Hash file
            byte[] addressBytes = md.digest(stringBuilder.toString().getBytes());
            return new BigInteger(1, addressBytes);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: SHA-256 DNE");
            e.printStackTrace();
        }

        return null;
    }

    public static BigInteger hash(FileInfo fileInfo) {
        MessageDigest md;

        try {
            // Create new SHA-1 digest
            md = MessageDigest.getInstance("SHA-256");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(fileInfo.getFilename()).append(fileInfo.getFileSize())
                    .append(fileInfo.getCreationTime());

            // Hash file
            byte[] addressBytes = md.digest(stringBuilder.toString().getBytes());
            return new BigInteger(1, addressBytes);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: SHA-256 DNE");
            e.printStackTrace();
        }

        return null;
    }

}