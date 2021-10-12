package com.cloud.phone.webrtc;

import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class DelayQueue<T> {

    private static final String TAG = "CandidateQueue";

    private java.util.concurrent.DelayQueue<DelayQueueElement<T>> delayQueue; //延迟队列
    private boolean isRunning = false;                   //队列运行标志
    private boolean reset = false;                       //重置标志位
    private boolean firstDelay = false;                  //第一个元素是否延迟
    private long intervalTimeOfFirst = 0;                //距离队列第一个元素的时间间隔
    private long delayTime;                              //延迟时间，即相邻两个元素出队的最短时间间隔，毫秒
    long startTime ;                                     //开始时间，默认十年之后，用于控制队列的运行状态

    private PopInterface<T> popInterface;                //对外暴露的接口，当从队列获取到一个数据就回调通知外界

    public DelayQueue(long delayTime){
        this.delayQueue = new java.util.concurrent.DelayQueue<>();
        this.delayTime = delayTime;
        //开始时间等于十年后
        this.startTime = System.currentTimeMillis() + TimeUnit.DAYS.convert(3650,TimeUnit.MILLISECONDS);
    }

    //入队
    public void push(T data){
        //如果需要重置
        if (reset){
            reset = false;
            //重新设置开始时间
            startTime = System.currentTimeMillis();
            //距离队列第一个元素的时间间隔也置0
            intervalTimeOfFirst = 0;
            if (firstDelay){
                intervalTimeOfFirst = delayTime;
            }
        }
        //TODO 后期考虑使用复用池避免内存抖动
        DelayQueueElement<T> delayQueueElement = new DelayQueueElement<>(intervalTimeOfFirst,data,this);
        boolean ok = delayQueue.offer(delayQueueElement);
        //添加成功之后时间间隔增加一个延迟时间，这样队列中每两个相邻元素的时间间隔都是delayTime
        if (ok){
            intervalTimeOfFirst += delayTime;
        }else {
            Log.d(TAG, "put failed,queue is full");
        }
    }

    /**
     * 开始运行队列
     * @param popInterface 队列取得数据的接口回调
     */
    public void run(PopInterface<T> popInterface){
        if (isRunning){
            throw new RuntimeException("the queue is already running!");
        }
        isRunning = true;
        //开始时间默认是十年后，这几乎意味着队列的数据不可获取，
        //当调用run方法后将startTime置为当前时间，队列开始正常运转
        startTime = System.currentTimeMillis();
        this.popInterface = popInterface;
        //开一个线程循环从队列中取数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning){
                    take();
                }
            }
        }).start();
    }

    private void take(){
        try {
            //从队列中取数据，超时时间为一个延时周期
            DelayQueueElement<T> delayQueueElement = delayQueue.poll(delayTime,TimeUnit.MILLISECONDS);
            //如果一个延时周期还取不到数据，说明队列里面已经没有数据了
            //重置数据，相当于新一轮的队列循环
            if (delayQueueElement == null && !reset){
                reset = true;
            }
            //如果取到数据了则回调通知外界
            if(delayQueueElement != null){
                DelayQueue.this.popInterface.pop(delayQueueElement);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setFirstDelay(boolean firstDelay) {
        this.firstDelay = firstDelay;
        if (firstDelay){
            intervalTimeOfFirst = delayTime;
        }
    }

    //获取队列大小
    public int size(){
        return delayQueue.size();
    }

    //终止队列循环
    public void quit(){
        isRunning = false;
    }

    public boolean isRunning(){
        return isRunning;
    }

    public interface PopInterface<T>{
        public void pop(DelayQueueElement<T> delayQueueElement);
    }
}
