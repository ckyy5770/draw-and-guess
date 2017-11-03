package edu.vanderbilt.cloudcomputing.team13.client;


import edu.vanderbilt.cloudcomputing.team13.client.GameBoard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import edu.vanderbilt.cloudcomputing.team13.server.GameServer;
import edu.vanderbilt.cloudcomputing.team13.util.AbstractAction;
import edu.vanderbilt.cloudcomputing.team13.util.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Killian on 10/31/17.
 */
public class GameClient implements Runnable{
    private static final Logger logger = LogManager.getLogger(GameClient.class.getName());
    // this context is shared among all zmq sockets in this app
    private ZMQ.Context context;
    // ip address of host machine
    private String ip;
    private String serverIp;
    // list of ports
    private String serverRepPort;
    private String serverPubPort;

    // a subscriber listening to the game server
    private ZMQ.Socket gameSub;
    // a requester to the game server
    private ZMQ.Socket gameRequester;

    // thread pool
    private ExecutorService threadPool;
    // command handler map <request name, action>
    private HashMap<String, AbstractAction> commandHandler;

    private boolean isClientStop = false;

    // game specs
    private GameState gameState = new GameState();

    // graphic interface
    private GraphicInterface gui = null;

    public GameClient(String ip, String playerName, String serverIp){
        this.ip = ip;
        this.serverIp = serverIp;

        context = ZMQ.context(1);
        gameSub = context.socket(ZMQ.SUB);
        gameRequester = context.socket(ZMQ.REQ);

        serverRepPort = "5555";
        serverPubPort = "5556";

        threadPool = Executors.newFixedThreadPool(10);

        initCommandHandler();

        gui = new GraphicInterface();
        GameBoard board = new GameBoard(gui, gameState);
        gui.init(this, board);

        initGame(playerName);
    }

    @Override
    public void run(){
        gameRequester.connect("tcp://" + serverIp + ":" + serverRepPort);
        gameSub.connect("tcp://"+ serverIp + ":" + serverPubPort);
        gameSub.subscribe("".getBytes());
        threadPool.submit(this::responder);
        logger.info("*** Client is running.");
        gui.startBoard();

        while(!isClientStop){
            try{
                Thread.sleep(1000 * 10);
            }catch (InterruptedException e){
                break;
            }
        }
        gameRequester.close();
        gameSub.close();
        context.term();
    }

    public void stop(){
        isClientStop = true;
    }

    public void makeRequest(String reqStr){
        String[] splited = reqStr.split("@");
        if(splited.length < 1) {
            logger.warn("invalid request: {}", reqStr);
            return;
        }
        AbstractAction handler = commandHandler.get(splited[0]);
        if(handler == null ){
            logger.warn("invalid request: {}", reqStr);
            return;
        }
        logger.debug("making request: {}", reqStr);
        handler.setPara(splited[1]);
        threadPool.submit(handler);
    }

    private void responder(){
        while(!Thread.currentThread().isInterrupted()){
            //  Wait for next cmd from server
            String topic = gameSub.recvStr();
            String content = gameSub.recvStr();
            String cmdStr = topic + "@" + content;
            logger.debug("received cmd: {}", cmdStr);
            tryExecute(cmdStr);
        }
    }

    private void tryExecute(String cmdStr){
        String[] splited = cmdStr.split("@");
        AbstractAction handler = commandHandler.get(splited[0]);
        if(handler == null){
            logger.warn("invalid command: {}", cmdStr);
        }else{
            handler.setPara(splited[1]);
            threadPool.submit(handler);
        }
    }


    private void initGame(String playerName){
        gameState.addPlayerMySelf(ip, playerName, ip + ":" + playerName);
        gameState.setGameEnd(true);

        AbstractAction action = commandHandler.get("CliNewPlayer");
        String playerId = ip + ":" + playerName;
        action.setPara(playerId + "%" + ip + "%" + playerName);
        threadPool.submit(action);
    }

