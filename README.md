## Thunder - Core Lightning node manager for Android

### How to instll Thunder

You can always grab the latest preview release here:

https://github.com/bubelov/thunder/releases/tag/preview

The app will be available on F-Droid eventually, but there is no ETA on that.

### How to Authorize with RaspiBlitz

You can add this script to your Blitz and it will generate a single QR for you:

```bash
#!/bin/bash

source /mnt/hdd/raspiblitz.conf

host=$(sudo cat /mnt/hdd/tor/clnGRPCport/hostname)
port=$clnGRPCport
server_pem=$(sudo awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' /home/bitcoin/.lightning/bitcoin/server.pem)
client_pem=$(sudo awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' /home/bitcoin/.lightning/bitcoin/client.pem)
client_key_pem=$(sudo awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' /home/bitcoin/.lightning/bitcoin/client-key.pem)
json=$(printf '{ "url": "%s:%s", "server_pem": "%s", "client_pem": "%s", "client_key_pem": "%s" }\n' "$host" "$port" "$server_pem" "$client_pem" "$client_key_pem")

qrencode -t utf8 "$json"
```
