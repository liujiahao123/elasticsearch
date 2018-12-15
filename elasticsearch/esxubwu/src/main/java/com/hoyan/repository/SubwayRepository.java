package com.hoyan.repository;

import com.hoyan.entity.Subway;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface SubwayRepository extends JpaRepository<Subway, Long> {
    List<Subway> findAllByCityEnName(String cityEnName);
}
