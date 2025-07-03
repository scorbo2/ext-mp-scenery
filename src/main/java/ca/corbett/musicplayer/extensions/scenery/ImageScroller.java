package ca.corbett.musicplayer.extensions.scenery;

import java.awt.Color;
import java.awt.Graphics2D;
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

    private ScrollSpeed scrollSpeed;
    private EasingStrength easingStrength;
    private final BufferedImage image;
    private int width;
    private int height;
    private float zoomFactor;
    private int xOffset;
    private int yOffset;
    private float xDelta;
    private float yDelta;
    private int xDirection = -1; // -1 for left, +1 for right
    private int yDirection = -1; // -1 for up, +1 for down
    private boolean scaleCalculationsDone;

    // Configuration for bounce behavior
    private float bounceZoneRatio = 0.06f; // What fraction of the scrollable area is the "bounce zone"
    private float minSpeedRatio = 0.1f;   // Minimum speed as a ratio of max speed (0.0 = complete stop, 1.0 = no slowdown)
    private float easingPower = 2.0f;     // Power for easing curve (1.0 = linear, 2.0 = quadratic, 3.0 = cubic, etc.)

    public ImageScroller(BufferedImage image) {
        this.image = image;
        scrollSpeed = ScrollSpeed.SLOW;
        easingStrength = EasingStrength.QUADRATIC;
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

    private void reset() {
        zoomFactor = 0f;
        xOffset = 0;
        yOffset = 0;
        xDelta = 0;
        yDelta = 0;
        scaleCalculationsDone = false;
    }

    public void stop() {
        image.flush();
    }

    /**
     * Renders a single frame of animation and handles scrolling the image by an appropriate amount.
     */
    public void renderFrame(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // Only do the scale calculations once:
        if (!scaleCalculationsDone) {
            xOffset = 0;
            yOffset = 0;
            scaleCalculationsDone = true;
            boolean isPortrait = image.getHeight() > image.getWidth();
            zoomFactor = isPortrait ? (float)width / image.getWidth() : (float)height / image.getHeight();
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
            if (imgWidth <= width && imgHeight <= height) {
                xDelta = 0;
                yDelta = 0;
                xOffset = (width / 2) - (imgWidth / 2);
                yOffset = (height / 2) - (imgHeight / 2);
            }
        }
        int imgWidth = (int)(image.getWidth() * zoomFactor);
        int imgHeight = (int)(image.getHeight() * zoomFactor);
        g.drawImage(image, xOffset, yOffset, imgWidth, imgHeight, null);

        // Calculate base speed
        float baseSpeed = scrollSpeed.getSpeed();

        // Update horizontal movement
        if (imgWidth > width) {
            float speedMultiplier = calculateSpeedMultiplier(xOffset, width - imgWidth, width);
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
            else if (xOffset <= (width - imgWidth)) {
                xOffset = width - imgWidth;
                xDirection = 1;
            }
        }

        // Update vertical movement
        if (imgHeight > height) {
            float speedMultiplier = calculateSpeedMultiplier(yOffset, height - imgHeight, height);
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
            else if (yOffset <= (height - imgHeight)) {
                yOffset = height - imgHeight;
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
