package com.leeroy.forwordpanel.forwordpanel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leeroy.forwordpanel.forwordpanel.dao.ForwardFlowDao;
import com.leeroy.forwordpanel.forwordpanel.dao.PortDao;
import com.leeroy.forwordpanel.forwordpanel.dao.ServerDao;
import com.leeroy.forwordpanel.forwordpanel.dao.UserPortForwardDao;
import com.leeroy.forwordpanel.forwordpanel.dto.ForwardFlowDTO;
import com.leeroy.forwordpanel.forwordpanel.model.ForwardFlow;
import com.leeroy.forwordpanel.forwordpanel.model.Port;
import com.leeroy.forwordpanel.forwordpanel.model.Server;
import com.leeroy.forwordpanel.forwordpanel.model.UserPortForward;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @program: forwordpanel
 * @description: 中转记录
 * @author: liruifeng
 * @created: 2020/09/19 20:28
 */
@Service
public class ForwardFlowService {

    @Autowired
    private ForwardFlowDao forwardFlowDao;

    @Autowired
    private UserPortForwardService userPortForwardService;

    @Autowired
    private UserPortForwardDao userPortForwardDao;

    @Autowired
    private ServerDao serverDao;

    @Autowired
    private PortDao portDao;

    @Autowired
    private RemoteForwardService remoteForwardService;

    /**
     * 保存中转记录
     * @param forwardFlow
     */
    public void saveFlow(ForwardFlow forwardFlow){
        LambdaQueryWrapper<ForwardFlow> queryWrapper = Wrappers.<ForwardFlow>lambdaQuery()
                .eq(ForwardFlow::getForwardId, forwardFlow.getForwardId())
                .eq(ForwardFlow::getRemoteIp, forwardFlow.getRemoteIp()).eq(ForwardFlow::getRemotePort, forwardFlow.getRemotePort());
        ForwardFlow oldForwardFlow = forwardFlowDao.selectOne(queryWrapper);
        //存在旧的, 更新, 否则插入
        if(oldForwardFlow!=null){
            if(forwardFlow.getDataUsage()==null||forwardFlow.getDataUsage().equals(0L)){
                return;
            }
            oldForwardFlow.setDataUsage(oldForwardFlow.getDataUsage()+forwardFlow.getDataUsage());
            oldForwardFlow.setUpdateTime(new Date());
            forwardFlowDao.updateById(oldForwardFlow);
        }else {
            forwardFlow.setCreateTime(new Date());
            forwardFlowDao.insert(forwardFlow);
        }
    }

    /**
     * 收集中转流量
     * @param userForwardList
     */
    public void collectForwardFlow(List<UserPortForward> userForwardList) {
        //查询用户设置的转发
        Map<Integer, String> portFlowMap = userPortForwardService.getPortFlowMap(userForwardList);
        for (Map.Entry<Integer, String> forwardDTOStringEntry : portFlowMap.entrySet()) {
            Integer forwardId = forwardDTOStringEntry.getKey();
            UserPortForward forward = userPortForwardDao.selectById(forwardId);
            ForwardFlow forwardFlow = new ForwardFlow();
            BeanUtils.copyProperties(forward, forwardFlow);
            forwardFlow.setId(null);
            forwardFlow.setForwardId(forward.getId());
            String dataUsage = forwardDTOStringEntry.getValue() == null ? "0" : forwardDTOStringEntry.getValue();
            forwardFlow.setDataUsage(Long.valueOf(dataUsage));
            saveFlow(forwardFlow);
        }
        for (UserPortForward userPortForward : userForwardList) {
            Server server = serverDao.selectById(userPortForward.getServerId());
            remoteForwardService.resetFlowCount(server, userPortForward.getRemoteIp(), userPortForward.getRemotePort());
        }
    }

    /**
     * 删除用户统计
     * @param userId
     */
    public void deleteUserFlow(Integer userId) {
        LambdaQueryWrapper<ForwardFlow> queryWrapper = Wrappers.<ForwardFlow>lambdaQuery()
                .eq(ForwardFlow::getUserId, userId);
        forwardFlowDao.delete(queryWrapper);
    }

