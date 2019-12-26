
# ABRPTransmitterCompanion

Allows you to easylie install ABRPTransmitter on your Hyundai Ioniq head unit and send the www.abetterrouteplanner.com token to it.<br />

Getting started:<br />
* Connect your smartphone and the head unit to the same WiFi network (works with Hotspot too).
* Find your head units IP (goto Android Settings -> WiFi, select your WiFi network and see the IP or look at your WiFi router/access point).
* Enable ADB Ethernet mode on your head unit. (See Wiki https://github.com/g4rb4g3/ABRPTransmitterCompanion/wiki/Enable-ADB-Ethernet-mode)
* Start ABRPTransmitterCompanion on your smartphone, insert the head units IP as "Companion IP".
* Choose a release from the drop down.
* Press "Download & Install"
* If everything worked out well ABRPTransmitter will be started on your head unit.
* Check "Autostart data exchange companion service" and press save.
* Insert the token into ABRPTransmitterCompanion (copy & paste is your friend) and press send.

You can uncheck "Autostart data exchnage companion service" and hit save again if you want.
<br />
Make sure your smartphone and your car are on the same WiFi.
![Screenshot](doc/screenshot.png)

If you like my work I'd be happy if you buy me a coffee. Thanks!<br />
[![](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=RT8WTFDGMLFPG)

Using some bits from:
https://github.com/aaronjwood/PortAuthority
https://github.com/cgutman/AdbLib
