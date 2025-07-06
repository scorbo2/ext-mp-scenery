package ca.corbett.musicplayer.extensions.scenery;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompanionTrigger {
    private final String artistName;
    private final String trackTitle;
    private final List<String> sceneryTags;
    private final List<String> responses;

    protected CompanionTrigger(String artistName, String trackTitle, List<String> sceneryTags, List<String> responses) {
        this.artistName = artistName;
        this.trackTitle = trackTitle;
        this.sceneryTags = convertListToLowerCase(sceneryTags);
        this.responses = responses == null ? new ArrayList<>() : new ArrayList<>(responses);
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

    public List<String> getResponses() {
        return new ArrayList<>(responses);
    }

    /**
     * Returns true if this trigger matches the given parameters.
     * A trigger matches if ALL of its non-null fields match the corresponding input parameters.
     * Any of the input parameters may be null.
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
            // If this trigger defines a scenery tag but we weren't given one, then it's not a match:
            if (sceneryTags == null) {
                matches = false;
            } else {
                // Otherwise, make sure all of our scenery tags are present in the supplied list (case insensitive)
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
            && Objects.equals(sceneryTags, that.sceneryTags)
            && Objects.equals(responses, that.responses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artistName, trackTitle, sceneryTags, responses);
    }

    public static CompanionTrigger parseTrigger(JsonNode triggerNode) throws IOException {
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

        // Get the list of responses and ensure at least one non-blank response exists:
        List<String> responses = parseResponses(triggerNode);

        return new CompanionTrigger(artistName, trackTitle, sceneryTags, responses);
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

