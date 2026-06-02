package com.chatapp.client.media;

import com.chatapp.client.network.ChatClient;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

/**
 * Manages video streaming for private chats.
 * Captures frames using OpenCV (JavaCV) and transmits JPEG bytes over UDP.
 */
public class VideoCallManager {
    private final ChatClient chatClient;
    private final String myUsername;
    private final String targetUser;
    
    private boolean isCalling;
    private Thread captureThread;
    private Thread receiveThread;
    private Thread audioCaptureThread;
    
    private DatagramSocket udpSocket;
    private InetAddress remoteAddress;
    private int remotePort;
    
    private TargetDataLine audioTargetLine;
    private SourceDataLine audioSourceLine;
    
    private final VideoPanel localPanel;
    private final VideoPanel remotePanel;
    
    private final Random random = new Random();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public VideoCallManager(ChatClient chatClient, String myUsername, String targetUser) {
        this.chatClient = chatClient;
        this.myUsername = myUsername;
        this.targetUser = targetUser;
        this.isCalling = false;
        
        this.localPanel = new VideoPanel(myUsername + " (You)");
        this.remotePanel = new VideoPanel(targetUser);
        
        try {
            this.udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Could not create UDP socket for VideoCall: " + e.getMessage());
        }
    }
    
    public VideoPanel getLocalPanel() { return localPanel; }
    public VideoPanel getRemotePanel() { return remotePanel; }
    
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
    
    public static AudioFormat getAudioFormat() {
        float sampleRate = 16000.0f;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public synchronized void startCall() {
        if (isCalling) return;
        isCalling = true;
        
        // Start playback source line for audio
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                audioSourceLine = (SourceDataLine) AudioSystem.getLine(info);
                audioSourceLine.open(format);
                audioSourceLine.start();
            } else {
                System.err.println("Audio playback line not supported by the system.");
            }
        } catch (LineUnavailableException e) {
            System.err.println("Audio playback line unavailable: " + e.getMessage());
        }
        
        captureThread = new Thread(this::captureVideo, "VideoCall-Capture-" + targetUser);
        captureThread.setDaemon(true);
        captureThread.start();
        
        audioCaptureThread = new Thread(this::captureAudio, "VideoCall-AudioCapture-" + targetUser);
        audioCaptureThread.setDaemon(true);
        audioCaptureThread.start();
        
        receiveThread = new Thread(this::receiveVideoLoop, "VideoCall-Receive-" + targetUser);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    public synchronized void stopCall() {
        if (!isCalling) return;
        isCalling = false;
        
        if (audioTargetLine != null) {
            try {
                audioTargetLine.stop();
                audioTargetLine.close();
            } catch (Exception ignored) {}
            audioTargetLine = null;
        }
        
        if (audioSourceLine != null) {
            try {
                audioSourceLine.stop();
                audioSourceLine.close();
            } catch (Exception ignored) {}
            audioSourceLine = null;
        }
        
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        
        if (audioCaptureThread != null) {
            audioCaptureThread.interrupt();
            audioCaptureThread = null;
        }
        
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        
        if (udpSocket != null) {
            udpSocket.close();
            udpSocket = null;
        }
        
        localPanel.clearFrame();
        remotePanel.clearFrame();
    }
    
