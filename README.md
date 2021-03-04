# Mobile Caller Demo Client (opentok)
Simple Video Caller Demo client

### Setup
1. Change the server host in `ApiService.kt` line 13 to your hosted server (Github Repo: https://github.com/nexmo-se/mobile-caller-demo-server).
2. Change the application ID in `app/build.gradle` to the one specified when creating the Pushy Application.
2. Sync, Build and Run the application.

### Using The Application
1. Login by entering your mobile number. This will be used as a user identifier which the other party will use to call you.
2. Call another person by entering his mobile number (user identifier entered when logging in).
3. The other phone should ring, answer and you will get into a video call.
