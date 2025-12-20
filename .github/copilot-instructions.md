# Copilot Instructions for ext-mp-scenery

## Repository Overview

**ext-mp-scenery** is a Java 17 visualizer extension for MusicPlayer 3.0+ that displays scrolling scenery with programmable tour guides offering commentary on tracks, artists, and scenery. Built with Maven 3.9.11+ as a standalone JAR.

**Key details:** Small project (~9 Java files, 1 test). Depends on `ca.corbett:musicplayer:3.0` (provided scope). Tests use JUnit Jupiter 5.12.1.

## Critical Build Information

### **IMPORTANT: Known Build Limitation**

**The project CANNOT be built with standard `mvn package`, `mvn compile`, or `mvn test` commands because the `musicplayer` dependency is marked as `provided` scope and is NOT available in Maven Central.**

This is by design - the extension is meant to be loaded by the MusicPlayer application at runtime, which provides the necessary classes. The dependency `ca.corbett:musicplayer:jar:3.0` does not exist in any public Maven repository.

### Build Commands

**DO NOT attempt these commands (they will fail):**
```bash
mvn compile      # FAILS - missing musicplayer dependency
mvn test         # FAILS - missing musicplayer dependency  
mvn package      # FAILS - missing musicplayer dependency
mvn test-compile # FAILS - missing musicplayer dependency
```

**Valid commands:**
```bash
mvn clean        # WORKS - Cleans target directory
mvn validate     # WORKS - Validates project structure
```

### Working With This Repository

When making code changes:
1. **DO NOT attempt to build or test** - the project structure prevents standard Maven builds
2. Focus on code correctness through:
   - Static analysis of the code
   - Understanding the extension API from imports and method signatures
   - Following existing code patterns in the codebase
3. The extension is meant to be built in a CI/CD environment or by users who have the MusicPlayer JAR in their local Maven repository

### Java and Maven Versions

Java 17 (OpenJDK 17.0.17 Temurin), Maven 3.9.11, UTF-8 encoding, compiler source/target 17.

## Project Structure

### Root Directory Files
- `.editorconfig` - IntelliJ code style (4 spaces, UTF-8, LF, 120 char max)
- `.gitignore` - Excludes target/, IDE files, OS files  
- `pom.xml` - Maven project (ca.corbett:ext-mp-scenery:3.0.0)
- `README.md` - User documentation
- `src/` - Source code

### Source Structure
```
src/
├── main/
│   ├── java/ca/corbett/musicplayer/extensions/scenery/
│   │   ├── SceneryExtension.java        # Main extension entry point
│   │   ├── SceneryVisualizer.java       # Core visualization logic
│   │   ├── Companion.java               # Tour guide model
│   │   ├── CompanionLoader.java         # Loads tour guides from JSON
│   │   ├── CompanionChooser.java        # Tour guide selection logic
│   │   ├── CompanionChooserProperty.java # UI property for companion selection
│   │   ├── CompanionTrigger.java        # Trigger matching logic for responses
│   │   ├── SceneryImage.java            # Scenery image model
│   │   └── SceneryLoader.java           # Loads scenery from JSON
│   └── resources/ca/corbett/musicplayer/extensions/scenery/
│       ├── extInfo.json                 # Extension metadata (name, version, description)
│       ├── sample_companions/           # Built-in tour guides (JSON + JPG)
│       │   ├── RoboButler.json/jpg
│       │   ├── BennyTheBear.json/jpg
│       │   └── HeinrichDerHund.json/jpg
│       └── sample_scenery/              # Built-in scenery images (JSON + JPG)
│           ├── Mountains.json/jpg
│           ├── Stonehenge.json/jpg
│           └── readme.txt
└── test/
    └── java/ca/corbett/musicplayer/extensions/scenery/
        └── CompanionTest.java           # JUnit tests for Companion and CompanionTrigger
```

### Key Configuration Files

- **pom.xml**: Artifact `ca.corbett:ext-mp-scenery:3.0.0`, musicplayer (provided), junit-jupiter (test). No special plugins.
- **.editorconfig**: 4 spaces, UTF-8, LF, 120 char max line, extensive Java formatting rules.
- **.gitignore**: Excludes target/, .idea/ (partial), IDE/OS files.

## Architecture and Code Organization

### Extension Entry Point
**SceneryExtension.java** is the main class that:
- Extends `MusicPlayerExtension` from the MusicPlayer API
- Loads built-in companions and scenery from JAR resources
- Defines configuration properties for user preferences
- Registers the `SceneryVisualizer` as a custom visualizer

### Core Components

1. **SceneryVisualizer.java**: Main visualization logic
   - Implements scrolling scenery animation
   - Renders tour guide commentary overlays
   - Manages timing for comments and scenery changes
   - Handles track change announcements

