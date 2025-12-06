package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.image.animation.AnimatedTextRenderer;
import ca.corbett.extras.image.animation.ImageAnimator;
import ca.corbett.extras.image.animation.ImageScroller;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.ListProperty;
import ca.corbett.extras.properties.PropertiesManager;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.ui.UIReloadable;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;
import ca.corbett.musicplayer.ui.VisualizationWindow;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class SceneryVisualizer extends VisualizationManager.Visualizer implements UIReloadable {

    private static final Logger log = Logger.getLogger(SceneryVisualizer.class.getName());

    public static final String NAME = "Scenery";

    /** A pixel margin to the left and right of the companion and text area **/
    private static final int MARGIN = 100;

    /** How long, in milliseconds, to leave a message up **/
    private static final int BASE_COMMENT_TIME_MS = 8888;

    private ImageScroller imageScroller;
    private AnimatedTextRenderer textRenderer;
    private ImageAnimator textAnimator;
    private ImageAnimator companionAnimator;

    // Companion props:
    private Companion companion;
    private SceneryImage scenery;
    private boolean announceTrackChange;
    private boolean allowStyleOverride;
    private boolean mixChatChatWithResponses;
    private Font defaultFont;
    private Color defaultBg;
    private Color defaultFg;
    private SceneryExtension.CommentaryInterval commentaryInterval;
    private float textOpacity;
    private boolean rotateCompanions;
    private SceneryExtension.Chattiness chattiness;

    private SceneryExtension.SceneryInterval sceneryInterval;
    private List<String> preferredSceneryTags;

    private int displayWidth;
    private int displayHeight;
    private int textX;
    private int textWidth;

    // State vars:
    boolean isTrackAnnounced;
    long lastCommentTime;
    long lastSceneryChangeTime;
    boolean isCommentingNow;
    String artist;
    String track;
    Font effectiveFont;
    Color effectiveTextFg;
    Color effectiveTextBg;
    boolean isFirstFrame;
    ThreadLocalRandom rand;

    public SceneryVisualizer() {
        super(NAME);
    }

    @Override
    public void initialize(int width, int height) {
        rand = ThreadLocalRandom.current();
        displayWidth = width;
        displayHeight = height;
        artist = "N/A";
        track = "N/A";
        isFirstFrame = true;

        // Text width is the display width minus a margin on either side minus the width of the companion image:
        textWidth = displayWidth - SceneryExtension.IMAGE_MAX_DIM - (MARGIN*2);

        // I want a slightly larger margin on the right side:
        textWidth -= 50;

        // Text left edge is just to the right of the companion image:
        textX = MARGIN + SceneryExtension.IMAGE_MAX_DIM;

        // Register for UI updates so we can reload our props if they change while we're running:
        ReloadUIAction.getInstance().registerReloadable(this);

        // And force a load now so we have the latest settings:
        reloadUI();

        // Background:
        scenery = SceneryExtension.sceneryLoader.loadRandom(preferredSceneryTags);
        imageScroller = new ImageScroller(scenery.getRandomImage(), width, height);

        // Text window:
        textRenderer = new AnimatedTextRenderer(textWidth, 350, "", 16, effectiveFont, effectiveTextFg, effectiveTextBg);
        textAnimator = new ImageAnimator(textRenderer.getBuffer(), textX, displayHeight + 100, textX, displayHeight + 100, 777, 1.0, ImageAnimator.EasingType.EASE_IN_OUT, 0.2);
        textAnimator.setTransparency(textOpacity);

        // Companion:
        companionAnimator = new ImageAnimator(companion.getRandomImage(effectiveTextBg), -500, height - 500, -100, height - 500, 777, 1.0, ImageAnimator.EasingType.EASE_IN_OUT, 0.2);

        // This might be a bit rude, but let's switch the text overlay off if it's on.
        // (user can always hit "i" to bring it back; we won't muck with it again)
        if (VisualizationWindow.getInstance().isTextOverlayEnabled()) {
            VisualizationWindow.getInstance().toggleTextOverlayEnabled();
        }

        lastCommentTime = 0; // force an immediate comment
        lastSceneryChangeTime = System.currentTimeMillis(); // but wait to swap scenery
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {

        // Note how long it's been since we had a comment or a scenery change:
        long timeSinceLastMessage = System.currentTimeMillis() - lastCommentTime;
        long timeSinceLastSceneryChange = System.currentTimeMillis() - lastSceneryChangeTime;

        // Look for track changes and make note of artist and track for text substitution:
        if (trackInfo != null) {
            if (! artist.equalsIgnoreCase(trackInfo.getArtist()) ||
                ! track.equalsIgnoreCase(trackInfo.getTitle())) {
                isTrackAnnounced = false;

                // Hard cancel any comment that was in progress and reset for the track announcement:
                isCommentingNow = false;
                textAnimator.setPosition(textX, displayHeight + 100);
                companionAnimator.setPosition(-500, displayHeight - 500);
            }
            artist = trackInfo.getArtist();
            track = trackInfo.getTitle();
        }

        // Render background scenery image
        imageScroller.renderFrame(g);

        // Change the scenery if the track has changed and we are in SceneryInterval.TRACK:
        if (sceneryInterval == SceneryExtension.SceneryInterval.TRACK && ! isTrackAnnounced && ! isFirstFrame && trackInfo != null) {
            scenery = SceneryExtension.sceneryLoader.loadRandom(preferredSceneryTags);
            imageScroller.setImage(scenery.getRandomImage());
            lastSceneryChangeTime = System.currentTimeMillis();
        }

        // Announce the track if it hasn't been done yet:
        if (! isTrackAnnounced && ! isCommentingNow && trackInfo != null && announceTrackChange) {
            if (rotateCompanions) {
                setCompanion(SceneryExtension.companionLoader.getRandom());
            }
            beginComment(companion.getRandomTrackChangeMessage(), trackInfo);
            isTrackAnnounced = true;
        }

        // Look for a conversation trigger:
        else if (! isCommentingNow && timeSinceLastMessage > commentaryInterval.getIntervalMs()) {
            if (rotateCompanions) {
                setCompanion(SceneryExtension.companionLoader.getRandom());
            }

            // Does the companion have something to say about this artist, track, or scenery image?
            List<String> responses = companion.getAllMatchingResponses(artist, track, scenery.getTags());

            // If nothing matched, OR if "include chit-chat" is selected, also add random chit-chat messages:
            if (responses.isEmpty() || mixChatChatWithResponses) {

                // If we're mixing chit-chat in with trigger responses, obey the
                // chattiness level configured in app props:
                boolean includeChitChat = true;
                if (! responses.isEmpty()) {
                    int diceRoll = rand.nextInt(SceneryExtension.Chattiness.VERY_HIGH.getAmount() + 1);
                    includeChitChat = chattiness.getAmount() >= diceRoll;
                }

                if (includeChitChat) {
                    responses.addAll(companion.getAllIdleChatter());
                }
            }

            // Pick any random entry from this list and go with it:
            if (! responses.isEmpty()) {
                beginComment(responses.get(rand.nextInt(responses.size())), trackInfo);
            }

            // If the companion had nothing to say, just skip (ideally this shouldn't happen... the companion needs more triggers!)
            else {
                log.info("SceneryVisualizer: the companion had no response and no chatter! (add more dialog to this companion to avoid this!)");
            }
        }

        // Remove companion if a message has been up for a certain time:
        else if (isCommentingNow && timeSinceLastMessage > BASE_COMMENT_TIME_MS) {
            companionAnimator.setDestination(-500, displayHeight - 500);
            textAnimator.setDestination(textX, displayHeight + 100);
            isCommentingNow = false;
        }

        // If "always announce track change" is off, just consider it done even though it wasn't:
        if (! announceTrackChange) {
            isTrackAnnounced = true;
        }

        // Swap scenery if it's time:
        if (sceneryInterval != SceneryExtension.SceneryInterval.TRACK &&
            timeSinceLastSceneryChange > sceneryInterval.getIntervalMs()) {
            // TODO a transition might be nice instead of just a hard cut to the new one...
            scenery = SceneryExtension.sceneryLoader.loadRandom(preferredSceneryTags);
            imageScroller.setImage(scenery.getRandomImage());
            lastSceneryChangeTime = System.currentTimeMillis();
        }

        companionAnimator.renderFrame(g);
        textRenderer.updateTextAnimation();
        textAnimator.setImage(textRenderer.getBuffer());
        textAnimator.renderFrame(g);

        isFirstFrame = false; // this is just to prevent scenery swap on the very first render
    }

    @Override
    public void stop() {
        if (imageScroller != null) {
            imageScroller.stop();
        }
        if (textRenderer != null) {
            textRenderer.dispose();
        }

        // Stop listening for UI updates now:
        ReloadUIAction.getInstance().unregisterReloadable(this);
    }

    @Override
    public void reloadUI() {
        PropertiesManager manager = AppConfig.getInstance().getPropertiesManager();
        AbstractProperty chooserProp = manager.getProperty(SceneryExtension.PROP_COMPANION);
        AbstractProperty announceTrackChangeProp = manager.getProperty(SceneryExtension.PROP_ANNOUNCE_TRACK);
        AbstractProperty commentaryIntervalProp = manager.getProperty(SceneryExtension.PROP_COMMENT_INTERVAL);
        AbstractProperty styleOverrideProp = manager.getProperty(SceneryExtension.PROP_STYLE_OVERRIDE);
        AbstractProperty mixChitChatProp = manager.getProperty(SceneryExtension.PROP_MIX_CHIT_CHAT);
        AbstractProperty defaultStyleProp = manager.getProperty(SceneryExtension.PROP_DEFAULT_FONT);
        AbstractProperty transparencyProp = manager.getProperty(SceneryExtension.PROP_TRANSPARENCY);
        AbstractProperty sceneryIntervalProp = manager.getProperty(SceneryExtension.PROP_SCENERY_INTERVAL);
        AbstractProperty rotateCompanionsProp = manager.getProperty(SceneryExtension.PROP_COMPANION_ROTATE);
        AbstractProperty chattinessProp = manager.getProperty(SceneryExtension.PROP_CHATTINESS_LEVEL);
        AbstractProperty sceneryTagsProp = manager.getProperty(SceneryExtension.PROP_SCENERY_TAGS);

        if (! (chooserProp instanceof CompanionChooserProperty) ||
            ! (announceTrackChangeProp instanceof BooleanProperty) ||
            ! (commentaryIntervalProp instanceof EnumProperty) ||
            ! (styleOverrideProp instanceof BooleanProperty) ||
            ! (mixChitChatProp instanceof BooleanProperty) ||
            ! (defaultStyleProp instanceof FontProperty) ||
            ! (transparencyProp instanceof DecimalProperty) ||
            ! (sceneryIntervalProp instanceof EnumProperty) ||
            ! (rotateCompanionsProp instanceof BooleanProperty) ||
            ! (chattinessProp instanceof EnumProperty) ||
            ! (sceneryTagsProp instanceof ListProperty)) {
            log.severe("SceneryVisualizer: our properties are of the wrong type!");
            return;
        }

        companion = ((CompanionChooserProperty)chooserProp).getSelectedItem();
        announceTrackChange = ((BooleanProperty)announceTrackChangeProp).getValue();
        allowStyleOverride = ((BooleanProperty)styleOverrideProp).getValue();
        mixChatChatWithResponses = ((BooleanProperty)mixChitChatProp).getValue();
        defaultFont = ((FontProperty)defaultStyleProp).getFont();
        defaultBg = ((FontProperty)defaultStyleProp).getBgColor();
        defaultFg = ((FontProperty)defaultStyleProp).getTextColor();
        textOpacity = (float)((DecimalProperty)transparencyProp).getValue();
        rotateCompanions = ((BooleanProperty)rotateCompanionsProp).getValue();

        setCompanion(companion);

        // We can't use instanceof to pre-check these class casts because of type erasure, but eh, it'll be fine.
        //noinspection unchecked
        commentaryInterval = ((EnumProperty<SceneryExtension.CommentaryInterval>)commentaryIntervalProp).getSelectedItem();
        //noinspection unchecked
        sceneryInterval = ((EnumProperty<SceneryExtension.SceneryInterval>)sceneryIntervalProp).getSelectedItem();
        //noinspection unchecked
        chattiness = ((EnumProperty<SceneryExtension.Chattiness>)chattinessProp).getSelectedItem();
        //noinspection unchecked
        preferredSceneryTags = ((ListProperty<String>)sceneryTagsProp).getSelectedItems();
    }

    private void setCompanion(Companion companion) {
        this.companion = companion;

        Font font = defaultFont;
        Color fg = defaultFg;
        Color bg = defaultBg;

        // If overrides are enabled and this companion supplies them, use them:
        if (allowStyleOverride && companion.getFont() != null) {
            font = companion.getFont();
        }
        if (allowStyleOverride && companion.getTextColor() != null) {
            fg = companion.getTextColor();
        }
        if (allowStyleOverride && companion.getTextBgColor() != null) {
            bg = companion.getTextBgColor();
        }
        effectiveFont = font;
        effectiveTextFg = fg;
        effectiveTextBg = bg;

        // Update the text renderer if it has been created:
        if (textRenderer != null) {
            textRenderer.setFont(effectiveFont);
            textRenderer.setTextColor(effectiveTextFg);
            textRenderer.setBackgroundColor(effectiveTextBg);
        }
    }

    private void beginComment(String comment, VisualizationTrackInfo trackInfo) {
        if (trackInfo != null) {
            // Do substitutions for ${artist}, ${track}, and ${album}:
            comment = comment.replaceAll("\\$\\{artist}", trackInfo.getArtist());
            comment = comment.replaceAll("\\$\\{track}", trackInfo.getTitle());
            comment = comment.replaceAll("\\$\\{album}", trackInfo.getAlbum());
        }

        isCommentingNow = true;
        lastCommentTime = System.currentTimeMillis() + comment.length() * 25L; // give time to read longer messages; 1 extra second per 40 chars or so
        companionAnimator.setImage(companion.getRandomImage(effectiveTextBg));
        companionAnimator.setDestination(textX - companionAnimator.getImage().getWidth(), displayHeight - 500);

        textRenderer.setText(comment);
        textAnimator.setDestination(textX, displayHeight - 450);
    }
}
