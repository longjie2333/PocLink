package com.example.remotecontrol;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkClient {
    public interface Listener {
        void onConnected();
        void onTextMessage(String text);
        void onClosed();
        void onError(Exception e);
    }

    private static final String TAG = "NetworkClient";
    private final String host;
    private final int port;
    private final Listener listener;

    private Socket socket;
    private Thread readThread;
    private volatile boolean running = false;
    private DataOutputStream dataOut;
    private BufferedWriter textOut;

    public NetworkClient(String host, int port, Listener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);
                running = true;
                listener.onConnected();

                OutputStream os = socket.getOutputStream();
                dataOut = new DataOutputStream(os);
                textOut = new BufferedWriter(new OutputStreamWriter(os));

                InputStream is = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                readThread = new Thread(() -> {
                    try {
                        String line;
                        while (running && (line = reader.readLine()) != null) {
                            listener.onTextMessage(line);
                        }
                    } catch (IOException e) {
                        if (running) listener.onError(e);
                    } finally {
                        listener.onClosed();
                    }
                }, "NetworkClient-Read");
                readThread.start();
            } catch (Exception e) {
                listener.onError(e);
            }
        }, "NetworkClient-Connect").start();
    }

    public synchronized void sendBinary(byte[] data, int off, int len) {
        if (dataOut == null) return;
        try {
            dataOut.writeInt(len);
            dataOut.write(data, off, len);
            dataOut.flush();
        } catch (IOException e) {
            listener.onError(e);
        }
    }

    public synchronized void sendText(String text) {
        if (textOut == null) return;
        try {
            textOut.write(text);
            textOut.write('\n');
            textOut.flush();
        } catch (IOException e) {
            listener.onError(e);
        }
    }

    public void close() {
        running = false;
        try { if (socket != null) socket.close(); } catch (Exception ignore) {}
        socket = null;
        dataOut = null;
        textOut = null;
    }
}
