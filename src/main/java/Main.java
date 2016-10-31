import net.rubygrapefruit.platform.Native;
import net.rubygrapefruit.platform.PosixFileInfo;
import net.rubygrapefruit.platform.PosixFiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

public class Main {
    public static void main(String[] args) throws IOException {
        File testDir = new File("build/work").getCanonicalFile();
        File dir = new File(testDir, "test-dir");
        dir.mkdirs();
        File file = new File(dir, "test-file");
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("content");
        }
        File missing = new File(dir, "missing");

        FileBackedStat fileStat = new FileBackedStat();
        NioBackedStat nioStat = new NioBackedStat();
        NativeBackedStat nativeStat = new NativeBackedStat();

        int warmup = 20000;
        measure(nativeStat, warmup, dir, file, missing);
        measure(fileStat, warmup, dir, file, missing);
        measure(nioStat, warmup, dir.toPath(), file.toPath(), missing.toPath());

        int test1 = 100000;
        measure(nativeStat, test1, dir, file, missing);
        measure(fileStat, test1, dir, file, missing);
        measure(nioStat, test1, dir.toPath(), file.toPath(), missing.toPath());
    }

    static <T> void measure(Stat<T> stat, int count, T... files) throws IOException {
        System.out.println("stat using " + stat);
        long start = System.nanoTime();
        for(int i = 0; i < count;i++) {
            for (T file : files) {
                stat.stat(file);
            }
        }
        long end = System.nanoTime();
        System.out.println(String.format("time: %12sns", (end - start)));
    }

    abstract static class Stat<T> {
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

        abstract FileDetails stat(T file) throws IOException;
    }

    static class FileDetails {
        enum Type {
            Missing, Dir, File
        }

        final Type type;
        final long size;
        final long lastModified;

        public FileDetails(Type type, long size, long lastModified) {
            this.type = type;
            this.size = size;
            this.lastModified = lastModified;
        }

        @Override
        public String toString() {
            return "type: " + type + ", length: " + size + ", modified: " + lastModified;
        }
    }

    private static class NativeBackedStat extends Stat<File> {

        private final PosixFiles posixFiles;

        public NativeBackedStat() {
            posixFiles = Native.get(PosixFiles.class);
        }

        @Override
        public FileDetails stat(File file) throws IOException {
            PosixFileInfo fileInfo = posixFiles.stat(file);
            switch (fileInfo.getType()) {
                case Missing:
                    return new FileDetails(FileDetails.Type.Missing, 0, 0);
                case Directory:
                    return new FileDetails(FileDetails.Type.Dir, 0, 0);
                case File:
                    return new FileDetails(FileDetails.Type.File, fileInfo.getSize(), fileInfo.getLastModifiedTime());
                default:
                    throw new IllegalArgumentException("Unsupported type");
            }
        }
    }

    private static class NioBackedStat extends Stat<Path> {
        @Override
        public FileDetails stat(Path file) throws IOException {
            BasicFileAttributes view;
            try {
                view = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();
            } catch (NoSuchFileException e) {
                return new FileDetails(FileDetails.Type.Missing, 0, 0);
            }
            if (view.isDirectory()) {
                return new FileDetails(FileDetails.Type.Dir, 0, 0);
            }
            return new FileDetails(FileDetails.Type.File, view.size(), view.lastModifiedTime().toMillis());
        }
    }

    private static class FileBackedStat extends Stat<File> {
        public FileDetails stat(File file) {
            if (!file.exists()) {
                return new FileDetails(FileDetails.Type.Missing, 0, 0);
            }
            if (file.isDirectory()) {
                return new FileDetails(FileDetails.Type.Dir, 0, 0);
            }
            return new FileDetails(FileDetails.Type.File, file.length(), file.lastModified());
        }
    }
}
