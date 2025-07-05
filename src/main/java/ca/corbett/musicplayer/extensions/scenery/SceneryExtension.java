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
    public static BufferedImage scLogo; // TEMP TODO remove me this is for testing the ImageAnimator

    private final AppExtensionInfo extInfo;

    public SceneryExtension() {
        BufferedImage logoImage = null;
        try {
            logoImage = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/images/sc_logo.png"));
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Can't load logo image!", ioe);
        }
        finally {
            scLogo = logoImage;
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
