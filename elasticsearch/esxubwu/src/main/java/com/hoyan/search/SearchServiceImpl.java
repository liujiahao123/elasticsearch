package com.hoyan.search;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.hoyan.entity.House;
import com.hoyan.entity.HouseDetail;
import com.hoyan.entity.HouseTag;
import com.hoyan.entity.SupportAddress;
import com.hoyan.form.MapSearch;
import com.hoyan.form.RentSearch;
import com.hoyan.repository.HouseDetailRepository;
import com.hoyan.repository.HouseRepository;
import com.hoyan.repository.HouseTagRepository;
import com.hoyan.repository.SupportAddressRepository;
import com.hoyan.services.ISupportAddressService;
import com.hoyan.services.ServiceMuleiResult;
import com.hoyan.services.ServiceResult;
import com.hoyan.utils.HouseSort;
import com.hoyan.utils.RentValueBlock;
import com.sun.org.apache.regexp.internal.RE;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Sort;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by ljh
 */
@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {

    private static final String INDEX_NAME = "xunwu";

    private static final String INDEX_TYPE = "house";
    /*kafka topic*/
    private static final String INDEX_TOPIC = "house_topic";


    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HouseTagRepository tagRepository;

    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private ISupportAddressService addressService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /*所有队列的方式异步操作索引*/
    @KafkaListener(topics = INDEX_TOPIC)
    private void receiveMessage(String content) {
        try {
            HouseIndexMessage message = JSON.parseObject(content, HouseIndexMessage.class);
            switch (message.getOperation()) {
                case HouseIndexMessage.INDEX:
                    this.createOrUpdateIndex(message);//构建或者修改索引
                    break;
                case HouseIndexMessage.REMOVE:
                    this.removeIndex(message);
                    break;
                default:
                    log.warn("Not support message content " + content);
                    break;
            }
        } catch (Exception e) {
            log.info("json 格式化出错  " + e.getMessage());
        }

    }


    public ServiceResult<List<String>> suggest1(String prefix) {
        CompletionSuggestionBuilder suggestion = SuggestBuilders.completionSuggestion("suggest").prefix(prefix).size(5);

        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("autocomplete", suggestion);

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .suggest(suggestBuilder);
        log.debug(requestBuilder.toString());

        SearchResponse response = requestBuilder.get();
        Suggest suggest = response.getSuggest();
        if (suggest == null) {
            return ServiceResult.of(new ArrayList<>());
        }
        Suggest.Suggestion result = suggest.getSuggestion("autocomplete");

        int maxSuggest = 0;
        Set<String> suggestSet = new HashSet<>();

        for (Object term : result.getEntries()) {
            if (term instanceof CompletionSuggestion.Entry) {
                CompletionSuggestion.Entry item = (CompletionSuggestion.Entry) term;

                if (item.getOptions().isEmpty()) {
                    continue;
                }

                for (CompletionSuggestion.Entry.Option option : item.getOptions()) {
                    String tip = option.getText().string();
                    if (suggestSet.contains(tip)) {
                        continue;
                    }
                    suggestSet.add(tip);
                    maxSuggest++;
                }
            }

            if (maxSuggest > 5) {
                break;
            }
        }
        List<String> suggests = Lists.newArrayList(suggestSet.toArray(new String[]{}));
        return ServiceResult.of(suggests);
    }

    @Override
    public ServiceResult<List<String>> suggest(String prefix) {
        CompletionSuggestionBuilder suggestionBuilder = SuggestBuilders.completionSuggestion("suggest").prefix(prefix).size(5);
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("autocomplete", suggestionBuilder);
        SearchRequestBuilder requestBuilder = esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE).suggest(suggestBuilder);

        log.debug(requestBuilder.toString());

        SearchResponse response = requestBuilder.get();
        Suggest suggest = response.getSuggest();
        if (suggest == null) {
            return ServiceResult.of(new ArrayList<>());
        }
        Suggest.Suggestion result = suggest.getSuggestion("autocomplete");

        int maxSuggest = 0;
        Set<String> suggestSet = new HashSet<>();

        for (Object term : result.getEntries()) {
            if (term instanceof CompletionSuggestion.Entry) {
                CompletionSuggestion.Entry item = (CompletionSuggestion.Entry) term;

                if (item.getOptions().isEmpty()) {
                    continue;
                }

                for (CompletionSuggestion.Entry.Option option : item.getOptions()) {
                    String tip = option.getText().string();
                    if (suggestSet.contains(tip)) {
                        continue;
                    }
                    suggestSet.add(tip);
                    maxSuggest++;
                }
            }

            if (maxSuggest > 5) {
                break;
            }
        }
        List<String> suggests = Lists.newArrayList(suggestSet.toArray(new String[]{}));
        return ServiceResult.of(suggests);
    }

    @Override
    public ServiceResult<Long> aggregateDistrictHouse(String cityEnName, String regionEnName, String district) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityEnName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, regionEnName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.DISTRICT, district));
        SearchRequestBuilder requestBuilder = esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addAggregation(
                        AggregationBuilders.terms(HouseIndexKey.AGG_DISTRICT)
                                .field(HouseIndexKey.DISTRICT))
                .setSize(0);
        log.debug("聚合小区出租==" + requestBuilder.toString());
        SearchResponse searchResponse = requestBuilder.get();
        if (searchResponse.status() == RestStatus.OK) {
            Terms terms = searchResponse.getAggregations().get(HouseIndexKey.AGG_DISTRICT);
            if (terms.getBuckets() != null && !terms.getBuckets().isEmpty()) {
                return ServiceResult.of(terms.getBucketByKey(district).getDocCount());
            }
        } else {
            log.error("聚合信息失败！");
        }
        return ServiceResult.of(0L);
    }


    /*补全*/

    private Boolean updateSuggest(HouseIndexTemplate houseIndexTemplate) {
        AnalyzeRequestBuilder requestBuilder = new AnalyzeRequestBuilder(
                this.esClient, AnalyzeAction.INSTANCE, INDEX_NAME, houseIndexTemplate.getTitle(),
                houseIndexTemplate.getLayoutDesc(), houseIndexTemplate.getRoundService(),
                houseIndexTemplate.getDescription(), houseIndexTemplate.getSubwayLineName(),
                houseIndexTemplate.getSubwayStationName()
        );
        requestBuilder.setAnalyzer("ik_smart");/*使用粗粒度分词*/
        AnalyzeResponse response = requestBuilder.get();
        List<AnalyzeResponse.AnalyzeToken> tokens = response.getTokens();
        if (tokens == null) {
            log.warn("你这个条件无法进行分词");
            return false;
        }
        List<HouseSuggest> suggests = Lists.newArrayList();
        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            if ("<NUM>".equals(token.getType()) || token.getTerm().length() < 2) {/*如果输入的是数字类型 或者 输入的字符少于两个*/
                continue;
            }
            HouseSuggest suggest = new HouseSuggest();
            suggest.setInput(token.getTerm());
            suggest.setWeight(suggest.getWeight());//可以改变权重
            suggests.add(suggest);
        }
        // 定制化小区自动补全
        HouseSuggest suggest = new HouseSuggest();
        suggest.setInput(houseIndexTemplate.getDistrict());
        suggests.add(suggest);
        houseIndexTemplate.setSuggest(suggests);
        return true;
    }

    private boolean updateSuggest1(HouseIndexTemplate indexTemplate) {
        AnalyzeRequestBuilder requestBuilder = new AnalyzeRequestBuilder(
                this.esClient, AnalyzeAction.INSTANCE, INDEX_NAME, indexTemplate.getTitle(),
                indexTemplate.getLayoutDesc(), indexTemplate.getRoundService(),
                indexTemplate.getDescription(), indexTemplate.getSubwayLineName(),
                indexTemplate.getSubwayStationName());

        requestBuilder.setAnalyzer("ik_smart");

        AnalyzeResponse response = requestBuilder.get();
        List<AnalyzeResponse.AnalyzeToken> tokens = response.getTokens();
        if (tokens == null) {
            log.warn("Can not analyze token for house: " + indexTemplate.getHouseId());
            return false;
        }

        List<HouseSuggest> suggests = new ArrayList<>();
        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            // 排序数字类型 & 小于2个字符的分词结果
            if ("<NUM>".equals(token.getType()) || token.getTerm().length() < 2) {
                continue;
            }

            HouseSuggest suggest = new HouseSuggest();
            suggest.setInput(token.getTerm());
            suggests.add(suggest);
        }

        // 定制化小区自动补全
        HouseSuggest suggest = new HouseSuggest();
        suggest.setInput(indexTemplate.getDistrict());
        suggests.add(suggest);

        indexTemplate.setSuggest(suggests);
        return true;
    }

    private void createOrUpdateIndex(HouseIndexMessage message) {
        log.debug("createOrUpdate message:========================== " + message);
        Long houseId = message.getHouseId();
        House house = houseRepository.findOne(message.getHouseId());
        HouseIndexTemplate tem = new HouseIndexTemplate();
        if (Objects.isNull(house)) {
            this.index(houseId, message.getRetry() + 1);
            return;
        }
        modelMapper.map(house, tem);
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(houseId);
        if (houseDetail == null) {
            //todo
        }
        modelMapper.map(houseDetail, tem);

        /*添加索引的时候根据百度api查询地理编码 cityLevel城市名 regionLevel区域名*/
        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(house.getCityEnName(), SupportAddress.Level.CITY.getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndLevel(house.getRegionEnName(), SupportAddress.Level.REGION.getValue());
        /*最详细的地址  城市+区域+街道+小区+。。。*/
        String address = city.getCnName() + region.getCnName() + house.getStreet() + house.getDistrict() + houseDetail.getDetailAddress();
        ServiceResult<BaiduMapLocation> baiduMapLocation = addressService.getBaiduMapLocation(city.getCnName(), address);
        if (!baiduMapLocation.isSuccess()) {
            this.index(message.getHouseId(), message.getRetry() + 1);
            return;
        }
        tem.setLocation(baiduMapLocation.getResult());

        List<HouseTag> houseTags = tagRepository.findByHouseId(houseId);
        // List<String> tagLists = Lists.newArrayList();
        if (houseTags != null && !houseTags.isEmpty()) {
            List<String> tagLists = houseTags.parallelStream().map(li -> li.getName()).collect(Collectors.toList());
            tem.setTags(tagLists);
        }
        SearchRequestBuilder requestBuilder = esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE).setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        log.debug(requestBuilder.toString());
        SearchResponse searchResponse = requestBuilder.get();
        long totalHits = searchResponse.getHits().getTotalHits();
        boolean success;
        if (totalHits == 0) {
            success = create(tem);
        } else if (totalHits == 1) {
            String esId = searchResponse.getHits().getAt(0).getId();
            success = update(esId, tem);
        } else {
            success = deleteAndCreate(totalHits, tem);
        }

        ServiceResult serviceResult = addressService.lbsUpload(baiduMapLocation.getResult(),
                house.getStreet() + house.getDistrict(),
                city.getCnName() + region.getCnName() + house.getStreet() + house.getDistrict(),
                message.getHouseId(),
                house.getPrice()
                , house.getArea());
        if (!success || !serviceResult.isSuccess()) {
            this.index(message.getHouseId(), message.getRetry() + 1);
        } else {
            log.debug("hoyan Index success with house" + houseId);
        }


        if (success) {
            log.debug("INdex success with house" + houseId);
        }
    }

    private void removeIndex(HouseIndexMessage message) {
        //log.debug("Delete message:========================== " + message);
        Long houseId = message.getHouseId();
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE
                .newRequestBuilder(esClient)
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))
                .source(INDEX_NAME);
        BulkByScrollResponse response = builder.get();
        long deleted = response.getDeleted();
        //log.debug("Delete total:========================== " + deleted);

        ServiceResult serviceResult = addressService.removeLbs(houseId);
        if (!serviceResult.isSuccess() || deleted <= 0) {
            log.debug("Delete index not success " + response);
            remove(houseId, message.getRetry() + 1);
        }
    }

    /*public void removeDef(Long houseId) {
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE
                .newRequestBuilder(esClient)
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))
                .source(INDEX_NAME);
        BulkByScrollResponse response = builder.get();
        long deleted = response.getDeleted();
        log.debug("Delete total: " + deleted);
    }*/

    public void remove(Long houseId) {
        remove(houseId, 0);
    }

    @Override
    public ServiceMuleiResult<Long> query(RentSearch rentSearch) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolQuery.filter(
                QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, rentSearch.getCityEnName())
        );

        if (rentSearch.getRegionEnName() != null && !"*".equals(rentSearch.getRegionEnName())) {
            boolQuery.filter(
                    QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, rentSearch.getRegionEnName())
            );
        }

        RentValueBlock area = RentValueBlock.matchArea(rentSearch.getAreaBlock());
        if (!RentValueBlock.ALL.equals(area)) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.AREA);
            if (area.getMax() > 0) {
                rangeQueryBuilder.lte(area.getMax());
            }
            if (area.getMin() > 0) {
                rangeQueryBuilder.gte(area.getMin());
            }
            boolQuery.filter(rangeQueryBuilder);
        }

        RentValueBlock price = RentValueBlock.matchPrice(rentSearch.getPriceBlock());
        if (!RentValueBlock.ALL.equals(price)) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);
            if (price.getMax() > 0) {
                rangeQuery.lte(price.getMax());
            }
            if (price.getMin() > 0) {
                rangeQuery.gte(price.getMin());
            }
            boolQuery.filter(rangeQuery);
        }

        if (rentSearch.getDirection() > 0) {
            boolQuery.filter(
                    QueryBuilders.termQuery(HouseIndexKey.DIRECTION, rentSearch.getDirection())
            );
        }

        if (rentSearch.getRentWay() > -1) {
            boolQuery.filter(
                    QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, rentSearch.getRentWay())
            );
        }
