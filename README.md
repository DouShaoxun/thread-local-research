# ThreadLocal研究

## 1.JThreadLocal的内部结构

### 1.1常见的误解

如果我们不去看源代码的话，可能会猜测`ThreadLocal`是这样子设计的：每个`ThreadLocal`都创建一个`Map`
，然后用线程作为Map的key，要存储的局部变量作为Map的value，这样就能达到各个线程的局部变量隔离的效果。这是最简单的设计方法，
JDK8之前的ThreadLocal 确实是这样设计的，但现在早已不是了.

### 1.2JDK8中的设计

在JDK8中 ThreadLocal的设计是：每个Thread维护一个ThreadLocalMap(`java.lang.Thread.threadLocals`)
，这个Map的key是ThreadLocal实例本身，value才是真正要存储的值Object。

具体的过程是这样的：

1. 每个Thread线程内部都有一个Map (变量名:`java.lang.Thread.threadLocals`,类型:`java.lang.ThreadLocal.ThreadLocalMap`)

2. `java.lang.Thread.threadLocals`里面存储`ThreadLocal`对象（key）和线程的变量副本（value）

3. Thread内部的Map是由ThreadLocal维护的，由ThreadLocal负责向map获取和设置线程的变量值。

4. 对于不同的线程，每次获取副本值时，别的线程并不能获取到当前线程的副本值，形成了副本的隔离，互不干扰。

### 1.3这样设计的好处

这样设计之后每个Map存储的Entry数量就会变少。因为之前的存储数量由Thread的数量决定，现在是由ThreadLocal的数量决定。
在实际运用当中，往往ThreadLocal的数量要少于Thread的数量。
当Thread销毁之后，对应的ThreadLocalMap也会随之销毁，能减少内存的使用。

### 1.4 常见方法

除了构造方法之外， ThreadLocal对外暴露的方法有以下4个：

| 方法声明                        | 描述 |
|-----------------------------| ------ |
| protected T initialValue()     |    返回当前线程局部变量的初始值 |
| public void set( T value)   | 设置当前线程绑定的局部变量 |
| public T get()              | 获取当前线程绑定的局部变量 |
| public void remove()  | 移除当前线程绑定的局部变量|
