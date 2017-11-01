package edu.vanderbilt.cloudcomputing.team13.util;

/**
 * Created by Chuilian Kong on 10/30/2017.
 */
public class Player {
    private String ip;
    private String name;
    private String id;
    boolean isReady = false;

    public Player(String ip, String name, String id){
        this.ip = ip;
        this.name = name;
        this.id = ip + ":" +name;
    }

    public void setReady(){
        isReady = true;
    }

    public void cancelReady(){
        isReady = false;
    }

    public boolean isReady() {
        return isReady;
    }

    public String getId() {
        return id;
    }
}
