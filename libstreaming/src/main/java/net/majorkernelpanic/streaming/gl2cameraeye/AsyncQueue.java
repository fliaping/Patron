package net.majorkernelpanic.streaming.gl2cameraeye;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AsyncQueue<E> {
    private boolean mStarted = false;
    private Queue<E> mQueue;
    private Handler<E> mHandler;
    private ProcessThread<E> mThread;

    public AsyncQueue() {
    }

    public void setHandler(Handler<E> handler) {
        this.mHandler = handler;
    }

    private void reset() {
        mQueue = new ConcurrentLinkedQueue<E>();
        mThread = new ProcessThread<E>(this);
    }

    public void start() {
        if(this.isStarted()){
            throw new IllegalStateException("queue already started! please call stop() first!");
        }
        reset();
        this.mStarted = true;
        if (this.mHandler != null) {
            this.mHandler.onStart();
        }
        this.mThread.start();
    }
    public void stop(){
        this.mStarted=false;
    }

    public boolean isStarted() {
        return this.mStarted;
    }

    public void put(E data) {
        if (this.isStarted()) {
            this.mQueue.add(data);
        }
    }
    private void process() {
        E e = null;
        do{
            e = this.mQueue.poll();
            if (e != null) {
                if (this.mHandler != null) {
                    this.mHandler.onData(e);
                }
                continue;
            }
            break;
        }while(true);
    }

    private void finish() {
        this.mQueue = null;
        this.mThread = null;

        if (this.mHandler != null) {
            this.mHandler.onFinish();
        }
    }

    public static interface Handler<E> {
        public void onStart();

        public void onData(E e);

        public void onFinish();
    }

    private static class ProcessThread<E> extends Thread {
        private AsyncQueue<E> mOwner;

        public ProcessThread(AsyncQueue<E> owner) {
            this.mOwner = owner;
        }

        public void run() {
            while (mOwner.isStarted()) {
                mOwner.process();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            mOwner.finish();
        }
    }
}
