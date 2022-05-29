package cn.cruder.tl;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dousx
 * @date 2022-05-29 13:01
 */

public class ThreadLocalTest01 {

    private static ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {

        int threadNum = 5000;
        AtomicInteger errorInt = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        List<Thread> threadList = new ArrayList<>();
        for (int i = 1; i <= threadNum; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    String beforeValue = dateFormat.format(new Date());
                    THREAD_LOCAL.set(beforeValue);
                    try {
                        TimeUnit.MICROSECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    String afterValue = THREAD_LOCAL.get();
                    boolean equals = beforeValue.equals(afterValue);
                    if (!equals) {
                        synchronized (ThreadLocalTest01.class) {
                            errorInt.getAndIncrement();
                        }
                    }
                    String format = String.format("beforeValue: %s - afterValue: %s - beforeValue.equals(afterValue): %s", beforeValue, afterValue, equals);
                    System.out.println(format);
                    countDownLatch.countDown();
                }
            }, "ThreadLocalTest-" + String.format("%04d", i));
            threadList.add(thread);
        }

        for (Thread thread : threadList) {
            thread.start();
        }
        countDownLatch.await();
        String format = String.format("errorInt: %s ", errorInt.get());
        System.out.println(format);
    }
}
