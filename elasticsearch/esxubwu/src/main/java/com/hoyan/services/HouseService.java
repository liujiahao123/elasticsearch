package com.hoyan.services;

import com.hoyan.dto.HouseDTO;
import com.hoyan.dto.HouseSubscribeDTO;
import com.hoyan.entity.House;
import com.hoyan.form.DatatableSearch;
import com.hoyan.form.HouseForm;
import com.hoyan.form.MapSearch;
import com.hoyan.form.RentSearch;
import com.hoyan.utils.HouseSubscribeStatus;
import org.springframework.data.util.Pair;
import org.springframework.web.multipart.MultipartResolver;

import java.util.Date;
import java.util.List;

/**
 * 房屋管理服务接口
 * Created by 20160709 on 2018/12/9.
 */
public interface HouseService {
    ServiceResult<HouseDTO> save(HouseForm houseForm);

    ServiceMuleiResult<HouseDTO> adminQuery(DatatableSearch search);

    ServiceResult<HouseDTO> findCompleteOne(Long id);

    ServiceResult update(HouseForm houseForm);

    /*修改房屋信息 移除封面*/
    ServiceResult removePhoto(Long id);

    ServiceResult addTag(Long houseId, String tag);

    ServiceResult removeTag(Long houseId, String tag);

    ServiceResult updateCover(Long coverId, Long targetId);

    /*审核接口*/
    ServiceResult updateStatus(Long id, int status);


    ServiceMuleiResult<HouseDTO> query(RentSearch rentSearch);

    List<House> findAllHouse();

    /*全地图查询*/
    ServiceMuleiResult<HouseDTO> wholeMapQuery(MapSearch mapSearch);

    /*精确范围数据查询*/
    ServiceMuleiResult<HouseDTO> boundMapQuery(MapSearch mapSearch);

    /*加入预约清单*/
    ServiceResult addSubscribeOrder(Long houseId);
    /*
    获取不同状态的预约列表
    * */

    ServiceMuleiResult<Pair<HouseDTO,HouseSubscribeDTO>> querySubscribeList(HouseSubscribeStatus of, int start, int size);

    ServiceResult subscribe(Long houseId, Date orderTime, String telephone, String desc);

    ServiceResult cancelSubscribe(Long houseId);

    /*管理员查询预约接口*/
    ServiceMuleiResult<Pair<HouseDTO,HouseSubscribeDTO>> findSubscribeList(int start, int size);

    ServiceResult finishSubscribe(Long houseId);
}
