package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.io.FileSystemUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneryLoader {

    private static final Logger log = Logger.getLogger(SceneryLoader.class.getName());

    private final File rootDir;
    private final List<File> metaFiles;
    private final Map<File, List<String>> sceneryTagMap;
    private final List<SceneryImage> builtIns;
    private final ThreadLocalRandom rand;

    public SceneryLoader(File rootDir, List<SceneryImage> builtIns) {
        this.rootDir = rootDir;
        this.builtIns = new ArrayList<>(builtIns);
        rand = ThreadLocalRandom.current();
        this.metaFiles = new ArrayList<>();
        this.sceneryTagMap = new HashMap<>();
        findMetaFiles(rootDir);
    }

    public File getRootDir() {
        return rootDir;
    }

    /**
     * Returns a randomly selected Scenery image from the list of available scenery.
     * If preferredTags is non-empty, the list will be filtered to just those
     * scenery images that contain all specified tags. If no images match the
     * filter, the filter is discarded. You can pass an empty list for no filter.
     */
    public SceneryImage loadRandom(List<String> preferredTags) {
        // Filter our list of meta files if we were given preferred tags:
        List<File> candidates = new ArrayList<>();
        if (! preferredTags.isEmpty()) {
            for (File f : metaFiles) {
                if (new HashSet<>(sceneryTagMap.get(f)).containsAll(preferredTags)) {
                    candidates.add(f);
                }
            }
        }

        // If our filter returned nothing, discard the filter:
        if (! preferredTags.isEmpty() && candidates.isEmpty()) {
            log.warning("SceneryLoader: preferred tags filter returned nothing! Will select scenery randomly.");
        }
        if (candidates.isEmpty()) {
            candidates.addAll(metaFiles);
        }

        // Keep picking a random candidate until we find one that loads (no guarantee they will actually load):
        SceneryImage scenery = null;
        while (! candidates.isEmpty() && scenery == null) {
            int index = rand.nextInt(candidates.size());
            try {
                scenery = SceneryImage.load(candidates.get(index));
            }
            catch (IOException ioe) {
                log.log(Level.SEVERE, "Error loading SceneryImage from file " + candidates.get(index).getAbsolutePath(),
                        ioe);

                // Remove this one from contention as we know now that it doesn't load:
                metaFiles.remove(candidates.get(index));
                sceneryTagMap.remove(candidates.get(index));
                candidates.remove(index);
            }
        }

        // If we get here and we didn't find one, load one of the built-ins instead:
        if (scenery == null) {
            scenery = builtIns.get(rand.nextInt(builtIns.size()));
        }

        // Return whatever we managed to find
        return scenery;
    }

    /**
     * Goes through all meta files and returns a list of unique tags across all of them.
     * The resulting list is sorted alphabetically.
     */
    public List<String> getUniqueTags() {
        Set<String> tags = new HashSet<>();

        for (File f : sceneryTagMap.keySet()) {
            tags.addAll(sceneryTagMap.get(f));
        }

        List<String> tagList = new ArrayList<>(tags);
        tagList.sort(null);
        return tagList;
    }

    private void findMetaFiles(File rootDir) {
        if (rootDir == null || ! rootDir.exists() || ! rootDir.isDirectory()) {
            log.warning("SceneryLoader: given root dir does not exist or is not valid.");
            return;
        }

        // Find all json files in our target dir:
        List<File> metaFileList = new ArrayList<>(FileSystemUtil.findFiles(rootDir, false, "json"));

        // Parse out the tags from each one, discarding any that have no tags:
        for (File f : metaFileList) {
            if (f.exists() && f.isFile() && f.canRead()) {
                List<String> tags = new ArrayList<>();
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(f);
                    if (rootNode.has("tags")) {
                        JsonNode tagsNode = rootNode.get("tags");
                        if (tagsNode.isArray()) {
                            for (JsonNode tagNode : tagsNode) {
                                tags.add(tagNode.asText().toLowerCase());
                            }
                        }
                    }
                }
                catch (Exception ignored) {
                    // skip this one so it doesn't get added
                    continue;
                }

                // This one has a meta file that parses, so add it to our list:
                // (note there's no guarantee the images for it will actually load, but we'll deal with that later)
                metaFiles.add(f);
                sceneryTagMap.put(f, tags);
            }
        }

        log.info("SceneryLoader: found "+metaFiles.size()+" scenery images.");
    }
}
