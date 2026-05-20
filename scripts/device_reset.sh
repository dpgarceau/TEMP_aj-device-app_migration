#!/bin/sh

#
# The following fixes some ownership errors that some devices had, removes all old contest data, and performs a device upgrade
#

sudo chown judge:judge .judge_last_release
sudo chown -R judge:judge /var/opt/judge
sudo chmod -R go-w /var/opt/judge/

cd /var/opt/judge
rm *.log *.dat *.zip *.json
rm pilots/scores/*

cd ~
rm .judge_last_release  
curl -sfS https://raw.githubusercontent.com/AeroJudge/aerojudge-device-app/main/scripts/fetch_update.sh -o /home/judge/fetch_update.sh
chmod +x /home/judge/fetch_update.sh
/home/judge/fetch_update.sh

# Fix default network settings to match AeroJudge config
# Comment out this line if running manually to keep the original (deprecated) defaults
sed -i 's/"score_host":"192.168.1.4"/"score_host":"192.168.8.100"/;s/"score_http_port":8181/"score_http_port":80/' /var/opt/judge/settings.json

# removes this script so it doesn't get accidentally run again
rm device_reset.sh