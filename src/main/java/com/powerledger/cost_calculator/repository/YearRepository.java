package com.powerledger.cost_calculator.repository;

import com.powerledger.cost_calculator.model.Year;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface YearRepository extends JpaRepository<Year,Long> {
    @Query("SELECT COUNT(y) > 0 FROM Year y WHERE y.year = :year")
    boolean existByYear(int year);
    @Query("SELECT y.id FROM Year y WHERE y.year = :year")
    Long findByYear(int year);
}
