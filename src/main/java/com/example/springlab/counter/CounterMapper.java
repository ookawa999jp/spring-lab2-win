package com.example.springlab.counter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for counter table.
 *
 * 本来は UPDATE counter SET counter_value = counter_value + 1 で足りるが、
 * 今回は Hibernate と MyBatis の混在トランザクション検証のため、
 * MyBatis の SELECT XML と UPDATE XML を両方通す冗長な実装にしている。
 */
@Mapper
public interface CounterMapper {

    Integer selectCounterValue(@Param("id") Long id);

    int updateCounterValue(@Param("id") Long id, @Param("currentValue") Integer currentValue);
}
