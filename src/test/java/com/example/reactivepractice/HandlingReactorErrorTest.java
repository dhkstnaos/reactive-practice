package com.example.reactivepractice;


import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class HandlingReactorErrorTest {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    @Test
    public void onErrorComplete() {
        final AtomicInteger datasource = new AtomicInteger(0);
        Mono<Integer> mono = Mono.just(datasource)
                                 .map(i -> 100 / i.get())
                                 .doOnEach(signal -> log.info("before {}", signal.toString()))
                                 .onErrorComplete()
                                 .doOnEach(signal -> log.info("after {}", signal.toString()));

        StepVerifier.create(mono).verifyComplete();
    }
}
