package com.hoyan.search;

import com.hoyan.form.MapSearch;
import com.hoyan.form.RentSearch;
import com.hoyan.services.ServiceMuleiResult;
import com.hoyan.services.ServiceResult;
import groovy.util.OrderBy;

import java.util.List;

/**
 * 检索接口
 * Created by 20160709 on 2018/12/11.
 */
public interface ISearchService {

    /*索引目标房源*/
    void index(Long houseId);

    /*删除房源或出租之后需要移除房源索引*/
    void remove(Long houseId);

    /*查询房源接口*/
    ServiceMuleiResult<Long> query(RentSearch rentSearch);

    /*获取补全关键字*/
    public ServiceResult<List<String>> suggest(String prefix);

    /*聚合特定小区出租的房间数*/
    ServiceResult<Long> aggregateDistrictHouse(String cityEnname, String regionEnName, String Dis);

    /*聚合城市区域的数据集*/

    ServiceMuleiResult<HouseBucketDTO> mapAggregate(String cityEnName);

    //城市级别查询
    ServiceMuleiResult<Long> mapQuery(String cityName,String OrderBy,String orderDirection,int start,int size);

    /*精确数据查询
    也就是地图缩小的时候*/
    ServiceMuleiResult<Long> mapQuery(MapSearch mapSearch);
}
