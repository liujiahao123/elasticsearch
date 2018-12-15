package com.hoyan.services.servivesImpl;

import com.google.common.collect.Lists;
import com.hoyan.dto.SubwayDTO;
import com.hoyan.dto.SubwayStationDTO;
import com.hoyan.entity.SubwayStation;
import com.hoyan.repository.SubwayStationRepository;
import com.hoyan.services.ServiceMuleiResult;
import com.hoyan.services.ServiceResult;
import com.hoyan.services.SubwayService;
import com.hoyan.services.SubwayStationService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
@Service
public class SubwayStationServiceImpl implements SubwayStationService {

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private ModelMapper modelMapper;


    @Override
    public ServiceMuleiResult<SubwayStationDTO> findAllBySubwayId(Long SubwayId) {
        List<SubwayStation> subwayStations =  subwayStationRepository.findAllBySubwayId(SubwayId);
        List<SubwayStationDTO> subwayStationDTOS = Lists.newArrayList();
        subwayStations.stream().forEach(li->{
            SubwayStationDTO subwayStationDTO = modelMapper.map(li,SubwayStationDTO.class);
            subwayStationDTOS.add(subwayStationDTO);
        });
        return new ServiceMuleiResult<>(subwayStationDTOS.size(),subwayStationDTOS);
    }

    @Override
    public ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId) {
        if (stationId == null) {
            return ServiceResult.notFound();
        }
        SubwayStation station = subwayStationRepository.findOne(stationId);
        if (station == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(station,SubwayStationDTO.class));
    }

}
