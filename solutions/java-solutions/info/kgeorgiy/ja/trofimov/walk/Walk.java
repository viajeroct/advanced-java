package info.kgeorgiy.ja.trofimov.walk;

public class Walk {
    public static void main(String[] args) {
        RecursiveWalk.walk(args, (path, shaFileVisitor) -> shaFileVisitor.calculateHash(path));
    }
}
