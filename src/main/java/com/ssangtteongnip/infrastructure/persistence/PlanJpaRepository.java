package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.plan.Plan;
import com.ssangtteongnip.domain.plan.PlanRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanJpaRepository extends JpaRepository<Plan, Long>, PlanRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Plan p where p.id = :id and p.userId = :userId")
    Optional<Plan> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Override
    @Query("select distinct p from Plan p left join fetch p.evidences "
            + "where p.userId = :userId and p.planDate between :from and :to order by p.planDate, p.id")
    List<Plan> findInPeriod(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Override
    @Query("select p from Plan p where p.planDate < :today and p.status = PLANNED")
    List<Plan> findPlannedBefore(@Param("today") LocalDate today);
}
