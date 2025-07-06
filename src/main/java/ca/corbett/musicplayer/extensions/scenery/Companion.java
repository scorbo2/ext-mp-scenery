package ca.corbett.musicplayer.extensions.scenery;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

    private final String name;
    private final File metaFile;
    private final List<BufferedImage> images;
    private final Map<CompanionTrigger, List<String>> triggerMap;

    protected Companion(String name, File metaFile, List<BufferedImage> images, Map<CompanionTrigger, List<String>> triggerMap) {
        this.name = name;
        this.metaFile = metaFile;
        this.images = images;
        this.triggerMap = triggerMap == null ? new HashMap<>() : triggerMap;
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
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON file: " + metaFile.getPath(), e);
        }

        // Parse the json metaFile and extract Companion information
        if (!rootNode.has("name")) {
            throw new IOException("Meta file must specify a 'name' field");
        }
        String name = rootNode.get("name").asText();
        if (name == null || name.trim().isEmpty()) {
            throw new IOException("Companion name cannot be empty");
        }

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
                images.add(image);
            } catch (Exception e) {
                throw new IOException("Error reading image file: " + imageFile.getPath(), e);
            }
        }

        // Parse triggers
        Map<CompanionTrigger, List<String>> triggerMap = new HashMap<>();
        if (rootNode.has("triggers")) {
            JsonNode triggersNode = rootNode.get("triggers");
            if (triggersNode.isArray()) {
                for (JsonNode triggerNode : triggersNode) {
                    CompanionTrigger trigger = parseTrigger(triggerNode);
                    List<String> responses = parseResponses(triggerNode);
                    triggerMap.put(trigger, responses);
                }
            }
        }
        if (triggerMap.isEmpty()) {
            throw new IOException("Companions must specify at least one trigger.");
        }

        return new Companion(name, metaFile, images, triggerMap);
    }

    public boolean hasTrigger(String artistName, String trackTitle, List<String> sceneryTags) {
        for (CompanionTrigger trigger : triggerMap.keySet()) {
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
        for (CompanionTrigger candidateTrigger : triggerMap.keySet()) {
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
        List<String> responses = triggerMap.get(trigger);
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

        for (CompanionTrigger candidateTrigger : triggerMap.keySet()) {
            if (candidateTrigger.matches(artistName, trackTitle, sceneryTags)) {
                List<String> responses = triggerMap.get(candidateTrigger);
                if (responses != null) {
                    allResponses.addAll(responses);
                }
            }
        }

        return allResponses;
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

    private static CompanionTrigger parseTrigger(JsonNode triggerNode) throws IOException {
        String artistName = triggerNode.has("artist") ? triggerNode.get("artist").asText() : null;
        String trackTitle = triggerNode.has("track") ? triggerNode.get("track").asText() : null;

        List<String> sceneryTags = new ArrayList<>();
        if (triggerNode.has("scenery")) {
            JsonNode sceneryNode = triggerNode.get("scenery");
            if (sceneryNode.isArray()) {
                for (JsonNode tagNode : sceneryNode) {
                    sceneryTags.add(tagNode.asText());
                }
            }
        }

        // Validate that at least one field is specified
        if ((artistName == null || artistName.trim().isEmpty()) &&
            (trackTitle == null || trackTitle.trim().isEmpty()) &&
            sceneryTags.isEmpty()) {
            throw new IOException("CompanionTrigger must specify at least one of: artist, track, or scenery tags");
        }

        return new CompanionTrigger(artistName, trackTitle, sceneryTags);
    }

    private static List<String> parseResponses(JsonNode triggerNode) throws IOException {
        List<String> responses = new ArrayList<>();
        if (triggerNode.has("responses")) {
            JsonNode responsesNode = triggerNode.get("responses");
            if (responsesNode.isArray()) {
                for (JsonNode responseNode : responsesNode) {
                    String response = responseNode.asText();
                    if (response != null && !response.trim().isEmpty()) {
                        responses.add(response);
                    }
                }
            }
        }

        // Validate that we have at least one non-empty response
        if (responses.isEmpty()) {
            throw new IOException("Each CompanionTrigger must have at least one non-empty response");
        }

        return responses;
    }

    public String getName() { return name; }
    public List<BufferedImage> getImages() { return new ArrayList<>(images); }

    public static class CompanionTrigger {
        private final String artistName;
        private final String trackTitle;
        private final List<String> sceneryTags;

        public CompanionTrigger(String artistName, String trackTitle, List<String> sceneryTags) {
            this.artistName = artistName;
            this.trackTitle = trackTitle;
            this.sceneryTags = convertListToLowerCase(sceneryTags);
        }

        public boolean hasArtistName() {
            return artistName != null && !artistName.isBlank();
        }

        public String getArtistName() {
            return artistName;
        }

        public boolean hasTrackTitle() {
            return trackTitle != null && !trackTitle.isBlank();
        }

        public String getTrackTitle() {
            return trackTitle;
        }

        public boolean hasSceneryTags() {
            return !sceneryTags.isEmpty();
        }

        public List<String> getSceneryTags() {
            return new ArrayList<>(sceneryTags);
        }

        /**
         * Fixed version: Returns true if this trigger matches the given parameters.
         * A trigger matches if ALL of its non-null fields match the corresponding input parameters.
         */
        public boolean matches(String artistName, String trackTitle, List<String> sceneryTags) {
            // Start with true - we'll only set to false if a specified field doesn't match
            boolean matches = true;

            // Check artist name if this trigger specifies one
            if (this.artistName != null && !this.artistName.isBlank()) {
                matches = this.artistName.equalsIgnoreCase(artistName);
            }

            // Check track title if this trigger specifies one
            if (this.trackTitle != null && !this.trackTitle.isBlank()) {
                matches = matches && this.trackTitle.equalsIgnoreCase(trackTitle);
            }

            // Check scenery tags if this trigger specifies any
            if (!this.sceneryTags.isEmpty()) {
                if (sceneryTags == null) {
                    matches = false;
                } else {
                    List<String> inputSceneryTags = convertListToLowerCase(sceneryTags);
                    for (String tag : this.sceneryTags) {
                        if (!inputSceneryTags.contains(tag)) {
                            matches = false;
                            break;
                        }
                    }
                }
            }

            return matches;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) { return false; }
            CompanionTrigger that = (CompanionTrigger) o;
            return Objects.equals(artistName, that.artistName)
                && Objects.equals(trackTitle, that.trackTitle)
                && Objects.equals(sceneryTags, that.sceneryTags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(artistName, trackTitle, sceneryTags);
        }

        private List<String> convertListToLowerCase(List<String> input) {
            if (input == null) {
                return new ArrayList<>();
            }
            List<String> lowerCaseList = new ArrayList<>();
            for (String tag : input) {
                if (tag != null) {
                    lowerCaseList.add(tag.toLowerCase());
                }
            }
            return lowerCaseList;
        }
    }
}