package com.leeroy.forwordpanel.forwordpanel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.leeroy.forwordpanel.forwordpanel.common.WebCurrentData;
import com.leeroy.forwordpanel.forwordpanel.common.enums.ForwardStatusEnum;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.common.util.BeanCopyUtil;
import com.leeroy.forwordpanel.forwordpanel.dao.*;
import com.leeroy.forwordpanel.forwordpanel.dto.UserPortForwardDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.UserPortForwardPageReq;
import com.leeroy.forwordpanel.forwordpanel.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户中转
 */
@Slf4j
@Service
public class UserPortForwardService {

    @Autowired
    private UserPortForwardDao userPortForwardDao;

    @Autowired
    private UserPortDao userPortDao;

    @Autowired
    private PortDao portDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ServerDao serverDao;

    @Autowired
    private RemoteForwardService forwardService;

    @Autowired
    ForwardFlowService forwardFlowService;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 查询用户中转
     *
     * @return
     */
    public ApiResponse getUserForwardList(UserPortForwardPageReq pageRequest) {
        Integer userId = WebCurrentData.getUserId();
        LambdaQueryWrapper<UserPortForward> queryWrapper;
        if (WebCurrentData.getUser().getUserType() == 0) {
            queryWrapper = Wrappers.<UserPortForward>lambdaQuery()
                    .eq(UserPortForward::getDeleted, false)
            .eq(UserPortForward::getUserId, pageRequest.getUserId()==null?userId:pageRequest.getUserId())
            .eq(UserPortForward::getServerId, pageRequest.getServerId())
                    .orderByDesc(UserPortForward::getCreateTime)
            ;
        } else {
            queryWrapper = Wrappers.<UserPortForward>lambdaQuery().eq(UserPortForward::getUserId, userId)
                    .eq(UserPortForward::getServerId, pageRequest.getServerId())
                    .eq(UserPortForward::getDeleted, false)
                    .orderByDesc(UserPortForward::getCreateTime);
        }
        Page page = PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize());
        List<UserPortForward> userPortForwardList = userPortForwardDao.selectList(queryWrapper);
        List<UserPortForwardDTO> userPortForwardDTOList = BeanCopyUtil.copyListProperties(userPortForwardList, UserPortForwardDTO::new);
        for (UserPortForwardDTO userPortForward : userPortForwardDTOList) {
            Port port = portDao.selectById(userPortForward.getPortId());
            if (port != null) {
                userPortForward.setLocalPort(port.getLocalPort());
            }
            User user = userDao.selectById(userPortForward.getUserId());
            if (user != null) {
                userPortForward.setUsername(user.getUsername());
            }
            Server server = serverDao.selectById(userPortForward.getServerId());
            if (server != null) {
                userPortForward.setServerHost(server.getHost());
                userPortForward.setServerName(server.getServerName());
            }
            userPortForward.setInternetPort(port.getInternetPort());
            Long forwardFlowTotal = forwardFlowService.getForwardFlowTotal(userPortForward.getId());
            userPortForward.setDataUsage(forwardFlowTotal);
        }
        PageInfo pageInfo = page.toPageInfo();
        pageInfo.setList(userPortForwardDTOList);
        return ApiResponse.ok(pageInfo);
    }

    /**
     * 查询用户流量转发
     *
     * @param userId
     * @return
     */
    public List<UserPortForward> findUserForwardList(Integer userId) {
        LambdaQueryWrapper<UserPortForward> queryWrapper;
        queryWrapper = Wrappers.<UserPortForward>lambdaQuery().eq(UserPortForward::getUserId, userId)
                .eq(UserPortForward::getDeleted, false).isNotNull(UserPortForward::getRemoteIp);
        return userPortForwardDao.selectList(queryWrapper);
    }

    /**
     * 获取服务器启动的中转
     * @param serverId
     * @return
     */
    public List<UserPortForward> findServerEnabledForwardList(Integer serverId) {
        LambdaQueryWrapper<UserPortForward> queryWrapper;
        queryWrapper = Wrappers.<UserPortForward>lambdaQuery().eq(UserPortForward::getServerId, serverId)
                .eq(UserPortForward::getDeleted, false)
                .eq(UserPortForward::getDisabled, false);
        return userPortForwardDao.selectList(queryWrapper);
    }


    /**
     * 获取流量
     *
     * @return
     */
    public Map<Integer, String> getPortFlowMap(List<UserPortForward> userPortForwardDTOList) {
        Map<Integer, String> result = new HashMap<>();
        //根据server分组
        Map<Integer, List<UserPortForward>> portForwardMap = userPortForwardDTOList.stream().collect(Collectors.groupingBy(UserPortForward::getServerId));
        for (Integer serverId : portForwardMap.keySet()) {
            Server server = serverDao.selectById(serverId);
            List<UserPortForward> userPortForwardDTOS = portForwardMap.get(serverId);
            List<String> remoteHostList = userPortForwardDTOS.stream().map(UserPortForward::getRemoteIp).collect(Collectors.toList());
            Map<String, String> portFlowMap = forwardService.getPortFlowMap(server, remoteHostList);
            for (UserPortForward userPortForward : userPortForwardDTOS) {
                result.put(userPortForward.getId(), portFlowMap.get(userPortForward.getRemoteIp()));
            }
        }
        return result;
    }


    /**
     * 创建端口转发
     *
     * @param portId
     * @param userId
     */
    public void createUserPortForward(Integer serverId, Integer portId, Integer userId) {
        LambdaQueryWrapper<UserPortForward> queryWrapper = Wrappers.<UserPortForward>lambdaQuery().eq(UserPortForward::getUserId, userId)
                .eq(UserPortForward::getPortId, portId).eq(UserPortForward::getDeleted, false);
        UserPortForward exist = userPortForwardDao.selectOne(queryWrapper);
        if (exist == null) {
            UserPortForward userPortForward = new UserPortForward();
            userPortForward.setPortId(portId);
            userPortForward.setServerId(serverId);
            userPortForward.setUserId(userId);
            userPortForward.setDeleted(false);
            userPortForward.setCreateTime(new Date());
            userPortForward.setDisabled(true);
            userPortForward.setState(ForwardStatusEnum.INIT.getCode());
            userPortForwardDao.insert(userPortForward);
        }
    }

    /**
     * 删除中转
     *
     * @param portId
     * @param userId
     */
    public void deleteUserPortForward(Integer portId, Integer userId) {
        LambdaQueryWrapper<UserPortForward> queryWrapper = Wrappers.<UserPortForward>lambdaQuery().eq(UserPortForward::getUserId, userId)
                .eq(UserPortForward::getPortId, portId).eq(UserPortForward::getDeleted, false).eq(UserPortForward::getDisabled, false);
        UserPortForward portForward = userPortForwardDao.selectOne(queryWrapper);
        if (portForward != null) {
            //停止中转
            Port port = portDao.selectById(portId);
            Server server = serverDao.selectById(portForward.getServerId());
            forwardService.stopForward(server, portForward.getRemoteIp(), portForward.getRemotePort(), port.getLocalPort());
        }
        //删除中转记录
        queryWrapper = Wrappers.<UserPortForward>lambdaQuery().eq(UserPortForward::getUserId, userId)
                .eq(UserPortForward::getPortId, portId);
        UserPortForward userPortForward = new UserPortForward();
        userPortForward.setDeleted(true);
        userPortForwardDao.update(userPortForward, queryWrapper);
    }

    /**
     * 开启中转
     *
     * @param userPortForward
     * @return
     */
    public ApiResponse startForward(UserPortForward userPortForward, boolean needLogin) {
        ApiResponse apiResponse = permissionCheck(userPortForward.getUserId());
        if (!apiResponse.getSuccess()) {
            return apiResponse;
        }
        User user = WebCurrentData.getUser();
        Integer userId = WebCurrentData.getUserId();
        //检查用户是否拥有此端口
        LambdaQueryWrapper<UserPort> userPortQueryWrapper = Wrappers.<UserPort>lambdaQuery().eq(UserPort::getUserId, userId)
                .eq(UserPort::getDeleted, false);
        List<UserPort> existUserPortList = userPortDao.selectList(userPortQueryWrapper);
        Boolean hasPort = false;
        for (UserPort userPort : existUserPortList) {
            if (userPort.getPortId().equals(userPortForward.getPortId())) {
                if (userPort.getDisabled()) {
                    return ApiResponse.error("403", "端口已被管理员禁用,请联系管理员");
                }
                hasPort = true;
                break;
            }
        }
        if (needLogin && user.getUserType() > 0) {
            if (!hasPort) {
                return ApiResponse.error("403", "用户没有此端口的权限");
            }
        }
        if (StringUtils.isBlank(userPortForward.getRemoteHost()) || userPortForward.getRemotePort() == null) {
            return ApiResponse.error("401", "请填写域名(IP)|端口");
        }
        LambdaQueryWrapper<UserPortForward> queryWrapper = Wrappers.<UserPortForward>lambdaQuery()
                .ne(UserPortForward::getUserId, userPortForward.getUserId())
                .eq(UserPortForward::getServerId, userPortForward.getServerId())
                .eq(UserPortForward::getDeleted, false)
                .eq(UserPortForward::getRemoteHost, userPortForward.getRemoteHost());
        if (userPortForward.getId() != null) {
            queryWrapper = queryWrapper.ne(UserPortForward::getId, userPortForward.getId());
        }
        List<UserPortForward> portForwardList = userPortForwardDao.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(portForwardList)) {
            return ApiResponse.error("401", "转发已存在,请使用已存在的转发");
        }
        //查询该端口已经存在的转发
        queryWrapper = Wrappers.<UserPortForward>lambdaQuery()
                .eq(UserPortForward::getPortId, userPortForward.getPortId()).eq(UserPortForward::getDeleted, false);
        UserPortForward portForward = userPortForwardDao.selectOne(queryWrapper);
        //收集之前的流量
        forwardFlowService.collectForwardFlow(Lists.newArrayList(portForward));
        if (!portForward.getDisabled()) {
            //停止中转
            Port port = portDao.selectById(portForward.getPortId());
            Server server = serverDao.selectById(portForward.getServerId());
            forwardService.stopForward(server, portForward.getRemoteIp(), portForward.getRemotePort(), port.getLocalPort());
        }
        String remoteIp = getRemoteIp(userPortForward.getRemoteHost());
        if (StringUtils.isBlank(remoteIp)) {
            return ApiResponse.error("401", "域名解析错误");
        }
        userPortForward.setRemoteIp(remoteIp);

        Port port = portDao.selectById(userPortForward.getPortId());
        Server server = serverDao.selectById(userPortForward.getServerId());
        //开始新的中转
        forwardService.addForward(server, userPortForward.getRemoteIp(), userPortForward.getRemotePort(), port.getLocalPort());
        //更新中转信息
        portForward.setUpdateTime(new Date());
        portForward.setRemoteIp(userPortForward.getRemoteIp());
        portForward.setRemoteHost(userPortForward.getRemoteHost());
        portForward.setRemotePort(userPortForward.getRemotePort());
        portForward.setDisabled(false);
        userPortForwardDao.updateById(portForward);
        return ApiResponse.ok();
    }

    /**
     * 停用中转
     *
     * @return
     */
    public ApiResponse stopForward(UserPortForward userPortForward) {
        Integer userId = userPortForward.getUserId();
        Port port = portDao.selectById(userPortForward.getPortId());
        ApiResponse apiResponse = permissionCheck(userId);
        if (!apiResponse.getSuccess()) {
            return apiResponse;
        }
        LambdaQueryWrapper<UserPortForward> queryWrapper = Wrappers.<UserPortForward>lambdaQuery()
                .eq(UserPortForward::getPortId, userPortForward.getPortId()).eq(UserPortForward::getDeleted, false);
        UserPortForward portForward = userPortForwardDao.selectOne(queryWrapper);
        if (!portForward.getDisabled()) {
            //停止中转
            Server server = serverDao.selectById(portForward.getServerId());
            forwardService.stopForward(server, portForward.getRemoteIp(), portForward.getRemotePort(), port.getLocalPort());
        }
        //停止中转记录
        userPortForwardDao.updateDisable(true, portForward.getId());
        return ApiResponse.ok();
    }

    /**
     * 越权检查
     *
     * @param userId
     * @return
     */
    private ApiResponse permissionCheck(Integer userId) {
        if (WebCurrentData.getUser() != null && WebCurrentData.getUser().getUserType() > 0 && !WebCurrentData.getUserId().equals(userId)) {
            return ApiResponse.error("403", "您没有权限执行该操作");
        }
        return ApiResponse.ok();
    }

    public static void main(String[] args) throws UnknownHostException {
        System.out.println(getRemoteIp("hk.xiaolifly.xyz"));
    }

    /**
     * 获取域名ip
     *
     * @param remoteHost
     * @return
     * @throws Exception
     */
    public static String getRemoteIp(String remoteHost) {
        if (isboolIp(remoteHost)) {
            return remoteHost;
        }
        try {
            InetAddress addr = InetAddress.getByName(remoteHost);
            return addr.getHostAddress();
        } catch (UnknownHostException e) {
            log.error("解析域名错误", e);
        }
        return null;
    }

    /**
     * 判断是否为合法IP * @return the ip
     */
    public static boolean isboolIp(String ipAddress) {
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }
    /**
     * 获取所有正在使用中的转发
     * @return
     */
    public List<UserPortForward> getUsingForwards(){
        LambdaQueryWrapper<UserPortForward> queryWrapper = Wrappers.<UserPortForward>lambdaQuery()
                .eq(UserPortForward::getDeleted, false).eq(UserPortForward::getDisabled, false);
        List<UserPortForward> userPortForwards = userPortForwardDao.selectList(queryWrapper);
        return userPortForwards;
    }
    /**
     * 更新转发信息
     */
    public void updateForward(UserPortForward userPortForward, Port port) {
        Server server = serverDao.selectById(userPortForward.getServerId());
        forwardService.stopForward(server,userPortForward.getRemoteIp(), userPortForward.getRemotePort(), port.getLocalPort());
        forwardService.addForward(server,userPortForward.getRemoteIp(), userPortForward.getRemotePort(), port.getLocalPort());
        userPortForwardDao.updateById(userPortForward);
    }

}
