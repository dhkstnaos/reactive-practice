package com.example.reactivepractice;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class DifferenceOfJustAndDeferAndFromCallable {

    int data = 99;
    public int getData(String caller) {
        System.out.println("called by " + caller);
        return data;
    }

    @Test
    void JustAndDeferAndFromCallable() {
        var just = Mono.just(getData("just"));  // print `called by just`
        var defer = Mono.defer(() -> Mono.just(getData("defer")));
        var callable = Mono.fromCallable(() -> getData("callable"));

        System.out.println("Call test start");

        just.subscribe(d -> System.out.println("just d = " + d));           // print `just d = 99`
        defer.subscribe(d -> System.out.println("defer d = " + d));         // print `called by defer\n defer d = 99`
        callable.subscribe(d -> System.out.println("callable d = " + d));   // print `called by callable\n defer d = 99`

        System.out.println("data value change");
        data = 111;
        just.subscribe(d -> System.out.println("just d = " + d));           // print `just d = 99`
        defer.subscribe(d -> System.out.println("defer d = " + d));         // print `called by defer\n defer d = 111`
        callable.subscribe(d -> System.out.println("callable d = " + d));   // print `called by callable\n defer d = 111`

        /*
           Just: 인스턴화가 되는 시간에 캡처되는 값을 반환, Eager
           Defer, Callable: 구독 시에 데이터 값이 캡처되어 반환 값에 변화가 있어도 최신 값을 반환할 수 있다. , Lazy
         */
    }

    Mono<String> externalServiceCall() {
        return Mono.just("Response");
    }

    Mono<String> executeWhenEmpty() {
        System.out.println("Execute When Empty !");
        return Mono.just("Other-data");
    }

    @Test
    void deferSwitchIfEmptyTest() {
        // (A)
        externalServiceCall()
                .switchIfEmpty(executeWhenEmpty())
                .subscribe();   // print 'Execute When Empty !'

        System.out.println(" next ");
        // (B)
        externalServiceCall()
                .switchIfEmpty(Mono.defer(this::executeWhenEmpty))
                .subscribe();   // print nothing
    }

    public Mono<String> responseMonoString1() {
        System.out.println("1: This method returns some string ~!");
        return Mono.just("some");
    }

    public Mono<String> responseMonoString2() {
        System.out.println("2: This method returns some string ~!");
        return Mono.defer(() -> Mono.just("some"));
    }

    public Mono<String> responseMonoString3() {
        System.out.println("3: This method returns some string ~!");
        return Mono.fromCallable(() -> "some");
    }

    public String responseString() {
        System.out.println("4: This method returns some string ~!");
        return "some";
    }

    @Test
    void deferTest() {
        // A
        responseMonoString1()
                .repeat(3)
                .subscribe(d -> System.out.println("d = " + d));

        // B
        responseMonoString2()
                .repeat(3)
                .subscribe(d -> System.out.println("d = " + d));

        // C
        responseMonoString3()
                .repeat(3)
                .subscribe(d -> System.out.println("d = " + d));

        // D
        Mono.defer(() -> responseMonoString1())
                .repeat(3)
                .subscribe(d -> System.out.println("d = " + d));

        // E
        Mono.fromCallable(() -> responseString())
                .repeat(3)
                .subscribe(d -> System.out.println("d = " + d));
    }
}
