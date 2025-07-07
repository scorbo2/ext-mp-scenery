package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.ui.VisualizationManager;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneryExtension extends MusicPlayerExtension {
    private static final Logger log = Logger.getLogger(SceneryExtension.class.getName());
    public static Companion roboButler;

    private final AppExtensionInfo extInfo;

    public SceneryExtension() {
        try {
            BufferedImage img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/RoboButler.jpg"));
            roboButler = Companion.loadCompanion(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/RoboButler.json"), img);
            log.info("Loaded RoboButler!");
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Can't load extension resources!", ioe);
        }
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(), "/ca/corbett/musicplayer/extensions/scenery/extInfo.json");
        if (extInfo == null) {
            throw new RuntimeException("SceneryExtension: can't parse extInfo.json from jar resources!");
        }
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public List<AbstractProperty> getConfigProperties() {
        // TODO:
        //   current companion (fallback to robobutler if selected doesn't exist)
        //   popup interval (on track change + some configurable interval)
        //   scenery change interval
        //   scenery scroll properties (speed and bounce easing)
        //
        return List.of();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public List<VisualizationManager.Visualizer> getCustomVisualizers() {
        return List.of(new SceneryVisualizer());
    }
}
