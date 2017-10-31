# draw-and-guess

## Game Server

there are 4 types of requests that server need to response:

1. "CliNewPoint@X%Y" from drawers: upon receiving this request, the server should publish the new drawing point immediately to all the guessers.

2. "CliNewWinner@USER_ID" from guessers: upon receiving this request, the server should publish the winner immediately to all the players.

3. "CliNewPlayer@USER_ID%USER_IP%USER_NAME" from players: upon receiving this request, the server should see if there is enough space for the new player, if there is not, reject the player.

4. "CliPlayerReady@USER_ID" from players: upon receiving this request, the server should mark this play to be ready to play, if all players are ready, then start the game.

there are 5 types of messages that server may send:

1. "ServerNewPoint@X%Y"

2. "ServerNewWinner@USER_ID"

3. "ServerNewPlayer@USER_ID%USER_IP%USER_NAME"

4. "ServerPlayerReady@USER_ID"

5. "ServerNewGame@DRAWER_ID%WORD"


