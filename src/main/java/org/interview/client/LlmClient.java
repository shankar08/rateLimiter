package org.interview.client;

import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class LlmClient {

    public void stream(String prompt, Consumer<String> consumer)throws InterruptedException{
        String[] chunks = {
                    "A ", "is ", "start ", "stream ", "w ", "rep ", "tom ", "vac "
        };

        for( String chunk: chunks){
            Thread.sleep(100);
            consumer.accept(chunk);
        }
    }


}
