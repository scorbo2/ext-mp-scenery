package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.image.animation.ImageScroller;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.extras.properties.ListProperty;
import ca.corbett.extras.properties.PropertyFormFieldChangeListener;
import ca.corbett.extras.properties.PropertyFormFieldValueChangedEvent;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.ui.VisualizationManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneryExtension extends MusicPlayerExtension {
    private static final Logger log = Logger.getLogger(SceneryExtension.class.getName());

    public static final int IMAGE_MAX_DIM = 450; // arbitrary size limit for width or height

    private List<Companion> builtInCompanions;
    private List<SceneryImage> builtInScenery;

    public static CompanionLoader companionLoader;
    public static SceneryLoader sceneryLoader;

    private final AppExtensionInfo extInfo;

    public enum CommentaryInterval {
        ONE("Every minute", 60 * 1000),
        TWO("Every two minutes", 120 * 1000),
        FIVE("Every five minutes", 300 * 1000),
        TEN("Every ten minutes", 600 * 1000),
        FIFTEEN("Every fifteen minutes", 900 * 1000);

        private final String label;
        private final int intervalMs;

        CommentaryInterval(String label, int ms) {
            this.label = label;
            this.intervalMs = ms;
        }

        @Override
        public String toString() {
            return label;
        }

        public int getIntervalMs() {
            return intervalMs;
        }
    }

    public enum SceneryInterval {
        TRACK("When current track changes", -1),
        TWO("Every two minutes", 120 * 1000),
        FIVE("Every five minutes", 300 * 1000),
        TEN("Every ten minutes", 600 * 1000),
        FIFTEEN("Every fifteen minutes", 900 * 1000);

        private final String label;
        private final int intervalMs;

        SceneryInterval(String label, int ms) {
            this.label = label;
            this.intervalMs = ms;
        }

        @Override
        public String toString() {
            return label;
        }

        public int getIntervalMs() {
            return intervalMs;
        }
    }

    /**
     * Determines the frequency of general chit-chat inclusion when
     * "include general chitchat" option is selected.
     */
    public enum Chattiness {
        LOW("Low", 25),
        MEDIUM("Medium", 50),
        HIGH("High", 75),
        VERY_HIGH("Very high", 100);

        final private int amount;
        final private String label;

        Chattiness(String label, int amount) {
            this.label = label;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return label;
        }

        public int getAmount() {
            return amount;
        }
    }

    public SceneryExtension() {
        try {
            // NOTE! We can ONLY load resources from our own jar file in the constructor!
            // I described this here: https://github.com/scorbo2/swing-extras/issues/34#issuecomment-2882106784
            // I have yet to find and fix the problem. So, all of our built-in resources
            // have to be loaded here in the constructor, as we will be unable to load them later.

            // Built-in companions:
            builtInCompanions = new ArrayList<>();
            BufferedImage img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/RoboButler.jpg"));
            builtInCompanions.add(Companion.loadCompanion(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/RoboButler.json"), img));

            img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/BennyTheBear.jpg"));
            builtInCompanions.add(Companion.loadCompanion(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/BennyTheBear.json"), img));

            img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/HeinrichDerHund.jpg"));
            builtInCompanions.add(Companion.loadCompanion(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_companions/HeinrichDerHund.json"), img));

            // Built-in scenery:
            builtInScenery = new ArrayList<>();
            img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_scenery/Mountains.jpg"));
            builtInScenery.add(SceneryImage.load(SceneryExtension.class.getResourceAsStream((
                "/ca/corbett/musicplayer/extensions/scenery/sample_scenery/Mountains.json")), List.of(img)));

            img = ImageUtil.loadImage(SceneryExtension.class.getResourceAsStream(
                "/ca/corbett/musicplayer/extensions/scenery/sample_scenery/Stonehenge.jpg"));
            builtInScenery.add(SceneryImage.load(SceneryExtension.class.getResourceAsStream((
                "/ca/corbett/musicplayer/extensions/scenery/sample_scenery/Stonehenge.json")), List.of(img)));

            log.info("SceneryExtension: loaded " +
                         builtInCompanions.size() +
                         " built-in companions and " +
                         builtInScenery.size() +
                         " built-in scenery images.");
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Can't load extension resources!", ioe);
        }
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(), "/ca/corbett/musicplayer/extensions/scenery/extInfo.json");
        if (extInfo == null) {
            throw new RuntimeException("SceneryExtension: can't parse extInfo.json from jar resources!");
        }

        // Peek the values of our external load dirs so we can initialize properly:
        String externalDirScenery = AppConfig.peek("Scenery.Scenery.externalDir.dir");
        String externalDirCompanions = AppConfig.peek("Scenery.Tour guide.externalDir.dir");
        sceneryLoader = new SceneryLoader(externalDirScenery.isEmpty() ? null : new File(externalDirScenery), builtInScenery);
        companionLoader = new CompanionLoader(externalDirCompanions.isEmpty() ? null : new File(externalDirCompanions), builtInCompanions);

        // Now we can update our config list with a fully initialized CompanionChooser:
        for (int i = 0; i < configProperties.size(); i++) {
            if (configProperties.get(i).getFullyQualifiedName().equals("Scenery.Tour guide.chooser")) {
                configProperties.set(i, new CompanionChooserProperty("Scenery.Tour guide.chooser","Choose your tour guide:", companionLoader.getAll(), 0));
            }
            else if (configProperties.get(i).getFullyQualifiedName().equals("Scenery.Scenery.preferredTags")) {
                configProperties.set(i, new ListProperty<String>("Scenery.Scenery.preferredTags", "Preferred tags:")
                                         .setItems(sceneryLoader.getUniqueTags()));

            }
        }
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        // Now we can build out our list of properties:
        List<AbstractProperty> configProperties = new ArrayList<>();

        // Companion properties:
        configProperties.add(LabelProperty.createLabel("Scenery.Overview.intro", "<html>The Scenery visualizer gives you gently scrolling beautiful<br>scenery, with a helpful tour guide to keep you company!</html>"));
        configProperties.add(new LabelProperty("Scenery.Tour guide.chooser", "placeholder")); // we can't create this one yet... will update at end of constructor
        configProperties.add(new BooleanProperty("Scenery.Tour guide.rotate", "Randomly rotate tour guides", false));
        configProperties.add(new BooleanProperty("Scenery.Tour guide.announceTrackChange", "Always comment when current track changes", true));
        configProperties.add(new EnumProperty<CommentaryInterval>("Scenery.Tour guide.interval", "Commentary interval:", CommentaryInterval.TWO));
        BooleanProperty mixChitChat = new BooleanProperty("Scenery.Tour guide.mixChitChat", "Mix general chit-chat with trigger responses if available", true);
        mixChitChat.addFormFieldChangeListener(new PropertyFormFieldChangeListener() {
            @Override
            public void valueChanged(PropertyFormFieldValueChangedEvent event) {
                ComboField combo = (ComboField)event.getFormPanel().getFormField("Scenery.Tour guide.chattiness");
                if (combo != null) {
                    combo.setVisible(((CheckBoxField)event.getFormField()).isChecked());
                }
            }
        });
        configProperties.add(mixChitChat);
        configProperties.add(new EnumProperty<Chattiness>("Scenery.Tour guide.chattiness", "Chatty level:", Chattiness.MEDIUM));
        configProperties.add(new BooleanProperty("Scenery.Tour guide.allowStyleOverride", "Allow tour guides to override default style settings", true));
        configProperties.add(new FontProperty("Scenery.Tour guide.defaultFont", "Default text style:", new Font(Font.SANS_SERIF, Font.PLAIN, 18), Color.GREEN, Color.BLACK));
        configProperties.add(new DecimalProperty("Scenery.Tour guide.transparency", "Text opacity:", 1.0, 0.1, 1.0, 0.05));
        configProperties.add(new DirectoryProperty("Scenery.Tour guide.externalDir", "Custom tour guides:", true));

        // Scenery properties:
        configProperties.add(new EnumProperty<SceneryInterval>("Scenery.Scenery.interval", "Change interval:", SceneryInterval.FIVE));
        configProperties.add(new EnumProperty<ImageScroller.ScrollSpeed>("Scenery.Scenery.scrollSpeed", "Scroll speed:", ImageScroller.ScrollSpeed.MEDIUM));
        configProperties.add(new DirectoryProperty("Scenery.Scenery.externalDir", "Custom scenery:", true));
        configProperties.add(new LabelProperty("Scenery.Scenery.preferredTags", "placeholder")); // we can't create this one yet... will update at end of constructor
        return configProperties;
    }

    @Override
    public List<VisualizationManager.Visualizer> getCustomVisualizers() {
        return List.of(new SceneryVisualizer());
    }

    public static String getBaseFileName(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    public static List<File> findImageFiles(String parentDir, String baseName) {
        List<File> imageFiles = new ArrayList<>();
        File directory = new File(parentDir);

        if (!directory.exists() || !directory.isDirectory()) {
            return imageFiles;
        }

        String[] extensions = {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"};
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    String fileBaseName = getBaseFileName(file);

                    // Check if this file starts with our base name
                    if (fileBaseName.startsWith(baseName)) {
                        // Check if it has a valid image extension
                        for (String ext : extensions) {
                            if (fileName.toLowerCase().endsWith(ext.toLowerCase())) {
                                imageFiles.add(file);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return imageFiles;
    }
}
