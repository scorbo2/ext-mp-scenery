package ca.corbett.musicplayer.extensions.scenery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CompanionTest {

    private Companion companion;
    private Companion.CompanionTrigger trigger;

    @BeforeEach
    void setUp() {
        companion = new Companion("test", "en", null, null, null);
    }

    @Test
    void testTriggerWithArtistOnly() {
        trigger = new Companion.CompanionTrigger("The Beatles", null, null);

        assertTrue(trigger.hasArtistName());
        assertFalse(trigger.hasTrackTitle());
        assertFalse(trigger.hasSceneryTags());
        assertEquals("The Beatles", trigger.getArtistName());
    }

    @Test
    void testTriggerWithTrackOnly() {
        trigger = new Companion.CompanionTrigger(null, "Hey Jude", null);

        assertFalse(trigger.hasArtistName());
        assertTrue(trigger.hasTrackTitle());
        assertFalse(trigger.hasSceneryTags());
        assertEquals("Hey Jude", trigger.getTrackTitle());
    }

    @Test
    void testTriggerWithSceneryOnly() {
        List<String> scenery = Arrays.asList("Forest", "Mountain");
        trigger = new Companion.CompanionTrigger(null, null, scenery);

        assertFalse(trigger.hasArtistName());
        assertFalse(trigger.hasTrackTitle());
        assertTrue(trigger.hasSceneryTags());
        assertEquals(Arrays.asList("forest", "mountain"), trigger.getSceneryTags());
    }

    @Test
    void testMatchesArtistOnly() {
        trigger = new Companion.CompanionTrigger("The Beatles", null, null);

        assertTrue(trigger.matches("The Beatles", "Any Track", Arrays.asList("any", "scenery")));
        assertTrue(trigger.matches("the beatles", null, null)); // Case insensitive
        assertFalse(trigger.matches("Queen", "Any Track", null));
        assertFalse(trigger.matches(null, "Any Track", null));
    }

    @Test
    void testMatchesTrackOnly() {
        trigger = new Companion.CompanionTrigger(null, "Hey Jude", null);

        assertTrue(trigger.matches("Any Artist", "Hey Jude", Arrays.asList("any", "scenery")));
        assertTrue(trigger.matches(null, "hey jude", null)); // Case insensitive
        assertFalse(trigger.matches("Any Artist", "Yesterday", null));
        assertFalse(trigger.matches("Any Artist", null, null));
    }

    @Test
    void testMatchesSceneryOnly() {
        trigger = new Companion.CompanionTrigger(null, null, Arrays.asList("Forest", "Mountain"));

        assertTrue(trigger.matches("Any Artist", "Any Track", Arrays.asList("forest", "mountain", "lake")));
        assertTrue(trigger.matches(null, null, Arrays.asList("Forest", "Mountain"))); // Case insensitive
        assertFalse(trigger.matches("Any Artist", "Any Track", Arrays.asList("forest"))); // Missing mountain
        assertFalse(trigger.matches("Any Artist", "Any Track", Arrays.asList("desert", "ocean")));
        assertFalse(trigger.matches("Any Artist", "Any Track", null));
    }

    @Test
    void testMatchesMultipleFields() {
        trigger = new Companion.CompanionTrigger("The Beatles", "Hey Jude", Arrays.asList("Studio"));

        assertTrue(trigger.matches("The Beatles", "Hey Jude", Arrays.asList("studio", "1960s")));
        assertFalse(trigger.matches("The Beatles", "Yesterday", Arrays.asList("studio"))); // Wrong track
        assertFalse(trigger.matches("Queen", "Hey Jude", Arrays.asList("studio"))); // Wrong artist
        assertFalse(trigger.matches("The Beatles", "Hey Jude", Arrays.asList("live"))); // Wrong scenery
    }

    @Test
    void testMatchesEmptyTrigger() {
        trigger = new Companion.CompanionTrigger(null, null, Collections.emptyList());

        // An empty trigger should match anything since it has no constraints
        assertTrue(trigger.matches("Any Artist", "Any Track", Arrays.asList("any", "scenery")));
        assertTrue(trigger.matches(null, null, null));
        assertTrue(trigger.matches("", "", Collections.emptyList()));
    }

    @Test
    void testMatchesBlankStrings() {
        trigger = new Companion.CompanionTrigger("", "  ", null);

        // Blank strings should be treated as null
        assertFalse(trigger.hasArtistName());
        assertFalse(trigger.hasTrackTitle());
        assertTrue(trigger.matches("Any Artist", "Any Track", null));
    }

    @Test
    void testEqualityAndHashCode() {
        trigger = new Companion.CompanionTrigger("Artist", "Track", Arrays.asList("Tag1", "Tag2"));
        Companion.CompanionTrigger sameTrigger = new Companion.CompanionTrigger("Artist", "Track", Arrays.asList("Tag1", "Tag2"));
        Companion.CompanionTrigger differentTrigger = new Companion.CompanionTrigger("Different", "Track", Arrays.asList("Tag1", "Tag2"));

        assertEquals(trigger, sameTrigger);
        assertEquals(trigger.hashCode(), sameTrigger.hashCode());
        assertNotEquals(trigger, differentTrigger);
        assertNotEquals(trigger.hashCode(), differentTrigger.hashCode());
    }

    @Test
    void testSceneryTagsConvertedToLowerCase() {
        trigger = new Companion.CompanionTrigger(null, null, Arrays.asList("FOREST", "Mountain", "lake"));

        List<String> expectedTags = Arrays.asList("forest", "mountain", "lake");
        assertEquals(expectedTags, trigger.getSceneryTags());
    }

    @Test
    void testNullSceneryHandling() {
        trigger = new Companion.CompanionTrigger(null, null, null);

        assertFalse(trigger.hasSceneryTags());
        assertTrue(trigger.getSceneryTags().isEmpty());
    }

    @Test
    void testGetResponse_withNoMatchingTrigger_shouldReturnNull() {
        // GIVEN a search where nothing matches:
        String artist = "Artist";
        String track = "Track";
        List<String> tags = List.of("tag1", "tag2");

        // WHEN we ask for a response:
        String actual = companion.getResponse(artist, track, tags);

        // THEN we should get null:
        assertNull(actual);
    }

    @Test
    void testGetResponse_withMatch_shouldReturnResponse() {
        // GIVEN a search where our trigger matches:
        Companion.CompanionTrigger trigger1 = new Companion.CompanionTrigger("Artist", "Track", List.of("tag1", "tag2"));
        List<String> responses = List.of("Response1");
        Map<Companion.CompanionTrigger, List<String>> triggerMap = new HashMap<>();
        triggerMap.put(trigger1, responses);
        Companion companion1 = new Companion("Test1", "en", null, null, triggerMap);

        // WHEN we ask for a response:
        String actual = companion1.getResponse("Artist", "Track", List.of("tag2", "tag1")); // order shouldn't matter

        // THEN we should get it:
        assertEquals("Response1", actual);
    }

    @Test
    void testGetAllMatchingResponses_withNoMatches_shouldReturnNothing() {
        // GIVEN a search where nothing matches:
        String artist = "Artist";
        String track = "Track";
        List<String> tags = List.of("tag1", "tag2");

        // WHEN we ask for a response:
        List<String> actual = companion.getAllMatchingResponses(artist, track, tags);

        // THEN we should get an empty list:
        assertEquals(0, actual.size());
    }

    @Test
    void testGetAllMatchingResponses_withMatches_shouldReturnEverything() {
        // GIVEN a search where our trigger matches:
        Companion.CompanionTrigger trigger1 = new Companion.CompanionTrigger("Artist", "Track", List.of("tag1", "tag2"));
        List<String> responses = List.of("Response1", "Response2", "Response3", "Response4");
        Map<Companion.CompanionTrigger, List<String>> triggerMap = new HashMap<>();
        triggerMap.put(trigger1, responses);
        Companion companion1 = new Companion("Test1", "en", null, null, triggerMap);

        // WHEN we ask for all responses:
        List<String> actual = companion1.getAllMatchingResponses("Artist", "Track", List.of("tag2", "tag1")); // order shouldn't matter

        // THEN we should get them all back:
        assertEquals(4, actual.size());
        assertTrue(actual.containsAll(responses));
    }
}