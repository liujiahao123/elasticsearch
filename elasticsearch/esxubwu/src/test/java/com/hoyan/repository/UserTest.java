

package com.hoyan.repository;

import com.hoyan.entity.UserInfo;
import com.hoyan.search.BaiduMapLocation;
import com.hoyan.search.ISearchService;
import com.hoyan.services.ISupportAddressService;
import com.hoyan.services.ServiceResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;


/**
 * Created by 20160709 on 2018/12/6.
 */


@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UserTest {

    @Autowired
    private ISupportAddressService addressService;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private
    ISupportAddressService supportAddressService;

    @Test
    public void findUserId() {
       supportAddressService.isLbsDataExists(32L);

    }
}
