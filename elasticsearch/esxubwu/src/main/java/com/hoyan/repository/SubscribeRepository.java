package com.hoyan.repository;

import com.hoyan.entity.HouseSubscribe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by 20160709 on 2018/12/14.
 */
public interface SubscribeRepository extends PagingAndSortingRepository<HouseSubscribe,Long>,JpaRepository<HouseSubscribe,Long> {

    HouseSubscribe findByHouseIdAndUserId(Long id, Long loginUserId);

    Page<HouseSubscribe> findAllByAdminIdAndStatus(Long userId, int value, Pageable pageable);

    Page<HouseSubscribe> findAllByUserIdAndStatus(Long userId, int value, Pageable pageable);

    HouseSubscribe findByHouseIdAndAdminId(Long houseId, Long adminId);

    @Modifying
    @Query("update HouseSubscribe as subscribe set subscribe.status = :status where subscribe.id = :id")
    void updateStatus(@Param(value = "id") Long id, @Param(value = "status") int status);
}
