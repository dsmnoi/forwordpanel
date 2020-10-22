package com.leeroy.forwordpanel.forwordpanel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leeroy.forwordpanel.forwordpanel.model.Port;
import com.leeroy.forwordpanel.forwordpanel.model.UserPort;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserPortDao extends BaseMapper<UserPort> {


    @Update("update user_port set disabled=#{disabled} where id=#{id}")
    void updateDisable(Boolean disabled, Integer id);

    @Select("select p.* from user_port up, port p where up.port_id = p.id and up.deleted = false and p.deleted = false and p.local_port=#{localPort} and p.server_id =#{serverId}")
    List<Port> selectByLocalPort(Integer localPort, Integer serverId);
}
