package com.leeroy.forwordpanel.forwordpanel.controller;

import com.leeroy.forwordpanel.forwordpanel.common.WebCurrentData;
import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import com.leeroy.forwordpanel.forwordpanel.dto.PageRequest;
import com.leeroy.forwordpanel.forwordpanel.dto.PortAddDTO;
import com.leeroy.forwordpanel.forwordpanel.dto.PortPageRequest;
import com.leeroy.forwordpanel.forwordpanel.model.Port;
import com.leeroy.forwordpanel.forwordpanel.service.PortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RequestMapping("port")
@Controller
public class PortController {

    @Autowired
    private PortService portService;

    @ResponseBody
    @PostMapping("getPortList")
    public ApiResponse getPortList(@RequestBody PortPageRequest pageRequest) {
        return ApiResponse.ok(portService.findListByServer(pageRequest));
    }

    @ResponseBody
    @PostMapping("getPage")
    public ApiResponse getUserPortList(@RequestBody PageRequest pageRequest) {
        return ApiResponse.ok(portService.findList(pageRequest));
    }

    @ResponseBody
    @GetMapping("getFreePortList")
    public ApiResponse getFreePortList(Integer serverId) {
        return ApiResponse.ok(portService.findFreePortList(serverId));
    }

    @ResponseBody
    @GetMapping("getFreePortPage")
    public ApiResponse getFreePortPage(PortPageRequest portPageRequest) {
        return ApiResponse.ok(portService.findFreePortPage(portPageRequest));
    }


    @ResponseBody
    @RequestMapping(value = "save", method = RequestMethod.POST)
    public ApiResponse saveUserPort(@RequestBody Port port) {
        if(WebCurrentData.getUser().getUserType()>0){
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        return portService.save(port);
    }

    @ResponseBody
    @RequestMapping(value = "batchSave", method = RequestMethod.POST)
    public ApiResponse saveUserPort(@RequestBody PortAddDTO port) {
        if(WebCurrentData.getUser().getUserType()>0){
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        return portService.save(port);
    }

    @ResponseBody
    @RequestMapping(value = "batchDelete", method = RequestMethod.POST)
    public ApiResponse batchDeleteUserPort(@RequestBody PortAddDTO port) {
        if(WebCurrentData.getUser().getUserType()>0){
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        return portService.batchDelete(port);
    }

    @ResponseBody
    @GetMapping("delete")
    public ApiResponse delete(Integer id) {
        if(WebCurrentData.getUser().getUserType()>0){
            return ApiResponse.error("403", "您没有权限执行此操作");
        }
        return portService.delete(id);
    }


}
