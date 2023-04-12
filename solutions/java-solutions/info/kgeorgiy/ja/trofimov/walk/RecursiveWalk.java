package info.kgeorgiy.ja.trofimov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    interface WalkOperation {
        void walk(Path path, ShaFileVisitor shaFileVisitor) throws IOException;
    }

    public static void walk(String[] args, WalkOperation walkOperation) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Usage: java Walk <input file> <output file>");
            return;
        }

        Path input, output;
        try {
            input = Paths.get(args[0]);
            output = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Invalid input or output file path: " + e.getMessage());
            return;
        }

        if (output.getParent() != null) {
            try {
                Files.createDirectories(output.getParent());
            } catch (IOException e) {
                System.err.println("Can't create a dir for output file: " + e.getMessage());
                return;
            }
        }

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)
        ) {
            ShaFileVisitor shaFileVisitor = new ShaFileVisitor(writer);

            for (String fileName = reader.readLine(); fileName != null; fileName = reader.readLine()) {
                try {
                    Path path = Paths.get(fileName);
                    walkOperation.walk(path, shaFileVisitor);
                } catch (IOException | InvalidPathException ignore) {
                    shaFileVisitor.writeHash(shaFileVisitor.zeroHash, fileName);
                }
            }
        } catch (IOException | SecurityException e) {
            System.err.println("Can't open input or output file or read from input file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        walk(args, Files::walkFileTree);
    }
}
