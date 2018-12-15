package com.hoyan.services;

import com.alibaba.fastjson.JSONObject;
import com.hoyan.dto.SupportAddressDTO;
import com.hoyan.entity.SupportAddress;
import com.hoyan.search.BaiduMapLocation;

import java.util.List;
import java.util.Map;

/**
 * Created by 20160709 on 2018/12/9.
 */
public interface ISupportAddressService {



    ServiceMuleiResult<SupportAddressDTO> findAllByCity();



    ServiceMuleiResult<SupportAddressDTO> findAllByLevelAndBelongTo(String enName);

    /**
     * 根据英文简写获取具体区域的信息
     * @param cityEnName
     * @param regionEnName
     * @return
     */
    Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName);

    ServiceMuleiResult<SupportAddressDTO> findAllRegionsByCityName(String cityName);

    /*根据城市英文简写获取城市详细详细*/
    ServiceResult<SupportAddressDTO> findCity(String cityEnName);

    /*根据城市以及具体的地位获取百度地图的经纬度*/
    ServiceResult<BaiduMapLocation> getBaiduMapLocation(String city,String address);

    //上传百度LBS数据
    ServiceResult lbsUpload(BaiduMapLocation location,String title,String address,long houseId ,int price,int
                             area);
    //下架数据lbs
    ServiceResult removeLbs(Long houseId);

    List<JSONObject> isLbsDataExists(Long houseId);

}
