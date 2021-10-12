package com.cloud.phone.webrtc;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class DelayQueueElement<T> implements Delayed {

    private DelayQueue<T> delayQueue;//延迟队列引用
    private long intervalTimeOfFirst;                  //距离队列中第一个元素的时间间隔
    private T data;                                    //队列中的关键数据

    /**
     * 构造函数
     * @param intervalTimeOfFirst 距离队列中第一个元素的时间间隔
     * @param data                队列中的关键数据
     * @param delayQueue 延迟队列引用
     */
    public DelayQueueElement(long intervalTimeOfFirst, T data, DelayQueue<T> delayQueue) {
        this.data = data;
        this.delayQueue = delayQueue;
        this.intervalTimeOfFirst = intervalTimeOfFirst;
    }

    //获取剩余时间
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.intervalTimeOfFirst + delayQueue.startTime - System.currentTimeMillis(),TimeUnit.MILLISECONDS);
    }

    //优先级排序
    @Override
    public int compareTo(Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    public T getData() {
        return data;
    }
}
