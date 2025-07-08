package ca.corbett.musicplayer.extensions.scenery;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a Companion and all of the associated configuration and images for it.
 * Companions have one or more "triggers" that allow them to offer commentary on a given
 * track, a given artist, and/or a given background scenery image. Companions are defined
 * in json with the following example structure:
 * <BLOCKQUOTE><PRE>
   {
     "name": "MyCompanion",
     "language": "en",
     "triggers": [
     {
       "artist": "The Beatles",
       "track": "Hey Jude",
       "scenery": ["studio", "1960s"],
       "responses": [
         "What a classic song!",
         "The Beatles were amazing!",
         "Hey Jude never gets old."
       ]
     },
     {
       "scenery": ["forest"],
       "responses": [
         "I love the peaceful forest atmosphere.",
         "The trees are so calming."
       ]
     }
     ]
   }
  </PRE></BLOCKQUOTE>
 * Companions can have one or more associated images, which are loaded from the same
 * directory as the json file and with the same base name. For example, Companion.json could
 * have matching image files Companion1.jpg and Companion2.png - either jpg or png format
 * is allowed. The loaded images may be scaled as needed. Smaller images will load more
 * rapidly and will take up less memory, just saying.
 * <p>
 * On every track change, and at regular (configurable) intervals, the currently selected
 * Companion will be queried to see if it has anything to say for the given artist, track,
 * or scenery.
 * </p>
 * <p>
 *     <b>Defining a trigger</b> - all trigger parameters are optional, with the following
 *     restrictions: you must specify at least one non-empty response, and you have to
 *     specify at least ONE of Artist, Track, and SceneryTags. If you specify more than one,
 *     then ALL of what you have specified must match in order to be considered a match.
 * </p>
 */
public class Companion {

    public static final int IMAGE_MAX_DIM = 450; // arbitrary size limit for width or height
    public static final String DEFAULT_TRACK_CHANGE_MSG = "You are listening to ${track} by ${artist}.";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final Color DEFAULT_TEXT_BG_COLOR = Color.BLACK;
    public static final Color DEFAULT_TEXT_COLOR = Color.GREEN;
    public static final String DEFAULT_FONT = Font.SANS_SERIF;
    public static final int DEFAULT_FONT_SIZE = 18;

    private final String name;
    private final String description;
    private String language;
    private final List<BufferedImage> images;
    private final List<String> trackChangeMessages;
    private final List<CompanionTrigger> triggers;
    private Font font;
    private Color textColor;
    private Color textBgColor;

    protected Companion(String name, String description, List<BufferedImage> images, List<CompanionTrigger> triggers) {
        this.name = name;
        this.description = description;
        this.images = images;
        this.triggers = triggers;
        this.trackChangeMessages = new ArrayList<>();
        trackChangeMessages.add(DEFAULT_TRACK_CHANGE_MSG);
    }

    protected Companion setLanguage(String language) {
        this.language = language;
        return this;
    }

    protected Companion setFont(Font font) {
        this.font = font;
        return this;
    }

    protected Companion setTextColors(Color textColor, Color textBgColor) {
        this.textColor = textColor;
        this.textBgColor = textBgColor;
        return this;
    }

    /**
     * Populate and return a new Companion instance using data from the given file.
     */
    public static Companion loadCompanion(File metaFile) throws IOException {
        // Verify the given json file exists and is well-formed
        if (!metaFile.exists() || !metaFile.isFile()) {
            throw new IOException("Meta file does not exist or is not a file: " + metaFile.getPath());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(metaFile);
        }
        catch (Exception e) {
            throw new IOException("Failed to parse JSON file: " + metaFile.getPath(), e);
        }

        return loadCompanion(rootNode, parseImages(metaFile, rootNode));
    }

