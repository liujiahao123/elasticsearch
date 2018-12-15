package com.hoyan.controller.admin;

/**
 * Created by 20160709 on 2018/12/8.
 */

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.hoyan.dto.*;
import com.hoyan.entity.House;
import com.hoyan.entity.HouseDetail;
import com.hoyan.entity.Subway;
import com.hoyan.entity.SupportAddress;
import com.hoyan.form.DatatableSearch;
import com.hoyan.form.HouseForm;
import com.hoyan.services.*;
import com.hoyan.utils.*;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class adminController {

    @Autowired
    private HouseService houseService;

    @Autowired
    private IQiNiuService qiNiuService;

    @Autowired
    private ISupportAddressService iSupportAddressService;

    @Autowired
    private SubwayService subwayService;

    @Autowired
    private SubwayStationService subwayStationService;

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private Gson gson;

    @GetMapping("/")
    public String index() {
        return "index";
    }


    @GetMapping("admin/house/list")
    public String list() {
        return "admin/house-list";
    }

    @GetMapping("/admin/center")
    public String adminCenterPage() {
        return "admin/center";
    }


    @GetMapping("/admin/welcome")
    public String welcomePage() {
        return "admin/welcome";
    }

    @GetMapping("/admin/login")
    public String welcomeLogin() {
        return "admin/login";
    }

    /**
     * 移除图片接口
     *
     * @param id
     * @return
     */
    @DeleteMapping("admin/house/photo")
    @ResponseBody
    public ApiResponse removeHousePhoto(@RequestParam(value = "id") Long id) {
        ServiceResult result = this.houseService.removePhoto(id);

        if (result.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), result.getMessage());
        }
    }

    /**
     * 修改封面接口
     *
     * @param coverId
     * @param targetId
     * @return
     */
    @PostMapping("admin/house/cover")
    @ResponseBody
    public ApiResponse updateCover(@RequestParam(value = "cover_id") Long coverId,
                                   @RequestParam(value = "target_id") Long targetId) {
        ServiceResult result = this.houseService.updateCover(coverId, targetId);

        if (result.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), result.getMessage());
        }
    }

    /**
     * 增加标签接口
     *
     * @param houseId
     * @param tag
     * @return
     */
    @PostMapping("admin/house/tag")
    @ResponseBody
    public ApiResponse addHouseTag(@RequestParam(value = "house_id") Long houseId,
                                   @RequestParam(value = "tag") String tag) {
        if (houseId < 1 || Strings.isNullOrEmpty(tag)) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        ServiceResult result = this.houseService.addTag(houseId, tag);
        if (result.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), result.getMessage());
        }
    }

    /**
     * 移除标签接口
     *
     * @param houseId
     * @param tag
     * @return
     */
    @DeleteMapping("admin/house/tag")
    @ResponseBody
    public ApiResponse removeHouseTag(@RequestParam(value = "house_id") Long houseId,
                                      @RequestParam(value = "tag") String tag) {
        if (houseId < 1 || Strings.isNullOrEmpty(tag)) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        ServiceResult result = this.houseService.removeTag(houseId, tag);
        if (result.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), result.getMessage());
        }
    }


    @PostMapping("admin/house/edit")
    @ResponseBody
    public ApiResponse saveEditHouse(@Valid HouseForm houseForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ApiResponse(HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
        }
        Map<SupportAddress.Level, SupportAddressDTO> cityAndRegion = iSupportAddressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (cityAndRegion.keySet().size() != 2) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }
        ServiceResult serviceResult = houseService.update(houseForm);
        if (serviceResult.isSuccess()) {
            return ApiResponse.ofSuccess(null);
        }
        return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
    }

    @GetMapping("/admin/house/edit")
    public String houseEdit(@RequestParam(value = "id") Long id, Model model) {
        if (id == null || id < 1) {
            return "404";
        }
        ServiceResult<HouseDTO> result = houseService.findCompleteOne(id);
        if (!result.isSuccess()) {
            return "404";
        }
        HouseDTO house = result.getResult();
        model.addAttribute("house", result);
        Map<SupportAddress.Level, SupportAddressDTO> map = iSupportAddressService.findCityAndRegion(house.getCityEnName(), house.getRegionEnName());

        model.addAttribute("city", map.get(SupportAddress.Level.CITY));
        model.addAttribute("region", map.get(SupportAddress.Level.REGION));
        HouseDetailDTO houseDetail = house.getHouseDetail();
        ServiceResult<SubwayDTO> subwayDTO = subwayService.findSubway(houseDetail.getSubwayLineId());
        if (subwayDTO.isSuccess()) {
            model.addAttribute("subway", subwayDTO);
        }
        ServiceResult<SubwayStationDTO> subwayStationServiceResult = subwayStationService.findSubwayStation(houseDetail.getSubwayStationId());
        if (subwayStationServiceResult.isSuccess()) {
            model.addAttribute("station", subwayStationServiceResult.getResult());
        }
        return "admin/house-edit";
    }

    @PostMapping("/admin/houses")
    @ResponseBody
    public ApiDataTablesResult findData(DatatableSearch search) {
        ServiceMuleiResult<HouseDTO> houseDTOs = houseService.adminQuery(search);
        ApiDataTablesResult result = new ApiDataTablesResult(ApiResponse.Status.SUCCESS);
        result.setDraw(search.getDraw());
        result.setData(houseDTOs.getResult());
        result.setRecordsTotal(houseDTOs.getTotal());
        result.setRecordsFiltered(houseDTOs.getTotal());
        return result;
    }

    @PostMapping(value = "admin/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ApiResponse uploadImg(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }
        String fileName = file.getOriginalFilename();
        try {
            InputStream inputStream = file.getInputStream();
            Response response = qiNiuService.uploadFile(inputStream);
            if (response.isOK()) {
                QiuNiuPutRet qiuNiuPutRet = gson.fromJson(response.bodyString(), QiuNiuPutRet.class);
                return ApiResponse.ofSuccess(qiuNiuPutRet);
            } else {
                return ApiResponse.ofMessage(response.statusCode, response.getInfo());
            }

        } catch (QiniuException e) {
            Response response = e.response;
            try {
                return ApiResponse.ofMessage(response.statusCode, response.bodyString());
            } catch (QiniuException e1) {
                e1.printStackTrace();
                return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }

        /*上传到本地文件夹*/
       /* String nameSuffix = fileName.substring(fileName.lastIndexOf("."));
        String path = "F:/elasticsearch/esxubwu/tmp/" + IdWorker.getStringCode() + new Date().getTime() + nameSuffix;
        File target = new File(path);
        try {
            file.transferTo(target);
        } catch (Exception e) {
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }
        return ApiResponse.ofSuccess(path);*/
    }


    @PostMapping("admin/add/house")
    @ResponseBody
    public ApiResponse addHouse(@Valid HouseForm houseForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.info(bindingResult.getAllErrors().get(0).getDefaultMessage());
            return new ApiResponse(HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
        }
        if (houseForm.getPhotos() == null) {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须上传图片");
        }
        Map<SupportAddress.Level, SupportAddressDTO> addressMap = iSupportAddressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (addressMap.keySet().size() != 2) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        ServiceResult<HouseDTO> result = houseService.save(houseForm);
        if (result.isSuccess()) {
            return ApiResponse.ofSuccess(result.getResult());
        }
        return ApiResponse.ofSuccess(ApiResponse.Status.NOT_VALID_PARAM);
    }

    /**
     * 审核接口
     *
     * @param id
     * @param operation
     * @return
     */
    @PutMapping("admin/house/operate/{id}/{operation}")
    @ResponseBody
    public ApiResponse operateHouse(@PathVariable(value = "id") Long id,
                                    @PathVariable(value = "operation") int operation) {
        if (id <= 0) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }
        ServiceResult result;
        switch (operation) {
            case HouseOperation.PASS:
                result = this.houseService.updateStatus(id, HouseStatus.PASSES.getValue());
                break;
            case HouseOperation.PULL_OUT:
                result = this.houseService.updateStatus(id, HouseStatus.NOT_AUDITED.getValue());
                break;
            case HouseOperation.DELETE:
                result = this.houseService.updateStatus(id, HouseStatus.DELETED.getValue());
                break;
            case HouseOperation.RENT:
                result = this.houseService.updateStatus(id, HouseStatus.RENTED.getValue());
                break;
            default:
                return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        if (result.isSuccess()) {
            return ApiResponse.ofSuccess(null);
        }
        return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(),
                result.getMessage());
    }

    /*发布所有*/
    @GetMapping("admin/houses/ReleaseAll")
    @ResponseBody
    public Boolean ReleaseAll() throws Exception{
        List<House> allHouse = houseService.findAllHouse();
        for (House li : allHouse) {
            ServiceResult serviceResult = this.houseService.updateStatus(li.getId(), HouseStatus.PASSES.getValue());
        }
        return true;
    }

    @GetMapping("admin/houses/RemoveAll")
    @ResponseBody
    public Boolean RemoveAll() throws Exception{
        List<House> allHouse = houseService.findAllHouse();
        for (House li : allHouse) {
            this.houseService.updateStatus(li.getId(), HouseStatus.NOT_AUDITED.getValue());
        }
        return true;
    }

    @GetMapping("admin/house/subscribe")
    public String houseSubscribe() {
        return "admin/subscribe";
    }

    @GetMapping("admin/house/subscribe/list")
    @ResponseBody
    public ApiResponse subscribeList(@RequestParam(value = "draw") int draw,
                                     @RequestParam(value = "start") int start,
                                     @RequestParam(value = "length") int size) {
        ServiceMuleiResult<Pair<HouseDTO, HouseSubscribeDTO>> result = houseService.findSubscribeList(start, size);

        ApiDataTablesResult response = new ApiDataTablesResult(ApiResponse.Status.SUCCESS);
        response.setData(result.getResult());
        response.setDraw(draw);
        response.setRecordsFiltered(result.getTotal());
        response.setRecordsTotal(result.getTotal());
        return response;
    }

    @GetMapping("admin/user/{userId}")
    @ResponseBody
    public ApiResponse getUserInfo(@PathVariable(value = "userId") Long userId) {
        if (userId == null || userId < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        ServiceResult<UserDTO> serviceResult = userInfoService.findById(userId);
        if (!serviceResult.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        } else {
            return ApiResponse.ofSuccess(serviceResult.getResult());
        }
    }

    @PostMapping("admin/finish/subscribe")
    @ResponseBody
    public ApiResponse finishSubscribe(@RequestParam(value = "house_id") Long houseId) {
        if (houseId < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        ServiceResult serviceResult = houseService.finishSubscribe(houseId);
        if (serviceResult.isSuccess()) {
            return ApiResponse.ofSuccess("");
        } else {
            return ApiResponse.ofMessage(ApiResponse.Status.BAD_REQUEST.getCode(), serviceResult.getMessage());
        }
    }

}
