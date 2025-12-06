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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an extension for the <li><A HREF="https://github.com/scorbo2/musicplayer">MusicPlayer application</a>
 * that offers a full-screen visualization to show gently scrolling scenery with customizable "tour guides"
 * to offer commentary, either on the currently playing track, or on the currently visible scenery.
 * <p>
 * There are a small number of fairly limited built-in tour guides and example scenery, but the
 * real power of this extension is that extra tour guides and scener can be provided dynamically,
 * by crafting json on the filesystem along with images.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class SceneryExtension extends MusicPlayerExtension {
    private static final Logger log = Logger.getLogger(SceneryExtension.class.getName());

    public static final int IMAGE_MAX_DIM = 450; // arbitrary size limit for width or height

    public static final String PROP_INTRO_LABEL = "Scenery.Overview.intro";
    public static final String PROP_COMPANION = "Scenery.Tour guide.chooser";
    public static final String PROP_COMPANION_ROTATE = "Scenery.Tour guide.rotate";
    public static final String PROP_ANNOUNCE_TRACK = "Scenery.Tour guide.announceTrackChange";
    public static final String PROP_COMMENT_INTERVAL = "Scenery.Tour guide.interval";
    public static final String PROP_MIX_CHIT_CHAT = "Scenery.Tour guide.mixChitChat";
    public static final String PROP_CHATTINESS_LEVEL = "Scenery.Tour guide.chattiness";
    public static final String PROP_STYLE_OVERRIDE = "Scenery.Tour guide.allowStyleOverride";
    public static final String PROP_DEFAULT_FONT = "Scenery.Tour guide.defaultFont";
    public static final String PROP_TRANSPARENCY = "Scenery.Tour guide.transparency";
    public static final String PROP_EXTERNAL_DIR = "Scenery.Tour guide.externalDir";
    public static final String PROP_SCENERY_INTERVAL = "Scenery.Scenery.interval";
    public static final String PROP_SCENERY_SPEED = "Scenery.Scenery.scrollSpeed";
    public static final String PROP_SCENERY_DIR = "Scenery.Scenery.externalDir";
    public static final String PROP_SCENERY_TAGS = "Scenery.Scenery.preferredTags";

    private static final String INTRO_LABEL_TEXT = """
        <html>
        The Scenery visualizer gives you gently scrolling beautiful<br>
        scenery, with a helpful tour guide to keep you company!
        </html>
        """;

    private List<Companion> builtInCompanions;
    private List<SceneryImage> builtInScenery;

    public static CompanionLoader companionLoader;
    public static SceneryLoader sceneryLoader;

    private final AppExtensionInfo extInfo;

    /**
     * Determines how frequently a tour guide will offer commentary while
     * the visualizer is running.
     */
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

    /**
     * Determines how frequently we cycle between background scenery images.
     */
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
        extInfo = AppExtensionInfo
            .fromExtensionJar(getClass(), "/ca/corbett/musicplayer/extensions/scenery/extInfo.json");
        if (extInfo == null) {
            throw new RuntimeException("SceneryExtension: can't parse extInfo.json from jar resources!");
        }
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public void loadJarResources() {
        try {
            // Built-in companions:
            builtInCompanions = new ArrayList<>();
            builtInCompanions.add(loadCompanion("sample_companions/RoboButler.json",
                                                loadImage("sample_companions/RoboButler.jpg")));
            builtInCompanions.add(loadCompanion("sample_companions/BennyTheBear.json",
                                                loadImage("sample_companions/BennyTheBear.jpg")));
            builtInCompanions.add(loadCompanion("sample_companions/HeinrichDerHund.json",
                                                loadImage("sample_companions/HeinrichDerHund.jpg")));

            // Built-in scenery:
            builtInScenery = new ArrayList<>();
            builtInScenery.add(loadScenery("sample_scenery/Mountains.json",
                                           loadImage("sample_scenery/Mountains.jpg")));
            builtInScenery.add(loadScenery("sample_scenery/Stonehenge.json",
                                           loadImage("sample_scenery/Stonehenge.jpg")));

            // Log summary:
            log.info("SceneryExtension: loaded " +
                         builtInCompanions.size() +
                         " built-in companions and " +
                         builtInScenery.size() +
                         " built-in scenery images.");
        }
        catch (IOException | NullPointerException ioe) {
            log.log(Level.SEVERE, "Can't load extension resources!", ioe);
        }

        // Peek the values of our external load dirs so we can initialize properly:
        String externalDirScenery = AppConfig.peek(PROP_SCENERY_DIR + ".dir");
        String externalDirCompanions = AppConfig.peek(PROP_EXTERNAL_DIR + ".dir");
        sceneryLoader = new SceneryLoader(externalDirScenery.isEmpty()
                                              ? null
                                              : new File(externalDirScenery),
                                          builtInScenery);
        companionLoader = new CompanionLoader(externalDirCompanions.isEmpty()
                                                  ? null
                                                  : new File(externalDirCompanions),
                                              builtInCompanions);

        // Now we can update our config list with a fully initialized CompanionChooser:
        for (int i = 0; i < configProperties.size(); i++) {
            if (configProperties.get(i).getFullyQualifiedName().equals(PROP_COMPANION)) {
                configProperties.set(i, new CompanionChooserProperty(PROP_COMPANION,
                                                                     "Choose your tour guide:",
                                                                     companionLoader.getAll(), 0));
            }
            else if (configProperties.get(i).getFullyQualifiedName().equals(PROP_SCENERY_TAGS)) {
                configProperties.set(i, new ListProperty<String>(PROP_SCENERY_TAGS, "Preferred tags:")
                    .setItems(sceneryLoader.getUniqueTags()));
            }
        }
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        // Now we can build out our list of properties:
        List<AbstractProperty> configProperties = new ArrayList<>();

        // Companion properties:
        configProperties.add(LabelProperty.createLabel(PROP_INTRO_LABEL, INTRO_LABEL_TEXT));
        configProperties.add(new LabelProperty(PROP_COMPANION, "placeholder"));
        configProperties.add(new BooleanProperty(PROP_COMPANION_ROTATE, "Randomly rotate tour guides", false));
        configProperties.add(new BooleanProperty(PROP_ANNOUNCE_TRACK,
                                                 "Always comment when current track changes", true));
        configProperties.add(new EnumProperty<>(PROP_COMMENT_INTERVAL, "Commentary interval:", CommentaryInterval.TWO));
        configProperties.add(new BooleanProperty(PROP_MIX_CHIT_CHAT,
                                                 "Mix general chit-chat with trigger responses if available",
                                                 true)
                                 .addFormFieldChangeListener(this::changeMixChitChat));
        configProperties.add(new EnumProperty<>(PROP_CHATTINESS_LEVEL, "Chatty level:", Chattiness.MEDIUM));
        configProperties.add(new BooleanProperty(PROP_STYLE_OVERRIDE,
                                                 "Allow tour guides to override default style settings",
                                                 true));
        configProperties.add(new FontProperty(PROP_DEFAULT_FONT, "Default text style:",
                                              new Font(Font.SANS_SERIF, Font.PLAIN, 18), Color.GREEN, Color.BLACK));
        configProperties.add(new DecimalProperty(PROP_TRANSPARENCY,
                                                 "Text opacity:", 1.0, 0.1, 1.0, 0.05));
        configProperties.add(new DirectoryProperty(PROP_EXTERNAL_DIR, "Custom tour guides:", true));

        // Scenery properties:
        configProperties.add(
            new EnumProperty<>(PROP_SCENERY_INTERVAL, "Change interval:", SceneryInterval.FIVE));
        configProperties.add(new EnumProperty<>(PROP_SCENERY_SPEED, "Scroll speed:", ImageScroller.ScrollSpeed.MEDIUM));
        configProperties.add(new DirectoryProperty(PROP_SCENERY_DIR, "Custom scenery:", true));
        configProperties.add(new LabelProperty(PROP_SCENERY_TAGS, "placeholder"));

        return configProperties;
    }

    @Override
    public List<VisualizationManager.Visualizer> getCustomVisualizers() {
        return List.of(new SceneryVisualizer());
    }

    /**
     * Return the name of the given file without any extension it might have.
     */
    public static String getBaseFileName(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    /**
     * Find all images with the given baseName in the given directory.
     */
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

    /**
     * Invoked internally when the "mix chit chat" checkbox is checked or unchecked.
     * We will show or hide the chattiness level combo accordingly.
     */
    private void changeMixChitChat(PropertyFormFieldValueChangedEvent event) {
        //noinspection unchecked
        ComboField<Chattiness> combo = (ComboField<Chattiness>)event.formPanel()
                                                                    .getFormField("Scenery.Tour guide.chattiness");
        if (combo != null) {
            combo.setVisible(((CheckBoxField)event.formField()).isChecked());
        }
    }

    /**
     * Invoked internally to load an image resource.
     */
    private BufferedImage loadImage(String name) throws IOException {
        String fullName = "/ca/corbett/musicplayer/extensions/scenery/" + name;
        InputStream is = SceneryExtension.class.getResourceAsStream(fullName);
        if (is == null) {
            throw new IOException("Unable to load resource " + fullName);
        }
        try (is) {
            return ImageUtil.loadImage(is);
        }
    }

    /**
     * Invoked internally to load a companion resource.
     */
    private Companion loadCompanion(String name, BufferedImage image) throws IOException {
        String fullName = "/ca/corbett/musicplayer/extensions/scenery/" + name;
        InputStream is = SceneryExtension.class.getResourceAsStream(fullName);
        if (is == null) {
            throw new IOException("Unable to load companion " + fullName);
        }
        try (is) {
            return Companion.loadCompanion(is, image);
        }
    }

    /**
     * Invoked internally to load a scenery resource.
     */
    private SceneryImage loadScenery(String name, BufferedImage image) throws IOException {
        String fullName = "/ca/corbett/musicplayer/extensions/scenery/" + name;
        InputStream is = SceneryExtension.class.getResourceAsStream(fullName);
        if (is == null) {
            throw new IOException("Unable to load scenery " + fullName);
        }
        try (is) {
            return SceneryImage.load(is, List.of(image));
        }
    }
}
