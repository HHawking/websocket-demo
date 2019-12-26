package com.hhawking.websocket.demo.controller;

import com.hhawking.websocket.demo.servicec.WebsocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebsocketController {

    @Autowired
    private WebsocketService websocketService;

    @GetMapping("setRedis")
    public String setRedis(Integer id,String msg){
        websocketService.setRedis(id,msg);
        return "OK";
    }

    @GetMapping("getRedis")
    public String getRedis(Integer id){
        return websocketService.getRedis(id);
    }
}
