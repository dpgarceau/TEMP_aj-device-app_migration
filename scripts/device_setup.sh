#!/bin/bash
clear

Green='\033[0;32m'
Yellow='\033[0;33m'
Blue='\033[0;34m'
Red='\033[0;31m'
NC="\033[0m"

REPO_RAW_BASE="https://raw.githubusercontent.com/AeroJudge/aerojudge-device-app/main/scripts"
BOOT_DIR="/boot"
CONFIG_TXT="$BOOT_DIR/config.txt"
SETTINGS_JSON="$BOOT_DIR/settings.json"
INSTALL_DIR="/var/opt/judge"
BIN_DIR="$INSTALL_DIR/bin"

echo -e "${Green}Setting up your AeroJudge Device${NC}"
echo -e "${Yellow}This script is for official AeroJudge devices with PCB revision 3.5x / 3.51.${NC}"
echo -e "${Red}Do not use this script for older legacy IMAC-ORG derived hardware.${NC}"
echo -e "${Red}This script should only be run once on a prepared device image.${NC}"
echo ""
echo -e "${Yellow}Continue with AeroJudge PCB v3.5x / v3.51 setup? (y/n)${NC}"
read response
case $response in
    y|Y) ;;
    *) echo "Setup cancelled."; exit 0 ;;
esac

install_required_packages() {
    echo -e "${Yellow}Updating OS packages...${NC}"
    sudo apt update > /dev/null 2>&1
    sudo apt upgrade -y > /dev/null 2>&1

    echo -e "${Yellow}Removing unwanted packages...${NC}"
    sudo apt purge wolfram-engine scratch nuscratch sonic-pi idle3 -y > /dev/null 2>&1
    sudo apt purge smartsim java-common libreoffice* lxplug-updater -y > /dev/null 2>&1

    echo -e "${Yellow}Installing required packages...${NC}"
    sudo apt install vim openjdk-17-jre xdotool unclutter sed locate xinput-calibrator curl wget unzip pcmanfm python3-evdev -y > /dev/null 2>&1
    sudo apt clean > /dev/null 2>&1
    sudo apt autoremove -y > /dev/null 2>&1
}

configure_system_preferences() {
    echo -e "${Yellow}Configuring system preferences...${NC}"
    echo "set mouse-=a" | sudo tee -a /root/.vimrc > /dev/null 2>&1
    echo "set mouse-=a" | tee -a /home/judge/.vimrc > /dev/null 2>&1

    echo "net.ipv6.conf.all.disable_ipv6 = 1" | sudo tee -a /etc/sysctl.conf > /dev/null 2>&1
    echo "net.ipv6.conf.default.disable_ipv6 = 1" | sudo tee -a /etc/sysctl.conf > /dev/null 2>&1
    echo "net.ipv6.conf.lo.disable_ipv6 = 1" | sudo tee -a /etc/sysctl.conf > /dev/null 2>&1
    sudo sysctl -p > /dev/null 2>&1

    echo "export DISPLAY=:0" >> /home/judge/.bashrc
    echo "xset r off # disable key repeat" >> /home/judge/.bashrc
    echo "xset s off # disable screen saver" >> /home/judge/.bashrc
    echo "xset -dpms # disable DPMS (Energy Star) features" >> /home/judge/.bashrc
    echo "xset s noblank # disable screen blanking" >> /home/judge/.bashrc
}

configure_boot_for_pcb_v351() {
    echo -e "${Yellow}Configuring boot settings for AeroJudge PCB v3.5x / v3.51...${NC}"

    if [ ! -f "$CONFIG_TXT" ]; then
        echo -e "${Red}$CONFIG_TXT not found. Cannot configure boot overlays.${NC}" >&2
        exit 1
    fi

    sudo cp "$CONFIG_TXT" "$CONFIG_TXT.aerojudge.bak.$(date +%Y%m%d%H%M%S)"
    sudo tee "$CONFIG_TXT" > /dev/null 2>&1 <<'EOF'
# AeroJudge Device config.txt
# Target hardware: official AeroJudge PCB v3.5x / v3.51
# Do not use this config on older legacy IMAC-ORG derived systems.

# Hardware interfaces
dtparam=i2c_arm=on

# Enable hardware PWM audio for AeroJudge headphone amp
# Pi 4 requires audremap overlay for GPIO12/GPIO13 audio routing
dtparam=audio=on
audio_pwm_mode=2
dtoverlay=audremap,pins_12_13

# Display and graphics defaults
display_auto_detect=1
dtoverlay=vc4-kms-v3d
max_framebuffers=2
disable_overscan=1

[pi4]
arm_boost=1

[all]
# Disabling Bluetooth is required because GPIO14/GPIO15 are used for buttons
dtoverlay=disable-bt

# GPIO key mappings for official AeroJudge PCB v3.5x / v3.51
dtoverlay=gpio-key,gpio=6,active_low=1,gpio_pull=off,keycode=77   # J3 NEXT (KP6)
dtoverlay=gpio-key,gpio=16,active_low=1,gpio_pull=off,keycode=81  # J4 -1.0 (KP3)
dtoverlay=gpio-key,gpio=19,active_low=1,gpio_pull=off,keycode=80  # J5 -0.5 (KP2)
dtoverlay=gpio-key,gpio=20,active_low=1,gpio_pull=off,keycode=76  # J6 ZERO (KP5)
dtoverlay=gpio-key,gpio=26,active_low=1,gpio_pull=off,keycode=71  # J7 BREAK (KP7)
dtoverlay=gpio-key,gpio=21,active_low=1,gpio_pull=off,keycode=83  # J8 DEADLINE (KPDOT)
#dtoverlay=gpio-key,gpio=4,active_low=1,gpio_pull=off,keycode=31  # J9 SPARE (KP S- unused)
dtoverlay=gpio-key,gpio=14,active_low=1,gpio_pull=off,keycode=79  # J10 NOT OBSERVED (KP1)
dtoverlay=gpio-key,gpio=15,active_low=1,gpio_pull=off,keycode=82  # J11 CALLER (KP0)
dtoverlay=gpio-key,gpio=17,active_low=1,gpio_pull=off,keycode=72  # J12 +0.5 (KP8)
dtoverlay=gpio-key,gpio=18,active_low=1,gpio_pull=off,keycode=73  # J13 +1.0 (KP9)
dtoverlay=gpio-key,gpio=27,active_low=1,gpio_pull=off,keycode=75  # J14 BACK (KP4)

# Audio volume thumbwheel mapping
dtoverlay=gpio-key,gpio=23,active_low=1,gpio_pull=off,keycode=114  # Vol Down (terminal 2)
#dtoverlay=gpio-key,gpio=22,active_low=1,gpio_pull=off,keycode=159 # Vol Down Fast (terminal 1)
dtoverlay=gpio-key,gpio=9,active_low=1,gpio_pull=off,keycode=115   # Vol Up (terminal 4)
#dtoverlay=gpio-key,gpio=11,active_low=1,gpio_pull=off,keycode=160 # Vol Up Fast (terminal 5)
#dtoverlay=gpio-key,gpio=10,active_low=1,gpio_pull=off,keycode=113 # Mute Toggle (terminal 3)

# Shutdown and power-off mapping
dtoverlay=gpio-shutdown,gpio_pin=25,active_low=1,gpio_pull=up,debounce=3000
dtoverlay=gpio-poweroff,gpiopin=24
EOF
}

