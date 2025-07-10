package ca.corbett.musicplayer.extensions.scenery;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Extracted from ExtraVisualizers (from the Album Art Visualizer), and this should
 * really be promoted up into the app so that other extensions could use it.
 * It's pretty neat.
 *
 * @author scorbo2
 */
public class ImageScroller {

    /**
     * Determines pixels per frame of animation movement.
     */
    public enum ScrollSpeed {
        VERY_SLOW("Very slow", 1),
        SLOW("Slow", 2),
        MEDIUM("Medium", 3),
        FAST("Fast", 4),
        VERY_FAST("Very fast", 5);

        private final String label;
        private final int speed;

        ScrollSpeed(String label, int speed) {
            this.label = label;
            this.speed = speed;
        }

        @Override
        public String toString() {
            return label;
        }

        public int getSpeed() {
            return speed;
        }
    }

    /**
     * Determines the extent to which we slow down as we approach a scroll limit, and also
     * how quickly we speed up as we move away from a scroll limit. This makes for a nice
     * and natural "bounce" effect when we scroll right to the limit, instead of just reversing
     * direction at the same speed instantly.
     */
    public enum EasingStrength {
        LINEAR("Linear", 1.0f),
        QUADRATIC("Quadratic", 2.0f),
        CUBIC("Cubic", 3.0f);

        private final String label;
        private final float value;

        EasingStrength(String label, float value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }

        public float getValue() {
            return value;
        }
    }

    private boolean isRunning;

    private ScrollSpeed scrollSpeed;
    private EasingStrength easingStrength;
    private BufferedImage image;
    private final int displayWidth;
    private final int displayHeight;
    private float zoomFactor;
    private int xOffset;
    private int yOffset;
    private float xDelta;
    private float yDelta;
    private int xDirection = -1; // -1 for left, +1 for right
    private int yDirection = -1; // -1 for up, +1 for down
    private boolean scaleCalculationsDone;

    // Configuration for bounce behavior (we could expose these as props):
    private final float bounceZoneRatio = 0.06f; // What fraction of the scrollable area is the "bounce zone"
    private final float minSpeedRatio = 0.1f;   // Minimum speed as a ratio of max speed (0.0 = complete stop, 1.0 = no slowdown)
    private final float easingPower = 2.0f;     // Power for easing curve (1.0 = linear, 2.0 = quadratic, 3.0 = cubic, etc.)

    public ImageScroller(BufferedImage image, int displayWidth, int displayHeight) {
        scrollSpeed = ScrollSpeed.SLOW;
        easingStrength = EasingStrength.QUADRATIC;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        setImage(image);
    }

    public ScrollSpeed getScrollSpeed() {
        return scrollSpeed;
    }

    public void setScrollSpeed(ScrollSpeed scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
    }

    public EasingStrength getEasingStrength() {
        return easingStrength;
    }

    public void setEasingStrength(EasingStrength easingStrength) {
        this.easingStrength = easingStrength;
    }

    public void setImage(BufferedImage image) {
        stop();

        boolean isLandscape = image.getWidth() > image.getHeight();
        int scaledWidth;
        int scaledHeight;

        if (isLandscape) {
            // Scale based on display height - the constraining dimension
            double scaleFactor = (double) displayHeight / image.getHeight();
            scaledWidth = (int) Math.round(image.getWidth() * scaleFactor);
            scaledHeight = displayHeight;
        } else {
            // For portrait and square images, scale based on display width
            double scaleFactor = (double) displayWidth / image.getWidth();
            scaledWidth = displayWidth;
            scaledHeight = (int) Math.round(image.getHeight() * scaleFactor);
        }

        // Create the new scaled image
        this.image = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = this.image.createGraphics();

        // Enable high-quality rendering
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the original image scaled to the new dimensions
        g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        reset();
    }

    private void reset() {
        zoomFactor = 0f;
        xOffset = 0;
        yOffset = 0;
        xDelta = 0;
        yDelta = 0;
        scaleCalculationsDone = false;
        isRunning = true;
    }

    public void stop() {
        if (image != null) {
            image.flush();
        }
        isRunning = false;
    }