    /**
     * 获取用户流量
     * @param userId
     * @return
     */
    public Long getUserFlowTotal(Integer userId){
        LambdaQueryWrapper<ForwardFlow> queryWrapper = Wrappers.<ForwardFlow>lambdaQuery()
                .eq(ForwardFlow::getUserId, userId);
        List<ForwardFlow> forwardFlows = forwardFlowDao.selectList(queryWrapper);
        return forwardFlows.stream().collect(Collectors.summingLong(ForwardFlow::getDataUsage));
    }

    /**
     * 获取用户流量
     * @param forwardId
     * @return
     */
    public Long getForwardFlowTotal(Integer forwardId){
        LambdaQueryWrapper<ForwardFlow> queryWrapper = Wrappers.<ForwardFlow>lambdaQuery()
                .eq(ForwardFlow::getForwardId, forwardId);
        List<ForwardFlow> forwardFlows = forwardFlowDao.selectList(queryWrapper);
        return forwardFlows.stream().collect(Collectors.summingLong(ForwardFlow::getDataUsage));
    }



    /**
     * 流量使用详情
     * @param userId
     * @return
     */
    public List<ForwardFlowDTO> getUserFlow(Integer userId) {
        LambdaQueryWrapper<ForwardFlow> queryWrapper = Wrappers.<ForwardFlow>lambdaQuery()
                .eq(ForwardFlow::getUserId, userId);
        List<ForwardFlow> forwardFlows = forwardFlowDao.selectList(queryWrapper);
        List<ForwardFlowDTO> forwardFlowDTOList = new ArrayList<>();
        for (ForwardFlow forwardFlow : forwardFlows) {
            ForwardFlowDTO forwardFlowDTO = new ForwardFlowDTO();
            BeanUtils.copyProperties(forwardFlow, forwardFlowDTO);
            Port port = portDao.selectById(forwardFlow.getPortId());
            Server server = serverDao.selectById(forwardFlow.getServerId());
            forwardFlowDTO.setServerName(server.getServerName());
            forwardFlowDTO.setLocalPort(port.getLocalPort());
            forwardFlowDTO.setInternetPort(port.getInternetPort());
            forwardFlowDTOList.add(forwardFlowDTO);
        }
        forwardFlowDTOList = forwardFlowDTOList.stream().filter(forwardFlowDTO -> forwardFlowDTO.getDataUsage()!=null&&forwardFlowDTO.getDataUsage()>0).collect(Collectors.toList());
        return forwardFlowDTOList;
    }


    /**
     * 流量使用详情
     * @param forwardId
     * @return
     */
    public List<ForwardFlowDTO> getPortForwardFlow(Integer forwardId) {
        LambdaQueryWrapper<ForwardFlow> queryWrapper = Wrappers.<ForwardFlow>lambdaQuery()
                .eq(ForwardFlow::getForwardId, forwardId);
        List<ForwardFlow> forwardFlows = forwardFlowDao.selectList(queryWrapper);
        List<ForwardFlowDTO> forwardFlowDTOList = new ArrayList<>();
        for (ForwardFlow forwardFlow : forwardFlows) {
            ForwardFlowDTO forwardFlowDTO = new ForwardFlowDTO();
            BeanUtils.copyProperties(forwardFlow, forwardFlowDTO);
            Port port = portDao.selectById(forwardFlow.getPortId());
            Server server = serverDao.selectById(forwardFlow.getServerId());
            forwardFlowDTO.setServerName(server.getServerName());
            forwardFlowDTO.setLocalPort(port.getLocalPort());
            forwardFlowDTO.setInternetPort(port.getInternetPort());
            forwardFlowDTOList.add(forwardFlowDTO);
        }
        forwardFlowDTOList = forwardFlowDTOList.stream().filter(forwardFlowDTO -> forwardFlowDTO.getDataUsage()!=null&&forwardFlowDTO.getDataUsage()>0).collect(Collectors.toList());
        return forwardFlowDTOList;
    }

}
