package com.hoyan.services.servivesImpl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hoyan.dto.HouseDTO;
import com.hoyan.dto.HouseDetailDTO;
import com.hoyan.dto.HousePictureDTO;
import com.hoyan.dto.HouseSubscribeDTO;
import com.hoyan.entity.*;
import com.hoyan.form.*;
import com.hoyan.repository.*;
import com.hoyan.search.ISearchService;
import com.hoyan.services.HouseService;
import com.hoyan.services.IQiNiuService;
import com.hoyan.services.ServiceMuleiResult;
import com.hoyan.services.ServiceResult;
import com.hoyan.utils.HouseSort;
import com.hoyan.utils.HouseStatus;
import com.hoyan.utils.HouseSubscribeStatus;
import com.hoyan.utils.LoginUserUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;

/**
 * Created by 20160709 on 2018/12/9.
 */

@Service
@Slf4j
public class HouseServiceImpl implements HouseService {

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private SubscribeRepository subscribeRepository;

    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private IQiNiuService qiNiuService;

    @Autowired
    private ISearchService searchService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {
        HouseDetail detail = new HouseDetail();
        ServiceResult<HouseDTO> houseDTOServiceResult = wrapperDetailInfo(detail, houseForm);
        if (houseDTOServiceResult != null) {
            return houseDTOServiceResult;
        }
        House house = new House();
        modelMapper.map(houseForm, house);
        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUserUtil.getLoginUserId());
        houseRepository.save(house);

        detail.setHouseId(house.getId());
        detail = houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.save(pictures);

        HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = modelMapper.map(detail, HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(modelMapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover(this.cdnPrefix + houseDTO.getCover());

        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            houseTagRepository.save(houseTags);
            houseDTO.setTags(tags);
        }

        return new ServiceResult<HouseDTO>(true, null, houseDTO);
    }

