package com.hoyan.repository;

import com.hoyan.entity.House;
import com.hoyan.entity.HouseDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface HouseDetailRepository extends JpaRepository<HouseDetail,Long> {

    HouseDetail findByHouseId(Long id);

    List<HouseDetail> findByHouseIdIn(List<Long> ids);

}