    private void initCommandHandler(){
        commandHandler = new HashMap<>();
        // report info to server
        commandHandler.put("CliNewPoint", new reportNewPoint());
        commandHandler.put("CliNewWinner", new reportNewWinner());
        commandHandler.put("CliNewPlayer", new reportNewPlayer());
        commandHandler.put("CliPlayerReady", new reportPlayerReady());
        // respond to the request from server
        commandHandler.put("ServerNewPlayer", new setupNewPlayer());
        commandHandler.put("ServerNewWinner", new setupNewWinner());
        commandHandler.put("ServerPlayerReady", new setPlayerReady());
        commandHandler.put("ServerNewPoint", new drawNewPoint());
        commandHandler.put("ServerNewGame", new startNewGame());
    }

    private class reportNewPoint extends AbstractAction{
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run(){
            if(para == null){
                logger.warn("invalid parameter for reportNewPoint.");
            }else{
                String request = "CliNewPoint@" + para;
                gameRequester.send(request,0);
                String reply = gameRequester.recvStr();
                logger.debug("sent req: {}, received rep: {}", request, reply);
            }
        }
    }

    private class reportNewWinner extends AbstractAction{
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run(){
            if(para == null){
                logger.warn("invalid parameter for reportNewWinner.");
            }else{
                String request = "CliNewWinner@" + para;
                gameRequester.send(request,0);
                byte[] bytes = gameRequester.recv(0);
                String reply = new String(bytes);
                logger.debug("sent req: {}, received rep: {}", request, reply);
            }
        }
    }

    private class reportNewPlayer extends AbstractAction{
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run(){
            if(para == null){
                logger.warn("invalid parameter for reportNewPlayer.");
            }else{
                String request = "CliNewPlayer@" + para;
                logger.debug("sent req: {}", request);
                gameRequester.send(request,0);
                byte[] bytes = gameRequester.recv(0);
                String reply = new String(bytes);
                logger.debug("received rep: {}", reply);
            }
        }
    }

    private class reportPlayerReady extends AbstractAction{
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run(){
            if(para == null){
                logger.warn("invalid parameter for reportPlayerReady.");
            }else{
                String request = "CliPlayerReady@" + para;
                gameRequester.send(request,0);
                String reply = gameRequester.recvStr();
                logger.debug("sent req: {}, received rep: {}", request, reply);
            }
        }
    }

    private class setupNewPlayer extends AbstractAction{
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run(){
            if(para == null){
                logger.warn("invalid parameter for setupNewPlayer: null");
            }else{
                String[] splited = para.split("%");
                if(splited.length != 3){
                    logger.warn("invalid parameter for setupNewPlayer: {}", para);
                    return;
                }
                String playerId = splited[0];
                String playerIp = splited[1];
                String playerName = splited[2];
                gameState.addPlayer(playerIp, playerName, playerId);
            }
        }
    }

    private class setupNewWinner extends AbstractAction{
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run() {
            if (para == null) {
                logger.warn("invalid parameter for setupNewWinner: null");
            } else {
                gameState.setWinnerId(para);

                // reset game state, clear ready state
                gameState.setGameEnd(true);
                gameState.clearReady();
                gameState.clearDrawer();
            }
        }
    }

    private class setPlayerReady extends AbstractAction {
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run() {
            if (para == null) {
                logger.warn("invalid parameter for setPlayerReady: null");
            } else {
                String[] splited = para.split("%");
                int readyState = Integer.parseInt(splited[1]);
                gameState.setPlayerReady(splited[0], readyState != 0);
            }
        }
    }

    private class drawNewPoint extends AbstractAction {
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run() {
            if (para == null) {
                logger.warn("invalid parameter for drawNewPoint: null");
            } else {
                gui.drawPoint(para);
            }
        }
    }

    private class startNewGame extends AbstractAction{
        String para = null;

        @Override
        public void setPara(String para) {
            this.para = para;
        }

        @Override
        public void run() {
            if(para == null){
                logger.warn("invalid parameter for startNewGame: null");
            }else{
                String[] splited = para.split("%");
                String drawerId = splited[0];
                String word = splited[1];
                gameState.startNewGame(drawerId, word);
                logger.info("new game started. drawerId: {}, word: {}", drawerId, word);
            }
        }

    }

    public static void main(String[] args) {
        new GameClient(args[0], args[1], args[2]).run();
    }

}
