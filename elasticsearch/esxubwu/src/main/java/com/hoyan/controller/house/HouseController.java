package com.hoyan.controller.house;

import com.hoyan.dto.*;
import com.hoyan.entity.SupportAddress;
import com.hoyan.form.MapSearch;
import com.hoyan.form.RentSearch;
import com.hoyan.search.HouseBucketDTO;
import com.hoyan.search.ISearchService;
import com.hoyan.services.*;
import com.hoyan.utils.ApiResponse;
import com.hoyan.utils.RentValueBlock;
import com.qiniu.util.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * Created by 20160709 on 2018/12/8.
 */
@Controller
public class HouseController {

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private ISupportAddressService supportAddressService;

    @Autowired
    private HouseService houseService;

    @Autowired
    private SubwayStationService subwayStationService;

    @Autowired
    private SubwayService subwayService;

    @Autowired
    private ISearchService searchService;

    /*自动补全*/
    @GetMapping("rent/house/autocomplete")
    @ResponseBody
    public ApiResponse autocomplete(@RequestParam("prefix") String prefix) {
        if ("".equals(prefix) || StringUtils.isNullOrEmpty(prefix)) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_PARAMETER);
        }
        ServiceResult<List<String>> suggest = searchService.suggest(prefix);
        return ApiResponse.ofSuccess(suggest.getResult());
    }

    @GetMapping("/admin/add/house")
    public String addHouse() {
        return "admin/house-add";
    }

    @GetMapping("address/support/cities")
    @ResponseBody
    public ApiResponse getSupporyCitied() {
        ServiceMuleiResult<SupportAddressDTO> result = supportAddressService.findAllByCity();
        if (result.getResultSize() == 0) {
            return ApiResponse.ofSuccess(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result);
    }

    @GetMapping("address/support/regions")
    @ResponseBody
    public ApiResponse regions(@RequestParam(value = "city_name") String cityName) {
        ServiceMuleiResult<SupportAddressDTO> result = supportAddressService.findAllByLevelAndBelongTo(cityName);
        if (result.getResultSize() == 0) {
            return ApiResponse.ofSuccess(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result);
    }

    @GetMapping("address/support/subway/line")
    @ResponseBody
    public ApiResponse line(@RequestParam(value = "city_name") String cityName) {
        ServiceMuleiResult<SubwayDTO> result = subwayService.findAllByCAndCityEnName(cityName);
        if (result.getResultSize() == 0) {
            return ApiResponse.ofSuccess(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result);
    }
    //http://localhost:8080/address/support/subway/station?subway_id=4

    @GetMapping("address/support/subway/station")
    @ResponseBody
    public ApiResponse station(@RequestParam(value = "subway_id") Long subwayId) {
        ServiceMuleiResult<SubwayStationDTO> result = subwayStationService.findAllBySubwayId(subwayId);
        if (result.getResultSize() == 0) {
            return ApiResponse.ofSuccess(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result);
    }

    @GetMapping("rent/house")
    public String rentHousePAge(RentSearch rentSearch, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
       /*判断城市是否为空 必须选择一个城市才能进行下一步*/
        if (rentSearch.getCityEnName() == null) {
            String SessioncityEnName = (String) session.getAttribute("cityEnName");
            if (StringUtils.isNullOrEmpty(SessioncityEnName)) {
                redirectAttributes.addAttribute("msg", "必须选择一个城市!");
                return "redirect:/index";
            } else {
                rentSearch.setCityEnName(SessioncityEnName);
            }
        } else {
            session.setAttribute("cityEnName", rentSearch.getCityEnName());
        }

        ServiceResult<SupportAddressDTO> city = supportAddressService.findCity(rentSearch.getCityEnName());
        if (!city.isSuccess()) {
            redirectAttributes.addAttribute("msg", "必须选择一个城市!");
            return "redirect:/index";
        }
        /*获取选择地址 如北京*/
        model.addAttribute("currentCity", city.getResult());
        /*城市信息是否正确*/
        ServiceMuleiResult<SupportAddressDTO> addressResult = supportAddressService.findAllRegionsByCityName(rentSearch.getCityEnName());
        if (addressResult.getResult() == null || addressResult.getResultSize() < 1) {
            redirectAttributes.addAttribute("msg", "必须选择一个城市!");
            return "redirect:/index";
        }
        ServiceMuleiResult serviceMuleiResult = houseService.query(rentSearch);
        model.addAttribute("total", serviceMuleiResult.getTotal());
        model.addAttribute("houses", serviceMuleiResult.getResult());
        /*如果区域为空匹配所有*/
        if (rentSearch.getRegionEnName() == null) {
            rentSearch.setRegionEnName("*");
        }

        model.addAttribute("searchBody", rentSearch);
        /*获取区域地址 如东城区*/
        model.addAttribute("regions", addressResult.getResult());
        model.addAttribute("priceBlocks", RentValueBlock.PRICE_BLOCK);
        model.addAttribute("areaBlocks", RentValueBlock.AREA_BLOCK);
        /*用户信息回显*/

        model.addAttribute("currentPriceBlock", RentValueBlock.matchPrice(rentSearch.getPriceBlock()));
        model.addAttribute("currentAreaBlock", RentValueBlock.matchArea(rentSearch.getAreaBlock()));
        return "rent-list";
    }

    @GetMapping("rent/house/show/{id}")
    public String show(@PathVariable(value = "id") Long houseId,
                       Model model) {
        if (houseId <= 0) {
            return "404";
        }

        ServiceResult<HouseDTO> serviceResult = houseService.findCompleteOne(houseId);
        if (!serviceResult.isSuccess()) {
            return "404";
        }

        HouseDTO houseDTO = serviceResult.getResult();
        Map<SupportAddress.Level, SupportAddressDTO>
                addressMap = supportAddressService.findCityAndRegion(houseDTO.getCityEnName(), houseDTO.getRegionEnName());

        SupportAddressDTO city = addressMap.get(SupportAddress.Level.CITY);
        SupportAddressDTO region = addressMap.get(SupportAddress.Level.REGION);

        model.addAttribute("city", city);
        model.addAttribute("region", region);

        ServiceResult<UserDTO> userDTOServiceResult = userInfoService.findById(houseDTO.getAdminId());
        model.addAttribute("agent", userDTOServiceResult.getResult());
        model.addAttribute("house", houseDTO);

        ServiceResult<Long> aggResult = searchService.aggregateDistrictHouse(city.getEnName(), region.getEnName(), houseDTO.getDistrict());
        model.addAttribute("houseCountInDistrict", aggResult.getResult());

        return "house-detail";
    }

    @GetMapping("rent/house/map")
    public String rentMapPage(String cityEnName, Model model, HttpSession session, RedirectAttributes redirectAttributes) {

        ServiceResult<SupportAddressDTO> city = supportAddressService.findCity(cityEnName);
        if (!city.isSuccess()) {
            redirectAttributes.addAttribute("msg", "必须选择一个城市");
            return "redirect:/index";
        } else {
            session.setAttribute("cityName", cityEnName);/*城市*/
            model.addAttribute("city", city.getResult());/*区域*/
        }
        ServiceMuleiResult<SupportAddressDTO> allRegionsByCityName = supportAddressService.findAllRegionsByCityName(cityEnName);

        ServiceMuleiResult<HouseBucketDTO> houseBucketResult = searchService.mapAggregate(cityEnName);
        model.addAttribute("total", houseBucketResult.getTotal());
        model.addAttribute("aggData", houseBucketResult.getResult());
        model.addAttribute("regions", allRegionsByCityName.getResult());
        return "rent-map";
    }


    @GetMapping("rent/house/map/houses")
    @ResponseBody
    public ApiResponse rentMapHouses(@ModelAttribute MapSearch mapSearch) {
        if (mapSearch.getCityEnName() == null) {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须选择城市");
        }
        ServiceMuleiResult<HouseDTO> result;
        if (mapSearch.getLevel() < 13) {
            result = houseService.wholeMapQuery(mapSearch);
        }else{
              /*范围查询 拖动地图加载执行*/
              //小地图查询必须要船体边界参数
            result = houseService.boundMapQuery(mapSearch);
        }

        ApiResponse response = ApiResponse.ofSuccess(result.getResult());
        response.setMore(result.getTotal() > (mapSearch.getStart() + mapSearch.getSize()));
        return response;
    }


}
