package com.example.reactivepractice;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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

    @Test
    public void onErrorMap() {
        //onErrorMap??? ????????? ????????? ?????? ????????? ???????????????.
        Mono<Integer> data = Mono.<Integer>error(new Exception())
                                 .onErrorMap(t -> new NullPointerException(t.getMessage()));

        StepVerifier.create(data)
                    .expectError(NullPointerException.class)
                    .verify();
    }

    @Test
    public void onErrorResume() {
        //onErrorResume() ???????????? ?????? ?????? ??? ????????? ???????????? ??? ???????????? ????????? ?????? ????????? ?????? fallback ??? ???????????????.
        Function<Throwable, Mono<String>> fallback = (thr) -> {		// Error occur: '<<ERROR>>'. ignore current element 'MONO_TEST'.
            return Mono.just("FALLBACK_MESSAGE");
        };

        Mono<String> data = (Mono<String>) Mono.just("MONO_TEST")
                                               .map(str -> { throw new NullPointerException("<<ERROR>>"); })
                                               .doOnEach(signal -> log.info("before {}", signal.toString()))
                                               .onErrorResume(fallback)
                                               .doOnEach(signal -> log.info("after {}", signal.toString()))
                                               .subscribe();
    }

    @Test
    public void onErrorReturn() {
        Mono<String> data = Mono.just("MONO_TEST");
        String fallbackValue = "FALLBACK_MESSAGE";

        data.map(str -> { throw new NullPointerException("<<ERROR>>"); })
            .doOnEach(signal -> log.info("before {}.", signal.toString()))		// before onError(com.gngsn.webClient.reactor.MonoOnErrorXxxTest$MonoException: <<ERROR>>).
            .onErrorReturn(fallbackValue)										// onError -> onComplete: all Exception will be transformed
            .doOnEach(signal -> log.info("after {}", signal.toString()))		// it is called twice. (1) after doOnEach_onNext(FALLBACK_MESSAGE). => (2) after onComplete()
            .subscribe(str -> log.info("fallback message is '{}'.", str.toString()));
    }
}