2. **Companion (Tour Guide) System**:
   - **Companion.java**: Model for tour guides with triggers and responses
   - **CompanionLoader.java**: Loads companions from JSON files (built-in + custom)
   - **CompanionChooser.java**: UI component for selecting tour guides
   - **CompanionTrigger.java**: Matches triggers (artist/track/scenery) to responses

3. **Scenery System**:
   - **SceneryImage.java**: Model for scenery with tags
   - **SceneryLoader.java**: Loads scenery from JSON files (built-in + custom)

### Data Format

**Companion JSON structure** (see sample_companions/RoboButler.json):
```json
{
  "name": "RoboButler",
  "description": "...",
  "language": "en",
  "fontFace": "Monospaced",
  "fontSize": 36,
  "textColor": "0x00FF00",
  "textBgColor": "0x000000",
  "trackChange": ["Track announcement templates with ${track} and ${artist}"],
  "triggers": [
    {
      "artist": "Artist Name",      // optional, case-insensitive match
      "track": "Track Title",        // optional, case-insensitive match
      "scenery": ["tag1", "tag2"],   // optional, all tags must match (case-insensitive)
      "responses": ["Response 1", "Response 2"]
    }
  ],
  "idleChatter": ["General commentary when no triggers match"]
}
```

**Scenery JSON structure** (see sample_scenery/Mountains.json):
```json
{
  "tags": ["mountains", "forest", "summer"]
}
```

### Important Patterns and Conventions

1. **Case-insensitive matching**: All trigger matching (artist, track, scenery tags) is case-insensitive
2. **Scenery tags**: Tags are converted to lowercase and ALL must be present for a trigger to match
3. **Resource loading**: Built-in resources loaded from `/ca/corbett/musicplayer/extensions/scenery/` in JAR
4. **External directories**: Users can provide custom companions and scenery via directory configuration
5. **Image files**: Companions and scenery can have associated JPG/PNG images with same base filename

## Code Style

Follow `.editorconfig`: 4 spaces, max 120 chars, LF endings, UTF-8. Align multiline parameters/methods. Use `Logger.getLogger()` for logging. Constants in SCREAMING_SNAKE_CASE. Minimal inline comments; Javadoc for public APIs with `<BLOCKQUOTE><PRE>` examples. Return immutable lists via `List.of()` or `new ArrayList<>(list)`.

## Testing

**Framework:** JUnit Jupiter 5.12.1 in `src/test/java/.../CompanionTest.java`. Use `@Test`, `@BeforeEach`, Given-When-Then structure. Cover nulls, case sensitivity, edge cases.

**CRITICAL: `mvn test` fails** due to missing musicplayer dependency. Write tests following CompanionTest.java patterns using only JUnit/Java standard libs.

## Common Pitfalls

1. **TODO (SceneryVisualizer.java:85)**: Scenery changes are instant; potential improvement for smooth transitions.
2. **Image size limit**: `IMAGE_MAX_DIM = 450` pixels (arbitrary limit).
3. **Resource paths**: Must use exact `/ca/corbett/musicplayer/extensions/scenery/[resource]` with `getResourceAsStream()`.
4. **Dependencies**: MusicPlayer classes (ca.corbett.*) provided at runtime only. Standard Java/JUnit OK.

## Validation

**No CI/CD pipeline** - no GitHub Actions, automated builds, or pre-commit hooks.

**Manual validation:**
1. Code review for Java syntax/logic
2. Pattern matching with existing code
3. No unavailable dependencies
4. Validate JSON syntax: `python3 -m json.tool file.json`
5. Update README.md for user-facing changes

## Making Changes

**Add tour guide:** Create JSON + JPG in `src/main/resources/.../sample_companions/`, update `SceneryExtension.loadJarResources()`. Follow RoboButler.json structure.

**Add scenery:** Create JSON + JPG in `src/main/resources/.../sample_scenery/`, update `SceneryExtension.loadJarResources()`. Follow Mountains.json structure.

**Modify logic:** Identify class (SceneryExtension/Visualizer/Companion), make minimal changes following patterns, add tests for Companion/Trigger logic. Double-check syntax (no build validation).

**Add config properties:** Add `PROP_*` constant, add to `createConfigProperties()` with appropriate type, update `loadJarResources()` if needed.

## Summary

**Trust these instructions.** Cannot build/test due to provided-scope MusicPlayer dependency not in Maven Central. Focus on code correctness through pattern matching. Follow existing patterns exactly. Keep changes minimal. Validate JSON syntax. Check imports (Java standard/JUnit test-only/MusicPlayer API). Do not attempt Maven builds.
