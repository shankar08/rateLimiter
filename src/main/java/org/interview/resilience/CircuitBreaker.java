package org.interview.resilience;


import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CircuitBreaker {
    //different circuitBreaker States
    /*
        CLOSED - Everything OK, calls allowed
        OPEN - Too many failures, block calls
        HALF_OPEN - Test with 1 call to see if recovered

        •	Allow 5 failures
        •	Then block all calls
        •	Wait 30 seconds
        •	Try again
     */
    public enum State { OPEN, CLOSED, HALF_OPEN };
    private State state = State.CLOSED;
    private int failureCount = 0;

    private static final int FAILURE_THRESHOLD = 5;
    private static final long OPEN_TIMEOUT_MS = 30_000;

    private Instant lastFailureTime;

    public synchronized boolean allowRequest(){
        if (state == State.OPEN){
            long elapsed = Instant.now().toEpochMilli()
                - lastFailureTime.toEpochMilli();
            //If 30 seconds passed:
            //	•	Move to HALF_OPEN
            //	•	Allow one test request
            if (elapsed > OPEN_TIMEOUT_MS) {
                state = State.HALF_OPEN;
                return true;
            }
            return false;
        }

        return true;
    }
    public synchronized  void recordSuccess(){
//      •	LLM worked
//	    •	Reset everything
//	    •	Resume normal traffic
        failureCount = 0;
        state = State.CLOSED;
    }
    public synchronized void recordFailure(){
        failureCount++;
        lastFailureTime = Instant.now();
//      •	Another failure happened
//	    •	Increment count
//	    •	If too many → OPEN circuit
        if(failureCount >= FAILURE_THRESHOLD){
            state = State.OPEN;
        }
    }
}
