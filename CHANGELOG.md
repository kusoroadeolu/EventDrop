# CHANGELOGS
This file includes the most improvements and fixes I made to event drop
Command to deploy jar to azure using CLI:
az webapp config appsettings set --resource-group "EventDrop-RG" --name "eventdrop1" --settings "SPRING_PROFILES_ACTIVE=prod"
az webapp deploy --resource-group EventDrop-RG --name eventdrop --src-path "C:\Users\eastw\Git Projects\Personal\EventDrop\target\EventDrop-0.0.1-SNAPSHOT.jar" --type jar

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
