package com.leeroy.forwordpanel.forwordpanel.controller;

import com.leeroy.forwordpanel.forwordpanel.common.WebCurrentData;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.dto.PageRequest;
import com.leeroy.forwordpanel.forwordpanel.dto.UserPortForwardPageReq;
import com.leeroy.forwordpanel.forwordpanel.model.User;
import com.leeroy.forwordpanel.forwordpanel.model.UserPortForward;
import com.leeroy.forwordpanel.forwordpanel.service.UserPortForwardService;
import com.leeroy.forwordpanel.forwordpanel.service.UserService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RequestMapping("forward")
@Controller
public class UserPortForwardController {

    @Autowired
    private UserPortForwardService forwardService;

    /**
     * 中转列表
     *
     * @return
     */
    @ResponseBody
    @PostMapping("getPage")
    public ApiResponse getList(@RequestBody UserPortForwardPageReq pageRequest) {
        return forwardService.getUserForwardList(pageRequest);
    }

    /**
     * 开始中转
     *
     * @param userPortForward
     * @return
     */
    @ResponseBody
    @PostMapping("start")
    public ApiResponse startForward(@RequestBody UserPortForward userPortForward) {
        userPortForward.setUserId(userPortForward.getUserId()==null?WebCurrentData.getUserId():userPortForward.getUserId());
        return forwardService.startForward(userPortForward, true);
    }

    /**
     * 停止中转
     *
     * @param userPortForward
     * @return
     */
    @ResponseBody
    @PostMapping("stop")
    public ApiResponse stopFroward(@RequestBody UserPortForward userPortForward) {
        userPortForward.setUserId(WebCurrentData.getUserId());
        return forwardService.stopForward(userPortForward);
    }

}
