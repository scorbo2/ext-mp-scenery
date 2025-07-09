package ca.corbett.musicplayer.extensions.scenery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single scenery image and its associated metadata.
 */
public class SceneryImage {

    private final List<String> tags;
    private final List<BufferedImage> images;

    protected SceneryImage(List<BufferedImage> images, List<String> tags) {
        this.images = images;
        this.tags = tags;
    }

    /**
     * Returns true if this SceneryImage has the specified tag value.
     */
    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        return tags.contains(tag.toLowerCase());
    }

    /**
     * Returns a list of all tags for this SceneryImage.
     */
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    /**
     * Attempts to load a SceneryImage (or set of related images) from the given json file.
     * Associated images are any jpg or png images whose filenames start with the json file's base name.
     * For example: FutureCityscape.json could have images FutureCityscape1.jpg and FutureCityscape2.jpg.
     * At least one image must be found, or an exception is thrown.
     * <p>
     *     The input json must contain a "tags" array with at least one entry. You can specify as many
     *     tags as you want, but don't go crazy with it as it may make it harder to filter on them
     *     in the UI. Best practice is to describe in VERY BROAD TERMS what the image contains. You don't
     *     need to tag every single detail in the image, and in fact it works better if you don't.
     * </p>
     * <BLOCKQUOTE><PRE>
     *     {
     *         "tags": [
     *           "sci-fi",
     *           "cityscape",
     *           "futuristic",
     *           "cyberpunk"
     *         ]
     *     }
     * </PRE></BLOCKQUOTE>
     *
     * @param metaFile The json file containing metadata for this image or set of images.
     * @return A SceneryImage instance if one could be loaded.
     * @throws IOException If the metadata is malformed or if no images are found.
     */
    public static SceneryImage load(File metaFile) throws IOException {
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

        return load(rootNode, parseImages(metaFile));
    }

    protected static SceneryImage load(InputStream inStream, List<BufferedImage> images) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(inStream);
        }
        catch (Exception e) {
            throw new IOException("Failed to parse JSON resource!", e);
        }

        return load(rootNode, images);
    }

    protected static SceneryImage load(JsonNode rootNode, List<BufferedImage> images) throws IOException {
        if (images == null || images.isEmpty()) {
            throw new IOException("Scenery: at least one image is required.");
        }

        List<String> tags = new ArrayList<>();
        if (rootNode.has("tags")) {
            JsonNode tagsNode = rootNode.get("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tagNode : tagsNode) {
                    tags.add(tagNode.asText().toLowerCase());
                }
            }
        }
        if (tags.isEmpty()) {
            throw new IOException("Scenery images must specify at least one tag.");
        }

        return new SceneryImage(images, tags);
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
                images.add(image);
            } catch (Exception e) {
                throw new IOException("Error reading image file: " + imageFile.getPath(), e);
            }
        }

        return images;
    }
}