    private void captureVideo() {
        int width = 320;
        int height = 240;
        
        OpenCVFrameGrabber grabber = null;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        
        try {
            grabber = new OpenCVFrameGrabber(0);
            grabber.setImageWidth(width);
            grabber.setImageHeight(height);
            grabber.start();
        } catch (Exception e) {
            System.err.println("OpenCV Webcam initialization failed, falling back to simulator: " + e.getMessage());
            grabber = null;
        }
        
        BufferedImage simImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D simG = simImage.createGraphics();
        
        int ballX = width / 2;
        int ballY = height / 2;
        int ballDX = 4;
        int ballDY = 3;
        int ballRadius = 25;
        
        int pulseCount = 0;
        int consecutiveBlackFrames = 0;
        boolean permissionWarned = false;
        int sendPacketCount = 0;
        
        while (isCalling && !Thread.currentThread().isInterrupted()) {
            BufferedImage currentFrame = null;
            if (grabber != null) {
                try {
                    Frame frame = grabber.grab();
                    if (frame != null) {
                        currentFrame = converter.convert(frame);
                        if (isFrameBlack(currentFrame)) {
                            consecutiveBlackFrames++;
                            if (consecutiveBlackFrames > 30) {
                                if (!permissionWarned) {
                                    System.err.println("[WARNING] Camera is returning black frames. This is usually caused by missing macOS Camera Permissions.");
                                    System.err.println("Please go to System Settings > Privacy & Security > Camera and ensure Terminal/IDE (where Java is running) is allowed.");
                                    permissionWarned = true;
                                }
                                currentFrame = null; // Fallback to simulator
                            }
                        } else {
                            consecutiveBlackFrames = 0;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to get opencv frame: " + e.getMessage());
                }
            }
            
            BufferedImage imageToSend;
            if (currentFrame != null) {
                imageToSend = new BufferedImage(currentFrame.getWidth(), currentFrame.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D og = imageToSend.createGraphics();
                og.drawImage(currentFrame, 0, 0, null);
                
                og.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                og.setFont(new Font("Monospaced", Font.BOLD, 12));
                og.setColor(new Color(0, 255, 0, 220));
                og.drawString("LIVE CAMERA: " + myUsername.toUpperCase(), 15, 25);
                
                String timeStr = LocalDateTime.now().format(TIME_FMT);
                og.setColor(new Color(255, 255, 255, 180));
                og.drawString(timeStr, 15, imageToSend.getHeight() - 15);
                
                pulseCount++;
                if ((pulseCount / 3) % 2 == 0) {
                    og.setColor(Color.RED);
                    og.fillOval(imageToSend.getWidth() - 55, 14, 10, 10);
                    og.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    og.setColor(Color.WHITE);
                    og.drawString("REC", imageToSend.getWidth() - 40, 23);
                }
                og.dispose();
            } else {
                simG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                simG.setColor(new Color(30, 30, 35));
                simG.fillRect(0, 0, width, height);
                
                simG.setColor(new Color(45, 45, 50));
                for (int i = 0; i < width; i += 40) {
                    simG.drawLine(i, 0, i, height);
                }
                for (int i = 0; i < height; i += 40) {
                    simG.drawLine(0, i, width, i);
                }
                
                ballX += ballDX;
                ballY += ballDY;
                if (ballX - ballRadius < 0 || ballX + ballRadius > width) {
                    ballDX = -ballDX;
                    ballX = Math.max(ballRadius, Math.min(width - ballRadius, ballX));
                }
                if (ballY - ballRadius < 0 || ballY + ballRadius > height) {
                    ballDY = -ballDY;
                    ballY = Math.max(ballRadius, Math.min(height - ballRadius, ballY));
                }
                
                float h = (System.currentTimeMillis() % 4000) / 4000.0f;
                Color ballColor = Color.getHSBColor(h, 0.8f, 0.9f);
                RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point(ballX - 5, ballY - 5), ballRadius,
                    new float[]{0.0f, 0.8f, 1.0f},
                    new Color[]{Color.WHITE, ballColor, ballColor.darker()}
                );
                simG.setPaint(rgp);
                simG.fillOval(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2);
                
                simG.setColor(new Color(0, 0, 0, 80));
                simG.drawOval(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2);
                
                for (int i = 0; i < 300; i++) {
                    int nx = random.nextInt(width);
                    int ny = random.nextInt(height);
                    int nval = random.nextInt(40) + 10;
                    simG.setColor(new Color(255, 255, 255, nval));
                    simG.fillRect(nx, ny, 1, 1);
                }
                
                int scanY = (int) ((System.currentTimeMillis() / 15) % height);
                simG.setColor(new Color(0, 255, 0, 25));
                simG.fillRect(0, scanY - 2, width, 4);
                simG.setColor(new Color(0, 255, 0, 40));
                simG.drawLine(0, scanY, width, scanY);
                
                simG.setFont(new Font("Monospaced", Font.BOLD, 12));
                simG.setColor(new Color(255, 255, 255, 180));
                simG.drawString("CAMERA SIMULATOR: " + myUsername.toUpperCase(), 15, 25);
                
                String timeStr = LocalDateTime.now().format(TIME_FMT);
                simG.drawString(timeStr, 15, height - 15);
                
                pulseCount++;
                if ((pulseCount / 3) % 2 == 0) {
                    simG.setColor(Color.RED);
                    simG.fillOval(width - 55, 14, 10, 10);
                    simG.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    simG.setColor(Color.WHITE);
                    simG.drawString("REC", width - 40, 23);
                }
                
                imageToSend = simImage;
            }
            
            localPanel.setFrame(imageToSend);
            
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(imageToSend, "jpg", baos);
                byte[] jpegData = baos.toByteArray();
                
                if (remoteAddress != null && remotePort > 0 && udpSocket != null) {
                    byte[] packetData = new byte[jpegData.length + 1];
                    packetData[0] = 0x00; // 0x00 for Video
                    System.arraycopy(jpegData, 0, packetData, 1, jpegData.length);
                    
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, remoteAddress, remotePort);
                    udpSocket.send(packet);
                    sendPacketCount++;
                }
            } catch (Exception e) {
                System.err.println("Video frame compression/send error: " + e.getMessage());
            }
            
            try {
                Thread.sleep(70); // ~14 FPS
            } catch (InterruptedException e) {
                break;
            }
        }
        
        simG.dispose();
        converter.close();
        
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.close();
            } catch (Exception ignored) {}
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
            audioTargetLine = (TargetDataLine) AudioSystem.getLine(info);
            audioTargetLine.open(format);
            audioTargetLine.start();
            
