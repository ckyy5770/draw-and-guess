package edu.vanderbilt.cloudcomputing.team13.client;

import edu.vanderbilt.cloudcomputing.team13.util.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Killian on 11/2/17.
 */
public class GameState {
    private static final Logger logger = LogManager.getLogger(GameState.class.getName());

    private int MAX_PLAYER = 5;
    private boolean isGameEnd = true;
    private String word = null;
    private String playerId = null;
    private String drawerId = null;
    private String winnerId = null;

    // a list of players <id, player>
    private ConcurrentHashMap<String, Player> playersMap = new ConcurrentHashMap<>();


    /**
     * GameState modifiers
     */

    public void setGameEnd(boolean gameEnd) {
        isGameEnd = gameEnd;
    }

    private void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void addPlayer(String playerIp, String playerName, String playerId){
        Player player = new Player(playerIp, playerName, playerId);
        player.setPosition(playersMap.size());
        playersMap.put(playerId, player);
    }

    public void addPlayerMySelf(String playerIp, String playerName, String playerId){
        addPlayer(playerIp, playerName, playerId);
        setPlayerId(playerId);
    }

    public void setDrawerId(String drawerId) {
        this.drawerId = drawerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public void clearReady(){
        for(Map.Entry<String, Player> entry : playersMap.entrySet()){
            entry.getValue().setReady(false);
        }
    }

    public void clearDrawer(){
        setDrawerId(null);
    }

    public void setPlayerReady(String playerId, boolean readyState){
        Player player = playersMap.get(playerId);
        if(player == null){
            logger.warn("invalid player id: {}", playerId);
            return;
        }
        player.setReady(readyState);
    }

    public void startNewGame(String drawerId, String word){
        this.drawerId = drawerId;
        this.word = word;
        clearReady();
        isGameEnd = false;
    }

    /**
     * GameState lookups
     */
    public boolean isDrawer(){
        return playerId != null && (playerId.equals(drawerId));
        //return true;
    }

    public int getMAX_PLAYER() {
        return MAX_PLAYER;
    }

    public Player getPlayerMyself(){
        return playersMap.get(playerId);
    }

    public String getPlayerId() {
        return playerId;
    }

    public ConcurrentHashMap<String, Player> getPlayersMap() {
        return playersMap;
    }
}
