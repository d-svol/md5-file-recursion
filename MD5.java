import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MD5 {
    private String fileDir;
    private int treadsAmount;

    public MD5(String fileDir, int treadsAmount) {
        this.fileDir = fileDir;
        this.treadsAmount = treadsAmount;
    }

    public List<String> getListFromPath() {
        Path path = Paths.get(fileDir);
        try (Stream<Path> walk = Files.walk(path)) {
            return walk
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Could not read files for path " + path);
        }
    }

    public String getMd5(String fileAbsolutePath) throws NoSuchAlgorithmException, IOException {
        int nRead = 0;
        byte[] buffer = new byte[1024 * 1024];
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        try (InputStream in = new FileInputStream(fileAbsolutePath)) {
            while ((nRead = in.read(buffer)) != -1) {
                md5.update(buffer, 0, nRead);
            }
        }
        return new BigInteger(1, md5.digest()).toString(16);
    }

    public  void printMd5DirTree() throws InterruptedException {
        List<String> files = getListFromPath();
        ExecutorService executorService = Executors.newFixedThreadPool(treadsAmount);
        //sleep 2 day
        CountDownLatch latch = new CountDownLatch(files.size());

        for (String file : files) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        String out = "File directory: " + file + " ===>>>  " + getMd5(file);
                        System.out.println(out);
                    } catch (NoSuchAlgorithmException | IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        //list out
        latch.await();
        executorService.shutdownNow();

    }

    private class Lock {
        private int total;
        private final Object internalLock = new Object();

        public Lock(int total) {
            if (total < 0) {
                throw new IllegalArgumentException("Total < 0");
            }
            this.total = total;
        }

        public void decrement() {
            synchronized (internalLock) {
                if (total > 0) {
                    total--;
                }
                if (total == 0){
                    internalLock.notifyAll();
                }
            }
        }

        public void waitZero() throws InterruptedException {
            synchronized (internalLock) {
                while (total > 0){
                    internalLock.wait();
                }
            }
        }
    }
}

//E:\My\work\Java