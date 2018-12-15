package com.hoyan.services;

import com.hoyan.dto.SubwayDTO;
import com.hoyan.dto.SubwayStationDTO;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface SubwayStationService {

    ServiceMuleiResult<SubwayStationDTO> findAllBySubwayId(Long SubwayId);

    ServiceResult<SubwayStationDTO> findSubwayStation(Long subwayId);

}
