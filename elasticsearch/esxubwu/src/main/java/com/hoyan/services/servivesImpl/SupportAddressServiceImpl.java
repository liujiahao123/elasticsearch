package com.hoyan.services.servivesImpl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hoyan.dto.SupportAddressDTO;
import com.hoyan.entity.SupportAddress;
import com.hoyan.repository.SupportAddressRepository;
import com.hoyan.search.BaiduMapLocation;
import com.hoyan.services.ISupportAddressService;
import com.hoyan.services.ServiceMuleiResult;
import com.hoyan.services.ServiceResult;
import com.hoyan.utils.ApiResponse;
import com.hoyan.utils.IdWorker;
import com.qiniu.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by 20160709 on 2018/12/9.
 */
@Service
@Slf4j
public class SupportAddressServiceImpl implements ISupportAddressService {

    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private ModelMapper modelMapper;


    private static final String BAIDU_MAP_KEY = "T1ziYUtk1uk3MBRY5mD71SFOw43Ekcow";

    private static final String BAIDU_MAP_GEOCONV_API = "http://api.map.baidu.com/geocoder/v2/?";
    /**
     * POI数据管理接口
     */
    private static final String LBS_CREATE_API = "http://api.map.baidu.com/geodata/v3/poi/create";

    private static final String LBS_QUERY_API = "http://api.map.baidu.com/geodata/v3/poi/list?";

    private static final String LBS_UPDATE_API = "http://api.map.baidu.com/geodata/v3/poi/update";

    private static final String LBS_DELETE_API = "http://api.map.baidu.com/geodata/v3/poi/delete";

    @Override
    public ServiceMuleiResult<SupportAddressDTO> findAllByCity() {
        List<SupportAddress> supportAddress = supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
        List<SupportAddressDTO> supportAddressDTOS = new ArrayList<>();
        supportAddress.stream().forEach(li -> {
            SupportAddressDTO supportAddressDTO = modelMapper.map(li, SupportAddressDTO.class);
            supportAddressDTOS.add(supportAddressDTO);
        });
        return new ServiceMuleiResult<>(supportAddressDTOS.size(), supportAddressDTOS);
    }

