# AeroJudge Device Settings

Fresh production setup for AeroJudge Device serial DPG-100 and above stores
device settings at:

```text
/var/opt/judge/settings.json
```

Older setup flows used `/boot/settings.json` and linked it into
`/var/opt/judge/settings.json`. That boot-partition workflow is not used by the
current fresh production setup.

For full editing capability, SSH into the device as `judge` and edit:

```sh
sudo vi /var/opt/judge/settings.json
```

Settings exposed in the AeroJudge admin screens can also be changed through the
app UI.
