package com.leeroy.forwordpanel.forwordpanel.service;

import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.dao.SysConfigDao;
import com.leeroy.forwordpanel.forwordpanel.model.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Title: 系统配置
 * @Description:
 * @author: leeroy
 * @version: 1.0
 * @date: 2020/11/21 11:04
 */
@Slf4j
@Service
public class SysConfigService {

    @Autowired
    SysConfigDao sysConfigDao;

    @Value("${panel.version:test}")
    private String version;

    /**
     * 保存clash
     */
    public ApiResponse save(SysConfig config) {
        if (config.getId() != null) {
            sysConfigDao.updateById(config);
        } else {
            config.setId(1);
            sysConfigDao.insert(config);
        }
        return ApiResponse.ok();
    }

    /**
     * 获取配置
     *
     * @return
     */
    public ApiResponse getConfig() {
        SysConfig sysConfig = sysConfigDao.selectById(1);

        if (sysConfig == null) {
            sysConfig = new SysConfig();
            sysConfig.setVersion(version);
            sysConfig.setId(1);
            sysConfig.setTelegram("");
            sysConfigDao.insert(sysConfig);
        } else {
            sysConfig.setVersion(version);
        }
        return ApiResponse.ok(sysConfig);
    }

}
