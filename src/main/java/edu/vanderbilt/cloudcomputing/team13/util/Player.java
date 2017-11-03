package edu.vanderbilt.cloudcomputing.team13.util;

/**
 * Created by Chuilian Kong on 10/30/2017.
 */
public class Player {
    private String ip;
    private String name;
    private String id;
    boolean isReady = false;
    private int position = 0;
    private int points = 0;

    public Player(String ip, String name, String id){
        this.ip = ip;
        this.name = name;
        this.id = ip + ":" +name;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public String getIp() {
        return ip;
    }
}
