# BookletX
Client for educational game Blooket (blooket.com)

## Features
- Join live games via game ID or deep link
- Cheat mode with support for setting money, doing glitches, etc.
- Legit mode (Soon™)

## How it works
Blooket is very weird and uses Firebase Realtime Database for multiplayer games.
All this app really does is write specific data to said database. Since the database
isn't very well protected, you can perform any action without trouble, especially
as it doesn't even know if you're doing questions or not.
