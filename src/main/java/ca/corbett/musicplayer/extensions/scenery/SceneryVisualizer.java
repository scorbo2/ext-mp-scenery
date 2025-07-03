package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.Graphics2D;

public class SceneryVisualizer extends VisualizationManager.Visualizer {

    public static final String NAME = "Scenery";
    private ImageScroller imageScroller;

    public SceneryVisualizer() {
        super(NAME);
    }

    @Override
    public void initialize(int width, int height) {
        // TODO load image, create ImageScroller
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {
        // Render background scenery image
        imageScroller.renderFrame(g);

        // TODO avatar overlay
    }

    @Override
    public void stop() {
        imageScroller.stop();
    }
}