    /*修改房源信息 需要添加索引*/
    @Transactional(rollbackFor = Exception.class)
    public ServiceResult update(HouseForm houseForm) {
        House house = houseRepository.findOne(houseForm.getId());
        if (house == null) {
            return ServiceResult.notFound();
        }
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(house.getId());
        if (house == null) {
            return ServiceResult.notFound();
        }

        ServiceResult serviceResult = wrapperDetailInfo(houseDetail, houseForm);
        if (serviceResult != null) {
            return serviceResult;
        }
        houseDetailRepository.save(houseDetail);
        List<HousePicture> pictures = generatePictures(houseForm, houseForm.getId());
        housePictureRepository.save(pictures);
        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }
        modelMapper.map(houseForm, house);
        house.setLastUpdateTime(new Date());
        houseRepository.save(house);
        // 审核通过  创建es索引
        if (house.getStatus() == HouseStatus.PASSES.getValue()) {
            searchService.index(house.getId());
        }
        return ServiceResult.success();
    }


    @Override
    public ServiceMuleiResult<HouseDTO> adminQuery(DatatableSearch searchBody) {
        List<HouseDTO> houseDTOList = Lists.newArrayList();
        Sort sort = new Sort(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
        //log.info(search.getOrderBy()+"======search.getOrderBy()");
        int page = searchBody.getStart() / searchBody.getLength();
        Pageable pageable = new PageRequest(page, searchBody.getLength(), sort);
        Specification<House> specification = (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("adminId"), LoginUserUtil.getLoginUserId());
            predicate = cb.and(predicate, cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            if (searchBody.getCity() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), searchBody.getCity()));
            }

            if (searchBody.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), searchBody.getStatus()));
            }

            if (searchBody.getCreateTimeMin() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMin()));
            }

            if (searchBody.getCreateTimeMax() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMax()));
            }

            if (searchBody.getTitle() != null) {
                predicate = cb.and(predicate, cb.like(root.get("title"), "%" + searchBody.getTitle() + "%"));
            }

            return predicate;
        };

        Page<House> houses = houseRepository.findAll(specification, pageable);
        houses.forEach(li -> {
            HouseDTO houseDTO = modelMapper.map(li, HouseDTO.class);
            //5893089143872757761544356688776.jpg
            houseDTO.setCover(cdnPrefix + houseDTO.getCover());
            //houseDTO.setCover("F:/elasticsearch/esxubwu/tmp/" + "5893089143872757761544356688776.jpg");
            houseDTOList.add(houseDTO);
        });
        return new ServiceMuleiResult<>(houses.getTotalElements(), houseDTOList);
    }

    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {
        House house = houseRepository.findOne(id);
        if (Objects.isNull(house)) {
            return ServiceResult.notFound();
        }
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(id);
        HouseDetailDTO houseDetailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);

        List<HouseTag> tags = houseTagRepository.findByHouseId(id);
        List<String> tagList = new ArrayList<String>();
        if (tags != null && !tags.isEmpty()) {
            tags.stream().forEach(tag -> {
                tagList.add(tag.getName());
            });
        }
        List<HousePicture> housePictures = housePictureRepository.findByHouseId(id);
        List<HousePictureDTO> housePictureDtos = Lists.newArrayList();
        if (housePictures != null && !housePictures.isEmpty()) {
            housePictures.forEach(hp -> {
                HousePictureDTO housePictureDTO = modelMapper.map(hp, HousePictureDTO.class);
                housePictureDtos.add(housePictureDTO);
            });
        }
        HouseDTO result = modelMapper.map(house, HouseDTO.class);
        result.setHouseDetail(houseDetailDTO);
        result.setPictures(housePictureDtos);
        result.setTags(tagList);
        ServiceResult<HouseDTO> serviceResult = ServiceResult.of(result);
        /*预约看房*/
        if (LoginUserUtil.getLoginUserId() > 0) { // 已登录用户
            /*根据房屋id 和登录用户id查看是否预约*/
            HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndUserId(house.getId(), LoginUserUtil.getLoginUserId());
            if (subscribe != null) {
                result.setSubscribeStatus(subscribe.getStatus());
            }
        }
        return serviceResult;
    }

    @Override
    @Transactional
    public ServiceResult addSubscribeOrder(Long houseId) {
        Long userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe != null) {
            return new ServiceResult(false, "已加入预约");
        }

        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return new ServiceResult(false, "查无此房");
        }

        subscribe = new HouseSubscribe();
        Date now = new Date();
        subscribe.setCreateTime(now);
        subscribe.setLastUpdateTime(now);
        subscribe.setUserId(userId);
        subscribe.setHouseId(houseId);
        /*预约状态*/
        subscribe.setStatus(HouseSubscribeStatus.IN_ORDER_LIST.getValue());
        subscribe.setAdminId(house.getAdminId());
        subscribeRepository.save(subscribe);
        return ServiceResult.success();
    }

    @Override
    public ServiceMuleiResult<Pair<HouseDTO, HouseSubscribeDTO>> querySubscribeList(
            HouseSubscribeStatus status,
            int start,
            int size) {
        Long userId = LoginUserUtil.getLoginUserId();
        Pageable pageable = new PageRequest(start / size, size, new Sort(Sort.Direction.DESC, "createTime"));

        Page<HouseSubscribe> page = subscribeRepository.findAllByUserIdAndStatus(userId, status.getValue(), pageable);

        return wrapper(page);
    }

    @Override
    @Transactional
    public ServiceResult subscribe(Long houseId, Date orderTime, String telephone, String desc) {
        Long userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe == null) {
            return new ServiceResult(false, "无预约记录");
        }

        if (subscribe.getStatus() != HouseSubscribeStatus.IN_ORDER_LIST.getValue()) {
            return new ServiceResult(false, "无法预约");
        }

        subscribe.setStatus(HouseSubscribeStatus.IN_ORDER_TIME.getValue());
        subscribe.setLastUpdateTime(new Date());
        subscribe.setTelephone(telephone);
        subscribe.setDesc(desc);
        subscribe.setOrderTime(orderTime);
        subscribeRepository.save(subscribe);
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult cancelSubscribe(Long houseId) {
        Long userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe == null) {
            return new ServiceResult(false, "无预约记录");
        }

        subscribeRepository.delete(subscribe.getId());
        return ServiceResult.success();
    }

    @Override
    public ServiceMuleiResult<Pair<HouseDTO, HouseSubscribeDTO>> findSubscribeList(int start, int size) {
        Long userId = LoginUserUtil.getLoginUserId();
        Pageable pageable = new PageRequest(start / size, size, new Sort(Sort.Direction.DESC, "orderTime"));
       /*根据管理员id */
        Page<HouseSubscribe> page = subscribeRepository.findAllByAdminIdAndStatus(userId, HouseSubscribeStatus.IN_ORDER_TIME.getValue(), pageable);

        return wrapper(page);
    }

    @Override
    @Transactional
    public ServiceResult finishSubscribe(Long houseId) {
        Long adminId = LoginUserUtil.getLoginUserId();
        //todo 这里存在bug条件应该在加上userid
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndAdminId(houseId, adminId);
        if (subscribe == null) {
            return new ServiceResult(false, "无预约记录");
        }

        subscribeRepository.updateStatus(subscribe.getId(), HouseSubscribeStatus.FINISH.getValue());
        houseRepository.updateWatchTimes(houseId);
        return ServiceResult.success();
    }

    private ServiceMuleiResult<Pair<HouseDTO, HouseSubscribeDTO>> wrapper(Page<HouseSubscribe> page) {
        List<Pair<HouseDTO, HouseSubscribeDTO>> result = new ArrayList<>();

        if (page.getSize() < 1) {
            return new ServiceMuleiResult<>(page.getTotalElements(), result);
        }

        List<HouseSubscribeDTO> subscribeDTOS = new ArrayList<>();
        List<Long> houseIds = new ArrayList<>();
        page.forEach(houseSubscribe -> {
            subscribeDTOS.add(modelMapper.map(houseSubscribe, HouseSubscribeDTO.class));
            houseIds.add(houseSubscribe.getHouseId());
        });

        Map<Long, HouseDTO> idToHouseMap = new HashMap<>();
        Iterable<House> houses = houseRepository.findAll(houseIds);
        houses.forEach(house -> {
            idToHouseMap.put(house.getId(), modelMapper.map(house, HouseDTO.class));
        });

        for (HouseSubscribeDTO subscribeDTO : subscribeDTOS) {
            Pair<HouseDTO, HouseSubscribeDTO> pair = Pair.of(idToHouseMap.get(subscribeDTO.getHouseId()), subscribeDTO);
            result.add(pair);
        }

        return new ServiceMuleiResult<>(page.getTotalElements(), result);
    }

    /**
     * 图片对象列表信息填充
     *
     * @param form
     * @param houseId
     * @return
     */
    private List<HousePicture> generatePictures(HouseForm form, Long houseId) {
        List<HousePicture> pictures = new ArrayList<>();
        if (form.getPhotos() == null || form.getPhotos().isEmpty()) {
            return pictures;
        }

        for (PhotoForm photoForm : form.getPhotos()) {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(cdnPrefix);
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }

    /**
     * 房源详细信息对象填充
     *
     * @param houseDetail
     * @param houseForm
     * @return
     */
    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        /*根据id查到地铁线路是否存在*/
        Subway subway = subwayRepository.findOne(houseForm.getSubwayLineId());
        if (subway == null) {
            return new ServiceResult<>(false, "Not valid subway line!");
        }
       /*根据id查到地铁站点是否存在*/
        SubwayStation subwayStation = subwayStationRepository.findOne(houseForm.getSubwayStationId());
        if (subwayStation == null || subway.getId() != subwayStation.getSubwayId()) {
            return new ServiceResult<>(false, "Not valid subway station!");
        }
        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());
        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());
        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;
    }

    @Override
    public ServiceResult removePhoto(Long id) {
        HousePicture picture = housePictureRepository.findOne(id);
        if (picture == null) {
            return ServiceResult.notFound();
        }

        try {
            Response response = this.qiNiuService.delete(picture.getPath());
            if (response.isOK()) {
                housePictureRepository.delete(id);
                return ServiceResult.success();
            } else {
                return new ServiceResult(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false, e.getMessage());
        }
    }

    @Override
    @Transactional
    public ServiceResult addTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }

        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult removeTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null) {
            return new ServiceResult(false, "标签不存在");
        }

        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult updateCover(Long coverId, Long targetId) {
        HousePicture cover = housePictureRepository.findOne(coverId);
        if (cover == null) {
            ServiceResult.notFound();
        }
        houseRepository.updateCover(targetId, cover.getPath());
        return ServiceResult.success();
    }

    @Transactional
    public ServiceResult updateStatus(Long id, int status) {
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }

        if (house.getStatus() == status) {
            return new ServiceResult(false, "状态没有发生变化");
        }

        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ServiceResult(false, "已出租的房源不允许修改状态");
        }

        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "已删除的资源不允许操作");
        }

        houseRepository.updateStatus(id, status);
        //上架需要更新索引 其它请情况都要删除索引
        if (status == HouseStatus.PASSES.getValue()) {
            searchService.index(house.getId());
        } else {
            searchService.remove(house.getId());
        }
        return ServiceResult.success();
    }

    @Override
    public ServiceMuleiResult<HouseDTO> query(RentSearch rentSearch) {
        /*有特定条件走es*/
        if (rentSearch.getKeywords() != null && !rentSearch.getKeywords().isEmpty()) {
            ServiceMuleiResult<Long> serviceResult = searchService.query(rentSearch);
            if (serviceResult.getTotal() == 0) {
                new ServiceMuleiResult<>(0, Lists.newArrayList());
            }
            return new ServiceMuleiResult<>(serviceResult.getTotal(),wrapperHouseResult(serviceResult.getResult()));
        }
        /*走mysql查询*/
        return  mysqlQuery(rentSearch);
    }

    @Override
    public List<House> findAllHouse() {
        return houseRepository.findAll();
    }

    /*全局查询*/
    @Override
    public ServiceMuleiResult<HouseDTO> wholeMapQuery(MapSearch mapSearch) {
        ServiceMuleiResult<Long> serviceResult = searchService.mapQuery(mapSearch.getCityEnName(),
                mapSearch.getOrderBy(), mapSearch.getOrderDirection(), mapSearch.getStart(), mapSearch.getSize());
        if(serviceResult.getTotal()==0){
            return new ServiceMuleiResult<>(0,Lists.newArrayList());
        }
        List<HouseDTO> houseDTOS =wrapperHouseResult(serviceResult.getResult());
        return new ServiceMuleiResult<>(serviceResult.getTotal(),houseDTOS);
    }

    /*缩小范围查询*/
    @Override
    public ServiceMuleiResult<HouseDTO> boundMapQuery(MapSearch mapSearch) {
        ServiceMuleiResult<Long> serviceResult = searchService.mapQuery(mapSearch);
        if(serviceResult.getTotal()==0){
            return new ServiceMuleiResult<>(0,Lists.newArrayList());
        }
        List<HouseDTO> houseDTOS =wrapperHouseResult(serviceResult.getResult());
        return new ServiceMuleiResult<>(serviceResult.getTotal(),houseDTOS);
    }

    private ServiceMuleiResult<HouseDTO>  mysqlQuery(RentSearch rentSearch){
          /*添加默认排序*/
        Sort sort = HouseSort.generateSort(rentSearch.getOrderBy(), rentSearch.getOrderDirection());

        int page = rentSearch.getStart() / rentSearch.getSize();
        Pageable pageable = new PageRequest(page, rentSearch.getSize(), sort);
        /*添加筛选条件*/

        Specification<House> specification = (root, criteriaQuery, cb) -> {
            /*只有审核通过的房源才能展示*/
            Predicate predicate = cb.equal(root.get("status"), HouseStatus.PASSES.getValue());
               /*符合城市*/
            predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), rentSearch.getCityEnName()));
            /*没用地铁*/
            if (HouseSort.DISTANCE_TO_SUBWAY_KEY.equals(rentSearch.getOrderBy())) {
                predicate = cb.and(predicate, cb.gt(root.get(HouseSort.DISTANCE_TO_SUBWAY_KEY), -1));
            }
            return predicate;
        };
        Page<House> houses = houseRepository.findAll(specification, pageable);
        List<HouseDTO> houseDTOList = Lists.newArrayList();
        List<Long> houseIds = Lists.newArrayList();
        Map<Long, HouseDTO> houseDTOMap = Maps.newHashMap();
        houses.forEach(li -> {
            HouseDTO houseDTO = modelMapper.map(li, HouseDTO.class);
            houseDTO.setCover(cdnPrefix + li.getCover());
            houseDTOList.add(houseDTO);
            houseIds.add(li.getId());
            houseDTOMap.put(li.getId(), houseDTO);
        });
        wrapperHouseList(houseIds, houseDTOMap);
        return new ServiceMuleiResult<>(houseDTOList.size(), houseDTOList);
    }


    private List<HouseDTO> wrapperHouseResult(List<Long> houseIds) {
        List<HouseDTO> result = new ArrayList<>();

        Map<Long, HouseDTO> idToHouseMap = new HashMap<>();
        Iterable<House> houses = houseRepository.findAll(houseIds);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            idToHouseMap.put(house.getId(), houseDTO);
        });

        wrapperHouseList(houseIds, idToHouseMap);

        // 矫正顺序
        for (Long houseId : houseIds) {
            result.add(idToHouseMap.get(houseId));
        }
        return result;
    }

    private void wrapperHouseList(List<Long> houseIds, Map<Long, HouseDTO> houseDTOMap) {
        List<HouseDetail> byHouseIdIn = houseDetailRepository.findByHouseIdIn(houseIds);
        if (byHouseIdIn != null && !byHouseIdIn.isEmpty()) {
            byHouseIdIn.stream().forEach(houseDetail -> {
                HouseDTO houseDTO = houseDTOMap.get(houseDetail.getHouseId());
                HouseDetailDTO houseDetailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);
                houseDTO.setHouseDetail(houseDetailDTO);
            });
        }
        List<HouseTag> tags = houseTagRepository.findByHouseIdIn(houseIds);
        if (tags != null && !tags.isEmpty()) {
            tags.forEach(houseTag -> {
                HouseDTO house = houseDTOMap.get(houseTag.getHouseId());
                house.getTags().add(houseTag.getName());
            });
        }

    }
}
