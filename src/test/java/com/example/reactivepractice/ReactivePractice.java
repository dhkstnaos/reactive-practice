package com.example.reactivepractice;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

public class ReactivePractice {

  @Test
  void FLUX_생성() {
    Flux.just(1,2,3);

    Flux.just(1,2,3)
        .doOnNext(i-> System.out.println("i = " + i))
        .subscribe(i-> System.out.println("Received = " + i));
  }
}
