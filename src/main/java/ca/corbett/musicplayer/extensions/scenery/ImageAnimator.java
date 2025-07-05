package ca.corbett.musicplayer.extensions.scenery;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageAnimator {
    private BufferedImage image;
    private double startX, startY;
    private double destX, destY;
    private double currentX, currentY;
    private double maxSpeed;
    private double easingStrength;

    private long lastUpdateTime;
    private boolean movementComplete;
    private double totalDistance;
    private double directionX, directionY; // Unit vector for direction

    // Easing types
    public enum EasingType {
        EASE_IN_OUT,
        EASE_IN,
        EASE_OUT,
        LINEAR
    }

    private EasingType easingType;

    /**
     * Creates an ImageAnimator with default easing settings.
     */
    public ImageAnimator(BufferedImage image, double startX, double startY,
                         double destX, double destY, double maxSpeed) {
        this(image, startX, startY, destX, destY, maxSpeed, 2.0, EasingType.EASE_IN_OUT);
    }

    /**
     * Creates an ImageAnimator with configurable easing.
     *
     * @param image The BufferedImage to animate
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param destX Destination X coordinate
     * @param destY Destination Y coordinate
     * @param maxSpeed Maximum movement speed in pixels per second
     * @param easingStrength Strength of easing effect (1.0 = linear, higher = more curved)
     * @param easingType Type of easing to apply
     */
    public ImageAnimator(BufferedImage image, double startX, double startY,
                         double destX, double destY, double maxSpeed,
                         double easingStrength, EasingType easingType) {
        this.image = image;
        this.startX = startX;
        this.startY = startY;
        this.destX = destX;
        this.destY = destY;
        this.currentX = startX;
        this.currentY = startY;
        this.maxSpeed = maxSpeed;
        this.easingStrength = Math.max(1.0, easingStrength);
        this.easingType = easingType;

        this.lastUpdateTime = System.nanoTime();
        this.movementComplete = false;

        calculateMovementParameters();
    }

    private void calculateMovementParameters() {
        double deltaX = destX - startX;
        double deltaY = destY - startY;
        this.totalDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (totalDistance > 0) {
            this.directionX = deltaX / totalDistance;
            this.directionY = deltaY / totalDistance;
        } else {
            this.directionX = 0;
            this.directionY = 0;
            this.movementComplete = true;
        }
    }

    /**
     * Updates the image position based on elapsed time and easing function.
     * Call this method once per frame in your animation loop.
     */
    public void update() {
        if (movementComplete) {
            return;
        }

        long currentTime = System.nanoTime();
        double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0; // Convert to seconds
        lastUpdateTime = currentTime;

        // Calculate current distance from start
        double currentDistanceFromStart = Math.sqrt(
            (currentX - startX) * (currentX - startX) +
                (currentY - startY) * (currentY - startY)
        );

        // Calculate progress (0.0 to 1.0)
        double progress = totalDistance > 0 ? currentDistanceFromStart / totalDistance : 1.0;

        // Apply easing function to get speed multiplier
        double speedMultiplier = calculateEasing(progress);

        // Calculate movement for this frame
        double frameDistance = maxSpeed * speedMultiplier * deltaTime;

        // Guarantee minimum movement of 1 pixel per frame (when not at destination)
        double remainingDistance = Math.sqrt(
            (destX - currentX) * (destX - currentX) +
                (destY - currentY) * (destY - currentY)
        );

        if (remainingDistance > 0) {
            // Ensure we move at least 1 pixel per frame, but not more than remaining distance
            frameDistance = Math.max(frameDistance, 1.0);
            frameDistance = Math.min(frameDistance, remainingDistance);
        }

        // Calculate new position
        double newX = currentX + directionX * frameDistance;
        double newY = currentY + directionY * frameDistance;

        // Check if we've reached the destination
        if (frameDistance >= remainingDistance || remainingDistance <= 0.5) {
            // We've reached the destination (within half a pixel)
            currentX = destX;
            currentY = destY;
            movementComplete = true;
        } else {
            currentX = newX;
            currentY = newY;
        }
    }

    private double calculateEasing(double progress) {
        // Clamp progress to [0, 1]
        progress = Math.max(0.0, Math.min(1.0, progress));

        switch (easingType) {
            case LINEAR:
                return 1.0;

            case EASE_IN:
                return Math.pow(progress, easingStrength);

            case EASE_OUT:
                return 1.0 - Math.pow(1.0 - progress, easingStrength);

            case EASE_IN_OUT:
            default:
                if (progress < 0.5) {
                    return 0.5 * Math.pow(2.0 * progress, easingStrength);
                } else {
                    return 1.0 - 0.5 * Math.pow(2.0 * (1.0 - progress), easingStrength);
                }
        }
    }

    /**
     * Renders the image at its current position.
     */
    public void render(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) Math.round(currentX), (int) Math.round(currentY), null);
        }
    }

    /**
     * @return true if the image has reached its destination
     */
    public boolean isMovementComplete() {
        return movementComplete;
    }

    /**
     * Sets a new destination for the image. The image will start moving towards
     * this new destination from its current position.
     */
    public void setDestination(double newDestX, double newDestY) {
        this.startX = this.currentX;
        this.startY = this.currentY;
        this.destX = newDestX;
        this.destY = newDestY;
        this.movementComplete = false;
        this.lastUpdateTime = System.nanoTime();
        calculateMovementParameters();
    }

    /**
     * Immediately positions the image at the specified coordinates without animation.
     */
    public void setPosition(double x, double y) {
        this.currentX = x;
        this.currentY = y;
        this.startX = x;
        this.startY = y;
    }

    /**
     * @return current X position of the image
     */
    public double getCurrentX() {
        return currentX;
    }

    /**
     * @return current Y position of the image
     */
    public double getCurrentY() {
        return currentY;
    }

    /**
     * @return the BufferedImage being animated
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Sets a new image to animate (maintains current position and destination).
     */
    public void setImage(BufferedImage newImage) {
        this.image = newImage;
    }

    /**
     * @return current movement progress as a value between 0.0 and 1.0
     */
    public double getProgress() {
        if (totalDistance == 0) return 1.0;

        double currentDistanceFromStart = Math.sqrt(
            (currentX - startX) * (currentX - startX) +
                (currentY - startY) * (currentY - startY)
        );
        return Math.min(1.0, currentDistanceFromStart / totalDistance);
    }

    /**
     * Updates the maximum movement speed.
     */
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    /**
     * Updates the easing strength.
     */
    public void setEasingStrength(double easingStrength) {
        this.easingStrength = Math.max(1.0, easingStrength);
    }

    /**
     * Updates the easing type.
     */
    public void setEasingType(EasingType easingType) {
        this.easingType = easingType;
    }
}