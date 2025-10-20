import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const RemoteControlApp());
}

class RemoteControlApp extends StatelessWidget {
  const RemoteControlApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Remote Control POC',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const MethodChannel _channel = MethodChannel('com.example.remotecontrol/remote_control');
  final TextEditingController _wsController = TextEditingController(text: 'ws://10.0.2.2:9002');
  String _status = 'Idle';

  Future<void> _start() async {
    try {
      await _channel.invokeMethod('setServerUrl', {'url': _wsController.text});
      await _channel.invokeMethod('start');
      setState(() => _status = 'Starting...');
    } catch (e) {
      setState(() => _status = 'Start failed: $e');
    }
  }

  Future<void> _stop() async {
    try {
      await _channel.invokeMethod('stop');
      setState(() => _status = 'Stopped');
    } catch (e) {
      setState(() => _status = 'Stop failed: $e');
    }
  }

  Future<void> _showCircle() async {
    try {
      await _channel.invokeMethod('showCircle', {'x': 400, 'y': 800});
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Android Remote Control POC')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('WebSocket Server URL'),
            TextField(
              controller: _wsController,
              decoration: const InputDecoration(hintText: 'ws://host:port/path'),
            ),
            const SizedBox(height: 12),
            Wrap(spacing: 8, children: [
              ElevatedButton(onPressed: _start, child: const Text('Start Service')),
              ElevatedButton(onPressed: _stop, child: const Text('Stop Service')),
              OutlinedButton(onPressed: _showCircle, child: const Text('Test Circle')),
            ]),
            const SizedBox(height: 12),
            Text('Status: $_status'),
            const SizedBox(height: 12),
            const Text('Tip: enable the Accessibility Service for input control in Settings.'),
          ],
        ),
      ),
    );
  }
}
