package com.chatapp.client.media;

import com.chatapp.client.network.ChatClient;
import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Random;

/**
 * Manages video streaming for private chats.
 * Captures animated mockup frames as standard JPEG stream to Base64 over TCP.
 */
public class VideoCallManager {
    private final ChatClient chatClient;
    private final String myUsername;
    private final String targetUser;
    
    private boolean isCalling;
    private Thread captureThread;
    
    private final VideoPanel localPanel;
    private final VideoPanel remotePanel;
    
    private final Random random = new Random();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public VideoCallManager(ChatClient chatClient, String myUsername, String targetUser) {
        this.chatClient = chatClient;
        this.myUsername = myUsername;
        this.targetUser = targetUser;
        this.isCalling = false;
        
        this.localPanel = new VideoPanel("Local Camera Preview");
        this.remotePanel = new VideoPanel("Remote Camera Stream");
    }
    
    public VideoPanel getLocalPanel() { return localPanel; }
    public VideoPanel getRemotePanel() { return remotePanel; }
    
    public synchronized void startCall() {
        if (isCalling) return;
        isCalling = true;
        
        captureThread = new Thread(this::captureVideo, "VideoCall-Capture-" + targetUser);
        captureThread.setDaemon(true);
        captureThread.start();
    }
    
    public synchronized void stopCall() {
        if (!isCalling) return;
        isCalling = false;
        
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        
        localPanel.clearFrame();
        remotePanel.clearFrame();
    }
    
    private void captureVideo() {
        int width = 320;
        int height = 240;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // Physics for bouncing ball
        int ballX = width / 2;
        int ballY = height / 2;
        int ballDX = 4;
        int ballDY = 3;
        int ballRadius = 25;
        
        int pulseCount = 0;
        
        while (isCalling && !Thread.currentThread().isInterrupted()) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 1. Draw Background
            g.setColor(new Color(30, 30, 35));
            g.fillRect(0, 0, width, height);
            
            // 2. Draw static background grid
            g.setColor(new Color(45, 45, 50));
            for (int i = 0; i < width; i += 40) {
                g.drawLine(i, 0, i, height);
            }
            for (int i = 0; i < height; i += 40) {
                g.drawLine(0, i, width, i);
            }
            
            // 3. Update and Draw bouncing ball
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
            
            // Pulsing color gradient ball
            float h = (System.currentTimeMillis() % 4000) / 4000.0f;
            Color ballColor = Color.getHSBColor(h, 0.8f, 0.9f);
            
            RadialGradientPaint rgp = new RadialGradientPaint(
                new Point(ballX - 5, ballY - 5), ballRadius,
                new float[]{0.0f, 0.8f, 1.0f},
                new Color[]{Color.WHITE, ballColor, ballColor.darker()}
            );
            g.setPaint(rgp);
            g.fillOval(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2);
            
            // Draw border outline
            g.setColor(new Color(0, 0, 0, 80));
            g.drawOval(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2);
            
            // 4. Subtle Analog static noise
            for (int i = 0; i < 300; i++) {
                int nx = random.nextInt(width);
                int ny = random.nextInt(height);
                int nval = random.nextInt(40) + 10;
                g.setColor(new Color(255, 255, 255, nval));
                g.fillRect(nx, ny, 1, 1);
            }
            
            // 5. Draw scanning line
            int scanY = (int) ((System.currentTimeMillis() / 15) % height);
            g.setColor(new Color(0, 255, 0, 25));
            g.fillRect(0, scanY - 2, width, 4);
            g.setColor(new Color(0, 255, 0, 40));
            g.drawLine(0, scanY, width, scanY);
            
            // 6. HUD info overlays
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 255, 255, 180));
            g.drawString("CAMERA: " + myUsername.toUpperCase(), 15, 25);
            
            String timeStr = LocalDateTime.now().format(TIME_FMT);
            g.drawString(timeStr, 15, height - 15);
            
            pulseCount++;
            if ((pulseCount / 3) % 2 == 0) {
                g.setColor(Color.RED);
                g.fillOval(width - 55, 14, 10, 10);
                g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g.setColor(Color.WHITE);
                g.drawString("REC", width - 40, 23);
            }
            
            localPanel.setFrame(image);
            
            // 7. Compress frame to JPEG & send
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] jpegData = baos.toByteArray();
                
                String base64Data = Base64.getEncoder().encodeToString(jpegData);
                Message msg = new Message(MessageType.VIDEO_DATA, myUsername, targetUser, base64Data);
                chatClient.sendMessage(msg);
            } catch (Exception e) {
                System.err.println("Video frame compression error: " + e.getMessage());
            }
            
            try {
                Thread.sleep(100); // 10 fps
            } catch (InterruptedException e) {
                break;
            }
        }
        
        g.dispose();
    }
    
    public void receiveVideo(String base64Data) {
        if (!isCalling) return;
        try {
            byte[] data = Base64.getDecoder().decode(base64Data);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img != null) {
                remotePanel.setFrame(img);
            }
        } catch (Exception e) {
            System.err.println("Video frame receive error: " + e.getMessage());
        }
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
            }
        }
    }
}
