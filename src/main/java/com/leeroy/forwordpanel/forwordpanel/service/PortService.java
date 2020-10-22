package com.leeroy.forwordpanel.forwordpanel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.dao.PortDao;
import com.leeroy.forwordpanel.forwordpanel.dao.ServerDao;
import com.leeroy.forwordpanel.forwordpanel.dao.UserPortDao;
import com.leeroy.forwordpanel.forwordpanel.dto.PageRequest;
import com.leeroy.forwordpanel.forwordpanel.dto.PortAddDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.PortDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.PortPageRequest;
import com.leeroy.forwordpanel.forwordpanel.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortService {

    @Autowired
    private PortDao portDao;

    @Autowired
    private ServerDao serverDao;

    @Autowired
    private UserPortDao userPortDao;

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);


    /**
     * 保存clash
     */
    public ApiResponse save(Port clash) {
        if (StringUtils.isEmpty(clash.getId())) {
            clash.setCreateTime(new Date());
            clash.setDeleted(false);
            portDao.insert(clash);
        } else {
            LambdaQueryWrapper<Port> queryWrapper = Wrappers.<Port>lambdaQuery()
                    .eq(Port::getDeleted, false)
                    .eq(Port::getServerId, clash.getServerId())
                    .eq(Port::getLocalPort, clash.getLocalPort()).ne(Port::getId, clash.getId());
            List<Port> port = portDao.selectList(queryWrapper);
            if (!CollectionUtils.isEmpty(port)) {
                return ApiResponse.error("400", "本地端口已存在");
            }
            Port existPort = portDao.selectById(clash.getId());
            BeanUtils.copyProperties(clash, existPort);
            existPort.setUpdateTime(new Date());
            portDao.updateById(existPort);
        }
        return ApiResponse.ok();
    }

    /**
     * 保存clash
     */
    public ApiResponse save(PortAddDTO portAddDTO) {
        String localPort = portAddDTO.getLocalPort();
        String internetPort = portAddDTO.getInternetPort();
        if(!checkPort(localPort)||(!StringUtils.isEmpty(internetPort)&&!checkPort(internetPort))){
            return ApiResponse.error("400", "端口格式错误");
        }
        if(localPort.indexOf("-")>0&&(!StringUtils.isEmpty(internetPort)&&internetPort.indexOf("-")<=0)){
            return ApiResponse.error("400", "端口格式错误");
        }
        if(localPort.indexOf("-")<=0&&(!StringUtils.isEmpty(internetPort)&&internetPort.indexOf("-")>0)){
            return ApiResponse.error("400", "端口格式错误");
        }
        if(getRange(localPort)>65535){
            return ApiResponse.error("400", "端口一次最多新增65535个");
        }
        if(!StringUtils.isEmpty(internetPort)&&getRange(localPort)!=getRange(internetPort)){
            return ApiResponse.error("400", "端口区间范围不匹配");
        }
        List<Integer> localPortList = getPortAddList(localPort);
        List<Integer> internetPortList = getPortAddList(internetPort);
        internetPortList = CollectionUtils.isEmpty(internetPortList)?localPortList:internetPortList;
        for (int i = 0; i < localPortList.size(); i++) {
            Integer addLocalPort = localPortList.get(i);
            Integer addInternetPort = internetPortList.get(i);
            executorService.execute(() -> {
                LambdaQueryWrapper<Port> queryWrapper = Wrappers.<Port>lambdaQuery()
                        .eq(Port::getDeleted, false)
                        .eq(Port::getServerId, portAddDTO.getServerId())
                        .eq(Port::getLocalPort, addLocalPort);
                List<Port> port = portDao.selectList(queryWrapper);
                if (!CollectionUtils.isEmpty(port)) {
                    return;
                }
                Port addPort = new Port();
                addPort.setServerId(portAddDTO.getServerId());
                addPort.setLocalPort(addLocalPort);
                addPort.setInternetPort(addInternetPort);
                addPort.setDeleted(false);
                addPort.setCreateTime(new Date());
                portDao.insert(addPort);
            });

        }
        return ApiResponse.ok();
    }

    /**
     * 保存clash
     */
    public ApiResponse batchDelete(PortAddDTO portAddDTO) {
        String localPort = portAddDTO.getLocalPort();
        if(!checkPort(localPort)){
            return ApiResponse.error("400", "端口格式错误");
        }
        if(localPort.indexOf("-")<=0){
            return ApiResponse.error("400", "端口格式错误");
        }
        if(getRange(localPort)>65535){
            return ApiResponse.error("400", "端口一次最多新增65535个");
        }
        List<Integer> localPortList = getPortAddList(localPort);
        for (int i = 0; i < localPortList.size(); i++) {
            Integer addLocalPort = localPortList.get(i);
            executorService.execute(() -> {
                List<Port> portList = userPortDao.selectByLocalPort(addLocalPort, portAddDTO.getServerId());
                if (!CollectionUtils.isEmpty(portList)) {
                    return;
                }
                LambdaQueryWrapper<Port> queryWrapper = Wrappers.<Port>lambdaQuery().eq(Port::getServerId, portAddDTO.getServerId())
                        .eq(Port::getLocalPort, addLocalPort)
                        .eq(Port::getDeleted, false);
                Port userPort = new Port();
                userPort.setDeleted(true);
                portDao.update(userPort,queryWrapper);
            });

        }
        return ApiResponse.ok();
    }

    /**
     * 获取端口列表
     * @param port
     * @return
     */
    private List<Integer> getPortAddList(String port){
        if(StringUtils.isEmpty(port)){
            return new ArrayList<>();
        }
        String[] ports = port.split("-");
        List<Integer> portList = new ArrayList<>();
        if(ports.length==1) {
            portList.add(Integer.valueOf(ports[0]));
            return portList;
        }
        Integer startPort = Integer.valueOf(ports[0]);
        Integer endPort = Integer.valueOf(ports[1]);
        for (int i = startPort; i <= endPort ; i++) {
            portList.add(i);
        }
        return portList;
    }

    /**
     * 端口区间长度
     * @param port
     * @return
     */
    private int getRange(String port){
        if(port.indexOf("-")>0){
            String[] ports = port.split("-");
            return Integer.valueOf(ports[1])-Integer.valueOf(ports[0]);
        }else {
            return 1;
        }
    }

    /**
     * 端口检查
     * @param port
     * @return
     */
    private boolean checkPort(String port) {
        if (NumberUtils.isDigits(port)) {
            return true;
        }
        if (port.indexOf("-") > 0) {
            String[] ports = port.split("-");
            if(ports.length>2){
                return false;
            }
            for (String item : ports) {
                if (!NumberUtils.isDigits(item)) {
                    return false;
                }
            }
            if(Integer.valueOf(ports[1])<Integer.valueOf(ports[0])){
                return false;
            }
        }else{
            return false;
        }
        return true;
    }


    /**
     * 查询clash列表
     *
     * @return
     */
    public PageInfo findList(PageRequest pageRequest) {
        LambdaQueryWrapper<Port> queryWrapper = Wrappers.<Port>lambdaQuery().eq(Port::getDeleted, false).orderByDesc(Port::getCreateTime);
        Page page = PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize());
        List<Port> portList = portDao.selectList(queryWrapper);
        List<PortDTO> portDTOList = new ArrayList<>();
        for (Port port : portList) {
            PortDTO portDTO = new PortDTO();
            BeanUtils.copyProperties(port, portDTO);
            Server server = serverDao.selectById(port.getServerId());
            if(server!=null){
                portDTO.setServerName(server.getServerName());
                portDTO.setServerHost(server.getHost());
            }
            portDTOList.add(portDTO);
        }
        PageInfo pageInfo = page.toPageInfo();
        pageInfo.setList(portDTOList);
        return pageInfo;
    }

    /**
     * 查询clash列表
     *
     * @return
     */
    public PageInfo<Port> findListByServer(PortPageRequest pageRequest) {
        LambdaQueryWrapper<Port> queryWrapper = Wrappers.<Port>lambdaQuery().eq(Port::getDeleted, false).eq(Port::getServerId, pageRequest.getServerId()).orderByDesc(Port::getCreateTime);
        Page<Port> page = PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize());
        List<Port> portList = portDao.selectList(queryWrapper);
        PageInfo<Port> pageInfo = page.toPageInfo();
        pageInfo.setList(portList);
        return pageInfo;
    }

    /**
     * 查询clash列表
     *
     * @return
     */
    public List<PortDTO> findFreePortList(Integer serverId) {
        //todo serverId权限校验;
        List<Port> portList = portDao.selectServerFreePort(serverId);
        List<PortDTO> portDTOList = new ArrayList<>();
        for (Port port : portList) {
            PortDTO portDTO = new PortDTO();
            BeanUtils.copyProperties(port, portDTO);
            Server server = serverDao.selectById(port.getServerId());
            portDTO.setServerName(server.getServerName());
            portDTOList.add(portDTO);
        }
        return portDTOList;
    }


    public PageInfo<PortDTO> findFreePortPage(PortPageRequest pageRequest) {
        Page<PortDTO> page = PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize());
        List<Port> portList = portDao.selectServerFreePort(pageRequest.getServerId());
        List<PortDTO> portDTOList = new ArrayList<>();
        for (Port port : portList) {
            PortDTO portDTO = new PortDTO();
            BeanUtils.copyProperties(port, portDTO);
            Server server = serverDao.selectById(port.getServerId());
            portDTO.setServerName(server.getServerName());
            portDTOList.add(portDTO);
        }
        PageInfo<PortDTO> pageInfo = page.toPageInfo();
        pageInfo.setList(portDTOList);
        return pageInfo;
    }

    /**
     * 删除clash
     */
    public ApiResponse delete(Integer id) {
        LambdaQueryWrapper<UserPort> queryWrapper = Wrappers.<UserPort>lambdaQuery().eq(UserPort::getPortId, id)
                .eq(UserPort::getDeleted, false);
        List<UserPort> userPorts = userPortDao.selectList(queryWrapper);
        if(!CollectionUtils.isEmpty(userPorts)){
            return ApiResponse.error("403", "端口已授权给用户, 请先删除");
        }
        Port userPort = new Port();
        userPort.setId(id);
        userPort.setDeleted(true);
        portDao.updateById(userPort);
        return ApiResponse.ok();
    }

}
