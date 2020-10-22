package com.leeroy.forwordpanel.forwordpanel.job;

import com.leeroy.forwordpanel.forwordpanel.dao.PortDao;
import com.leeroy.forwordpanel.forwordpanel.model.Port;
import com.leeroy.forwordpanel.forwordpanel.model.UserPortForward;
import com.leeroy.forwordpanel.forwordpanel.service.UserPortForwardService;
import com.leeroy.forwordpanel.forwordpanel.service.UserPortService;
import com.leeroy.forwordpanel.forwordpanel.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 检查用户转发ddns变化
 */
@Slf4j
@EnableScheduling
@Configurable
@Component
public class CheckDDNSJob {

    @Autowired
    private UserService userService;

    @Autowired
    private UserPortService userPortService;
    @Autowired
    private PortDao portDao;
    @Autowired
    private UserPortForwardService userPortForwardService;

    @Scheduled(cron = "0 0/3 * * * ?")
    public void execute() {
        List<UserPortForward> userPortForwards = userPortForwardService.getUsingForwards();
        for(UserPortForward forward: userPortForwards){
            log.info(">>>>开始执行DDNS检查任务");
            String remoteIp = userPortForwardService.getRemoteIp(forward.getRemoteHost());
            if (remoteIp == null) {
               userPortForwardService.stopForward(forward);
            } else if (!remoteIp.equals(forward.getRemoteIp())) {
                Port port = portDao.selectById(forward.getPortId());
                forward.setRemoteIp(remoteIp);
                userPortForwardService.updateForward(forward, port);
            }
            log.info(">>>>结束执行DDNS检查任务");
        }
    }
}
