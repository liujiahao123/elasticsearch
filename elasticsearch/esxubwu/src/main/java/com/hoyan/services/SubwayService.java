package com.hoyan.services;

import com.hoyan.dto.SubwayDTO;
import com.hoyan.entity.Subway;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface SubwayService {
    ServiceMuleiResult<SubwayDTO> findAllByCAndCityEnName(String cityName);

    ServiceResult<SubwayDTO> findSubway(Long subwayId);
}
