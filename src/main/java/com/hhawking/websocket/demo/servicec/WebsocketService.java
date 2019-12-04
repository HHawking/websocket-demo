package com.hhawking.websocket.demo.servicec;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebsocketService {

    Map<Integer,String> map = new HashMap<>();

    public String getMsg(Integer id) {
        return map.get(id);
    }

    public void setMsg(Integer id,String msg) {
        map.put(id,msg);
    }
}
