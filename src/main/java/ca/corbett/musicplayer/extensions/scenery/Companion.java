package ca.corbett.musicplayer.extensions.scenery;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Array;
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

    public static final String DEFAULT_TRACK_CHANGE_MSG = "You are listening to ${track} by ${artist}.";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final int DEFAULT_FONT_SIZE = 36;
    public static final int BORDER_WIDTH = 4;

    private final String name;
    private final String description;
    private String language;
    private final List<BufferedImage> images;
    private final List<String> trackChangeMessages;
    private final List<CompanionTrigger> triggers;
    private final List<String> idleChatter;
    private Font font;
    private Color textColor;
    private Color textBgColor;

    protected Companion(String name, String description, List<BufferedImage> images, List<CompanionTrigger> triggers) {
        this.name = name;
        this.description = description;
        this.images = images;
        this.triggers = triggers;
        this.trackChangeMessages = new ArrayList<>();
        this.idleChatter = new ArrayList<>();
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

    protected Companion setTrackChangeMessages(List<String> trackChangeMessages) {
        this.trackChangeMessages.addAll(trackChangeMessages);
        return this;
    }

    protected Companion setIdleChatter(List<String> idleChatter) {
        this.idleChatter.addAll(idleChatter);
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

        return loadCompanion(rootNode, parseImages(metaFile));
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

        return loadCompanion(rootNode, List.of(SceneryExtension.scaleImage(image, SceneryExtension.IMAGE_MAX_DIM - BORDER_WIDTH*2)));
    }

    protected static Companion loadCompanion(JsonNode rootNode, List<BufferedImage> images) throws IOException {
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
        List<String> trackChangeMessages = parseTrackChangeMessages(rootNode);
        List<CompanionTrigger> triggers = parseTriggers(rootNode);
        List<String> idleChatter = parseIdleChatter(rootNode);

        return new Companion(name, description, images, triggers)
            .setLanguage(language)
            .setFont(font)
            .setTextColors(textColor, textBgColor)
            .setTrackChangeMessages(trackChangeMessages)
            .setIdleChatter(idleChatter);
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
        // Find all triggers that match these parameters and note their possible responses:
        List<String> responses = new ArrayList<>();
        for (CompanionTrigger candidateTrigger : triggers) {
            if (candidateTrigger.matches(artistName, trackTitle, sceneryTags)) {
                responses.addAll(candidateTrigger.getResponses());
            }
        }

        // If we matched nothing, we're done here:
        if (responses.isEmpty()) {
            return null;
        }

        // Pick one at random:
        ThreadLocalRandom rand = ThreadLocalRandom.current();
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
        String fontFace = null;
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

        return (fontFace != null) ? new Font(fontFace, Font.PLAIN, fontSize) : null;
    }

    private static Color parseTextColor(JsonNode rootNode) throws IOException {
        Color textColor = null;
        if (rootNode.has("textColor")) {
            textColor = parseRgbString(rootNode.get("textColor").asText());
        }
        return textColor;
    }

    private static Color parseTextBgColor(JsonNode rootNode) throws IOException {
        Color textBgColor = null;
        if (rootNode.has("textBgColor")) {
            textBgColor = parseRgbString(rootNode.get("textBgColor").asText());
        }
        return textBgColor;
    }

    private static List<BufferedImage> parseImages(File metaFile) throws IOException {
        // Verify one or more jpg/png images starting with the base file name exist and are readable
        String baseName = SceneryExtension.getBaseFileName(metaFile);
        String parentDir = metaFile.getParent();
        List<File> imageFiles = SceneryExtension.findImageFiles(parentDir, baseName);
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
                images.add(SceneryExtension.scaleImage(image, SceneryExtension.IMAGE_MAX_DIM - BORDER_WIDTH * 2));
            } catch (Exception e) {
                throw new IOException("Error reading image file: " + imageFile.getPath(), e);
            }
        }

        return images;
    }

    private static List<String> parseTrackChangeMessages(JsonNode rootNode) throws IOException {
        List<String> messages = new ArrayList<>();
        if (rootNode.has("trackChange")) {
            JsonNode changeNode = rootNode.get("trackChange");
            if (changeNode.isArray()) {
                for (JsonNode node : changeNode) {
                    String msg = node.asText();
                    if (msg != null && ! msg.isEmpty()) {
                        messages.add(msg);
                    }
                }
            }
        }
        return messages;
    }

    private static List<CompanionTrigger> parseTriggers(JsonNode rootNode) throws IOException {
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

    private static List<String> parseIdleChatter(JsonNode rootNode) throws IOException {
        List<String> messages = new ArrayList<>();
        if (rootNode.has("idleChatter")) {
            JsonNode node = rootNode.get("idleChatter");
            if (node.isArray()) {
                for (JsonNode chatterNode : node) {
                    messages.add(chatterNode.asText());
                }
            }
        }
        return messages;
    }

    private static Color parseRgbString(String rgbString) throws IOException {
        if (rgbString == null || rgbString.length() != 8 || ! rgbString.startsWith("0x")) {
            throw new IOException("Color specifiers must be in the format 0xRRGGBB");
        }
        try {
            return new Color(Long.decode(rgbString).intValue());
        }
        catch (NumberFormatException nfe) {
            throw new IOException("Invalid color value \""+rgbString+"\"");
        }
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

    public BufferedImage getRandomImage(Color borderColor) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        BufferedImage companionImage = images.get(rand.nextInt(images.size()));

        BufferedImage image = new BufferedImage(companionImage.getWidth() + BORDER_WIDTH * 2,
                                                companionImage.getHeight() + BORDER_WIDTH * 2,
                                                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(borderColor);
        g.fillRect(0,0,image.getWidth(),image.getHeight());
        g.drawImage(companionImage, (image.getWidth() - companionImage.getWidth()) / 2, (image.getHeight() - companionImage.getHeight()) / 2, null);
        g.dispose();
        return image;
    }

    public String getRandomTrackChangeMessage() {
        // There's a wonky case where a Companion was created without any track change messages.
        // In this case, we'll just supply our default:
        if (trackChangeMessages.isEmpty()) {
            return DEFAULT_TRACK_CHANGE_MSG;
        }

        // Otherwise, pick one at random out of our list.
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        return trackChangeMessages.get(rand.nextInt(trackChangeMessages.size()));
    }

    public String getRandomIdleChatter() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        return idleChatter.get(rand.nextInt(idleChatter.size()));
    }
}