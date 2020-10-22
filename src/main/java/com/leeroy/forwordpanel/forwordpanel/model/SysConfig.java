package com.leeroy.forwordpanel.forwordpanel.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

/**
 * 系统配置
 */
@Entity
@Data
public class SysConfig {

    @Id
    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;


    /**
     * tg
     */
    private String telegram;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱
     */
    private String qq;

    /**
     * 当前版本
     */
    private String version;


}
