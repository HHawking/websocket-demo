package com.hhawking.websocket.demo.thread;

import com.hhawking.websocket.demo.servicec.WebsocketService;

public class StatisticsThread implements Runnable {

    private WebsocketService websocketService;
    private Integer id;
    public boolean exit = false;

    public StatisticsThread(WebsocketService websocketService, Integer id) {
        this.websocketService = websocketService;
        this.id = id;
    }

    @Override
    public void run() {
        while (!exit) {
            String msg = websocketService.getMsg(id);
            System.out.println(msg);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
