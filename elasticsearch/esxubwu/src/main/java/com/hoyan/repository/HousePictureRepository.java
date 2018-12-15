package com.hoyan.repository;

import com.hoyan.entity.HousePicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface HousePictureRepository extends JpaRepository<HousePicture,Long> {

    List<HousePicture> findByHouseId(Long id);

}
