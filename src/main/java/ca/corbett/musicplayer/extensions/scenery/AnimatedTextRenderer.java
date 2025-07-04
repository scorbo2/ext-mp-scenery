package ca.corbett.musicplayer.extensions.scenery;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class AnimatedTextRenderer {
    // Default styling constants
    private static final Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 48);
    private static final Color DEFAULT_TEXT_COLOR = Color.GREEN;
    private static final Color DEFAULT_BACKGROUND_COLOR = Color.BLACK;

    // Animation state
    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;
    private String textToRender;
    private List<String> wrappedLines;
    private int totalChars;
    private int currentCharIndex;
    private double charsPerSecond;
    private long lastUpdateTime;
    private double charAccumulator; // For smooth fractional character progression

    // Styling
    private Font font;
    private Color textColor;
    private Color backgroundColor;

    // Layout
    private int padding;
    private boolean needsReflow;

    public AnimatedTextRenderer(int width, int height, String text) {
        this(width, height, text, 3.0, DEFAULT_FONT, DEFAULT_TEXT_COLOR, DEFAULT_BACKGROUND_COLOR);
    }

    public AnimatedTextRenderer(int width, int height, String text, double charsPerSecond) {
        this(width, height, text, charsPerSecond, DEFAULT_FONT, DEFAULT_TEXT_COLOR, DEFAULT_BACKGROUND_COLOR);
    }

    public AnimatedTextRenderer(int width, int height, String text, double charsPerSecond,
                                Font font, Color textColor, Color backgroundColor) {
        this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.bufferGraphics = buffer.createGraphics();
        this.textToRender = text;
        this.charsPerSecond = charsPerSecond;
        this.font = font;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.padding = 10;
        this.needsReflow = true;

        // Initialize animation state
        this.currentCharIndex = 0;
        this.charAccumulator = 0.0;
        this.lastUpdateTime = System.currentTimeMillis();

        // Set up graphics context
        setupGraphics();

        // Initial clear
        clearBuffer();
    }

    private void setupGraphics() {
        bufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        bufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        bufferGraphics.setFont(font);
        bufferGraphics.setColor(textColor);
    }

    private void clearBuffer() {
        bufferGraphics.setColor(backgroundColor);
        bufferGraphics.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
        bufferGraphics.setColor(textColor);
    }

    /**
     * Updates the animation state and renders new characters if needed.
     * Call this once per frame from your animation loop.
     */
    public void updateTextAnimation() {
        if (textToRender == null || textToRender.isEmpty()) {
            return;
        }

        // Reflow text if needed (first time or after changes)
        if (needsReflow) {
            reflowText();
            needsReflow = false;
        }

        // Calculate time delta
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0; // Convert to seconds
        lastUpdateTime = currentTime;

        // Update character accumulator
        charAccumulator += charsPerSecond * deltaTime;

        // Check if we need to reveal more characters
        int newCharIndex = Math.min((int)charAccumulator, totalChars);

        if (newCharIndex > currentCharIndex) {
            // Render new characters
            renderUpToCharacter(newCharIndex);
            currentCharIndex = newCharIndex;
        }
    }

    /**
     * Reflows the text to fit within the buffer bounds
     */
    private void reflowText() {
        if (textToRender == null) {
            return;
        }

        FontRenderContext frc = bufferGraphics.getFontRenderContext();
        int availableWidth = buffer.getWidth() - (padding * 2);

        this.wrappedLines = wrapText(textToRender, frc, font, availableWidth);
        this.totalChars = textToRender.length();

        // Clear buffer and reset animation
        clearBuffer();
        this.currentCharIndex = 0;
        this.charAccumulator = 0.0;
    }

    /**
     * Renders text up to the specified character index
     */
    private void renderUpToCharacter(int charIndex) {
        if (wrappedLines == null || wrappedLines.isEmpty()) {
            return;
        }

        // Clear buffer
        clearBuffer();

        FontMetrics fm = bufferGraphics.getFontMetrics();
        int lineHeight = fm.getHeight();
        int y = padding + fm.getAscent();

        int charsProcessed = 0;

        for (String line : wrappedLines) {
            if (charsProcessed >= charIndex) {
                break;
            }

            // Determine how many characters of this line to draw
            int charsToDrawFromLine = Math.min(line.length(), charIndex - charsProcessed);
            String textToDraw = line.substring(0, charsToDrawFromLine);

            // Draw the text
            System.out.println("drawString("+textToDraw+")");
            bufferGraphics.drawString(textToDraw, padding, y);

            charsProcessed += line.length();
            y += lineHeight;

            // Check if we've exceeded the bottom boundary
            if (y > buffer.getHeight() - padding) {
                break;
            }
        }
    }

    /**
     * Wraps text to fit within the specified width
     */
    private List<String> wrapText(String text, FontRenderContext frc, Font font, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");

        if (words.length == 0) {
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;

            // Check if adding this word would exceed the width
            TextLayout layout = new TextLayout(testLine, font, frc);
            if (layout.getBounds().getWidth() > maxWidth && !currentLine.isEmpty()) {
                // Start a new line
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
            else {
                currentLine = new StringBuilder(testLine);
            }
        }

        // Add the last line if it has content
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Returns the current buffer image
     */
    public BufferedImage getBuffer() {
        return buffer;
    }

    /**
     * Changes the text to animate (restarts animation)
     */
    public void setText(String text) {
        this.textToRender = text;
        this.needsReflow = true;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Changes the animation speed
     */
    public void setCharsPerSecond(double charsPerSecond) {
        this.charsPerSecond = charsPerSecond;
    }

    /**
     * Changes the font (triggers reflow)
     */
    public void setFont(Font font) {
        this.font = font;
        this.needsReflow = true;
        bufferGraphics.setFont(font);
    }

    /**
     * Changes text color
     */
    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        // Re-render current text with new color
        if (currentCharIndex > 0) {
            renderUpToCharacter(currentCharIndex);
        }
    }

    /**
     * Changes background color
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        // Re-render current text with new background
        if (currentCharIndex > 0) {
            renderUpToCharacter(currentCharIndex);
        }
    }

    /**
     * Sets padding around text
     */
    public void setPadding(int padding) {
        this.padding = padding;
        this.needsReflow = true;
    }

    /**
     * Checks if animation is complete
     */
    public boolean isAnimationComplete() {
        return currentCharIndex >= totalChars;
    }

    /**
     * Immediately shows all text (skips animation)
     */
    public void showAllText() {
        if (wrappedLines != null) {
            renderUpToCharacter(totalChars);
            currentCharIndex = totalChars;
            charAccumulator = totalChars;
        }
    }

    /**
     * Resets animation to beginning
     */
    public void resetAnimation() {
        this.currentCharIndex = 0;
        this.charAccumulator = 0.0;
        this.lastUpdateTime = System.currentTimeMillis();
        clearBuffer();
    }

    /**
     * Gets current progress as a percentage (0.0 to 1.0)
     */
    public double getProgress() {
        return totalChars > 0 ? (double)currentCharIndex / totalChars : 0.0;
    }

    /**
     * Cleans up resources
     */
    public void dispose() {
        if (bufferGraphics != null) {
            bufferGraphics.dispose();
        }
    }
}
