# BookletX
Android client for educational game Blooket ([blooket.com](http://blooket.com))

## Features
- Join live games via game ID or deep link
- Cheat mode with support for setting money, doing glitches, etc. and no question-answering
- Dark theme if your Android supports it
- Legit mode (Soon™)

## How do I get it
At some point I'll make an F-Droid repo or put it in the official one, but for now
you can get it from the GitHub Actions page:
1. Go to [here](https://github.com/AdamantPenguin/BookletX/actions)
2. Click the most recent run (at the top of the list)
3. Click on `debug-apk` under Artifacts to download it
4. Unzip the file to get apk
5. Install on phone (plenty of tutorials online if you don't know how)

## How it works
Blooket uses a Firebase Realtime Database for multiplayer games;
all this app does is write specific data to it. Since the database
isn't very well protected, you can perform almost any action and there
is no check to see if you're actually answering questions.
