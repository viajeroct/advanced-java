package info.kgeorgiy.ja.trofimov.walk;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShaFileVisitor extends SimpleFileVisitor<Path> {
    private final BufferedWriter bufferedWriter;

    ShaFileVisitor(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public final String zeroHash = "0".repeat(64);

    public String calculateHashFromStream(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int readSize;
        while ((readSize = inputStream.read(buffer)) != -1) {
            messageDigest.update(buffer, 0, readSize);
        }
        return String.format("%064x", new BigInteger(1, messageDigest.digest()));
    }

    public FileVisitResult calculateHash(Path file) throws IOException {
        String hash = zeroHash;
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            hash = calculateHashFromStream(inputStream);
        } catch (NoSuchAlgorithmException | IOException ignored) {
        } finally {
            writeHash(hash, file.toString());
        }
        return FileVisitResult.CONTINUE;
    }

    public void writeHash(String hash, String fileName) throws IOException {
        bufferedWriter.write(hash + " " + fileName);
        bufferedWriter.newLine();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        return calculateHash(file);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        writeHash(zeroHash, file.toString());
        return FileVisitResult.CONTINUE;
    }
}
