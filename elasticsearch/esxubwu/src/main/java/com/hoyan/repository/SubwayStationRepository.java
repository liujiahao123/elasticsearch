package com.hoyan.repository;

import com.google.common.collect.Lists;
import com.hoyan.entity.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface SubwayStationRepository extends JpaRepository<SubwayStation, Long> {
    List<SubwayStation> findAllBySubwayId(Long subwayId);
}
