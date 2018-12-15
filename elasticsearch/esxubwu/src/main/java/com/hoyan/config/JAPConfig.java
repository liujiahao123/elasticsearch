package com.hoyan.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by 20160709 on 2018/12/6.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.hoyan.repository")
@EnableTransactionManagement
public class JAPConfig {

}
