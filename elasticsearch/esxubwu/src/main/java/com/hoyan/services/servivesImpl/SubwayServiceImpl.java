package com.hoyan.services.servivesImpl;

import com.google.common.collect.Lists;
import com.hoyan.dto.SubwayDTO;
import com.hoyan.entity.Subway;
import com.hoyan.repository.SubwayRepository;
import com.hoyan.services.ServiceMuleiResult;
import com.hoyan.services.ServiceResult;
import com.hoyan.services.SubwayService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
@Service
public class SubwayServiceImpl implements SubwayService {

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ServiceMuleiResult<SubwayDTO> findAllByCAndCityEnName(String cityEnName) {
        List<Subway> list =  subwayRepository.findAllByCityEnName(cityEnName);
        List<SubwayDTO> subwayDTOS = Lists.newArrayList();
        list.stream().forEach(li->{
            SubwayDTO subwayDTO = modelMapper.map(li,SubwayDTO.class);
            subwayDTOS.add(subwayDTO);
        });
          return new ServiceMuleiResult<>(subwayDTOS.size(),subwayDTOS);
    }

    @Override
    public ServiceResult<SubwayDTO> findSubway(Long subwayId) {
        if (subwayId == null) {
            return ServiceResult.notFound();
        }
        Subway subway = subwayRepository.findOne(subwayId);
        if (subway == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(subway, SubwayDTO.class));
    }
}