    protected static Companion loadCompanion(InputStream inStream, BufferedImage image) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(inStream);
        }
        catch (Exception e) {
            throw new IOException("Failed to parse JSON resource!", e);
        }

        return loadCompanion(rootNode, List.of(scaleImage(image, IMAGE_MAX_DIM)));

    }

    public static Companion loadCompanion(JsonNode rootNode, List<BufferedImage> images) throws IOException {
        if (images == null || images.isEmpty()) {
            throw new IOException("Companion must have at least one image.");
        }

        // Parse the json metaFile and extract Companion information
        String name = parseName(rootNode);
        String description = parseDescription(rootNode);
        String language = parseLanguage(rootNode);
        Font font = parseFont(rootNode);
        Color textColor = parseTextColor(rootNode);
        Color textBgColor = parseTextBgColor(rootNode);
        List<CompanionTrigger> triggers = parseTriggers(rootNode);

        return new Companion(name, description, images, triggers)
            .setLanguage(language)
            .setFont(font)
            .setTextColors(textColor, textBgColor);
    }

    public boolean hasTrigger(String artistName, String trackTitle, List<String> sceneryTags) {
        for (CompanionTrigger trigger : triggers) {
            if (trigger.matches(artistName, trackTitle, sceneryTags)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a random response from triggers that match the given parameters.
     * If multiple triggers match, one is selected randomly. If the selected trigger
     * has multiple responses, one is selected randomly from those.
     *
     * @param artistName the artist name to match against (can be null)
     * @param trackTitle the track title to match against (can be null)
     * @param sceneryTags the scenery tags to match against (can be null)
     * @return a random response string, or null if no triggers match
     */
    public String getResponse(String artistName, String trackTitle, List<String> sceneryTags) {
        // Find all triggers that match these parameters:
        List<CompanionTrigger> matchingTriggers = new ArrayList<>();
        for (CompanionTrigger candidateTrigger : triggers) {
            if (candidateTrigger.matches(artistName, trackTitle, sceneryTags)) {
                matchingTriggers.add(candidateTrigger);
            }
        }

        // If none of our triggers matches, we have no response:
        if (matchingTriggers.isEmpty()) {
            return null;
        }

        // Apparently ThreadLocalRandom has better performance than java.util.Random
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // Pick one matching trigger at random and get its list of possible responses:
        CompanionTrigger trigger = matchingTriggers.get(rand.nextInt(matchingTriggers.size()));
        List<String> responses = trigger.getResponses();
        if (responses == null || responses.isEmpty()) {
            return null;
        }

        // Pick a response at random and return it:
        return responses.get(rand.nextInt(responses.size()));
    }

    /**
     * Returns all responses from triggers that match the given parameters.
     * If you just want one at random, use getResponse() instead.
     * This is mainly here for testing purposes.
     *
     * @param artistName the artist name to match against (can be null)
     * @param trackTitle the track title to match against (can be null)
     * @param sceneryTags the scenery tags to match against (can be null)
     * @return a list of all possible responses (may be empty if no triggers match)
     */
    public List<String> getAllMatchingResponses(String artistName, String trackTitle, List<String> sceneryTags) {
        List<String> allResponses = new ArrayList<>();

        for (CompanionTrigger candidateTrigger : triggers) {
            if (candidateTrigger.matches(artistName, trackTitle, sceneryTags)) {
                List<String> responses = candidateTrigger.getResponses();
                if (responses != null) {
                    allResponses.addAll(responses);
                }
            }
        }

        return allResponses;
    }

    private static String parseName(JsonNode rootNode) throws IOException {
        if (!rootNode.has("name")) {
            throw new IOException("Meta file must specify a 'name' field");
        }
        String name = rootNode.get("name").asText();
        if (name == null || name.trim().isEmpty()) {
            throw new IOException("Companion name cannot be empty");
        }
        return name;
    }

    private static String parseDescription(JsonNode rootNode) {
        String description = "";
        if (rootNode.has("description")) {
            description = rootNode.get("description").asText();
        }
        return description;
    }

    private static String parseLanguage(JsonNode rootNode) throws IOException {
        String language = DEFAULT_LANGUAGE;
        if (rootNode.has("language")) {
            language = rootNode.get("language").asText();
            if (language == null || language.trim().isEmpty()) {
                // This message is misleading, because it's actually optional in the json.
                // But if you do specify it, it can't be a blank string or empty.
                throw new IOException("Companion must specify a language");
            }
        }
        return language;
    }

    private static Font parseFont(JsonNode rootNode) {
        String fontFace = DEFAULT_FONT;
        if (rootNode.has("fontFace")) {
            fontFace = rootNode.get("fontFace").asText();
        }
        int fontSize = DEFAULT_FONT_SIZE;
        if (rootNode.has("fontSize") && rootNode.get("fontSize").isInt()) {
            fontSize = rootNode.get("fontSize").asInt();
            if (fontSize < 4) {
                fontSize = 4;
            }
            else if (fontSize > 88) {
                fontSize = 88;
            }
        }

        return new Font(fontFace, Font.PLAIN, fontSize);
    }

    private static Color parseTextColor(JsonNode rootNode) throws IOException {
        Color textColor = DEFAULT_TEXT_COLOR;
        if (rootNode.has("textColor")) {
            textColor = parseRgbString(rootNode.get("textColor").asText());
        }
        return textColor;
    }

    private static Color parseTextBgColor(JsonNode rootNode) throws IOException {
        Color textBgColor = DEFAULT_TEXT_BG_COLOR;
        if (rootNode.has("textBgColor")) {
            textBgColor = parseRgbString(rootNode.get("textBgColor").asText());
        }
        return textBgColor;
    }

    private static List<BufferedImage> parseImages(File metaFile, JsonNode rootNode) throws IOException {
        // Verify one or more jpg/png images starting with the base file name exist and are readable
        String baseName = getBaseFileName(metaFile);
        String parentDir = metaFile.getParent();
        List<File> imageFiles = findImageFiles(parentDir, baseName);
        if (imageFiles.isEmpty()) {
            throw new IOException("No corresponding image files (jpg/png) found starting with: " + baseName);
        }

        // Load all associated image files
        List<BufferedImage> images = new ArrayList<>();
        for (File imageFile : imageFiles) {
            try {
                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) {
                    throw new IOException("Failed to load image file: " + imageFile.getPath());
                }
                images.add(scaleImage(image, IMAGE_MAX_DIM));
            } catch (Exception e) {
                throw new IOException("Error reading image file: " + imageFile.getPath(), e);
            }

            // TODO add a border maybe?
        }

        return images;
    }

    private static List<CompanionTrigger> parseTriggers(JsonNode rootNode) throws IOException {
        // Parse triggers
        List<CompanionTrigger> triggers = new ArrayList<>();
        if (rootNode.has("triggers")) {
            JsonNode triggersNode = rootNode.get("triggers");
            if (triggersNode.isArray()) {
                for (JsonNode triggerNode : triggersNode) {
                    CompanionTrigger trigger = CompanionTrigger.parseTrigger(triggerNode);
                    triggers.add(trigger);
                }
            }
        }
        if (triggers.isEmpty()) {
            throw new IOException("Companions must specify at least one trigger.");
        }

        return triggers;
    }

    private static Color parseRgbString(String rgbString) throws IOException {
        if (rgbString == null || rgbString.length() != 8 || ! rgbString.startsWith("0x")) {
            throw new IOException("Color specifies must be in the format 0xRRGGBB");
        }
        try {
            return new Color(Long.decode(rgbString).intValue());
        }
        catch (NumberFormatException nfe) {
            throw new IOException("Invalid color value \""+rgbString+"\"");
        }
    }

    private static String getBaseFileName(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private static List<File> findImageFiles(String parentDir, String baseName) {
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

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLanguage() { return language; }
    public Font getFont() { return font; }
    public Color getTextColor() { return textColor; }
    public Color getTextBgColor() { return textBgColor; }
    public List<BufferedImage> getImages() { return new ArrayList<>(images); }
    public int getTriggerCount() { return triggers.size(); }
    public int getTotalResponseCount() {
        int total = trackChangeMessages.size();
        for (CompanionTrigger trigger : triggers) {
            total += trigger.getResponses().size();
        }
        return total;
    }

    /**
     * Scales an image down proportionally so that the largest of its dimensions matches the given desired size.
     * For example, a landscape image will be scaled down so that its with matches maxDimension.
     * A portrait image will be scaled down so that its height matches maxDimension.
     * A square image will be scaled down until both width and height equals maxDimension.
     * This should probably live in swing-extras ImageUtils or such.
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