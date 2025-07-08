package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.LabelProperty;
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
    public static Companion bennyTheBear;

    private final AppExtensionInfo extInfo;

    public enum CommentaryInterval {
        ONE("Every minute"),
        TWO("Every two minutes"),
        FIVE("Every five minutes"),
        TEN("Every ten minutes"),
        FIFTEEN("Every fifteen minutes");

        private final String label;

        CommentaryInterval(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public SceneryExtension() {
        try {
            BufferedImage img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/RoboButler.jpg"));
            roboButler = Companion.loadCompanion(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/RoboButler.json"), img);
            log.info("Loaded RoboButler!");

            img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/BennyTheBear.jpg"));
            bennyTheBear = Companion.loadCompanion(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/BennyTheBear.json"), img);
            log.info("Loaded Benny the Bear!");

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

        List<AbstractProperty> props = new ArrayList<>();

        props.add(LabelProperty.createLabel("Scenery.Overview.intro", "<html>The Scenery visualizer gives you gently scrolling beautiful<br>scenery, with a helpful tour guide to keep you company!</html>"));
        props.add(new CompanionChooserProperty("Scenery.Tour guide.chooser","Choose your tour guide:", List.of(roboButler, bennyTheBear), 0));
        props.add(new BooleanProperty("Scenery.Tour guide.announceTrackChange", "Always announce when current track changes", true));
        props.add(new EnumProperty<CommentaryInterval>("Scenery.Tour guide.interval", "Commentary interval:", CommentaryInterval.TWO));
        props.add(new IntegerProperty("Scenery.Scenery.interval", "Change interval:", 5, 1, 30, 1));

        return props;
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