    @Override
    public ServiceMuleiResult<SupportAddressDTO> findAllByLevelAndBelongTo(String enName) {
        List<SupportAddress> supportAddress = supportAddressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION.getValue(), enName);
        List<SupportAddressDTO> supportAddressDTOS = new ArrayList<>();
        supportAddress.stream().forEach(li -> {
            SupportAddressDTO supportAddressDTO = modelMapper.map(li, SupportAddressDTO.class);
            supportAddressDTOS.add(supportAddressDTO);
        });
        return new ServiceMuleiResult<>(supportAddressDTOS.size(), supportAddressDTOS);
    }

    @Override
    public Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName) {
        Map<SupportAddress.Level, SupportAddressDTO> result = new HashMap<>();

        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY
                .getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndBelongTo(regionEnName, city.getEnName());

        result.put(SupportAddress.Level.CITY, modelMapper.map(city, SupportAddressDTO.class));
        result.put(SupportAddress.Level.REGION, modelMapper.map(region, SupportAddressDTO.class));
        return result;
    }

    @Override
    public ServiceMuleiResult<SupportAddressDTO> findAllRegionsByCityName(String cityName) {
        if (cityName == null) {
            return new ServiceMuleiResult<>(0, null);
        }

        List<SupportAddressDTO> result = new ArrayList<>();

        List<SupportAddress> regions = supportAddressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION
                .getValue(), cityName);
        for (SupportAddress region : regions) {
            result.add(modelMapper.map(region, SupportAddressDTO.class));
        }
        return new ServiceMuleiResult<>(regions.size(), result);
    }

    @Override
    public ServiceResult<SupportAddressDTO> findCity(String cityEnName) {
        if (StringUtils.isNullOrEmpty(cityEnName)) {
            return ServiceResult.notFound();
        }
        SupportAddress byEnNameAndLevel = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY.getValue());
        if (byEnNameAndLevel == null) {
            return ServiceResult.notFound();
        }
        SupportAddressDTO spDto = modelMapper.map(byEnNameAndLevel, SupportAddressDTO.class);
        return ServiceResult.of(spDto);
    }

    @Override
    public ServiceResult<BaiduMapLocation> getBaiduMapLocation(String city, String address) {
        String encodeAddress;
        String encodeCity;

        try {
            encodeAddress = URLEncoder.encode(address, "UTF-8");
            encodeCity = URLEncoder.encode(city, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("地址编码失败><", e);
            return new ServiceResult<BaiduMapLocation>(false, "Error to encode hosue address");
        }

        HttpClient httpClient = HttpClients.createDefault();
        StringBuilder sb = new StringBuilder(BAIDU_MAP_GEOCONV_API);
        sb.append("address=").append(encodeAddress).append("&")
                .append("city=").append(encodeCity).append("&")
                .append("output=json&")
                .append("ak=").append(BAIDU_MAP_KEY);

        HttpGet get = new HttpGet(sb.toString());
        try {
            HttpResponse response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return new ServiceResult<BaiduMapLocation>(false, "Can not get baidu map location");
            }
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            log.info("baiduResult" + result);
            JsonNode jsonNode = objectMapper.readTree(result);
            int status = jsonNode.get("status").asInt();
            if (status != 0) {
                return new ServiceResult<BaiduMapLocation>(false, "Error to get map location for status: " + status);
            }
            {
                BaiduMapLocation location = new BaiduMapLocation();
                JsonNode jsonLocation = jsonNode.get("result").get("location");
                location.setLongitude(jsonLocation.get("lng").asDouble());
                location.setLatitude(jsonLocation.get("lat").asDouble());
                return ServiceResult.of(location);
            }

        } catch (IOException e) {
            log.error("调用百度地理编码接口失败异常", e);
            return new ServiceResult<BaiduMapLocation>(false, "Error to fetch baidumap api");
        }
    }

    //上传百度LBS数据
    //http://lbsyun.baidu.com/index.php?title=lbscloud/api/geodata
    @Override
    public ServiceResult lbsUpload(BaiduMapLocation location, String title,
                                   String address,
                                   long houseId, int price,
                                   int area) {
        HttpClient httpClient = HttpClients.createDefault();
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("latitude", String.valueOf(location.getLatitude())));
        nvps.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));
        nvps.add(new BasicNameValuePair("coord_type", "3")); // 百度坐标系
        nvps.add(new BasicNameValuePair("geotable_id", "197534"));
        nvps.add(new BasicNameValuePair("ak", BAIDU_MAP_KEY));
        nvps.add(new BasicNameValuePair("houseId", String.valueOf(houseId)));
        nvps.add(new BasicNameValuePair("price", String.valueOf(price)));
        nvps.add(new BasicNameValuePair("area", String.valueOf(area)));
        nvps.add(new BasicNameValuePair("title", title));
        nvps.add(new BasicNameValuePair("address", address));

        HttpPost post;
        List<JSONObject> lbsDataExists = isLbsDataExists(houseId);
        log.debug(lbsDataExists + " 数据是否存在");
        if (!lbsDataExists.isEmpty() && lbsDataExists.size() > 0) {//判断数据是否存在
            post = new HttpPost(LBS_UPDATE_API);
        } else {
            post = new HttpPost(LBS_CREATE_API);
        }

        try {
            //UrlEncodedFormEntity 解释 https://www.aliyun.com/jiaocheng/786157.html
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

            HttpResponse response = httpClient.execute(post);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("hoyan Can not upload lbs data for response: " + result);
                return new ServiceResult(false, "Can not upload baidu lbs data");
            } else {
                JsonNode jsonNode = objectMapper.readTree(result);
                int status = jsonNode.get("status").asInt();
                if (status != 0) {
                    String message = jsonNode.get("message").asText();
                    log.error("hoyan Error to upload lbs data for status: {}, and message: {}", status, message);
                    return new ServiceResult(false, "Error to upload lbs data");
                } else {
                    return ServiceResult.success();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ServiceResult(false);
    }

    public List<JSONObject> isLbsDataExists(Long houseId) {
        List<JSONObject> list = Lists.newArrayList();
        HttpClient httpClient = HttpClients.createDefault();
        StringBuilder sb = new StringBuilder(LBS_QUERY_API);
        sb.append("geotable_id=").append("197534").append("&")
                .append("ak=").append(BAIDU_MAP_KEY).append("&")
                .append("houseId=").append(houseId).append(",").append(houseId);
        log.debug("isLbsDataExists  url+" + sb.toString());
        HttpGet get = new HttpGet(sb.toString());
        try {
            HttpResponse response = httpClient.execute(get);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("查询百度lbs失败: " + result);
                return list;
            }

           /* JsonNode jsonNode = objectMapper.readTree(result);*/
            JSONObject jsonObject = JSON.parseObject(result);
            int status = jsonObject.getInteger("status");
            JSONArray array = jsonObject.getJSONArray("pois");

            if (array != null) {
                list = JSONObject.parseArray(array.toJSONString(), JSONObject.class);
                List<JSONObject> listBaidu = list.stream().map(li -> {
                    if (li.getLong("houseId") == houseId) {
                        log.info(li.getString("houseId") + "===========houseId");
                        return li;
                    }
                    return null;
                }).filter(mp -> mp != null).collect(Collectors.toList());
                if (listBaidu != null && !listBaidu.isEmpty()) {
                    return listBaidu;
                } else {
                    return listBaidu;
                }
            }


            /*int status = jsonNode.get("status").asInt();
            if (status != 0) {
                log.error("请求结果存在问题 0代表成功，其它取值含义另行说明: " + status);
                return false;
            } else {
                long size = jsonNode.get("size").asLong();
                if (size > 0) {

                    return true;
                } else {
                    return false;
                }
            }*/
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return list;
        }
    }

    @Override
    public ServiceResult removeLbs(Long houseId) {

        List<JSONObject> lbsDataExists = isLbsDataExists(houseId);
        if(!lbsDataExists.isEmpty() && lbsDataExists.size()>0){
            houseId = lbsDataExists.get(0).getLong("id");
        }

        HttpClient httpClient = HttpClients.createDefault();
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("geotable_id", "197534"));
        nvps.add(new BasicNameValuePair("ak", BAIDU_MAP_KEY));
        nvps.add(new BasicNameValuePair("id", String.valueOf(houseId)));

        HttpPost delete = new HttpPost(LBS_DELETE_API);
        try {
            delete.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpResponse response = httpClient.execute(delete);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("hoyan Error to delete lbs data for response: " + result);
                return new ServiceResult(false);
            }

            JsonNode jsonNode = objectMapper.readTree(result);
            int status = jsonNode.get("status").asInt();
            if (status != 0) {
                String message = jsonNode.get("message").asText();
                log.error("hoyan removeLbs Error to delete lbs data for message: " + message);
                return new ServiceResult(false, "hoyan Error to delete lbs data for: " + message);
            }
            return ServiceResult.success();
        } catch (IOException e) {
            log.error("hoyan Error to delete lbs data.", e);
            return new ServiceResult(false);
        }
    }
}
