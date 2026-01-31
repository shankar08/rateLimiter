package org.interview.resilience;

import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class RateLimiter {

    private static final int MAX_REQUEST = 60;
    private static final long WINDOW_MS = 60_000;

    private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();

    public synchronized boolean allowRequest(){
        long now = System.currentTimeMillis();

        //	•	Remove requests older than 1 minute
        while(!timestamps.isEmpty() && now - timestamps.peek() > WINDOW_MS){
            timestamps.poll();
        }
        //  * If under limit:
        //	•	Allow request
        //	•	Record timestamp
        if(timestamps.size() < MAX_REQUEST){
            timestamps.offer(now);
            return true;
        }

        return false;
    }
}
