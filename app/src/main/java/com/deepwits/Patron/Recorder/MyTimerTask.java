package com.deepwits.Patron.Recorder;

import android.os.Handler;

//import android.util.Log;

/**
 * Created by xp on 15-10-10.
 * 自定义定时任务 抽象函数
 */
public abstract class MyTimerTask implements Runnable {

    private boolean isPauseTask = false;
    private boolean isFirst = true;
    private Handler handler;
    private long delay;
    private long period = 0;
    public long lastTime;
    public MyTimerTask(Handler handler, long delay){     //先执行任务，后延时
        this.delay = delay;
        this.handler = handler;
    }
    public MyTimerTask(Handler handler, long delay, long period){   //先延时period ms,后执行任务
        this.delay = delay;
        this.handler = handler;
        this.period = period;
    }
    @Override
    public void run() {
        if(!isPauseTask && handler !=null){
            timerTask();
            handler.postDelayed(this,delay);    //执行定时任务
        }
    }

    public void setDelay(long delay){       //设置定时任务时长
        this.delay = delay;
    }

    abstract public void timerTask();    //抽象函数,实现将要执行的任务

    public void resume(){      //定时任务启动
        if(isFirst && handler !=null){
            handler.postDelayed(this,period);
            isFirst = false;
        }
        if(isPauseTask && handler !=null){
            handler.postDelayed(this, period);
            isPauseTask = false;
        }
    }
    public void pause(){        //定时任务暂停
        isPauseTask = true;
        handler.removeCallbacks(this);
    }

}
