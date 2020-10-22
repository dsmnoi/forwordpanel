package com.leeroy.forwordpanel.forwordpanel.service;

import com.leeroy.forwordpanel.forwordpanel.common.util.remotessh.SSHCommandExecutor;
import com.leeroy.forwordpanel.forwordpanel.common.util.remotessh.SessionPool;
import com.leeroy.forwordpanel.forwordpanel.model.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RemoteForwardService {

    private Map<Server, SSHCommandExecutor> sshCommandExecutorMap = new ConcurrentHashMap<>();

    /**
     * 获取sshExecutor
     * @param server
     * @return
     */
    public SSHCommandExecutor getSshExecutor(Server server) {
        SSHCommandExecutor sshCommandExecutor = sshCommandExecutorMap.get(server);
        if(sshCommandExecutor==null){
            sshCommandExecutor = new SSHCommandExecutor(server, new SessionPool(server));
            sshCommandExecutorMap.put(server, sshCommandExecutor);
        }
        return sshCommandExecutor;
    }



    /**
     * 添加转发
     *
     * @param remoteHost
     * @param remotePort
     * @param localPort
     */
    public void addForward(Server server,String remoteHost,  Integer remotePort,  Integer localPort) {
        SSHCommandExecutor sshExecutor = getSshExecutor(server);
        stopForward(server, remoteHost, remotePort, localPort);
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("@localport", localPort.toString());
        paramMap.put("@remoteIp", remoteHost);
        paramMap.put("@remoteport", remotePort.toString());
        sshExecutor.executeScript("addForward.sh", paramMap);
    }


    /**
     * 获取转发流量
     *
     * @param server
     * @param remotePort
     * @return
     */
    public String getPortFlow(Server server, String remoteHost, Integer remotePort) {
        SSHCommandExecutor sshExecutor = getSshExecutor(server);
        if(StringUtils.isEmpty(remoteHost)){
            return "0";
        }
        sshExecutor.execute(String.format("iptables -n -v -L -t filter -x | grep %s | awk '{print $2}'", remoteHost));
        String result = sshExecutor.getResult();
        result = result.replaceAll("\n", "");
        log.info("flow:{}", result);
        return StringUtils.isEmpty(result) ? "0" : result;
    }


    /**
     * 获取转发流量
     *
     * @param remoteHostList
     * @return
     */
    public Map<String, String> getPortFlowMap(Server server, List<String> remoteHostList) {
        SSHCommandExecutor sshExecutor = getSshExecutor(server);
        Map<String, String> resultMap = new HashMap<>();
        Map<String, String> commandList = new HashMap<>();
        for (String remoteHost : remoteHostList) {
            if(StringUtils.isEmpty(remoteHost)){
                resultMap.put(remoteHost, "0");
            }else {
                commandList.put(remoteHost,String.format("iptables -n -v -L -t filter -x | grep %s | awk '{print $2}'", remoteHost));
            }
        }

        sshExecutor.execute(commandList.values().toArray(new String[]{}));
        Vector<String> resultSet = sshExecutor.getResultSet();
        try {
            for (int i = 0; i < resultSet.size(); i++) {
                resultMap.put((String) commandList.keySet().toArray()[i],resultSet.get(i));
            }
        }catch (Exception e){
            log.error("流量监控重复");
        }
        return resultMap;
    }


    /**
     * 重置流量
     *
     * @param remoteHost
     * @param remotePort
     */
    public void resetFlowCount(Server server, String remoteHost, Integer remotePort) {
        SSHCommandExecutor sshExecutor = getSshExecutor(server);
        sshExecutor.execute(String.format("flowCount=(`iptables -n -v -L -t filter -x | grep %s |wc -l`);for((i=1;i<=$flowCount;i++));do iptables -D FORWARD -s @remoteIp;done;iptables -I FORWARD -s %s", remoteHost,remoteHost));
    }

    /**
     * 获取上次重启时间
      * @param server
     * @return
     */
    public String getLastRestart(Server server){
        SSHCommandExecutor sshExecutor = getSshExecutor(server);
        sshExecutor.execute("who -b");
        return sshExecutor.getResult();
    }


    /**
     * 重置流量
     *
     * @param remoteHost
     * @param remotePort
     */
    public void deleteFlowCount(Server server, String remoteHost, Integer remotePort) {
        SSHCommandExecutor sshExecutor = getSshExecutor(server);
        sshExecutor.execute(String.format("iptables -D FORWARD -p tcp --dport %s -d %s", remotePort, remoteHost), String.format("iptables -D FORWARD -p udp --dport %s -d %s", remotePort, remoteHost));
    }


    /**
     * 停止转发
     *
     * @param remoteHost
     * @param remotePort
     * @param localPort
     */
    public void stopForward(Server server, String remoteHost, Integer remotePort, Integer localPort) {
        SSHCommandExecutor sshExecutor = getSshExecutor(server);
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("@localPort", localPort.toString());
        sshExecutor.executeScript("stopForward.sh", paramMap);
    }

    private String getForwardKey(String remoteHost, Integer remotePort, Integer localPort) {
        return remoteHost + remotePort + localPort;
    }


    public static void main(String[] args) {
        String a = " ";
        System.out.println();
    }
}
