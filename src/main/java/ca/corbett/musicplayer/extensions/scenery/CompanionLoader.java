package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.io.FileSystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompanionLoader {

    private static final Logger log = Logger.getLogger(CompanionLoader.class.getName());

    private final File rootDir;
    private final List<Companion> companions;

    public CompanionLoader(File rootDir, List<Companion> builtIns) {
        this.rootDir = rootDir;
        companions = loadCompanions(rootDir, builtIns);
    }

    public List<Companion> getAll() {
        return new ArrayList<>(companions);
    }

    private List<Companion> loadCompanions(File rootDir, List<Companion> builtIns) {
        List<Companion> list = new ArrayList<>(builtIns);

        if (rootDir == null || ! rootDir.exists() || ! rootDir.isDirectory()) {
            log.warning("CompanionLoader: given root dir does not exist or is not valid.");
            return list;
        }

        List<File> metaFiles = FileSystemUtil.findFiles(rootDir, false, "json");
        for (File f : metaFiles) {
            try {
                list.add(Companion.loadCompanion(f));
            }
            catch (IOException ioe) {
                log.log(Level.SEVERE, "CompanionLoader: can't load from file "+f.getAbsolutePath(), ioe);
            }
        }

        log.info("CompanionLoader: found "+list.size()+" companions.");
        return list;
    }
}
