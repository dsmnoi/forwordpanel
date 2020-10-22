package com.leeroy.forwordpanel.forwordpanel.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leeroy.forwordpanel.forwordpanel.model.Port;
import com.leeroy.forwordpanel.forwordpanel.model.Server;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ServerDao extends BaseMapper<Server> {


    @Update("update server set disabled=#{disabled} where id=#{id}")
    void updateDisable(Boolean disabled, Integer id);


    @Select("select s.* from server s, user_server us where s.id = us.server_id and s.deleted = false and us.deleted = false and us.user_id =#{userId} ")
    List<Server> selectUserServer(Integer userId);

    @Select("select s.* from server s where exists ( select 1 from user_port_forward upf where s.id = upf.server_id and s.deleted = false and upf.deleted = false and upf.user_id =#{userId}) ")
    List<Server> selectForwardServer(Integer userId);
}
