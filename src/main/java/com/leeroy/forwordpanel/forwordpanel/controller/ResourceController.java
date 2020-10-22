package com.leeroy.forwordpanel.forwordpanel.controller;

import com.leeroy.forwordpanel.forwordpanel.common.WebCurrentData;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.model.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源管理
 */
@RestController
public class ResourceController {

    private static Map<Integer, List<Resource>> USER_TYPE_RESOURCE = new HashMap<>();

    /**
     * 初始化权限
     */
    static {
        List<Resource> adminResourceList = new ArrayList<>();
        adminResourceList.add(new Resource(1, "看板", "/dashboard", "dashboard"));
        adminResourceList.add(new Resource(2, "服务器管理", "/server", "el-icon-cpu"));
//        adminResourceList.add(new Resource(3, "端口管理", "/port", "el-icon-s-help"));
        adminResourceList.add(new Resource(4, "账号管理", "/user", "el-icon-user-solid"));
        adminResourceList.add(new Resource(5, "中转管理", "/forward", "el-icon-attract"));
        adminResourceList.add(new Resource(6, "配置文件", "/config", "el-icon-tickets"));
        adminResourceList.add(new Resource(7, "系统设置", "/system", "el-icon-s-tools"));
//        adminResourceList.add(new Resource(7, "更新记录", "/system", "el-icon-notebook-2"));
        adminResourceList.add(new Resource(8, "打赏", "/reward", "el-icon-sugar"));


        USER_TYPE_RESOURCE.put(0, adminResourceList);
        List<Resource> userResourceList = new ArrayList<>();
        userResourceList.add(new Resource(1, "看板", "/dashboard", "dashboard"));
        userResourceList.add(new Resource(5, "中转管理", "/forward", "el-icon-attract"));
        userResourceList.add(new Resource(6, "配置文件", "/config", "el-icon-tickets"));
//        userResourceList.add(new Resource(6, "更新记录", "/config", "el-icon-notebook-2"));
        userResourceList.add(new Resource(8, "打赏", "/reward", "el-icon-sugar"));
        USER_TYPE_RESOURCE.put(1, userResourceList);
    }

    /**
     * 获取资源
     *
     * @return
     */
    @GetMapping("/getMenus")
    public ApiResponse getResource() {
        Integer userType = WebCurrentData.getUser().getUserType();
        return ApiResponse.ok(USER_TYPE_RESOURCE.get(userType));
    }
}
