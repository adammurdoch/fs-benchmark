import net.rubygrapefruit.platform.DirEntry;
import net.rubygrapefruit.platform.Native;
import net.rubygrapefruit.platform.PosixFileInfo;
import net.rubygrapefruit.platform.PosixFiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        File testDir = new File("build/work").getCanonicalFile();
        File dir = new File(testDir, "test-dir");
        createTree(dir, 2);
        File file = new File(dir, "test-file");
        createFile(file);
        File missing = new File(dir, "missing");

        FileBackedStat fileStat = new FileBackedStat();
        NioBackedStat nioStat = new NioBackedStat();
        NativeBackedStat nativeStat = new NativeBackedStat();

        int warmup = 1000;
        System.out.println();
        System.out.println("stat warmup: " + warmup + " iterations");
        measureStat(nativeStat, warmup, dir, file, missing);
        measureStat(fileStat, warmup, dir, file, missing);
        measureStat(nioStat, warmup, dir.toPath(), file.toPath(), missing.toPath());

        System.out.println();
        System.out.println("stat warmup: " + warmup + " iterations");
        measureStat(nativeStat, warmup, dir, file, missing);
        measureStat(fileStat, warmup, dir, file, missing);
        measureStat(nioStat, warmup, dir.toPath(), file.toPath(), missing.toPath());

        System.out.println();
        System.out.println("stat warmup: " + warmup + " iterations");
        measureStat(nativeStat, warmup, dir, file, missing);
        measureStat(fileStat, warmup, dir, file, missing);
        measureStat(nioStat, warmup, dir.toPath(), file.toPath(), missing.toPath());

        int test1 = 2000;
        System.out.println();
        System.out.println("stat test 1: " + test1 + " iterations");
        measureStat(nativeStat, test1, dir, file, missing);
        measureStat(fileStat, test1, dir, file, missing);
        measureStat(nioStat, test1, dir.toPath(), file.toPath(), missing.toPath());

        System.out.println();
        System.out.println("stat test 2: " + test1 + " iterations");
        measureStat(nativeStat, test1, dir, file, missing);
        measureStat(fileStat, test1, dir, file, missing);
        measureStat(nioStat, test1, dir.toPath(), file.toPath(), missing.toPath());

        int test2 = 500000;
        System.out.println();
        System.out.println("stat test 3: " + test2 + " iterations");
        measureStat(nativeStat, test2, dir, file, missing);
        measureStat(fileStat, test2, dir, file, missing);
        measureStat(nioStat, test2, dir.toPath(), file.toPath(), missing.toPath());

        System.out.println();
        System.out.println("stat test 4: " + test2 + " iterations");
        measureStat(nativeStat, test2, dir, file, missing);
        measureStat(fileStat, test2, dir, file, missing);
        measureStat(nioStat, test2, dir.toPath(), file.toPath(), missing.toPath());

        warmup = 500;
        System.out.println();
        System.out.println("walk warmup: " + warmup + " iterations");
        measureWalk(nativeStat, warmup, dir);
        measureWalk(fileStat, warmup, dir);
        measureWalk(nioStat, warmup, dir.toPath());

        System.out.println();
        System.out.println("walk warmup: " + warmup + " iterations");
        measureWalk(nativeStat, warmup, dir);
        measureWalk(fileStat, warmup, dir);
        measureWalk(nioStat, warmup, dir.toPath());

        System.out.println();
        System.out.println("walk warmup: " + warmup + " iterations");
        measureWalk(nativeStat, warmup, dir);
        measureWalk(fileStat, warmup, dir);
        measureWalk(nioStat, warmup, dir.toPath());

        test1 = 2000;
        System.out.println();
        System.out.println("walk test 1: " + test1 + " iterations");
        measureWalk(nativeStat, test1, dir);
        measureWalk(fileStat, test1, dir);
        measureWalk(nioStat, test1, dir.toPath());

        System.out.println();
        System.out.println("walk test 2: " + test1 + " iterations");
        measureWalk(nativeStat, test1, dir);
        measureWalk(fileStat, test1, dir);
        measureWalk(nioStat, test1, dir.toPath());

        test1 = 5000;
        System.out.println();
        System.out.println("walk test 3: " + test1 + " iterations");
        measureWalk(nativeStat, test1, dir);
        measureWalk(fileStat, test1, dir);
        measureWalk(nioStat, test1, dir.toPath());

        System.out.println();
        System.out.println("walk test 4: " + test1 + " iterations");
        measureWalk(nativeStat, test1, dir);
        measureWalk(fileStat, test1, dir);
        measureWalk(nioStat, test1, dir.toPath());
    }

    private static void createFile(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("content");
        }
    }

    private static void createTree(File dir, int depth) throws IOException {
        dir.mkdirs();
        if (depth > 0) {
            for (int i = 0; i < 5; i++) {
                File childDir = new File(dir, "dir-" + i);
                createTree(childDir, depth - 1);
            }
        }
        for (int i = 0; i < 5; i++) {
            createFile(new File(dir, "file-" + i));
        }
    }

    static <T> void measureStat(Stat<T> stat, int count, T... files) throws IOException {
        System.out.println("stat using " + stat);
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            for (T file : files) {
                stat.stat(file);
            }
        }
        long end = System.nanoTime();
        System.out.println(String.format("time: %12sns", (end - start)));
    }

    static <T> void measureWalk(Stat<T> stat, int count, T dir) throws IOException {
        System.out.println("walk using " + stat);
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            ArrayList<FileDetails> result = new ArrayList<>();
            stat.walk(dir, result);
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

        abstract void walk(T dir, List<FileDetails> details) throws IOException;
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

        @Override
        void walk(File dir, List<FileDetails> details) throws IOException {
            List<? extends DirEntry> entries = posixFiles.listDir(dir);
            for (DirEntry entry : entries) {
                switch (entry.getType()) {
                case Directory:
                    details.add(new FileDetails(FileDetails.Type.Dir, 0, 0));
                    walk(new File(dir, entry.getName()), details);
                    break;
                case File:
                    details.add(new FileDetails(FileDetails.Type.File, entry.getSize(), entry.getLastModifiedTime()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type");
                }
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

        @Override
        void walk(Path dir, final List<FileDetails> details) throws IOException {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    details.add(new FileDetails(FileDetails.Type.Dir, 0, 0));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    details.add(new FileDetails(FileDetails.Type.File, attrs.size(), attrs.lastModifiedTime().toMillis()));
                    return FileVisitResult.CONTINUE;
                }
            });
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

        @Override
        void walk(File dir, List<FileDetails> details) throws IOException {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    details.add(new FileDetails(FileDetails.Type.Dir, 0, 0));
                    walk(file, details);
                } else {
                    details.add(new FileDetails(FileDetails.Type.File, file.length(), file.lastModified()));
                }
            }
        }
    }
}
