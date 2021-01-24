package org.upstox.tickerService.service;

import org.upstox.tickerService.model.Bar;
import org.upstox.tickerService.model.Tick;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 *
 */
public class InMemoryStorageService implements StorageService, Runnable{

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Queue<Tick> tickerQueue;
    private final Queue<Bar> history;  // maintain history of all bars
    private final Queue<Bar> barQueue;  // to websocket

    private Bar bar;
    private final Object barLock = new Object();
    private int barId = 0;

    public InMemoryStorageService(Queue<Tick> tickerQueue, Queue<Bar> barQueue) {
        this.tickerQueue = tickerQueue;
        this.barQueue = barQueue;
        this.history = new LinkedList<>();
    }

    private void initBar(){
        logger.info("Starting bar creation routine at 15 second delay");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (barLock){
                    // TODO: try to modify this with AutomaticReference
                    if (bar != null) {
                        Bar barCopy = new Bar(bar); // deep copy
                        barQueue.add(barCopy);  // send to websocket
                        history.add(barCopy);   // maintain history for rest api
                    }
                    bar = new Bar(++barId);
                    logger.info("Generating a new bar: " + bar.getId());
                }
            }
        }, 0, 15000);
    }

    @Override
    public void run() {
        logger.info("MemoryStorage thread started");
        while(true){    // TODO: add some logic to stop this thread
            while(tickerQueue.isEmpty());   // wait till we get new tick
            logger.info("Received tick");
            onTick(tickerQueue.remove());   // add it to current bar
        }
    }

    @Override
    public void onTick(Tick tick){
        if(this.bar == null){
            initBar();
            while (bar==null);
        }
//        synchronized (barLock){
//            bar.addTick(tick);
//            Bar barCopy = new Bar(bar); // deep copy
//            barQueue.add(barCopy);  // send to websocket
//        }
        logger.info("Processed tick for "+ tick.getSymbol()
                + " on bar "+ bar.getId());
    }
}