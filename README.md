# android-gps-poster

## Overview

This is a project I worked on for Hospice of the Chesapeake. The goal was to
reuse a weather balloon they were going to use for an event. This application
was written to service multiple tasks.

1. Send GPS coordnates to a map server so we can track the balloon.
 - James wrote the majority of this part. He got a map server up and running,
   and had the android phone seend it's GPS coords to the server.
2. Control the altitude of the balloon.
 - I wrote the majority of this part. Once the balloon had been released, we
   were tasked with bringing it down without damaging it. We accomplished this
   by attaching a valve to the balloon that we used to release the air.

## Setup

```
Project Setup:

      _________               Map/Control Server
     (         )                      |
    (  balloon  )                 Android App
     (_________)                      |
         |                     Arduino Leonardo
         \                            |
       Nexus 5                      Valve
```

### Physical

We just had a weather balloon with a phone strapped to it. We surrounded the
phone and arduino in a protective casing.

### Logical

#### Map/Control Server
 - Displayed location of balloon on map.
 - RESTful interface for releasing air from the balloon.

#### Android App
 - Sent GPS coordnates to server.
 - Sent serial commands over USB to the arduino.

#### Arduino
 - On recieved specific serial commands, send voltage across a pin.

#### Valve
 - On voltage, open valve.
