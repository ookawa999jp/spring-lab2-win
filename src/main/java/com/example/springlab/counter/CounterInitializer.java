package com.example.springlab.counter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * アプリ起動時に counter テーブルの id=1 行が無ければ初期値 0 で作成する。
 * 既に存在する場合は上書きしない。
 * DBeaver で before / after を比較したいため、起動のたびにリセットしない設計にしている。
 */
@Component
public class CounterInitializer implements ApplicationRunner {

    private final CounterRepository counterRepository;

    public CounterInitializer(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!counterRepository.existsById(1L)) {
            counterRepository.save(new Counter(1L, 0));
        }
    }
}
