package com.chatapp.client.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Emoji picker popup panel.
 * Displays a grid of emojis that can be inserted into the chat input.
 */
public class EmojiPicker extends JPopupMenu {

    private static final Color BG = new Color(50, 50, 55);
    private static final Color HOVER = new Color(70, 70, 75);

    // Emoji collection (minimum 5 required, providing more for variety)
    private static final String[] EMOJIS = {
            "😀", "😂", "😍", "🤣", "😎",
            "😢", "😡", "🤔", "👍", "👎",
            "🩷", "🔥", "🎉", "👋", "🙏",
            "💯", "✅", "⭐", "😱", "🥳"
    };

    private EmojiSelectListener listener;

    public EmojiPicker() {
        initUI();
    }

    public void setEmojiSelectListener(EmojiSelectListener listener) {
        this.listener = listener;
    }

    private void initUI() {
        setBackground(BG);
        setBorder(BorderFactory.createLineBorder(new Color(80, 80, 85)));
        setLayout(new BorderLayout());

        // Title
        JLabel title = new JLabel("  Emoji", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(new Color(160, 160, 165));
        title.setBorder(new EmptyBorder(6, 8, 4, 8));
        title.setOpaque(true);
        title.setBackground(BG);
        add(title, BorderLayout.NORTH);

        // Emoji grid
        JPanel grid = new JPanel(new GridLayout(4, 5, 2, 2));
        grid.setBackground(BG);
        grid.setBorder(new EmptyBorder(4, 6, 6, 6));

        for (String emoji : EMOJIS) {
            JLabel emojiLabel = new JLabel(emoji, SwingConstants.CENTER);
            emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
            emojiLabel.setOpaque(true);
            emojiLabel.setBackground(BG);
            emojiLabel.setPreferredSize(new Dimension(38, 38));
            emojiLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            emojiLabel.setToolTipText(emoji);

            emojiLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    emojiLabel.setBackground(HOVER);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    emojiLabel.setBackground(BG);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (listener != null) {
                        listener.onEmojiSelected(emoji);
                    }
                    setVisible(false);
                }
            });

            grid.add(emojiLabel);
        }

        add(grid, BorderLayout.CENTER);
    }

    /**
     * Callback when an emoji is selected.
     */
    public interface EmojiSelectListener {
        void onEmojiSelected(String emoji);
    }
}
