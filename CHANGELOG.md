# CHANGELOGS
**This file includes the most improvements and fixes I made to event drop**


</br>**Command to deploy jar to azure using CLI:**
</br>az webapp config appsettings set --resource-group "EventDrop-RG" --name "eventdrop1" --settings "SPRING_PROFILES_ACTIVE=prod"
</br>az webapp deploy --resource-group EventDrop-RG --name eventdrop1 --src-path "C:\Users\eastw\Git Projects\Personal\EventDrop\target\EventDrop-0.0.1-SNAPSHOT.jar" --type jar

### 9/18/25
#### Fixed
+ Had a race condition issue with batch uploads where multiple files could bypass quotas by checking limits simultaneously.
+ Fixed it by validating the entire batch as one unit instead of individual files.

### 9/21/25
#### Fixed
+ Fixed an issue where rabbit mq messages continuously failed to get process  due to low reply timeout
+ Successfully deployed to the fat jar to azure, redis to redis cloud(this took longer cause of aiven for valkey and their restrictions), rabbit mq amqp cloud

### 9/22/25
#### Added
Made multiple UI and UX improvements:
+ Included toasts when a user is uploading files for better feedback
+ Added consistent header layout and fonts across all pages
+ Added username display in room interface so users can see their logged-in identity
+ Added a copy button besides room code for easy room code copies
+ Implemented shared header component system to reduce code duplication across pages
+ Stricter file upload rules, users cannot upload duplicate files
+ A simple retry template for producers and failing rabbit mq messages due to listener container errors
+ Standardized typography to MiSans font across all pages for better readability

#### Fixed
+ Fixed an issue which occurred when a user redirected to a blank page after a room expired or got deleted
+ Fixed an issue where users got stuck in rooms after they expired
+ Fixed header inconsistencies and layout differences between pages
+ Fixed room details layout to properly display all information (username, roomcode, expires at, occupant count)

### 9/24/25
#### Added
UI/UX improvements
+ Made mobile responsive additions to all pages
+ Feat: Made the app a pwa to ensure mobile friendly

#### Fixed 
+ Fixed a race condition where multiple events tried to emit at once causing an sse disconnect and keeping the user in the room. 
Averted this by adding a queue based implementation, whereby events can only be emitted one at a time. Tradeoff is that events arent async anymore
but the frontend state is less prone to errors

### 10/12/25
#### Fixed
+ An issue which cause redis indexed room keys to not be properly deleted from the DB

### 11/13/25
#### Updated
+ Made the room state more accurate after occupant leaves by ensuring the event is only sent after the occupant is deleted from the DB 

### 11/24/25
#### Updated
+ Removed rabbit mq queues for room joins. Rather room joins are treated as one complete atomic event. Reducing rabbit mq overhead and latency when joining rooms

#### Fixed
+ Fixed an issue where room leaves failed to display the current state of occupants in the room
+ Fixed an issue where room leaves weren't triggered at all till room expiry
