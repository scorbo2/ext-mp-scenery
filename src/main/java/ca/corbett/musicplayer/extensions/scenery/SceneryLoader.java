package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.io.FileSystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneryLoader {

    private static final Logger log = Logger.getLogger(SceneryLoader.class.getName());

    private final File rootDir;
    private final List<File> metaFiles;
    private final List<SceneryImage> builtIns;
    private final ThreadLocalRandom rand;

    public SceneryLoader(File rootDir, List<SceneryImage> builtIns) {
        this.rootDir = rootDir;
        this.builtIns = new ArrayList<>(builtIns);
        rand = ThreadLocalRandom.current();
        this.metaFiles = findMetaFiles(rootDir);
    }

    public File getRootDir() {
        return rootDir;
    }

    public SceneryImage loadRandom() {
        // Pick a random array index... if it overflows our json file list, take it from the builtins list:
        int index = rand.nextInt(metaFiles.size() + builtIns.size());
        SceneryImage scenery = null;

        // Loop to find one that loads:
        while (! metaFiles.isEmpty() && scenery == null) {
            try {
                // Are we loading a built-in?
                if (index >= metaFiles.size()) {
                    int builtInIndex = index - metaFiles.size();
                    scenery = builtIns.get(builtInIndex);
                }

                // If not, we're loading a custom one:
                else {
                    scenery = SceneryImage.load(metaFiles.get(index));
                }
            }
            catch (IOException ioe) {
                log.log(Level.SEVERE, "Error loading SceneryImage from file " + metaFiles.get(index).getAbsolutePath(),
                        ioe);

                // Remove this one from contention as we know now that it doesn't load:
                if (index < metaFiles.size()) {
                    metaFiles.remove(index);
                }

                // Now try another one:
                index = rand.nextInt(metaFiles.size() + builtIns.size());
            }
        }

        // It can't be null at this point. If we couldn't load a custom one, we're returning a built-in:
        return scenery;
    }

    private List<File> findMetaFiles(File rootDir) {
        List<File> list = new ArrayList<>();

        if (rootDir == null || ! rootDir.exists() || ! rootDir.isDirectory()) {
            log.warning("SceneryLoader: given root dir does not exist or is not valid.");
            return list;
        }

        list.addAll(FileSystemUtil.findFiles(rootDir, false, "json"));
        log.info("SceneryLoader: found "+list.size()+" scenery images.");
        return list;
    }
}