    /**
     * Renders a single frame of animation and handles scrolling the image by an appropriate amount.
     */
    public void renderFrame(Graphics2D g) {
        // If we're stopped (e.g. to load the next image), just return:
        if (! isRunning) {
            return;
        }

        // Only do the scale calculations once:
        if (!scaleCalculationsDone) {
            xOffset = 0;
            yOffset = 0;
            scaleCalculationsDone = true;
            boolean isPortrait = image.getHeight() > image.getWidth();
            zoomFactor = isPortrait ? (float)displayWidth / image.getWidth() : (float)displayHeight / image.getHeight();
            if (zoomFactor <= 0.0) {
                zoomFactor = 1;
            }
            if (isPortrait) {
                yDirection = -1; // start scrolling up
            }
            else {
                xDirection = -1; // start scrolling left
            }
            int imgWidth = (int)(image.getWidth() * zoomFactor);
            int imgHeight = (int)(image.getHeight() * zoomFactor);

            // Wonky case: if we scale it down and it ends up fitting inside the screen,
            // we can't scroll around inside it, so just center it instead:
            if (imgWidth <= displayWidth && imgHeight <= displayHeight) {
                xDelta = 0;
                yDelta = 0;
                xOffset = (displayWidth / 2) - (imgWidth / 2);
                yOffset = (displayHeight / 2) - (imgHeight / 2);
            }
        }
        int imgWidth = (int)(image.getWidth() * zoomFactor);
        int imgHeight = (int)(image.getHeight() * zoomFactor);
        g.drawImage(image, xOffset, yOffset, imgWidth, imgHeight, null);

        // Calculate base speed
        float baseSpeed = scrollSpeed.getSpeed();

        // Update horizontal movement
        if (imgWidth > displayWidth) {
            float speedMultiplier = calculateSpeedMultiplier(xOffset, displayWidth - imgWidth, displayWidth);
            xDelta = xDirection * baseSpeed * speedMultiplier;

            // Ensure minimum movement of 1 pixel to prevent animation from getting stuck
            if (xDirection == -1 && xDelta > -1) {
                xDelta = -1;
            }
            else if (xDirection == 1 && xDelta < 1) {
                xDelta = 1;
            }

            xOffset += (int)xDelta;

            // Check bounds and reverse direction if needed
            if (xOffset >= 0) {
                xOffset = 0;
                xDirection = -1;
            }
            else if (xOffset <= (displayWidth - imgWidth)) {
                xOffset = displayWidth - imgWidth;
                xDirection = 1;
            }
        }

        // Update vertical movement
        if (imgHeight > displayHeight) {
            float speedMultiplier = calculateSpeedMultiplier(yOffset, displayHeight - imgHeight, displayHeight);
            yDelta = yDirection * baseSpeed * speedMultiplier;

            // Ensure minimum movement of 1 pixel to prevent animation from getting stuck
            if (yDirection == -1 && yDelta > -1) {
                yDelta = -1;
            }
            else if (yDirection == 1 && yDelta < 1) {
                yDelta = 1;
            }

            yOffset += (int)yDelta;

            // Check bounds and reverse direction if needed
            if (yOffset >= 0) {
                yOffset = 0;
                yDirection = -1;
            }
            else if (yOffset <= (displayHeight - imgHeight)) {
                yOffset = displayHeight - imgHeight;
                yDirection = 1;
            }
        }
    }

    /**
     * Calculates speed multiplier based on distance from bounce points
     *
     * @param currentPos Current position (xOffset or yOffset)
     * @param minPos     Minimum position (boundary)
     * @param screenSize Screen dimension (width or height)
     * @return Speed multiplier between minSpeedRatio and 1.0
     */
    private float calculateSpeedMultiplier(int currentPos, int minPos, int screenSize) {
        // Calculate total scrollable distance
        int totalDistance = Math.abs(minPos);
        if (totalDistance == 0) { return 1.0f; }

        // Calculate bounce zone size
        int bounceZoneSize = (int)(totalDistance * bounceZoneRatio);
        if (bounceZoneSize == 0) { return 1.0f; }

        // Distance from top boundary (0)
        int distanceFromTop = Math.abs(currentPos);

        // Distance from bottom boundary
        int distanceFromBottom = Math.abs(currentPos - minPos);

        // Find the minimum distance to any boundary
        int distanceFromNearestBound = Math.min(distanceFromTop, distanceFromBottom);

        // If we're outside the bounce zone, use full speed
        if (distanceFromNearestBound >= bounceZoneSize) {
            return 1.0f;
        }

        // Calculate easing factor (0.0 at boundary, 1.0 at edge of bounce zone)
        float easingFactor = (float)distanceFromNearestBound / bounceZoneSize;

        // Apply easing curve
        easingFactor = (float)Math.pow(easingFactor, easingPower);

        // Interpolate between minimum and maximum speed
        return minSpeedRatio + (1.0f - minSpeedRatio) * easingFactor;
    }
}
