package com.example.reactivepractice;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReactiveFruit {

    /*
    과일바구니 예제
    여기서 사용할 예제는 과일바구니 예제입니다. basket1부터 basket3까지 3개의 과일바구니가 있으며, 과일바구니 안에는 과일을 중복해서 넣을 수 있습니다.
    그리고 이 바구니를 List로 가지는 baskets가 있습니다. Flux.fromIterable에 Iterable type의 인자를 넘기면 이 Iterable을 Flux로 변환해줍니다.
    이러한 예제를 만든 이유는 스프링에서 WebClient를 이용하여 특정 HTTP API를 호출하고 받은 JSON 응답에 여러 배열이 중첩되어 있고, 여기서 또 다른 API를
    호출하거나 데이터를 조작하는 경우가 있었습니다. 처음엔 API 호출을 몇 번하고 원하는 데이터로 조작하고자 하는 필요에서 시작하였지만, 받은 데이터를 막상 적절한
    연산자의 조합으로 하려고 접근하다 보면 어려워지고, 어떻게 하면 쉽게 풀 수 있을까 생각해보았습니다. 그리고 간단한 예제를 만들어 연습해보는 것이 좋겠다는 생각이
    들었습니다. 따라서 이러한 연습을 위해 여기서는 과일바구니를 표현하는 리스트를 만들어 시작을 해보도록 하겠습니다.
     */

    final List<String> basket1 = Arrays.asList(new String[]{"kiwi", "orange", "lemon", "orange", "lemon", "kiwi"});
    final List<String> basket2 = Arrays.asList(new String[]{"banana", "lemon", "lemon", "kiwi"});
    final List<String> basket3 = Arrays.asList(new String[]{"strawberry", "orange", "lemon", "grape", "strawberry"});
    final List<List<String>> baskets = Arrays.asList(basket1, basket2, basket3);
    final Flux<List<String>> basketFlux = Flux.fromIterable(baskets);
    final CountDownLatch countDownLatch = new CountDownLatch(1);


    @Test
    void Fruit_Basket() {
        basketFlux.concatMap(basket -> {
            final Mono<List<String>> distinctFruits = Flux.fromIterable(basket).distinct().collectList();
            final Mono<Map<String, Long>> countFruitsMono = Flux.fromIterable(basket)
                    .groupBy(fruit -> fruit) // 바구니로 부터 넘어온 과일 기준으로 group을 묶는다.
                    .concatMap(groupedFlux -> groupedFlux.count()
                            .map(count -> {
                                final Map<String, Long> fruitCount = new LinkedHashMap<>();
                                fruitCount.put(groupedFlux.key(), count);
                                return fruitCount;
                            }) // 각 과일별로 개수를 Map으로 리턴
                    ) // concatMap으로 순서보장
                    .reduce((accumulatedMap, currentMap) -> new LinkedHashMap<>() {
                        {
                            putAll(accumulatedMap);
                            putAll(currentMap);
                        }
                    }); // 그동안 누적된 accumulatedMap에 현재 넘어오는 currentMap을 합쳐서 새로운 Map을 만든다. // map끼리 putAll하여 하나의 Map으로 만든다.
            return Flux.zip(distinctFruits, countFruitsMono, (distinct, count) -> new FruitInfo(distinct, count));
        }).subscribe(System.out::println);
    }

    @Test
    void Fruit_Parallel() throws InterruptedException {
        basketFlux.concatMap(basket -> {
            final Mono<List<String>> distinctFruits = Flux.fromIterable(basket).log().distinct().collectList().subscribeOn(Schedulers.parallel());
            final Mono<Map<String, Long>> countFruitsMono = Flux.fromIterable(basket).log()
                    .groupBy(fruit -> fruit) // 바구니로 부터 넘어온 과일 기준으로 group을 묶는다.
                    .concatMap(groupedFlux -> groupedFlux.count()
                            .map(count -> {
                                final Map<String, Long> fruitCount = new LinkedHashMap<>();
                                fruitCount.put(groupedFlux.key(), count);
                                return fruitCount;
                            }) // 각 과일별로 개수를 Map으로 리턴
                    ) // concatMap으로 순서보장
                    .reduce((accumulatedMap, currentMap) -> new LinkedHashMap<>() {
                        {
                            putAll(accumulatedMap);
                            putAll(currentMap);
                        }
                    }) // 그동안 누적된 accumulatedMap에 현재 넘어오는 currentMap을 합쳐서 새로운 Map을 만든다. // map끼리 putAll하여 하나의 Map으로 만든다.
                    .subscribeOn(Schedulers.parallel());
            return Flux.zip(distinctFruits, countFruitsMono, (distinct, count) -> new FruitInfo(distinct, count));
        }).subscribe(
                System.out::println,  // 값이 넘어올 때 호출 됨, onNext(T)
                error -> {
                    System.err.println(error);
                    countDownLatch.countDown();
                }, // 에러 발생시 출력하고 countDown, onError(Throwable)
                () -> {
                    System.out.println("complete");
                    countDownLatch.countDown();
                } // 정상적 종료시 countDown, onComplete()
        );
        countDownLatch.await(2, TimeUnit.SECONDS);
    }

    /*
        단계별로 알아보자.
        ● subscribe 는 메인 스레드에서 호출되지만 서브스크립션은 상위의 subscribeOn 때문에 빠르게 elastic 스케줄러로 전환된다.
        ● 하위에서 상위까지 위의 모든 오퍼레이터들은 elastic 에서 구독된다.
        ● just 는 elastic 스케줄러에서 값을 전달한다.
        ● 첫번째 doOnNext 는 elastic 스케줄러상에서 데이터를 받는다. 따라서 "just elastic-2" 를 표시한다.
        ● 그리고 나서 위에서 아래로 내려가는 데이터 경로상에, publishOn 을 만나게 되면서 위의 doOnNext 에서 발생된 데이터는 하위의 boundedElastic 스케줄러로 전달된다.
        ● 두번째 doOnNext 는 boundedElastic 상에서 데이터를 받는다 따라서 "publish boundedElastic-3" 을 찍는다.
        ● delayelements 는 타임 오퍼레이터다. 이 오퍼레이터는 기본적으로 Schedulers.parallel() 스케줄러 상에 데이터를 발행한다.
        ● 데이터 경로상의 subscribeOn 은 아무것도 하지 않으며, 동일한 스레드에 시그널을 전달한다.
        ● 데이터 경로상 subscribe() 메소드에 전달된 람다는 데이터 시그널들을 받은 스레드에서 실행된다. 따라서 람다는 "hello delayed parallel-1" 을 프린트 한다.
     */
    @Test
    void Flux_SubscribeOn_PublishOn() throws InterruptedException {
        Flux.just("hello")
                .doOnNext(v -> System.out.println("just " + Thread.currentThread().getName()))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(v -> System.out.println("publish " + Thread.currentThread().getName()))
                .delayElements(Duration.ofMillis(500))
                .subscribeOn(Schedulers.elastic())
                .subscribe(v -> System.out.println(v + " delayed " + Thread.currentThread().getName()));
        Thread.sleep(2000); // 결과를 보기 위한 대기 코드
    }

    @Test
    void Flux_Cold_To_Hot() {
        /*
            Connectable Flux는 Cold에서 Hot으로 바꿀 수 있도록 도와준다.
            publish 연산자를 호출하면 바꿀 수 있다. connect()로 연결하고, autoConnect의 숫자만큼 연결되면 구독한 Flux를 리턴한다.

         */
        basketFlux.concatMap(basket -> {
            final Flux<String> source = Flux.fromIterable(basket).log().publish().autoConnect(2);
            final Mono<List<String>> distinctFruits = source.distinct().collectList();
            final Mono<Map<String, Long>> countFruitsMono = source
                    .groupBy(fruit -> fruit) // 바구니로 부터 넘어온 과일 기준으로 group을 묶는다.
                    .concatMap(groupedFlux -> groupedFlux.count()
                            .map(count -> {
                                final Map<String, Long> fruitCount = new LinkedHashMap<>();
                                fruitCount.put(groupedFlux.key(), count);
                                return fruitCount;
                            }) // 각 과일별로 개수를 Map으로 리턴
                    ) // concatMap으로 순서보장
                    .reduce((accumulatedMap, currentMap) -> new LinkedHashMap<String, Long>() {
                        {
                            putAll(accumulatedMap);
                            putAll(currentMap);
                        }
                    }); // 그동안 누적된 accumulatedMap에 현재 넘어오는 currentMap을 합쳐서 새로운 Map을 만든다. // map끼리 putAll하여 하나의 Map으로 만든다.
            return Flux.zip(distinctFruits, countFruitsMono, (distinct, count) -> new FruitInfo(distinct, count));
        }).subscribe(
                System.out::println,  // 값이 넘어올 때 호출 됨, onNext(T)
                error -> {
                    System.err.println(error);
                    countDownLatch.countDown();
                }, // 에러 발생시 출력하고 countDown, onError(Throwable)
                () -> {
                    System.out.println("complete");
                    countDownLatch.countDown();
                } // 정상적 종료시 countDown, onComplete()
        );
    }

}
