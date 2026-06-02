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
    private com.github.sarxos.webcam.Webcam activeWebcam;
    
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
        
        if (activeWebcam != null) {
            try {
                activeWebcam.close();
            } catch (Exception ignored) {}
            activeWebcam = null;
        }
        
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
        
        try {
            activeWebcam = com.github.sarxos.webcam.Webcam.getDefault();
            if (activeWebcam != null) {
                java.awt.Dimension[] sizes = activeWebcam.getViewSizes();
                boolean sizeSupported = false;
                for (java.awt.Dimension d : sizes) {
                    if (d.width == width && d.height == height) {
                        sizeSupported = true;
                        break;
                    }
                }
                if (sizeSupported) {
                    activeWebcam.setViewSize(new java.awt.Dimension(width, height));
                } else if (sizes.length > 0) {
                    activeWebcam.setViewSize(sizes[0]);
                    width = sizes[0].width;
                    height = sizes[0].height;
                }
                activeWebcam.open();
            }
        } catch (Exception e) {
            System.err.println("Webcam initialization failed, falling back to simulator: " + e.getMessage());
            activeWebcam = null;
        }
        
        BufferedImage simImage = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D simG = simImage.createGraphics();
        
        int ballX = 320 / 2;
        int ballY = 240 / 2;
        int ballDX = 4;
        int ballDY = 3;
        int ballRadius = 25;
        
        int pulseCount = 0;
        
        while (isCalling && !Thread.currentThread().isInterrupted()) {
            BufferedImage currentFrame = null;
            if (activeWebcam != null && activeWebcam.isOpen()) {
                try {
                    currentFrame = activeWebcam.getImage();
                } catch (Exception e) {
                    System.err.println("Failed to get webcam frame: " + e.getMessage());
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
                simG.fillRect(0, 0, 320, 240);
                
                simG.setColor(new Color(45, 45, 50));
                for (int i = 0; i < 320; i += 40) {
                    simG.drawLine(i, 0, i, 240);
                }
                for (int i = 0; i < 240; i += 40) {
                    simG.drawLine(0, i, 320, i);
                }
                
                ballX += ballDX;
                ballY += ballDY;
                if (ballX - ballRadius < 0 || ballX + ballRadius > 320) {
                    ballDX = -ballDX;
                    ballX = Math.max(ballRadius, Math.min(320 - ballRadius, ballX));
                }
                if (ballY - ballRadius < 0 || ballY + ballRadius > 240) {
                    ballDY = -ballDY;
                    ballY = Math.max(ballRadius, Math.min(240 - ballRadius, ballY));
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
                    int nx = random.nextInt(320);
                    int ny = random.nextInt(240);
                    int nval = random.nextInt(40) + 10;
                    simG.setColor(new Color(255, 255, 255, nval));
                    simG.fillRect(nx, ny, 1, 1);
                }
                
                int scanY = (int) ((System.currentTimeMillis() / 15) % 240);
                simG.setColor(new Color(0, 255, 0, 25));
                simG.fillRect(0, scanY - 2, 320, 4);
                simG.setColor(new Color(0, 255, 0, 40));
                simG.drawLine(0, scanY, 320, scanY);
                
                simG.setFont(new Font("Monospaced", Font.BOLD, 12));
                simG.setColor(new Color(255, 255, 255, 180));
                simG.drawString("CAMERA SIMULATOR: " + myUsername.toUpperCase(), 15, 25);
                
                String timeStr = LocalDateTime.now().format(TIME_FMT);
                simG.drawString(timeStr, 15, 240 - 15);
                
                pulseCount++;
                if ((pulseCount / 3) % 2 == 0) {
                    simG.setColor(Color.RED);
                    simG.fillOval(320 - 55, 14, 10, 10);
                    simG.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    simG.setColor(Color.WHITE);
                    simG.drawString("REC", 320 - 40, 23);
                }
                
                imageToSend = simImage;
            }
            
            localPanel.setFrame(imageToSend);
            
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(imageToSend, "jpg", baos);
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
        
        simG.dispose();
        
        if (activeWebcam != null) {
            try {
                activeWebcam.close();
            } catch (Exception ignored) {}
            activeWebcam = null;
        }
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
