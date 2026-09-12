package com.myfave.api.domain.saleevent.repository;

import com.myfave.api.domain.saleevent.entity.SaleEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleEventRepository extends JpaRepository<SaleEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SaleEvent s WHERE s.saleId = :saleId")
    Optional<SaleEvent> findByIdWithLock(@Param("saleId") Long saleId);

    Optional<SaleEvent> findFirstBySaleStartAtAfterOrderBySaleStartAtAsc(ZonedDateTime now);

    @Query("SELECT s FROM SaleEvent s WHERE :now BETWEEN s.saleStartAt AND s.saleEndAt")
    Optional<SaleEvent> findLiveEvent(ZonedDateTime now);

    List<SaleEvent> findBySaleStartAtAfter(ZonedDateTime now);
}