create_settings_json() {
    echo -e "${Yellow}Configure settings.json file...${NC}"
    echo -e "${Blue}Judge_ID:${NC}"
    read judgeid
    echo -e "${Blue}Flight Line:${NC}"
    read flightline
    echo -e "${Blue}Score IP:${NC}"
    read scoreip
    echo -e "${Blue}Score Port:${NC}"
    read scoreport
    echo -e "${Blue}Enter two digit season year:${NC}"
    read seasonyear

    echo -e "${Yellow}Creating $SETTINGS_JSON...${NC}"
    sudo tee "$SETTINGS_JSON" > /dev/null 2>&1 <<EOF
{
    "judge_id":$judgeid,
    "line_number":$flightline,
    "score_host":"$scoreip",
    "score_http_port":$scoreport,
    "language":"en",
    "score_poll_timeout":2,
    "score_timeout":10,
    "seasonYear":$seasonyear
}
EOF
}

install_device_app() {
    echo -e "${Yellow}Installing AeroJudge Device App scripts and services...${NC}"
    sudo mkdir -p "$BIN_DIR" > /dev/null 2>&1
    sudo chown -R judge:judge "$INSTALL_DIR" > /dev/null 2>&1

    sudo wget -O /lib/systemd/system/judge.service "$REPO_RAW_BASE/judge.service" > /dev/null 2>&1
    wget -O "$BIN_DIR/judge.sh" "$REPO_RAW_BASE/judge.sh" > /dev/null 2>&1
    wget -O /home/judge/fetch_update.sh "$REPO_RAW_BASE/fetch_update.sh" > /dev/null 2>&1
    sudo wget -O /lib/systemd/system/kiosk.service "$REPO_RAW_BASE/kiosk.service" > /dev/null 2>&1
    wget -O "$BIN_DIR/kiosk.sh" "$REPO_RAW_BASE/kiosk.sh" > /dev/null 2>&1
    wget -O /home/judge/ajdesktop.png "$REPO_RAW_BASE/ajdesktop.png" > /dev/null 2>&1

    chmod +x /home/judge/fetch_update.sh > /dev/null 2>&1
    chmod +x "$BIN_DIR/judge.sh" > /dev/null 2>&1
    chmod +x "$BIN_DIR/kiosk.sh" > /dev/null 2>&1

    sudo systemctl enable judge.service > /dev/null 2>&1
    sudo systemctl enable kiosk.service > /dev/null 2>&1

    rm -f "$INSTALL_DIR/settings.json" > /dev/null 2>&1
    sudo ln -s "$SETTINGS_JSON" "$INSTALL_DIR/settings.json" > /dev/null 2>&1

    pcmanfm --set-wallpaper /home/judge/ajdesktop.png --wallpaper-mode=stretch > /dev/null 2>&1
}

install_default_data_and_update() {
    echo -e "${Yellow}Creating default pilots for testing...${NC}"
    wget -O /tmp/data.zip "$REPO_RAW_BASE/data.zip" > /dev/null 2>&1
    unzip -o /tmp/data.zip -d "$INSTALL_DIR/" > /dev/null 2>&1

    /home/judge/fetch_update.sh
}

install_required_packages
configure_system_preferences
configure_boot_for_pcb_v351
create_settings_json
install_device_app
install_default_data_and_update

echo -e "${Yellow}AeroJudge Device App installation complete.${NC}"
echo -e "${Yellow}Reboot the device to apply boot config changes.${NC}"
