package com.hhawking.websocket.demo.servicec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebsocketService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void setRedis(Integer id, String msg) {
        stringRedisTemplate.opsForHash().put("online","user"+id,msg);
    }

    public String getRedis(Integer id) {
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        return opsForHash.get("online","user"+id);
    }
}
