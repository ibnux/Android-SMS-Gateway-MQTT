
# Android SMS Gateway With MQTT
  
This is modified from my [SMS Gateway Based Push Notification](https://github.com/ibnux/Android-SMS-Gateway)
to turn your android phone as sms gateway

# HOW IT WORKS

Sending SMS

1. You send data to MQTT Server like [hivemq](https://www.hivemq.com/public-mqtt-broker/)
2. App will receive message MQTT Server, and route it to sms
3. App receive sent notification, and post it to your server
4. App receive delivered notification, and post it to your server

RECEIVE SMS
1. App receive SMS
2. App send it to your server
  
# HOW TO USE?  
  
Download APK from [release](https://github.com/ibnux/Android-SMS-Gateway-MQTT/releases)

you can find backend folder **php-gateway** for server side in this source

# FEATURES

- SENDING SMS
- RECEIVE SMS to SERVER
- SENT NOTIFICATION to SERVER
- DELIVERED NOTIFICATION to SERVER
- USSD
- MULTIPLE SIMCARD
- RETRY SMS FAILED TO SENT 3 TIMES

## USSD Request

Not all phone and carrier work with this, this feature need accessibility to read message and auto close USSD dialog, but some device failed to close Dialog, i use samsung S10 Lite and it cannot close dialog

## MULTIPLE SIMCARD

i think not all phone will work too, because of different of API for some OS which vendor has modification

# Compile it yourself

You need to understand how to build android Apps, and compile your own version.

You will see MyObjectBox error, just build it once, it will create automatically, read in [here](https://docs.objectbox.io/getting-started#generate-objectbox-code)

## Firebase Push Version

https://github.com/ibnux/Android-SMS-Gateway/

***

## Traktir @ibnux

[<img src="https://ibnux.github.io/KaryaKarsa-button/karyaKarsaButton.png" width="128">](https://karyakarsa.com/ibnux)

[<img src="https://ibnux.github.io/Trakteer-button/trakteer_button.png" width="120">](https://trakteer.id/ibnux)

## DONATE @ibnux

[paypal.me/ibnux](https://paypal.me/ibnux)

# LICENSE  
## Apache License 2.0  
  
Permissions  
  
    ✓ Commercial use  
    ✓ Distribution  
    ✓ Modification  
    ✓ Patent use  
    ✓ Private use  
  
Conditions  
  
    License and copyright notice  
    State changes  
  
Limitations  
  
    No Liability  
    No Trademark use  
    No Warranty  
  
you can find license file inside folder