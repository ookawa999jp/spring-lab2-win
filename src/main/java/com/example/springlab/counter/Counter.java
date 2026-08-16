package com.example.springlab.counter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "counter")
public class Counter {

    @Id
    private Long id;

    @Column(name = "counter_value", nullable = false)
    private Integer counterValue;

    protected Counter() {}

    public Counter(Long id, Integer counterValue) {
        this.id = id;
        this.counterValue = counterValue;
    }

    public Long getId() {
        return id;
    }

    public Integer getCounterValue() {
        return counterValue;
    }
}
