package com.hhawking.websocket.demo.server;

import com.hhawking.websocket.demo.servicec.WebsocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @title: websocket
 * @Author: HH
 * @Date: 2019/6/11 9:48
 */
@ServerEndpoint("/websocket/{userId}")
@Component
public class WebsocketServer {

    /**
     * 在线用户
     */
    private static final Map<Integer, Session> SESSION_MAP = new HashMap<>();

    private static WebsocketService websocketService;

    @Autowired
    public void setMonitoringService(WebsocketService websocketService) {
        WebsocketServer.websocketService = websocketService;
    }

    @OnOpen
    public synchronized void onOpen(@PathParam("userId") Integer userId, Session session) {
        System.out.println("---------------用户" + userId + "请求连接---------");
        addUser(userId, session);
        System.out.println("用户" + userId + "连接成功，当前人数: " + SESSION_MAP.size());

    }

    @OnClose
    public synchronized void onClose(@PathParam("userId") Integer userId, CloseReason reason) {
        System.out.println("---------------用户" + userId + "请求断开---------");
        removeUser(userId, reason);
        System.out.println("用户" + userId + "断开连接,剩余人数:" + SESSION_MAP.size());
    }

    /**
     * 链接错误执行
     */
    @OnError
    public synchronized void onError(@PathParam("userId") Integer userId, Throwable error) {
        removeUser(userId, new CloseReason(CloseReason.CloseCodes.NO_EXTENSION, "客户端异常"));
        System.err.println("用户 " + userId + " 连接时发生了错误 : " + error.getMessage());
    }

    /**
     * 收到订阅请求
     */
    @OnMessage
    public synchronized void onMessage(@PathParam("userId") Integer userId, String msg) {
        System.out.println("收到用户" + userId + "的消息:" + msg);
    }

    /**
     * 发送信息(String)给指定ID用户
     */
    public synchronized void sendToUser(String message, Integer sendUserId) {
        if (SESSION_MAP.size() <= 0) {
            return;
        }
        Session session = SESSION_MAP.get(sendUserId);
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            System.err.println("websocket IO异常" + e.getMessage());
        }
    }

    /**
     * 发送信息给所有人
     */
    private synchronized void sendToAll(String message) {
        if (SESSION_MAP.size() == 0) {
            return;
        }
        Set<Integer> set = SESSION_MAP.keySet();
        for (Integer key : set) {
            Session session = SESSION_MAP.get(key);
            if (session != null) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    System.err.println("websocket 异常,用户" + key + "发送失败 : " + e.getMessage());
                }
            }
        }
    }

    /**
     * 用户上线
     */
    private synchronized void addUser(Integer id, Session session) {
        Session oldSession = SESSION_MAP.get(id);
        if (null != oldSession) {
            try {
                oldSession.close();
            } catch (IOException e) {
                System.err.println("websocket IO异常" + e.getMessage());
            }
        }
        SESSION_MAP.remove(id);
        SESSION_MAP.put(id, session);
    }

    /**
     * 用户下线
     */
    private synchronized void removeUser(Integer userId, CloseReason reason) {
        Session session = SESSION_MAP.get(userId);
        if (session != null) {
            try {
                session.close(reason);
            } catch (IOException e) {
                System.err.println("websocket 异常 : " + e.getMessage());
            }
        }
        SESSION_MAP.remove(userId);
    }
}
