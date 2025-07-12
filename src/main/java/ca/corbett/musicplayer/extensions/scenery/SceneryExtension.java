package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.extras.properties.PropertiesManager;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.extensions.MusicPlayerExtension;
import ca.corbett.musicplayer.ui.VisualizationManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

    public static Companion roboButler;
    public static Companion bennyTheBear;
    private List<Companion> builtInCompanions;

    public static CompanionLoader companionLoader;
    public static SceneryLoader sceneryLoader;

    private final AppExtensionInfo extInfo;

    private List<AbstractProperty> configProperties;

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
        TRACK("When current track changes"),
        TWO("Every two minutes"),
        FIVE("Every five minutes"),
        TEN("Every ten minutes"),
        FIFTEEN("Every fifteen minutes");

        private final String label;

        SceneryInterval(String label) {
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

            builtInCompanions = new ArrayList<>();
            builtInCompanions.add(roboButler);
            builtInCompanions.add(bennyTheBear);
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Can't load extension resources!", ioe);
        }
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(), "/ca/corbett/musicplayer/extensions/scenery/extInfo.json");
        if (extInfo == null) {
            throw new RuntimeException("SceneryExtension: can't parse extInfo.json from jar resources!");
        }

        createConfigProperties();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public List<AbstractProperty> getConfigProperties() {
        return configProperties;
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

    /**
     * There's a bug in swing-extras (covered in issue 62) where our getConfigProperties() method
     * will be invoked by the calling app multiple times. This is a problem if we create our config
     * properties in that method before returning them (there will be multiple instances of each
     * property floating around). So, for now, create them here and then just return the list of them
     * each time getConfigProperties() is invoked.
     * <p>
     *     TODO clean this up once (A HREF="https://github.com/scorbo2/swing-extras/issues/62">swing-extras issue 62</a>
     *     is resolved.
     * </p>
     */
    private void createConfigProperties() {
        // Peek the values of our external load dirs so we can initialize properly:
        String externalDirScenery = AppConfig.peek("Scenery.Scenery.externalDir.dir");
        String externalDirCompanions = AppConfig.peek("Scenery.Tour guide.externalDir.dir");
        sceneryLoader = new SceneryLoader(externalDirScenery.isEmpty() ? null : new File(externalDirScenery), List.of()); // TODO built-in scenery
        companionLoader = new CompanionLoader(externalDirCompanions.isEmpty() ? null : new File(externalDirCompanions), builtInCompanions);

        // Now we can build out our list of properties:
        configProperties = new ArrayList<>();

        // Companion properties:
        configProperties.add(LabelProperty.createLabel("Scenery.Overview.intro", "<html>The Scenery visualizer gives you gently scrolling beautiful<br>scenery, with a helpful tour guide to keep you company!</html>"));
        configProperties.add(new CompanionChooserProperty("Scenery.Tour guide.chooser","Choose your tour guide:", companionLoader.getAll(), 0));
        configProperties.add(new BooleanProperty("Scenery.Tour guide.announceTrackChange", "Always comment when current track changes", true));
        configProperties.add(new EnumProperty<CommentaryInterval>("Scenery.Tour guide.interval", "Commentary interval:", CommentaryInterval.TWO));
        configProperties.add(new BooleanProperty("Scenery.Tour guide.allowStyleOverride", "Allow tour guides to override default style settings", true));
        configProperties.add(new FontProperty("Scenery.Tour guide.defaultFont", "Default text style:", new Font(Font.SANS_SERIF, Font.PLAIN, 18), Color.GREEN, Color.BLACK));
        configProperties.add(new DecimalProperty("Scenery.Tour guide.transparency", "Text opacity:", 1.0, 0.1, 1.0, 0.05));
        configProperties.add(new DirectoryProperty("Scenery.Tour guide.externalDir", "Custom tour guides:", true));

        // Scenery properties:
        configProperties.add(new EnumProperty<SceneryInterval>("Scenery.Scenery.interval", "Change interval:", SceneryInterval.FIVE));
        configProperties.add(new EnumProperty<ImageScroller.ScrollSpeed>("Scenery.Scenery.scrollSpeed", "Scroll speed:", ImageScroller.ScrollSpeed.MEDIUM));
        configProperties.add(new DirectoryProperty("Scenery.Scenery.externalDir", "Custom scenery:", true));

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

    /**
     * Scales an image down proportionally so that the largest of its dimensions matches the given desired size.
     * For example, a landscape image will be scaled down so that its with matches maxDimension.
     * A portrait image will be scaled down so that its height matches maxDimension.
     * A square image will be scaled down until both width and height equals maxDimension.
     * TODO: This should probably live in swing-extras ImageUtils or such.
     *
     * @param image The image to scale.
     * @param maxDimension The desired largest dimension of the scaled image.
     * @return The scaled image.
     */
    public static BufferedImage scaleImage(BufferedImage image, int maxDimension) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // Check if the image already fits within the max dimension
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return image; // No scaling needed
        }

        // Calculate scaling factor based on the larger dimension
        double scaleFactor;
        if (originalWidth > originalHeight) {
            // Landscape - scale based on width
            scaleFactor = (double) maxDimension / originalWidth;
        } else {
            // Portrait (or square) - scale based on height
            scaleFactor = (double) maxDimension / originalHeight;
        }

        // Calculate new dimensions
        int newWidth = (int) Math.round(originalWidth * scaleFactor);
        int newHeight = (int) Math.round(originalHeight * scaleFactor);

        // Create the scaled image
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return scaledImage;
    }
}
