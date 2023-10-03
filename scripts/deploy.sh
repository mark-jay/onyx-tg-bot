#!/bin/bash

SERVER="$1"
sbt docker:publishLocal && docker image save -o /tmp/image tg-bot-user/onyx-telegram-bot:0.1.0 && scp /tmp/image "$SERVER":/home/mark/tgbot/onyx/image
ssh root@"$SERVER" docker rm -f onyx
ssh root@"$SERVER" 'bash -c "cat /home/mark/tgbot/onyx/image | docker image load"'
ssh root@"$SERVER" 'bash -c "docker run -d --restart=unless-stopped --name onyx -e TOKEN=$(cat /home/mark/tgbot/onyx/token.txt) -e MAIN_CHAT_ID=-1001891958439 -e CHAT_ID_TO_FORWARD_TO=-4076585971 tg-bot-user/onyx-telegram-bot:0.1.0"'


# then run:
# mkdir -p /home/mark/tgbot/onyx/persistence
# then create token file at /home/mark/tgbot/onyx/token.txt
# then on server as root(install docker first and then make folder with root owner named '/home/mark/tgbot/onyx/persistence'):
# cat /home/mark/tgbot/onyx/image | docker image load
# docker run -d --restart=unless-stopped --name onyx -e TOKEN="$(cat /home/mark/tgbot/onyx/token.txt)" -e MAIN_CHAT_ID=XXX -e CHAT_ID_TO_FORWARD_TO=YYY tg-bot-user/onyx-telegram-bot:0.1.0
#
# First run with any MAIN_CHAT_ID and CHAT_ID_TO_FORWARD_TO, call
# /getChatId
# And the restart with correct MAIN_CHAT_ID and CHAT_ID_TO_FORWARD_TO
# make sure bot has disabled 'Group Privacy'(configured with botfather)

