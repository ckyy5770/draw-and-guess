package edu.vanderbilt.cloudcomputing.team13.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Killian on 10/31/17.
 */
public class GraphicInterface {
    private static final Logger logger = LogManager.getLogger(GraphicInterface.class.getName());
    GameClient client;
    GameBoard board;

    public GraphicInterface(){

    }

    public void init(GameClient client, GameBoard board){
        this.client = client;
        this.board = board;
    }

    // command from client to game board
    public void startBoard(){
        board.run();
    }

    public void drawPoint(String coord){
        String[] splited = coord.split("%");
        if(splited.length != 2){
            logger.warn("cant draw point with following coord: {}", coord);
            return;
        }
        double x = Double.parseDouble(splited[0]);
        double y = Double.parseDouble(splited[1]);
        if(x == Double.MAX_VALUE && y == Double.MAX_VALUE){
            board.addSeparatorToDrawnList();
        }else{
            board.addPointToDrawnList(x, y);
        }
    }

    // reports from game board to client
    public void reportDrawnPoint(double x, double y){
        String str = "CliNewPoint@" + Double.toString(x) + "%" + Double.toString(y);
        client.makeRequest(str);
    }

    public void reportPlayerReady(String playerId, boolean isReady){
        String str = "CliPlayerReady@" + playerId + "%" + (isReady ? "1" : "0");
        client.makeRequest(str);
    }
}
