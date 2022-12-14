package com.example.reactivepractice;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
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

    @Test
    public void onErrorCompleteIfArithmeticException() {
        final AtomicInteger datasource = new AtomicInteger(0);
        Mono<Integer> mono = Mono.just(datasource)
                                 .map(i -> 100 / i.get())
                                 .onErrorComplete(ArithmeticException.class);

        StepVerifier.create(mono).verifyComplete();
    }

    @Test
    public void onErrorContinue() {
        List<String> valueDropped = new ArrayList<>();
        List<Throwable> errorDropped = new ArrayList<>();

        Flux<String> test = Flux.just("foo", "", "bar", "baz")
                                .filter(s -> 3 / s.length() == 1)
                                .onErrorContinue(ArithmeticException.class,
                                                 (t, v) -> {
                                                     errorDropped.add(t);
                                                     valueDropped.add((String) v);
                                                 });

        StepVerifier.create(test)
                    .expectNext("foo")
                    .expectNext("bar")
                    .expectNext("baz")
                    .verifyComplete();

        assertThat(valueDropped).isEqualTo(List.of(""));
        assertThat(errorDropped.get(0).getMessage()).isEqualTo("/ by zero");
    }
}
