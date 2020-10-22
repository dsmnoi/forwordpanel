package com.leeroy.forwordpanel.forwordpanel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leeroy.forwordpanel.forwordpanel.common.WebCurrentData;
import com.leeroy.forwordpanel.forwordpanel.common.enums.ServerStatusEnum;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.common.response.PageDataResult;
import com.leeroy.forwordpanel.forwordpanel.dao.ServerDao;
import com.leeroy.forwordpanel.forwordpanel.dao.UserPortDao;
import com.leeroy.forwordpanel.forwordpanel.dao.UserServerDao;
import com.leeroy.forwordpanel.forwordpanel.dto.PageRequest;
import com.leeroy.forwordpanel.forwordpanel.dto.UserPortDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.UserSearchDTO;
import com.leeroy.forwordpanel.forwordpanel.model.Server;
import com.leeroy.forwordpanel.forwordpanel.model.User;
import com.leeroy.forwordpanel.forwordpanel.model.UserPort;
import com.leeroy.forwordpanel.forwordpanel.model.UserServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ServerService {

    @Autowired
    private ServerDao serverDao;

    @Autowired
    private UserServerDao userServerDao;

    @Autowired
    private UserPortDao userPortDao;

    @Autowired
    private RemoteForwardService remoteForwardService;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 保存clash
     */
    public ApiResponse save(Server server) {
        if (StringUtils.isEmpty(server.getId())) {
            server.setCreateTime(new Date());
            server.setDeleted(false);
            server.setKey(UUID.randomUUID().toString());
            server.setOwnerId(WebCurrentData.getUserId());
            server.setState(ServerStatusEnum.INIT.getCode());
            serverDao.insert(server);
            UserServer userServer = new UserServer();
            userServer.setUserId(WebCurrentData.getUserId());
            userServer.setServerId(server.getId());
            userServer.setDeleted(false);
            userServerDao.insert(userServer);
            testConnect(server);
        } else {
            Server existPort = serverDao.selectById(server.getId());
            String password = StringUtils.isEmpty(server.getPassword())?existPort.getPassword():server.getPassword();
            BeanUtils.copyProperties(server, existPort);
            existPort.setUpdateTime(new Date());
            existPort.setState(ServerStatusEnum.INIT.getCode());
            serverDao.updateById(existPort);
            existPort.setPassword(password);
            testConnect(existPort);

        }
        return ApiResponse.ok();
    }

    /**
     * 尝试连接
     * @param server
     */
    public void testConnect(Server server){
        executorService.execute(() -> {
            String response = remoteForwardService.getLastRestart(server);
            if(StringUtils.isEmpty(response)){
                server.setState(ServerStatusEnum.CONNECT_FAIL.getCode());
            }else {
                server.setLastRebootTime(response);
                server.setState(ServerStatusEnum.ONLINE.getCode());
            }
            serverDao.updateById(server);
        });

    }

    public PageInfo<Server> getServerPage(PageRequest pageRequest) {
        Page<Server> page = PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize());
        List<Server> serverList = serverDao.selectUserServer(WebCurrentData.getUserId());
        serverList.stream().forEach(server -> {
            server.setPassword("******");
        });
        PageInfo<Server> pageInfo = page.toPageInfo();
        pageInfo.setList(serverList);
        return pageInfo;
    }


    public List<Server> getForwardServerList(Integer userId) {
        List<Server> serverList = serverDao.selectForwardServer(userId);
        serverList.stream().forEach(server -> {
            server.setPassword("******");
        });
        return serverList;
    }

    /**
     * 查询clash列表
     *
     * @return
     */
    public List<Server> findList() {
        LambdaQueryWrapper<Server> queryWrapper = Wrappers.<Server>lambdaQuery().eq(Server::getDeleted, false);
        List<Server> serverList = serverDao.selectList(queryWrapper);
        LambdaQueryWrapper<UserServer> userServerQueryWrapper = Wrappers.<UserServer>lambdaQuery().eq(UserServer::getDeleted, false);
        if (WebCurrentData.getUser().getUserType() > 0) {
            userServerQueryWrapper = userServerQueryWrapper.eq(UserServer::getUserId, WebCurrentData.getUserId());
        }
        List<UserServer> userServerList = userServerDao.selectList(userServerQueryWrapper);
        serverList = serverList.stream().filter(server -> {
            for (UserServer userServer : userServerList) {
                if (server.getId().equals(userServer.getServerId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        serverList.stream().forEach(server -> {
            server.setPassword(null);
        });
        return serverList;
    }

    /**
     * 查询clash列表
     *
     * @return
     */
    public List<Server> findListWithoutLogin() {
        LambdaQueryWrapper<Server> queryWrapper = Wrappers.<Server>lambdaQuery().eq(Server::getDeleted, false);
        List<Server> serverList = serverDao.selectList(queryWrapper);
        LambdaQueryWrapper<UserServer> userServerQueryWrapper = Wrappers.<UserServer>lambdaQuery().eq(UserServer::getDeleted, false);
        List<UserServer> userServerList = userServerDao.selectList(userServerQueryWrapper);
        serverList = serverList.stream().filter(server -> {
            for (UserServer userServer : userServerList) {
                if (server.getId().equals(userServer.getServerId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        return serverList;
    }

    /**
     * 删除clash
     */
    public ApiResponse delete(Integer id) {
        LambdaQueryWrapper<UserPort> queryWrapper = Wrappers.<UserPort>lambdaQuery().eq(UserPort::getServerId, id)
                .eq(UserPort::getDeleted, false);
        List<UserPort> userPorts = userPortDao.selectList(queryWrapper);
        if(!CollectionUtils.isEmpty(userPorts)){
            return ApiResponse.error("403", "服务端口已授权给用户, 请先删除");
        }
        Server userPort = new Server();
        userPort.setId(id);
        userPort.setDeleted(true);
        serverDao.updateById(userPort);
        LambdaQueryWrapper<UserServer> userServerQueryWrapper = Wrappers.<UserServer>lambdaQuery().eq(UserServer::getDeleted, false).eq(UserServer::getServerId, id);
        userServerDao.delete(userServerQueryWrapper);
        return ApiResponse.ok();
    }

    /**
     * 删除clash
     */
    public ApiResponse check(Integer id) {
        Server server = serverDao.selectById(id);
        String response = remoteForwardService.getLastRestart(server);
        if(StringUtils.isEmpty(response)){
            server.setState(ServerStatusEnum.CONNECT_FAIL.getCode());
        }else {
            server.setLastRebootTime(response);
            server.setState(ServerStatusEnum.ONLINE.getCode());
        }
        serverDao.updateById(server);
        return ApiResponse.ok();
    }

}
