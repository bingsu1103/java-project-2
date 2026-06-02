package com.chatapp.client.media;

import com.chatapp.client.network.ChatClient;
import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Manages audio recording and playback for voice chats.
 * Uses Java Sound API and transmits raw audio frames over UDP.
 */
public class VoiceCallManager {
    private final ChatClient chatClient;
    private final String myUsername;
    private final String targetUser;
    
    private TargetDataLine targetLine;
    private SourceDataLine sourceLine;
    private boolean isCalling;
    
    private Thread captureThread;
    private Thread receiveThread;
    
    private DatagramSocket udpSocket;
    private InetAddress remoteAddress;
    private int remotePort;
    
    public VoiceCallManager(ChatClient chatClient, String myUsername, String targetUser) {
        this.chatClient = chatClient;
        this.myUsername = myUsername;
        this.targetUser = targetUser;
        this.isCalling = false;
        
        try {
            this.udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Could not create UDP socket for VoiceCall: " + e.getMessage());
        }
    }
    
    /**
     * Define the audio format (telephony standard).
     * 16000 Hz, 16-bit, mono, signed, big-endian = false.
     * Highly portable, low-bandwidth, works on macOS CoreAudio.
     */
    public static AudioFormat getAudioFormat() {
        float sampleRate = 16000.0f;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
    
    public int getLocalPort() {
        return udpSocket != null ? udpSocket.getLocalPort() : 0;
    }
    
    public void setRemoteAddress(String ip, int port) {
        try {
            this.remoteAddress = InetAddress.getByName(ip);
            this.remotePort = port;
        } catch (java.io.IOException e) {
            System.err.println("Invalid remote address: " + e.getMessage());
        }
    }
    
    public synchronized void startCall() {
        if (isCalling) return;
        isCalling = true;
        
        // Start playback source line
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Playback line not supported by the system.");
            } else {
                sourceLine = (SourceDataLine) AudioSystem.getLine(info);
                sourceLine.open(format);
                sourceLine.start();
            }
        } catch (LineUnavailableException e) {
            System.err.println("Playback line unavailable: " + e.getMessage());
        }
        
        // Start capture thread
        captureThread = new Thread(this::captureAudio, "VoiceCall-Capture-" + targetUser);
        captureThread.setDaemon(true);
        captureThread.start();
        
        // Start receive thread
        receiveThread = new Thread(this::receiveAudioLoop, "VoiceCall-Receive-" + targetUser);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    public synchronized void stopCall() {
        if (!isCalling) return;
        isCalling = false;
        
        if (targetLine != null) {
            try {
                targetLine.stop();
                targetLine.close();
            } catch (Exception ignored) {}
            targetLine = null;
        }
        
        if (sourceLine != null) {
            try {
                sourceLine.stop();
                sourceLine.close();
            } catch (Exception ignored) {}
            sourceLine = null;
        }
        
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        
        if (udpSocket != null) {
            udpSocket.close();
            udpSocket = null;
        }
    }
    
    private void captureAudio() {
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("Microphone line not supported by the system.");
            return;
        }
        
        try {
            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();
            
            byte[] buffer = new byte[1024]; // Low latency buffer
            int packetCount = 0;
            while (isCalling && !Thread.currentThread().isInterrupted()) {
                int read = targetLine.read(buffer, 0, buffer.length);
                if (read > 0 && remoteAddress != null && remotePort > 0 && udpSocket != null) {
                    DatagramPacket packet = new DatagramPacket(buffer, read, remoteAddress, remotePort);
                    udpSocket.send(packet);
                    packetCount++;
                }
            }
        } catch (LineUnavailableException e) {
            System.err.println("Microphone line unavailable: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Audio capture error: " + e.getMessage());
        }
    }
    
    private void receiveAudioLoop() {
        byte[] receiveBuffer = new byte[4096];
        int packetCount = 0;
        while (isCalling && !Thread.currentThread().isInterrupted() && udpSocket != null && !udpSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(packet);
                packetCount++;
                
                if (sourceLine != null) {
                    sourceLine.write(packet.getData(), packet.getOffset(), packet.getLength());
                }
            } catch (java.io.IOException e) {
                if (isCalling) {
                    System.err.println("Audio receive error: " + e.getMessage());
                }
            }
        }
    }
    
    public void receiveAudio(String base64Data) {
        // Backwards compatibility stub, not used for UDP streaming
    }
}