            byte[] buffer = new byte[1024]; // Low latency buffer
            int packetCount = 0;
            while (isCalling && !Thread.currentThread().isInterrupted()) {
                int read = audioTargetLine.read(buffer, 0, buffer.length);
                if (read > 0 && remoteAddress != null && remotePort > 0 && udpSocket != null) {
                    byte[] packetData = new byte[read + 1];
                    packetData[0] = 0x01; // 0x01 for Audio
                    System.arraycopy(buffer, 0, packetData, 1, read);
                    
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, remoteAddress, remotePort);
                    try {
                        udpSocket.send(packet);
                        packetCount++;
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        } catch (LineUnavailableException e) {
            System.err.println("Microphone line unavailable: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Audio capture error: " + e.getMessage());
        }
    }

    private void receiveVideoLoop() {
        byte[] receiveBuffer = new byte[65507];
        int videoCount = 0;
        int audioCount = 0;
        while (isCalling && !Thread.currentThread().isInterrupted() && udpSocket != null && !udpSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(packet);
                
                int len = packet.getLength();
                if (len < 1) continue;
                
                byte type = packet.getData()[packet.getOffset()];
                if (type == 0x00) { // Video packet
                    videoCount++;
                    
                    byte[] data = new byte[len - 1];
                    System.arraycopy(packet.getData(), packet.getOffset() + 1, data, 0, len - 1);
                    
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                    if (img != null) {
                        remotePanel.setFrame(img);
                    }
                } else if (type == 0x01) { // Audio packet
                    audioCount++;
                    
                    if (audioSourceLine != null) {
                        audioSourceLine.write(packet.getData(), packet.getOffset() + 1, len - 1);
                    }
                }
            } catch (java.io.IOException e) {
                if (isCalling) {
                    System.err.println("Video/Audio receive error: " + e.getMessage());
                }
            }
        }
    }
    
    public void receiveVideo(String base64Data) {
        // Backwards compatibility stub, not used for UDP streaming
    }
    
    private boolean isFrameBlack(BufferedImage img) {
        if (img == null) return true;
        int w = img.getWidth();
        int h = img.getHeight();
        int[] samples = {
            img.getRGB(w / 2, h / 2),
            img.getRGB(w / 4, h / 4),
            img.getRGB(3 * w / 4, 3 * h / 4),
            img.getRGB(w / 4, 3 * h / 4),
            img.getRGB(3 * w / 4, h / 4)
        };
        for (int rgb : samples) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            if (r > 8 || g > 8 || b > 8) {
                return false;
            }
        }
        return true;
    }
    
    public static class VideoPanel extends JPanel {
        private BufferedImage currentFrame;
        private final String label;
        
        public VideoPanel(String label) {
            this.label = label;
            setBackground(new Color(25, 25, 30));
            setPreferredSize(new Dimension(320, 240));
            setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 2));
        }
        
        public synchronized void setFrame(BufferedImage frame) {
            BufferedImage copy = new BufferedImage(frame.getWidth(), frame.getHeight(), frame.getType());
            Graphics g = copy.getGraphics();
            g.drawImage(frame, 0, 0, null);
            g.dispose();
            
            this.currentFrame = copy;
            repaint();
        }
        
        public synchronized void clearFrame() {
            this.currentFrame = null;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            synchronized (this) {
                if (currentFrame != null) {
                    g.drawImage(currentFrame, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g.setColor(new Color(40, 40, 45));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    
                    g.setColor(new Color(70, 70, 75));
                    g.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    FontMetrics fm = g.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(label)) / 2;
                    int y = getHeight() / 2 - 10;
                    g.drawString(label, x, y);
                    
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    fm = g.getFontMetrics();
                    String sub = "Stream offline";
                    x = (getWidth() - fm.stringWidth(sub)) / 2;
                    y = getHeight() / 2 + 15;
                    g.drawString(sub, x, y);
                }
                
                // Translucent label overlay badge at top-left corner
                g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                int labelHeight = fm.getHeight();
                
                // Background badge
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(8, 8, labelWidth + 16, labelHeight + 8, 8, 8);
                
                // Border badge
                g.setColor(new Color(255, 255, 255, 60));
                g.drawRoundRect(8, 8, labelWidth + 16, labelHeight + 8, 8, 8);
                
                // Text
                g.setColor(Color.WHITE);
                g.drawString(label, 16, 8 + labelHeight + 2);
            }
        }
    }
}
