package com.hoyan.repository;

import com.hoyan.entity.HouseTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface HouseTagRepository extends JpaRepository<HouseTag,Long> {
    List<HouseTag> findByHouseId(Long id);

    HouseTag findByNameAndHouseId(String tag, Long houseId);

    List<HouseTag> findByHouseIdIn(List<Long> id);
}
