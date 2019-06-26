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

    //在线用户
    private static final Map<Integer,Session> sessionMap = new HashMap<>();
    //推送统计数的线程
    private static final Map<Integer,StatisticsThread> threadMap = new HashMap<>();
    //推送订阅车辆信息的线程
    private static final Map<Integer,FollowThread> followThreadMap = new HashMap<>();

    //注入service
    private static WebsocketService websocketService;
    @Autowired
    public void setMonitoringService(WebsocketService websocketService) {
        WebsocketServer.websocketService = websocketService;
    }

    @OnOpen
    public synchronized void onOpen(@PathParam("userId") Integer userId, Session session){
        System.out.println("---------------用户" + userId + "请求连接---------");
        addUser(userId,session);
        System.out.println("用户" + userId + "连接成功，当前人数: " + sessionMap.size() + "列表线程数: " + threadMap.size() + " , 订阅线程数: " + followThreadMap.size());

    }

    @OnClose
    public synchronized void onClose(@PathParam("userId") Integer userId,CloseReason reason){
        System.out.println("---------------用户" + userId + "请求断开---------");
        removeUser(userId,reason);
        System.out.println("用户"+userId+"断开连接,剩余人数:"+sessionMap.size()  + "列表线程数: " + threadMap.size() + " , 订阅线程数: " + followThreadMap.size());
    }

    /**
     * 链接错误执行
     */
    @OnError
    public synchronized void onError(@PathParam("userId") Integer userId,Throwable error){
        removeUser(userId,new CloseReason(CloseReason.CloseCodes.NO_EXTENSION, "客户端异常"));
        System.err.println("用户 "+userId+" 连接时发生了错误 : "+error.getMessage());
    }

    /**
     * 收到订阅请求
     */
    @OnMessage
    public synchronized void onMessage(@PathParam("userId") Integer userId, String ids){
        FollowThread oldThread = followThreadMap.get(userId);
        if (null!=oldThread)
            oldThread.exit=true;
        followThreadMap.remove(userId);
        FollowThread followThread = new FollowThread(userId, ids);
        Thread thread = new Thread(followThread);
        thread.start();
        followThreadMap.put(userId,followThread);
        System.out.println("用户"+userId+"订阅成功，当前订阅线程数"+ followThreadMap.size());
    }

    /**
     * 发送信息(String)给指定ID用户
     */
    private synchronized void sendToUser(String message,Integer sendUserId){
        if(sessionMap.size()<=0)
            return;
        Session session = sessionMap.get(sendUserId);
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            System.err.println("websocket IO异常"+e.getMessage());
        }
    }

    /**
     * 发送信息给所有人
     */
    private synchronized void sendToAll(String message)  {
        if(sessionMap.size() == 0)
            return;

        Set<Integer> set = sessionMap.keySet();
        for(Integer key : set){
            try {
                sessionMap.get(key).getBasicRemote().sendText(message);
            } catch (Exception e) {
                System.err.println("websocket 异常,用户" + key +"发送失败 : "+e.getMessage());
            }
        }
    }

    /**
     * 用户上线,开启实时数据推送线程
     */
    private synchronized void addUser(Integer id, Session session){

        StatisticsThread oldThread = threadMap.get(id);
        if (null != oldThread)
            oldThread.exit = true;
        threadMap.remove(id);

        FollowThread oldFollowThread = followThreadMap.get(id);
        if (oldFollowThread!=null)
            oldFollowThread.exit = true;
        followThreadMap.remove(id);

        Session oldSession = sessionMap.get(id);
        if (null != oldSession) {
            try {
                oldSession.close();
            } catch (IOException e) {
                System.err.println("websocket IO异常"+e.getMessage());
            }
        }
        sessionMap.remove(id);
        sessionMap.put(id,session);

        StatisticsThread statisticsThread = new StatisticsThread(id);
        Thread thread = new Thread(statisticsThread);
        thread.start();
        threadMap.put(id,statisticsThread);
    }


    /**
     * 用户下线
     */
    private synchronized void removeUser(Integer userId, CloseReason reason) {
        StatisticsThread thread = threadMap.get(userId);
        FollowThread followThread = followThreadMap.get(userId);
        if (thread!=null)
            thread.exit = true;
        if (followThread!=null)
            followThread.exit = true;
        threadMap.remove(userId);
        followThreadMap.remove(userId);

        Session session = sessionMap.get(userId);

        if (session!=null) {
            try {
                session.close(reason);
            } catch (IOException e) {
                System.err.println("websocket 异常 : "+e.getMessage());
            }
        }

        sessionMap.remove(userId);
    }

    /**
     * 每5秒推送实时统计的数据
     */
    private class StatisticsThread implements Runnable{
        private volatile boolean exit = false;
        private Integer id;
        //传入用户id，用于查询他对应的数据
        private StatisticsThread(Integer id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (!exit) {

                sendToUser("实时统计的数据", id);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 每5秒推送一次订阅的车辆信息
     */
    private class FollowThread implements Runnable{
        private volatile boolean exit = false;
        private Integer id;
        private String ids;
        //传入用户的id,和需要订阅的车辆id
        FollowThread(Integer id, String ids) {
            this.id = id;
            this.ids = ids;
        }
        @Override
        public void run() {
            while (!exit){

                sendToUser("订阅的车辆:"+ids+"的信息",id);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}
