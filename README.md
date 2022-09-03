# `ThreadLocal`研究
- [GitHub](https://github.com/DouShaoxun/thread-local-research)
- [Gitee](https://gitee.com/DouShaoxun/thread-local-research)
## 1. `ThreadLocal`是什么？

> `ThreadLocal` 叫做本地线程变量，意思是说，`ThreadLocal` 中填充的的是当前线程的变量，该变量对其他线程而言是封闭且隔离的，`ThreadLocal` 为变量在每个线程中创建了一个副本，这样每个线程都可以访问自己内部的副本变量

应用场景举例

- 在进行对象跨层传递的时候，使用`ThreadLocal`可以避免多次传递，打破层次间的约束。

- 线程间数据隔离

- 进行事务操作，用于存储线程事务信息。

- 数据库连接、`Session`会话管理。

## 2.使用示例

```java
public class ThreadLocalTest {
    
    private static ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        int threadNum = 100;
        // 统计
        AtomicInteger equalsFalseCount = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        List<Thread> threadList = new ArrayList<>();
        for (int i = 1; i <= threadNum; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    String beforeValue = dateFormat.format(new Date());
                    THREAD_LOCAL.set(beforeValue);
                    Thread.yield();
                    Thread.yield();
                    Thread.yield();
                    Thread.yield();
                    Thread.yield();
                    String afterValue = THREAD_LOCAL.get();
                    // 使用完及时调用remove,避免内存泄漏
                    THREAD_LOCAL.remove();
                    boolean equals = beforeValue.equals(afterValue);
                    if (!equals) {
                        synchronized (ThreadLocalTest.class) {
                            equalsFalseCount.getAndIncrement();

                        }
                    }
                    String format = String.format("thread:%s - beforeValue: %s - afterValue: %s - beforeValue.equals(afterValue): %s", Thread.currentThread().getName(), beforeValue, afterValue, equals);
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
        String format = String.format("equalsFalseCount: %s ,time:%sms", equalsFalseCount.get(), System.currentTimeMillis() - start);
        System.out.println(format);
    }
}
```

运行结果

```sh
thread:ThreadLocalTest-0051 - beforeValue: 2022-09-03 20:20:06.167 - afterValue: 2022-09-03 20:20:06.167 - beforeValue.equals(afterValue): true
thread:ThreadLocalTest-0075 - beforeValue: 2022-09-03 20:20:06.167 - afterValue: 2022-09-03 20:20:06.167 - beforeValue.equals(afterValue): true
thread:ThreadLocalTest-0003 - beforeValue: 2022-09-03 20:20:06.167 - afterValue: 2022-09-03 20:20:06.167 - beforeValue.equals(afterValue): true
thread:ThreadLocalTest-0058 - beforeValue: 2022-09-03 20:20:06.168 - afterValue: 2022-09-03 20:20:06.168 - beforeValue.equals(afterValue): true
thread:ThreadLocalTest-0100 - beforeValue: 2022-09-03 20:20:06.167 - afterValue: 2022-09-03 20:20:06.167 - beforeValue.equals(afterValue): true
thread:ThreadLocalTest-0019 - beforeValue: 2022-09-03 20:20:06.167 - afterValue: 2022-09-03 20:20:06.167 - beforeValue.equals(afterValue): true
equalsFalseCount: 0 ,time:81ms
```

从结果可以看到，每一个线程都有自己的`local`值，这就是`TheadLocal`的基本使用

## 3.`ThreadLocal`的内部结构

### 3.1 常见的误解

如果我们不去看源代码的话，可能会猜测`ThreadLocal`是这样子设计的：每个`ThreadLocal`都创建一个`Map`
，然后用线程作为Map的key，要存储的局部变量作为Map的value，这样就能达到各个线程的局部变量隔离的效果。这是最简单的设计方法，
`Jdk8`之前的`ThreadLocal` 确实是这样设计的，但现在早已不是了.

### 3.2 `Jdk8`中的设计

在`Jdk8`中` ThreadLocal`的设计是：每个Thread维护一个`ThreadLocalMap(java.lang.Thread.threadLocals`)
，这个Map的key是`ThreadLocal`实例本身，value才是真正要存储的值Object。

具体的过程是这样的：

1. 每个Thread线程内部都有一个Map (变量名:`java.lang.Thread.threadLocals`,类型:`java.lang.ThreadLocal.ThreadLocalMap`)

2. `java.lang.Thread.threadLocals`里面存储`ThreadLocal`对象（key）和线程的变量副本（value）

3. Thread内部的Map是由`ThreadLocal`维护的，由`ThreadLocal`负责向map获取和设置线程的变量值。

4. 对于不同的线程，每次获取副本值时，别的线程并不能获取到当前线程的副本值，形成了副本的隔离，互不干扰。

   ![image-20220903204308197](https://cruder-figure-bed.oss-cn-beijing.aliyuncs.com/markdown/2022/09/03/08-43-18-133.png)

### 3.3 这样设计的好处

这样设计之后每个Map存储的Entry数量就会变少。因为之前的存储数量由Thread的数量决定，现在是由`ThreadLocal`的数量决定。
在实际运用当中，往往`ThreadLocal`的数量要少于Thread的数量。
当Thread销毁之后，对应的`ThreadLocalMap`也会随之销毁，能减少内存的使用。

### 3.4 常见方法

除了构造方法之外， `ThreadLocal`对外暴露的方法有以下4个：

| 方法声明                   | 描述                         |
| -------------------------- | ---------------------------- |
| protected T initialValue() | 返回当前线程局部变量的初始值 |
| public void set( T value)  | 设置当前线程绑定的局部变量   |
| public T get()             | 获取当前线程绑定的局部变量   |
| public void remove()       | 移除当前线程绑定的局部变量   |

## 4. `ThreadLocal` 内存泄漏问题

```java
/**
* The entries in this hash map extend WeakReference, using
* its main ref field as the key (which is always a
* ThreadLocal object).  Note that null keys (i.e. entry.get()
* == null) mean that the key is no longer referenced, so the
* entry can be expunged from table.  Such entries are referred to
* as "stale entries" in the code that follows.
*/
static class Entry extends WeakReference<ThreadLocal<?>> {
    /** The value associated with this ThreadLocal. */
    Object value;

    Entry(ThreadLocal<?> k, Object v) {
        super(k);
        value = v;
    }
}
```

**此哈希映射中的条目扩展了` WeakReference`，使用其主 ref 字段作为键（始终是 `ThreadLocal` 对象）。请注意，空键（即 `entry.get() == null`）意味着不再引用该键，因此可以从表中删除该条目。在下面的代码中，此类条目称为“stale entries”。**

**`java.lang.Thread#threadLocals`属性为`java.lang.ThreadLocal.ThreadLocalMap`类型,其生命周期和当前`Thread`一样,如果创建`ThreadLocal`的线程一直持续运行如线程池中的线程，那么这个Entry对象中的value就有可能一直得不到回收，发生内存泄露。**

**解决方法: **

- 用完`ThreadLocal`后，执行`remove`操作，避免出现内存溢出情况

- **另外`Jdk8`中已经做了一些优化如，在`ThreadLocal`的`get()、set()、remove()`方法调用的时候会清除掉线程`ThreadLocalMap`中所有`Entry`中`Key`为`null`的`Value`，并将整个`Entry`设置为`null`，利于下次内存回收。**

`java.lang.ThreadLocal#get`源码

```java
    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
```

