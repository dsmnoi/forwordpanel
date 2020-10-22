package com.leeroy.forwordpanel.forwordpanel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leeroy.forwordpanel.forwordpanel.common.WebCurrentData;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.common.response.PageDataResult;
import com.leeroy.forwordpanel.forwordpanel.common.util.DigestUtils;
import com.leeroy.forwordpanel.forwordpanel.dao.ServerDao;
import com.leeroy.forwordpanel.forwordpanel.dao.UserDao;
import com.leeroy.forwordpanel.forwordpanel.dto.ForwardFlowDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.UserDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.UserSearchDTO;
import com.leeroy.forwordpanel.forwordpanel.model.ForwardFlow;
import com.leeroy.forwordpanel.forwordpanel.model.Server;
import com.leeroy.forwordpanel.forwordpanel.model.User;
import com.leeroy.forwordpanel.forwordpanel.model.UserPortForward;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Title: AdminUserServiceImpl
 * @Description:
 * @author: leeroy
 * @version: 1.0
 * @date: 2020/11/21 11:04
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    ServerDao serverDao;

    @Autowired
    private ForwardFlowService forwardFlowService;

    @Value("${panel.default-password}")
    private String defaultPassword;

    @Autowired
    private RemoteForwardService remoteForwardService;

    @Autowired
    private UserPortForwardService userPortForwardService;

    public PageInfo<User> getUserPage(UserSearchDTO userSearch) {
        Integer userType = WebCurrentData.getUser().getUserType();
        if(userType>0){
            userSearch.setId(WebCurrentData.getUserId());
        }
        Page<User> page = PageHelper.startPage(userSearch.getPageNum(), userSearch.getPageSize());
        List<User> baseAdminUsers = userDao.getUsers(userSearch);
        //统计流量
        for (User baseAdminUser : baseAdminUsers) {
            baseAdminUser.setDataUsage(forwardFlowService.getUserFlowTotal(baseAdminUser.getId()));
        }
        PageInfo<User> pageInfo = page.toPageInfo();
        if (baseAdminUsers.size() != 0) {
            pageInfo.setList(baseAdminUsers);
        }
        return pageInfo;
    }

    /**
     * 获取用户详情
     * @param userId
     * @return
     */
    public User getUserDetail(Integer userId) {
        Integer userType = WebCurrentData.getUser().getUserType();
        userId=userId==null?WebCurrentData.getUserId():userId;
        if (userType > 0) {
            userId = WebCurrentData.getUserId();
        }
        User user = userDao.selectById(userId);
        user.setDataUsage(forwardFlowService.getUserFlowTotal(user.getId()));
        return user;
    }

    public List<User> getUserList(UserSearchDTO userSearch) {
        Integer userType = WebCurrentData.getUser().getUserType();
        if(userType>0){
            userSearch.setId(WebCurrentData.getUserId());
        }
        List<User> baseAdminUsers = userDao.getUsers(userSearch);
        return baseAdminUsers;
    }

    public List<ForwardFlowDTO> getForwardFlow(Integer userId){
        Integer userType = WebCurrentData.getUser().getUserType();
        userId=userId==null?WebCurrentData.getUserId():userId;
        if (userType > 0) {
            userId = WebCurrentData.getUserId();
        }
        return forwardFlowService.getUserFlow(userId);
    }

    public List<ForwardFlowDTO> getPortForwardFlow(Integer portForwardId){
        return forwardFlowService.getPortForwardFlow(portForwardId);
    }

    public UserDTO getCurrentUser() {
        Integer userId = WebCurrentData.getUserId();
        if (userId == null) {
            return null;
        }
        User user = userDao.selectById(userId);
        UserDTO result = new UserDTO();
        BeanUtils.copyProperties(user, result);
        return result;
    }


    public ApiResponse addUser(User user) {
        try {
            if(WebCurrentData.getUser().getUserType()>0){
                return ApiResponse.error("403", "您没有权限新增用户");
            }
            LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery().eq(User::getUsername, user.getUsername())
                    .eq(User::getDeleted, false);
            User old = userDao.selectOne(queryWrapper);
            if (old != null) {
                return ApiResponse.error("400", "用户名已存在");
            }
            String username = user.getUsername();
            if (StringUtils.isBlank(user.getPassword())) {
                String password = DigestUtils.Md5(username, defaultPassword);
                user.setPassword(password);
            } else {
                String password = DigestUtils.Md5(username, user.getPassword());
                user.setPassword(password);
            }
            user.setRegTime(new Date());
            user.setDisabled(false);
            user.setDeleted(false);
            userDao.insert(user);
        } catch (Exception e) {
            log.error("新增用户失败", e);
            return ApiResponse.error("400", "用户新增异常:" + e.getMessage());
        }
        return ApiResponse.ok();
    }

    /**
     * 增加流量
     */
    public void incrementData(String dataCountStr, Integer userId){
        Long dataCount = null;
        if(org.apache.commons.lang3.StringUtils.isEmpty(dataCountStr)){
            dataCount = 0L;
        }else {
            dataCount = Long.valueOf(dataCountStr);
        }
        User user = new User();
        user.setId(userId);
        user.setDataUsage(user.getDataUsage()+dataCount);
        userDao.updateById(user);
    }
    /**
     * 更新用户
     *
     * @param user
     * @return
     */
    public ApiResponse updateUser(User user) {
        Integer id = user.getId();
        if(WebCurrentData.getUser().getUserType()>0){
            return ApiResponse.error("403", "您没有权限用户信息");
        }
        User old = userDao.getUserByUserName(user.getUsername(), id);
        if (old != null) {
            log.info("用户[更新]，结果=用户名已存在！");
            return ApiResponse.error("400", "用户名已存在");
        }
        String username = user.getUsername();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(user.getPassword())) {
            String password = DigestUtils.Md5(username, user.getPassword());
            user.setPassword(password);
        }else {
            user.setPassword(null);
        }
        userDao.updateById(user);
        return ApiResponse.ok();
    }

    /**
     * 根据id查询用户
     *
     * @param id
     * @return
     */
    public User getUserById(Integer id) {
        return userDao.selectById(id);
    }


    /**
     * 删除用户
     *
     * @param id
     * @return
     */
    public ApiResponse delUser(Integer id) {
        if(WebCurrentData.getUser().getUserType()>0&&!WebCurrentData.getUserId().equals(id)){
            return ApiResponse.error("403", "您没有权限修改其他用户");
        }
        if (id.equals(WebCurrentData.getUserId())) {
            return ApiResponse.error("400", "您不能删除自己");
        }
        userDao.deleteUser(id);
        return ApiResponse.ok();
    }

    /**
     * 禁用用户
     *
     * @param id
     * @return
     */
    public ApiResponse disableUser(Integer id) {
        if(WebCurrentData.getUser().getUserType()>0&&!WebCurrentData.getUserId().equals(id)){
            return ApiResponse.error("403", "您没有权限修改其他用户");
        }
        if (id.equals(WebCurrentData.getUserId())) {
            return ApiResponse.error("400", "您不能禁用自己");
        }
        userDao.updateDisable(true, id);
        return ApiResponse.ok();
    }


    /**
     * 禁用用户
     *
     * @param id
     * @return
     */
    public ApiResponse disableUserById(Integer id) {
        userDao.updateDisable(true, id);
        return ApiResponse.ok();
    }

    /**
     * 启用用户
     *
     * @param id
     * @return
     */
    public ApiResponse enableUser(Integer id) {
        if(WebCurrentData.getUser().getUserType()>0&&!WebCurrentData.getUserId().equals(id)){
            return ApiResponse.error("403", "您没有权限修改其他用户");
        }
        if (id.equals(WebCurrentData.getUserId())) {
            return ApiResponse.error("400", "您不能启用自己");
        }
        userDao.updateDisable(false, id);
        return ApiResponse.ok();
    }

    /**
     * 根据用户名查询用户
     *
     * @param userName
     * @return
     */
    public User findByUserName(String userName) {
        return userDao.getUserByUserName(userName, null);
    }


    /**
     * 更新密码
     *
     * @param userName
     * @param password
     */
    public void updatePwd(String userName, String password) {
        password = DigestUtils.Md5(userName, password);
        userDao.updatePwd(userName, password);
    }


    public List<User> findExpireUserList(){
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery().lt(User::getExpireTime, DateUtils.addDays(new Date(), 1))
                .eq(User::getDeleted, false).eq(User::getDisabled, false);
        return userDao.selectList(queryWrapper);
    }

    /**
     * 获取启用的用户
     * @return
     */
    public List<User> findEnableUserList(){
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery()
                .eq(User::getDeleted, false).eq(User::getDisabled, false);
        return userDao.selectList(queryWrapper);
    }

    /**
     * 禁用用户所有端口
     *
     * @param userId
     * @return
     */
    public ApiResponse resetUserFlow(Integer userId) {
        List<UserPortForward> forwardList = userPortForwardService.findUserForwardList(userId);
        for (UserPortForward userPortForward : forwardList) {
            Server server = serverDao.selectById(userPortForward.getServerId());
            remoteForwardService.resetFlowCount(server, userPortForward.getRemoteIp(), userPortForward.getRemotePort());
        }
        forwardFlowService.deleteUserFlow(userId);
        return ApiResponse.ok();
    }
}