//must
        /*boolQuery.must(
                QueryBuilders.multiMatchQuery(rentSearch.getKeywords(), HouseIndexKey.TITLE)
                        .boost(2.0f)
        );*/
        boolQuery.must(
                QueryBuilders.multiMatchQuery(rentSearch.getKeywords(),
                        HouseIndexKey.TITLE,
                        HouseIndexKey.TRAFFIC,
                        HouseIndexKey.DISTRICT,
                        HouseIndexKey.ROUND_SERVICE,
                        HouseIndexKey.SUBWAY_LINE_NAME,
                        HouseIndexKey.SUBWAY_STATION_NAME
                ));

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addSort(
                        HouseSort.getSortKey(rentSearch.getOrderBy()),
                        SortOrder.fromString(rentSearch.getOrderDirection())
                )
                .setFrom(rentSearch.getStart())
                .setSize(rentSearch.getSize())
                .setFetchSource(HouseIndexKey.HOUSE_ID, null);

        log.debug(requestBuilder.toString());

        List<Long> houseIds = new ArrayList<>();
        SearchResponse response = requestBuilder.get();
        if (response.status() != RestStatus.OK) {
            log.warn("Search status is no ok for " + requestBuilder);
            return new ServiceMuleiResult<>(0, houseIds);
        }

        for (SearchHit hit : response.getHits()) {
            System.out.println(hit.getSource());
            houseIds.add(Longs.tryParse(String.valueOf(hit.getSource().get(HouseIndexKey.HOUSE_ID))));
        }

        return new ServiceMuleiResult<>(response.getHits().totalHits, houseIds);
    }

    private void remove(Long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            log.error("这条删除索引的消息已经往复发送了3次都没有消费成功 houseId" + houseId);
            return;
        }
        HouseIndexMessage data = new HouseIndexMessage(houseId, HouseIndexMessage.REMOVE, retry);
        try {
            kafkaTemplate.send(INDEX_TOPIC, JSON.toJSONString(data));
        } catch (Exception e) {
            log.error("这条删除索引的消息发送总失败 houseId" + houseId);
        }

    }


    @Override
    public void index(Long houseId) {
        index(houseId, 0);
    }

    private void index(Long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            log.error("这条消息已经往复发送了3次都没有消费成功 houseId" + houseId);
            return;
        }
        HouseIndexMessage data = new HouseIndexMessage(houseId, HouseIndexMessage.INDEX, retry);
        try {
            kafkaTemplate.send(INDEX_TOPIC, JSON.toJSONString(data));
        } catch (Exception e) {
            log.error("这条消息发送总失败 houseId" + houseId);
        }

    }

    /*暂时没用了*/


    private boolean create(HouseIndexTemplate indexTemplate) {
        if (!updateSuggest(indexTemplate)) return false;

        try {
            IndexResponse response = this.esClient.prepareIndex(INDEX_NAME, INDEX_TYPE)
                    .setSource(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON).get();

            log.debug("Create index with house: " + indexTemplate.getHouseId());
            if (response.status() == RestStatus.CREATED) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            log.error("Error to index house " + indexTemplate.getHouseId(), e);
            return false;
        }
    }

    private boolean update(String esId, HouseIndexTemplate indexTemplate) {
        if (!updateSuggest(indexTemplate)) return false;
        try {
            UpdateResponse response = this.esClient.prepareUpdate(INDEX_NAME, INDEX_TYPE, esId).setDoc(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON).get();

            log.debug("Update index with house: " + indexTemplate.getHouseId());
            if (response.status() == RestStatus.OK) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            log.error("Error to index house " + indexTemplate.getHouseId(), e);
            return false;
        }
    }

    private boolean deleteAndCreate(long totalHit, HouseIndexTemplate indexTemplate) {
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE
                .newRequestBuilder(esClient)
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, indexTemplate.getHouseId()))
                .source(INDEX_NAME);

        log.debug("Delete by query for house: " + builder);

        BulkByScrollResponse response = builder.get();
        long deleted = response.getDeleted();
        if (deleted != totalHit) {
            log.warn("Need delete {}, but {} was deleted!", totalHit, deleted);
            return false;
        } else {
            return create(indexTemplate);
        }
    }


    @Override
    public ServiceMuleiResult<HouseBucketDTO> mapAggregate(String cityEnName) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityEnName));/*符合bj的区域*/
        AggregationBuilder aggBuilder = AggregationBuilders.terms(HouseIndexKey.AGG_REGION)
                .field(HouseIndexKey.REGION_EN_NAME);/*根据区域进行聚合 比如朝阳区*/
        SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addAggregation(aggBuilder);
        log.debug(searchRequestBuilder.toString());
        SearchResponse searchResponse = searchRequestBuilder.get();
        List<HouseBucketDTO> buckets = Lists.newArrayList();
        if (searchResponse.status() != RestStatus.OK) {
            log.warn("查询不出任何的结果" + searchResponse);
            return new ServiceMuleiResult<>(0, buckets);
        }
        Terms terms = searchResponse.getAggregations().get(HouseIndexKey.AGG_REGION);/*根据名字获取数据*/
        terms.getBuckets().forEach(li -> {
            buckets.add(new HouseBucketDTO(li.getKeyAsString(), li.getDocCount()));
        });
        return new ServiceMuleiResult<>(searchResponse.getHits().getTotalHits(), buckets);
    }

    @Override
    public ServiceMuleiResult<Long> mapQuery(String cityName, String OrderBy, String orderDirection, int start, int size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityName));
        SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addSort(HouseSort.getSortKey(OrderBy), SortOrder.fromString(orderDirection))//设置排序
                .setFrom(start)/*开始位置*/
                .setSize(size);/*大小*/
        SearchResponse searchResponse = searchRequestBuilder.get();
        List<Long> houseIds = new ArrayList<>();
        if (searchResponse.status() != RestStatus.OK) {
            log.warn(searchResponse.toString() + "mapQuery=====searchResponse");
            new ServiceMuleiResult<>(0, houseIds);
        }
        searchResponse.getHits().forEach(li -> {
            houseIds.add(Longs.tryParse(String.valueOf(li.getSource().get(HouseIndexKey.HOUSE_ID))));
        });
        return new ServiceMuleiResult<>(searchResponse.getHits().getTotalHits(), houseIds);
    }

    @Override
    public ServiceMuleiResult<Long> mapQuery(MapSearch mapSearch) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, mapSearch.getCityEnName()));
        //todo 注意
        boolQuery.filter(
                QueryBuilders.geoBoundingBoxQuery("location")
                        /*顺序必须这样  左上角*/
                        .setCorners(
                                new GeoPoint(mapSearch.getLeftLatitude(), mapSearch.getLeftLongitude()),
                                new GeoPoint(mapSearch.getRightLatitude(), mapSearch.getRightLongitude())
                        )
        );
        SearchRequestBuilder searchResponse = esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE).setQuery(boolQuery)
                .addSort(HouseSort.getSortKey(mapSearch.getOrderBy()),
                        SortOrder.fromString(mapSearch.getOrderDirection()))//设置排序
                .setFrom(mapSearch.getStart())/*开始位置*/
                .setSize(mapSearch.getSize());/*大小*/
        SearchResponse response = searchResponse.get();
        List<Long> houseIds = new ArrayList<>();
        if (response.status() != RestStatus.OK) {
            log.warn(searchResponse.toString() + "mapQuery=====searchResponse");
            new ServiceMuleiResult<>(0, houseIds);
        }
        response.getHits().forEach(li -> {
            houseIds.add(Longs.tryParse(String.valueOf(li.getSource().get(HouseIndexKey.HOUSE_ID))));
        });
        return new ServiceMuleiResult<>(response.getHits().getTotalHits(), houseIds);
    }

}
