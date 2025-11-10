package com.musinsa.point.service;

import com.musinsa.point.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Cacheable(value = "systemConfig", key = "'max.earn.per.transaction'")
    public Long getMaxEarnPerTransaction() {
        return systemConfigRepository.findByConfigKey("point.max.earn.per.transaction")
                .map(config -> Long.parseLong(config.getConfigValue()))
                .orElse(100000L);
    }

    @Cacheable(value = "systemConfig", key = "'max.balance.per.user'")
    public Long getMaxBalancePerUser() {
        return systemConfigRepository.findByConfigKey("point.max.balance.per.user")
                .map(config -> Long.parseLong(config.getConfigValue()))
                .orElse(10000000L);
    }

    @Cacheable(value = "systemConfig", key = "'default.expiration.days'")
    public Integer getDefaultExpirationDays() {
        return systemConfigRepository.findByConfigKey("point.default.expiration.days")
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(365);
    }

    @Cacheable(value = "systemConfig", key = "'min.expiration.days'")
    public Integer getMinExpirationDays() {
        return systemConfigRepository.findByConfigKey("point.min.expiration.days")
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(1);
    }

    @Cacheable(value = "systemConfig", key = "'max.expiration.days'")
    public Integer getMaxExpirationDays() {
        return systemConfigRepository.findByConfigKey("point.max.expiration.days")
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(1825);
    }
}
