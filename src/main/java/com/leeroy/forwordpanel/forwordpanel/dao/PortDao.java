package com.leeroy.forwordpanel.forwordpanel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leeroy.forwordpanel.forwordpanel.model.Port;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PortDao extends BaseMapper<Port> {


    @Update("update port set disabled=#{disabled} where id=#{id}")
    void updateDisable(Boolean disabled, Integer id);

    @Select("select a.* from port a where not exists (select 1 from user_port up where up.port_id = a.id and up.deleted=0) and a.deleted=0 and a.server_id = #{serverId} order by a.create_time desc")
    List<Port> selectServerFreePort(Integer serverId);

    @Update("delete from port where local_port>= #{startPort} and local_port <=#{endPort} and server_id = #{serverId}")
    void batchDelete(Integer startPort, Integer endPort, Integer serverId);
}
