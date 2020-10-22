package com.leeroy.forwordpanel.forwordpanel.dto;

import lombok.Data;

/**
 * @program: forwordpanel
 * @description: 中转分页查询
 * @author: liruifeng
 * @created: 2020/10/13 09:24
 */
@Data
public class UserPortForwardPageReq extends PageRequest {
    private Integer userId;
    private Integer serverId;
}
