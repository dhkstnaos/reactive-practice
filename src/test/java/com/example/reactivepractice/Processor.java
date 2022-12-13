package com.example.reactivepractice;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.*;

public class Processor {

        @Test
        void Direct_Processor() {
                /*
                DirectProcessor여러 소비자를 가질 수 있으며 여러 생산자를 지원합니다. 그러나 모든 생성자는 동일한 스레드에서 메시지를 생성해야 합니다.
                그러나 backpressure를 처리하지 못한다는 제약이 있다. 결과적으로 N개를 푸쉬했는데 구독자 중 하나라도 N개 미만을 요청했다면, IllegalStateException을 보낸다.
                Processor가 종료되면(error, complete에 의해), 다른 구독자로 구독할 수 있지만, 구독 즉시 종료 신호만 반복한다.
                 */
                DirectProcessor<Long> data = DirectProcessor.create();
                data.subscribe(t -> System.out.println(t),
                    Throwable::printStackTrace,
                    () -> System.out.println("Finished 1"));
                data.onNext(10L);
                data.onComplete();
                data.subscribe(t -> System.out.println(t),
                    Throwable::printStackTrace,
                    () -> System.out.println("Finished 2"));
                data.onNext(12L);
        }

        @Test
        void Unicast_Processor() {
                /*
                내부 버퍼로 backpressure를 처리할 수 있다. 그러나 구독자가 최대 1개만 가능하다.
                기본적으로 unbounded하다.
                 */
                UnicastProcessor<Long> data = UnicastProcessor.create();
                data.subscribe(t -> {
                        System.out.println(t);
                });
                data.sink().next(10L);
        }

        @Test
        void Emitter_Processor() {
                //여러 publisher와 subscriber가 가능하다.
                EmitterProcessor<Long> data = EmitterProcessor.create(1);
                data.subscribe(t -> System.out.println(t));
                FluxSink<Long> sink = data.sink();
                sink.next(10L);
                sink.next(11L);
                sink.next(12L);
                data.subscribe(t -> System.out.println(t));
                sink.next(13L);
                sink.next(14L);
                sink.next(15L);
        }

        @Test
        void Replay_Processor() {

        }

        @Test
        void Topic_Processor() {

        }

        @Test
        void WorkQueue_Processor() {

        }
}
