package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.Color;
import java.awt.Graphics2D;

public class SceneryVisualizer extends VisualizationManager.Visualizer {

    public static final String NAME = "Scenery";
    private ImageScroller imageScroller;
    private AnimatedTextRenderer textRenderer;
    private ImageAnimator imageAnimator;
    private int displayWidth;
    private int displayHeight;

    public SceneryVisualizer() {
        super(NAME);
    }

    @Override
    public void initialize(int width, int height) {
        displayWidth = width;
        displayHeight = height;

        // TODO load image, create ImageScroller and text animator
        textRenderer = new AnimatedTextRenderer(width - 200, 500, "Hello there! This is a test of the text animation code! Just ignore me for now, it will get a lot slicker soon!", 12);
        imageAnimator = new ImageAnimator(SceneryExtension.scLogo, 0, 0, 800, 100, 240, 1.0, ImageAnimator.EasingType.EASE_IN_OUT, 0.05);
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {
        // TODO remove this, ideally the scenery render should make this obsolete:
        g.setColor(Color.BLACK);
        g.fillRect(0,0,displayWidth, displayHeight);

        // Render background scenery image
        //imageScroller.renderFrame(g);

        // TODO avatar overlay

        // TODO avatar text animation
        textRenderer.updateTextAnimation();

        // TODO avatar movement:
        imageAnimator.update();
        imageAnimator.render(g);

        // Step 4: Render the animated text
        int leftEdge = 100; // 100px margin on either side
        int topEdge = displayHeight - 500; // bottom - 400px text window height - 100px margin
        // TODO the text area should ideally be configurable instead of hard coded...
        //      although, really, how much config do we want to push on the user here?
        //      maybe it's more like "what you get is what you get, use it and like it"
        //      The alternative is to let the user configure it all, but it would get
        //      pretty complicated pretty quickly.
        g.drawImage(textRenderer.getBuffer(), leftEdge, topEdge, null);

        // TODO at certain (configurable) intervals, we load or generate the next text string
        //      and give it to our textRenderer. The animation should restart.
        //if (textRenderer.isAnimationComplete()) {
            //textRenderer.setText("Hello again!");
        //}
    }

    @Override
    public void stop() {
        imageScroller.stop();
        textRenderer.dispose();
    }
}
