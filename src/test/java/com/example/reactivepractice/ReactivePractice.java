package com.example.reactivepractice;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Random;

public class ReactivePractice {

    @Test
    void FLUX_생성() {
        Flux.just(1, 2, 3);

        Flux.just(1, 2, 3)
                .doOnNext(i -> System.out.println("i = " + i))
                .subscribe(i -> System.out.println("Received = " + i));
    }

    @Test
    void FLUX_생성_V2() {
        Flux<Integer> range = Flux.range(11, 5);
    }

    @Test
    void FLUX_MAP() {
        Flux.just("a", "bc", "def", "wxyz")
                .map(str -> str.length());
    }

    @Test
    void FLUX_FLAT_MAP() {
        Flux.just(1, 2, 3)
                .flatMap(i -> Flux.range(1, i))
                .subscribe(i -> System.out.println("i = " + i));
    }

    @Test
    void FLUX_FILTER() {
        Flux.range(1, 10)
                .filter(num -> num % 2 == 0)
                .subscribe(x -> System.out.print(x + " -> "));
    }

    @Test
    void FLUX_DEFAULT_IF_EMPTY() {
        Flux<Integer> defaults = Flux.just(1, 2, 3);
        Flux<Object> defaultIfEmpty = Flux.empty().defaultIfEmpty(defaults);
        System.out.println(defaultIfEmpty);
    }

    @Test
    void FLUX_SWITCH_IF_EMPTY() {
        Flux<Integer> defaults = Flux.just(1, 2, 3);
        Flux<Object> switchIfEmpty = Flux.empty().switchIfEmpty(defaults);
    }

    @Test
    void FLUX_MERGE_WITH() {
        Flux<String> tick1 = Flux.interval(Duration.ofSeconds(1)).map(tick -> tick + "초틱");
        Flux<String> tick2 = Flux.interval(Duration.ofMillis(700)).map(tick -> tick + "밀리초틱");
        tick1.mergeWith(tick2).subscribe(System.out::println);
    }

    @Test
    void FLUX_ZIP_WITH() {
        Flux<String> tick1 = Flux.interval(Duration.ofSeconds(1)).map(tick -> tick + "초틱");
        Flux<String> tick2 = Flux.interval(Duration.ofMillis(700)).map(tick -> tick + "밀리초틱");
        tick1.zipWith(tick2).subscribe(tup -> System.out.println(tup));
    }

    @Test
    void MAKE_TUPLE2() {
        Flux<String> tick1 = Flux.interval(Duration.ofSeconds(1)).map(tick -> tick + "초틱");
        Flux<String> tick2 = Flux.interval(Duration.ofMillis(700)).map(tick -> tick + "밀리초틱");
        Flux<Tuple2<String, String>> tuple2Flux = tick1.zipWith(tick2);
        System.out.println("tuple2Flux = " + tuple2Flux); // FluxZip 클래스
        System.out.println("tuple2Flux.getPrefetch() = " + tuple2Flux.getPrefetch()); //prefetch는 미리 채울 값
    }

    @Test
    void FLUX_ERR() {
        Flux.range(1, 10)
                .map(x -> {
                    if (x == 5) throw new RuntimeException("exception"); // 에러 발생
                    else return x;
                })
                .subscribe(
                        i -> System.out.println(i), // next 신호 처리
                        ex -> System.err.println(ex.getMessage()), // error 신호 처리
                        () -> System.out.println("complete") // complete 신호 처리
                );
    }

    @Test
    void FLUX_ON_ERROR_RETURN() {
        Flux<Integer> seq = Flux.range(1, 10)
                .map(x -> {
                    if (x == 5) throw new RuntimeException("exception");
                    else return x;
                })
                .onErrorReturn(-1);
        seq.subscribe(System.out::println);
    }

    @Test
    void FLUX_ON_ERROR_RESUME_WITH_DEFAULT() {
        Random random = new Random();
        Flux<Integer> seq = Flux.range(1, 10)
                .map(x -> {
                    int rand = random.nextInt(8);
                    if (rand == 0) throw new IllegalArgumentException("illarg");
                    if (rand == 1) throw new IllegalStateException("illstate");
                    if (rand == 2) throw new RuntimeException("exception");
                    return x;
                })
                .onErrorResume(error -> {
                    return Flux.error(error);
                });

        seq.subscribe(System.out::println);
    }

    @Test
    void FLUX_ON_ERROR_RESUME_WITH_CUSTOM() {
        Random random = new Random();
        Flux<Integer> seq = Flux.range(1, 10)
                .map(x -> {
                    int rand = random.nextInt(8);
                    if (rand == 0) throw new IllegalArgumentException("illarg");
                    if (rand == 1) throw new IllegalStateException("illstate");
                    if (rand == 2) throw new RuntimeException("exception");
                    return x;
                })
                .onErrorResume(error -> {
                    if (error instanceof IllegalArgumentException) {
                        return Flux.just(21, 22);
                    }
                    if (error instanceof IllegalStateException) {
                        return Flux.just(31, 32);
                    }
                    return Flux.error(error);
                });

        seq.subscribe(System.out::println);
    }

    @Test
    void FLUX_RETRY() {
        Flux.range(1, 5)
                .map(input -> {
                    if (input < 4) return "num " + input;
                    throw new RuntimeException("boom");
                })
                .retry(1) // 에러 신호 발생시 1회 재시도
                .subscribe(System.out::println, System.err::println, System.out::println);
    }

    @Test
    void FLUX_COUNT() {
        Mono<Long> countMono = Flux.just(1, 2, 3, 4).count();
        Long block = countMono.block();
        System.out.println("block = " + block);
    }
}
