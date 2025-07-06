package ca.corbett.musicplayer.extensions.scenery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CompanionTest {

    private Companion companion;
    private CompanionTrigger trigger;

    @BeforeEach
    void setUp() {
        CompanionTrigger trigger = new CompanionTrigger("artist", "track", null, List.of("response"));
        companion = new Companion("test", "description", null, List.of(trigger));
    }

    @Test
    void testTriggerWithArtistOnly() {
        trigger = new CompanionTrigger("The Beatles", null, null, List.of("Test response"));

        assertTrue(trigger.hasArtistName());
        assertFalse(trigger.hasTrackTitle());
        assertFalse(trigger.hasSceneryTags());
        assertEquals("The Beatles", trigger.getArtistName());
    }

    @Test
    void testTriggerWithTrackOnly() {
        trigger = new CompanionTrigger(null, "Hey Jude", null, List.of("Test response"));

        assertFalse(trigger.hasArtistName());
        assertTrue(trigger.hasTrackTitle());
        assertFalse(trigger.hasSceneryTags());
        assertEquals("Hey Jude", trigger.getTrackTitle());
    }

    @Test
    void testTriggerWithSceneryOnly() {
        List<String> scenery = Arrays.asList("Forest", "Mountain");
        trigger = new CompanionTrigger(null, null, scenery, List.of("Test response"));

        assertFalse(trigger.hasArtistName());
        assertFalse(trigger.hasTrackTitle());
        assertTrue(trigger.hasSceneryTags());
        assertEquals(Arrays.asList("forest", "mountain"), trigger.getSceneryTags());
    }

    @Test
    void testMatchesArtistOnly() {
        trigger = new CompanionTrigger("The Beatles", null, null, List.of("Test response"));

        assertTrue(trigger.matches("The Beatles", "Any Track", Arrays.asList("any", "scenery")));
        assertTrue(trigger.matches("the beatles", null, null)); // Case insensitive
        assertFalse(trigger.matches("Queen", "Any Track", null));
        assertFalse(trigger.matches(null, "Any Track", null));
    }

    @Test
    void testMatchesTrackOnly() {
        trigger = new CompanionTrigger(null, "Hey Jude", null, List.of("Test response"));

        assertTrue(trigger.matches("Any Artist", "Hey Jude", Arrays.asList("any", "scenery")));
        assertTrue(trigger.matches(null, "hey jude", null)); // Case insensitive
        assertFalse(trigger.matches("Any Artist", "Yesterday", null));
        assertFalse(trigger.matches("Any Artist", null, null));
    }

    @Test
    void testMatchesSceneryOnly() {
        trigger = new CompanionTrigger(null, null, Arrays.asList("Forest", "Mountain"), List.of("Test response"));

        assertTrue(trigger.matches("Any Artist", "Any Track", Arrays.asList("forest", "mountain", "lake")));
        assertTrue(trigger.matches(null, null, Arrays.asList("Forest", "Mountain"))); // Case insensitive
        assertFalse(trigger.matches("Any Artist", "Any Track", Arrays.asList("forest"))); // Missing mountain
        assertFalse(trigger.matches("Any Artist", "Any Track", Arrays.asList("desert", "ocean")));
        assertFalse(trigger.matches("Any Artist", "Any Track", null));
    }

    @Test
    void testMatchesMultipleFields() {
        trigger = new CompanionTrigger("The Beatles", "Hey Jude", Arrays.asList("Studio"), List.of("Test response"));

        assertTrue(trigger.matches("The Beatles", "Hey Jude", Arrays.asList("studio", "1960s")));
        assertFalse(trigger.matches("The Beatles", "Yesterday", Arrays.asList("studio"))); // Wrong track
        assertFalse(trigger.matches("Queen", "Hey Jude", Arrays.asList("studio"))); // Wrong artist
        assertFalse(trigger.matches("The Beatles", "Hey Jude", Arrays.asList("live"))); // Wrong scenery
    }

    @Test
    void testMatchesEmptyTrigger() {
        trigger = new CompanionTrigger(null, null, Collections.emptyList(), List.of("Test response"));

        // An empty trigger should match anything since it has no constraints
        assertTrue(trigger.matches("Any Artist", "Any Track", Arrays.asList("any", "scenery")));
        assertTrue(trigger.matches(null, null, null));
        assertTrue(trigger.matches("", "", Collections.emptyList()));
    }

    @Test
    void testMatchesBlankStrings() {
        trigger = new CompanionTrigger("", "  ", null, List.of("Test response"));

        // Blank strings should be treated as null
        assertFalse(trigger.hasArtistName());
        assertFalse(trigger.hasTrackTitle());
        assertTrue(trigger.matches("Any Artist", "Any Track", null));
    }

    @Test
    void testEqualityAndHashCode() {
        trigger = new CompanionTrigger("Artist", "Track", Arrays.asList("Tag1", "Tag2"), List.of("Test response"));
        CompanionTrigger sameTrigger = new CompanionTrigger("Artist", "Track", Arrays.asList("Tag1", "Tag2"), List.of("Test response"));
        CompanionTrigger differentTrigger = new CompanionTrigger("Different", "Track", Arrays.asList("Tag1", "Tag2"), List.of("Test response"));

        assertEquals(trigger, sameTrigger);
        assertEquals(trigger.hashCode(), sameTrigger.hashCode());
        assertNotEquals(trigger, differentTrigger);
        assertNotEquals(trigger.hashCode(), differentTrigger.hashCode());
    }

    @Test
    void testSceneryTagsConvertedToLowerCase() {
        trigger = new CompanionTrigger(null, null, Arrays.asList("FOREST", "Mountain", "lake"), List.of("Test response"));

        List<String> expectedTags = Arrays.asList("forest", "mountain", "lake");
        assertEquals(expectedTags, trigger.getSceneryTags());
    }

    @Test
    void testNullSceneryHandling() {
        trigger = new CompanionTrigger(null, null, null, null);

        assertFalse(trigger.hasSceneryTags());
        assertTrue(trigger.getSceneryTags().isEmpty());
        assertTrue(trigger.getResponses().isEmpty());
    }

    @Test
    void testGetResponse_withNoMatchingTrigger_shouldReturnNull() {
        // GIVEN a search where nothing matches:
        String artist = "Somebody";
        String track = "Some track";
        List<String> tags = List.of("tag1", "tag2");

        // WHEN we ask for a response:
        String actual = companion.getResponse(artist, track, tags);

        // THEN we should get null:
        assertNull(actual);
    }

    @Test
    void testGetResponse_withMatch_shouldReturnResponse() {
        // GIVEN a search where our trigger matches:
        CompanionTrigger trigger1 = new CompanionTrigger("Artist", "Track", List.of("tag1", "tag2"), List.of("Response1"));
        Companion companion1 = new Companion("Test1", "desc", null, List.of(trigger1));

        // WHEN we ask for a response:
        String actual = companion1.getResponse("Artist", "Track", List.of("tag2", "tag1")); // order shouldn't matter

        // THEN we should get it:
        assertEquals("Response1", actual);
    }

    @Test
    void testGetAllMatchingResponses_withNoMatches_shouldReturnNothing() {
        // GIVEN a search where nothing matches:
        String artist = "Somebody";
        String track = "Some track";
        List<String> tags = List.of("tag1", "tag2");

        // WHEN we ask for a response:
        List<String> actual = companion.getAllMatchingResponses(artist, track, tags);

        // THEN we should get an empty list:
        assertEquals(0, actual.size());
    }

    @Test
    void testGetAllMatchingResponses_withMatches_shouldReturnEverything() {
        // GIVEN a search where our trigger matches:
        CompanionTrigger trigger1 = new CompanionTrigger("Somebody", "Some track", List.of("tag1", "tag2"),
                                                         List.of("Response1", "Response2", "Response3", "Response4"));
        List<CompanionTrigger> triggers = new ArrayList<>();
        triggers.add(trigger1);
        Companion companion1 = new Companion("Test1", "desc", null, triggers);

        // WHEN we ask for all responses:
        List<String> actual = companion1.getAllMatchingResponses("Somebody", "Some track", List.of("tag2", "tag1")); // order shouldn't matter

        // THEN we should get them all back:
        assertEquals(4, actual.size());
        assertTrue(actual.containsAll(List.of("Response1", "Response2", "Response3", "Response4")));
    }
}