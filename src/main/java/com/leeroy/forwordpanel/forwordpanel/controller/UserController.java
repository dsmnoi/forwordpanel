package com.leeroy.forwordpanel.forwordpanel.controller;


import com.leeroy.forwordpanel.forwordpanel.common.WebCurrentData;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.dto.UserPortDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.UserSearchDTO;
import com.leeroy.forwordpanel.forwordpanel.model.User;
import com.leeroy.forwordpanel.forwordpanel.service.UserPortService;
import com.leeroy.forwordpanel.forwordpanel.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理
 */
@Controller
@RequestMapping("user")
public class UserController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private UserPortService userPortService;


    /**
     * 设置密码
     *
     * @param pwd
     * @param isPwd
     * @return
     */
    @RequestMapping("setPwd")
    @ResponseBody
    public ApiResponse setPassword(String pwd, String isPwd) {
        logger.info("进行密码重置");
        Map<String, Object> data = new HashMap();
        if (!pwd.equals(isPwd)) {
            return ApiResponse.error("400", "两次密码不一致");
        }
        //获取当前登陆的用户信息
        User user = WebCurrentData.getUser();
        userService.updatePwd(user.getUsername(), pwd);
        return ApiResponse.ok();
    }

    /**
     * 功能描述: 跳到系统用户列表
     *
     * @param:
     * @return:
     * @auther: youqing
     * @date: 2018/11/21 13:50
     */
    @RequestMapping("/userManage")
    public String userManage() {
        return "user/userManage";
    }

    /**
     * 用户分页
     *
     * @param userSearch
     * @return
     */
    @RequestMapping(value = "/getPage", method = RequestMethod.POST)
    @ResponseBody
    public ApiResponse getUserPage(@RequestBody UserSearchDTO userSearch) {
        // 获取用户列表
        return ApiResponse.ok(userService.getUserPage(userSearch));
    }

    /**
     * 用户列表
     *
     * @param userSearch
     * @return
     */
    @RequestMapping(value = "/getList", method = RequestMethod.POST)
    @ResponseBody
    public ApiResponse getUserList(@RequestBody UserSearchDTO userSearch) {
        // 获取用户列表
        return ApiResponse.ok(userService.getUserList(userSearch));
    }


    /**
     * 新增/更新用户
     *
     * @param user
     * @return
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    public ApiResponse setUser(@RequestBody User user) {
        logger.info("设置用户[新增或更新]！user:" + user);
        if (user.getId() == null) {
            return userService.addUser(user);
        } else {
            return userService.updateUser(user);
        }
    }


    /**
     * 禁用用户
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/disable", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse disable(@RequestParam("id") Integer id) {
        if (WebCurrentData.getUser().getUserType() > 0) {
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        return userService.disableUser(id);
    }

    /**
     * 启用用户
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse enable(@RequestParam("id") Integer id) {
        if (WebCurrentData.getUser().getUserType() > 0) {
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        return userService.enableUser(id);
    }

    /**
     * 获取用户信息
     *
     * @return
     */
    @RequestMapping(value = "/getCurrentUser", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse getCurrentUser() {
        return ApiResponse.ok(userService.getCurrentUser());
    }

    /**
     * 启用用户
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/delete")
    @ResponseBody
    public ApiResponse delete(@RequestParam("id") Integer id) {
        if (WebCurrentData.getUser().getUserType() > 0) {
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        List<UserPortDTO> userPortList = userPortService.findUserPortList(id);
        if (!CollectionUtils.isEmpty(userPortList)) {
            return ApiResponse.error("401", "请先删除用户端口");
        }
        return userService.delUser(id);
    }

    /**
     * 启用用户
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/resetFlow")
    @ResponseBody
    public ApiResponse resetFlow(@RequestParam("id") Integer id) {
        if (WebCurrentData.getUser().getUserType() > 0) {
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        return userService.resetUserFlow(id);
    }

    /**
     * 获取用户流量信息
     *
     * @param userId
     * @return
     */
    @GetMapping(value = "/getUserDetail")
    @ResponseBody
    public ApiResponse getUserDetail(@RequestParam(value = "userId", required = false) Integer userId) {
        return ApiResponse.ok(userService.getUserDetail(userId));
    }

    /**
     * 获取用户详细流量信息
     *
     * @param userId
     * @return
     */
    @GetMapping(value = "/getForwardFlow")
    @ResponseBody
    public ApiResponse getForwardFlow(@RequestParam(value = "userId", required = false) Integer userId) {
        return ApiResponse.ok(userService.getForwardFlow(userId));
    }

    /**
     * 获取用户详细流量信息
     *
     * @param forwardId
     * @return
     */
    @GetMapping(value = "/getPortForwardFlow")
    @ResponseBody
    public ApiResponse getPortForwardFlow(@RequestParam(value = "forwardId", required = false) Integer forwardId) {
        return ApiResponse.ok(userService.getPortForwardFlow(forwardId));
    }
}
