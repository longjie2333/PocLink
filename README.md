Remote Control POC (Flutter + Java, Android)

This is a proof-of-concept Android app built with Flutter (UI) and Java (Android platform code) that demonstrates:
- Real-time screen capture using MediaProjection + MediaCodec (H.264) and streaming over a TCP socket
- Network transport to a control endpoint
- Basic performance optimizations (hardware encoder, surface input, downscaling, bitrate/framerate tuning)
- Visual feedback: draw a circle on the screen at coordinates received over the network or via Flutter
- Accessibility-based input control (tap/swipe) driven by network commands
- Custom URL scheme poclink://start to launch the app and start the service

Project structure
- lib/main.dart: Minimal Flutter UI to start/stop and set server URL
- android/... Java code implementing screen capture, streaming, accessibility overlay, and deep link handling

How it works
- Start the app or open poclink://start
- The foreground service connects to the TCP server (default ws://10.0.2.2:9002 in the UI; scheme ignored) and requests screen-capture permission
- After permission is granted, MediaProjection + MediaCodec produce H.264 frames which are sent as length-prefixed binary messages over TCP
- Commands received from the server (newline-delimited JSON) are executed by the AccessibilityService and visualized with an overlay circle

Network protocol
- Outbound: each video frame is sent as 4-byte big-endian length followed by the raw H.264 NAL bytes
- Inbound: text lines (one JSON object per line). Examples:
  {"type":"input","action":"tap","x":540,"y":960}
  {"type":"input","action":"swipe","x":200,"y":800,"x2":900,"y2":800,"duration":300}
  {"type":"start_capture"}

Accessibility
- You must enable the Accessibility service: Settings > Accessibility > Remote Control POC
- The service provides tap/swipe gestures and shows a temporary circle overlay at the commanded coordinates

Deep link
- Opening poclink://start launches the app and starts the remote-control foreground service

Notes
- This POC uses a simple TCP client instead of a WebSocket to avoid extra dependencies. The scheme in the URL is ignored; only host and port are used
- The encoder downscales the screen if needed to keep max dimension around 1280 to reduce bandwidth
- For Android 13+, a notification permission may be needed for the foreground notification

Security
- No authentication is implemented. Use only in a trusted environment for experimentation

