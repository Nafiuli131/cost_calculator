package com.powerledger.cost_calculator.repository;

import com.powerledger.cost_calculator.model.MonthCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthCostRepository extends JpaRepository<MonthCost, Long> {
    @Query("SELECT COUNT(m) > 0 FROM MonthCost m WHERE m.year.id = :yearId AND m.month = :month")
    boolean existByMonthAndYear(@Param("yearId") Long yearId, @Param("month") String month);

    @Query("SELECT m FROM MonthCost m WHERE m.year.year = :year")
    List<MonthCost> findByYearValue(@Param("year") int year);
}
