package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.image.Filter;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.sound.Sounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

/**
 * An object that builds animations.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class AnimationBuilder {
    private static final Logger log = LoggerFactory.getLogger(AnimationBuilder.class);

    /**
     * The parent {@link Configuration} object of this {@code AnimationBuilder}.
     */
    private final Configuration configuration;

    /**
     * The name of the image set with which this animation is associated.
     */
    private final String imageSet;

    /**
     * The condition that must evaluate to {@code true} for this animation to be applied to a mascot.
     * This will be parsed into a {@link Variable} when this animation is built.
     * <p>
     * If this attribute is absent from the Animation node, the animation will always be able to be applied to a mascot.
     * Effectively, this means the condition is defaulted to {@code true}.
     */
    private final String condition;

    /**
     * A sequence of poses through which this animation will iterate. This must not be empty.
     */
    private final List<Pose> poses;

    /**
     * The hotspots that this animation will apply to a mascot.
     */
    private final List<Hotspot> hotspots;

    /**
     * Whether this animation is used for when a mascot changes walking direction.
     * If this attribute is not present in the Animation node, it is defaulted to {@code false}.
     */
    private final boolean turn;

    /**
     * The duration of this animation. This value will equal the sum of the durations in {@link #poses}.
     *
     * @see Pose#duration()
     */
    private final int duration;

    /**
     * Creates a new {@code AnimationBuilder} from the data contained within the specified Animation node.
     *
     * @param configuration the parent {@link Configuration} object of this {@code AnimationBuilder}
     * @param animationNode the Animation node from which to load this animation
     * @param imageSet the name of the image set with which this animation is associated
     * @throws ConfigurationException if an error occurs whilst reading the Animation node, or if the Animation
     * node has no Pose nodes within its children
     */
    public AnimationBuilder(final Configuration configuration, final Entry animationNode, final String imageSet) throws ConfigurationException {
        this.imageSet = imageSet;
        this.configuration = configuration;
        ResourceBundle schema = configuration.getSchema();
        condition = animationNode.getAttribute(schema.getString("Condition"));
        turn = animationNode.hasAttribute(schema.getString("IsTurn")) &&
                Boolean.parseBoolean(animationNode.getAttribute(schema.getString("IsTurn")));

        log.debug("Loading an animation");

        if (condition != null) {
            try {
                // Verify that the condition can be parsed
                Variable.parse(condition);
            } catch (final VariableException e) {
                throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString(
                        "FailedConditionEvaluationErrorMessage"), e);
            }
        }

        List<Entry> poseNodes = animationNode.selectChildren(schema.getString("Pose"));
        if (poseNodes.isEmpty()) {
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString(
                    "NoPosesInAnimationErrorMessage"));
        }
        Pose[] poseArray = new Pose[poseNodes.size()];
        for (int i = 0; i < poseNodes.size(); i++) {
            Entry poseNode = poseNodes.get(i);
            try {
                poseArray[i] = loadPose(poseNode);
            } catch (IOException | RuntimeException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "FailedLoadPoseErrorMessage"), poseNode.getAttributes()), e);
            }
        }
        poses = List.of(poseArray);
        duration = poses.stream().mapToInt(Pose::duration).sum();

        List<Entry> hotspotNodes = animationNode.selectChildren(schema.getString("Hotspot"));
        if (hotspotNodes.isEmpty()) {
            hotspots = List.of();
        } else {
            Hotspot[] hotspotArray = new Hotspot[hotspotNodes.size()];
            for (int i = 0; i < hotspotNodes.size(); i++) {
                Entry hotspotNode = hotspotNodes.get(i);
                try {
                    hotspotArray[i] = loadHotspot(hotspotNode);
                } catch (RuntimeException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedLoadHotspotErrorMessage"), hotspotNode.getAttributes()), e);
                }
            }
            hotspots = List.of(hotspotArray);
        }
    }

    /**
     * Loads a pose from the specified XML node.
     *
     * @param poseNode the Pose node from which to load the pose
     * @return the loaded pose
     * @throws IOException if an error occurs whilst reading from the image/sound paths in the Pose node
     * @throws ArrayIndexOutOfBoundsException if the Pose node's ImageAnchor or Velocity attribute
     * contains fewer than 2 entries when {@code split(",")} is called on it
     * @throws NumberFormatException if an attribute that is expected to be numeric cannot be parsed
     */
    private Pose loadPose(final Entry poseNode) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Loading pose: {}", poseNode.getAttributes());
        }

        ResourceBundle schema = configuration.getSchema();

        final Path imagePath = poseNode.hasAttribute(schema.getString("Image")) ?
                Path.of(imageSet, poseNode.getAttribute(schema.getString("Image"))) : null;
        final Path imageRightPath = poseNode.hasAttribute(schema.getString("ImageRight")) ?
                Path.of(imageSet, poseNode.getAttribute(schema.getString("ImageRight"))) : null;
        final String anchorText = poseNode.getAttribute(schema.getString("ImageAnchor"));
        final String velocityText = poseNode.getAttribute(schema.getString("Velocity"));
        final String durationText = poseNode.getAttribute(schema.getString("Duration"));
        final String soundText = poseNode.getAttribute(schema.getString("Sound"));
        final String volumeText = poseNode.hasAttribute(schema.getString("Volume")) ?
                poseNode.getAttribute(schema.getString("Volume")) : "0";

        final double opacity = Main.getInstance().getSettings().opacity;
        final double scaling = Main.getInstance().getSettings().scaling;
        final Filter filter = Main.getInstance().getSettings().filter;

        String imageKey = null;
        if (imagePath != null) {
            final String[] anchorCoordinates = anchorText.split(",");
            final int anchorX = Integer.parseInt(anchorCoordinates[0]);
            final int anchorY = Integer.parseInt(anchorCoordinates[1]);

            try {
                imageKey = ImagePairs.load(imagePath, imageRightPath, anchorX, anchorY, scaling, filter, opacity);
                ImagePairs.addUsage(imageKey, imageSet);
            } catch (NumberFormatException | IOException e) {
                String imagePairString = imagePath.toString();
                if (imageRightPath != null) {
                    imagePairString += ", " + imageRightPath;
                }
                throw new IOException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "FailedLoadImageErrorMessage"), imagePairString), e);
            }
        }

        final String[] velocityCoordinates = velocityText.split(",");
        int dx = Integer.parseInt(velocityCoordinates[0]);
        int dy = Integer.parseInt(velocityCoordinates[1]);
        int scaledDx = (int) Math.round(dx * scaling);
        int scaledDy = (int) Math.round(dy * scaling);
        /* If the scaling rounded a nonzero value to 0, set the value to +-1
         to prevent the mascot from getting being unable to move */
        scaledDx = dx != 0 && scaledDx == 0 ? dx < 0 ? -1 : 1 : scaledDx;
        scaledDy = dy != 0 && scaledDy == 0 ? dy < 0 ? -1 : 1 : scaledDy;

        final int duration = Integer.parseInt(durationText);

        String soundKey = null;
        if (soundText != null) {
            try {
                Path soundPath;
                if (Files.isRegularFile(Main.IMAGE_DIRECTORY.resolve(imageSet).resolve(Main.SOUND_DIRECTORY).resolve(soundText))) {
                    soundPath = Main.IMAGE_DIRECTORY.resolve(imageSet).resolve(Main.SOUND_DIRECTORY).resolve(soundText);
                } else if (Files.isRegularFile(Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText))) {
                    soundPath = Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText);
                } else {
                    soundPath = Main.SOUND_DIRECTORY.resolve(soundText);
                }

                float volume = Float.parseFloat(volumeText);
                soundKey = Sounds.load(soundPath.toString(), volume);
                Sounds.addUsage(soundKey, imageSet);
            } catch (NumberFormatException | LineUnavailableException | UnsupportedAudioFileException | IOException e) {
                throw new IOException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "FailedLoadSoundErrorMessage"), soundText), e);
            }
        }

        final Pose pose = new Pose(imageKey, scaledDx, scaledDy, duration, soundKey);

        if (log.isDebugEnabled()) {
            log.debug("Finished loading pose: {}", pose);
        }

        return pose;
    }

    /**
     * Loads a hotspot from the specified XML node.
     *
     * @param hotspotNode the Hotspot node from which to load the hotspot
     * @return the loaded hotspot
     * @throws ArrayIndexOutOfBoundsException if either the Origin or Size attributes contain fewer than 2 entries
     * when {@code split(",")} is called on them
     * @throws NumberFormatException if an attribute that is expected to be numeric cannot be parsed
     * @throws IllegalArgumentException if the Hotspot node's Shape attribute has an unsupported value
     * (i.e., it is neither "Rectangle" nor "Ellipse")
     */
    private Hotspot loadHotspot(final Entry hotspotNode) {
        if (log.isDebugEnabled()) {
            log.debug("Loading hotspot: {}", hotspotNode.getAttributes());
        }

        ResourceBundle schema = configuration.getSchema();

        final String shapeText = hotspotNode.getAttribute(schema.getString("Shape"));
        final String originText = hotspotNode.getAttribute(schema.getString("Origin"));
        final String sizeText = hotspotNode.getAttribute(schema.getString("Size"));
        final String behaviorText = hotspotNode.getAttribute(schema.getString("Behaviour"));

        final double scaling = Main.getInstance().getSettings().scaling;

        final String[] originCoordinates = originText.split(",");
        final String[] sizeCoordinates = sizeText.split(",");

        final int originX = (int) Math.round(Integer.parseInt(originCoordinates[0]) * scaling);
        final int originY = (int) Math.round(Integer.parseInt(originCoordinates[1]) * scaling);
        final int width = (int) Math.round(Integer.parseInt(sizeCoordinates[0]) * scaling);
        final int height = (int) Math.round(Integer.parseInt(sizeCoordinates[1]) * scaling);

        Shape shape;
        if (shapeText.equalsIgnoreCase("Rectangle")) {
            shape = new Rectangle(originX, originY, width, height);
        } else if (shapeText.equalsIgnoreCase("Ellipse")) {
            shape = new Ellipse2D.Float(originX, originY, width, height);
        } else {
            throw new IllegalArgumentException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "HotspotShapeNotSupportedErrorMessage"), shapeText));
        }

        final Hotspot hotspot = new Hotspot(behaviorText, shape);

        if (log.isDebugEnabled()) {
            log.debug("Finished loading hotspot: {}", hotspot);
        }

        return hotspot;
    }

    /**
     * Ensures the validity of any data loaded by this {@code AnimationBuilder} object that
     * could not be validated when this {@code AnimationBuilder} object was being initialized.
     * Specifically, this ensures that none of the {@link Hotspot} objects in this {@code AnimationBuilder}
     * reference nonexistent behaviors.
     *
     * @throws ConfigurationException if a hotspot in this animation references a nonexistent behavior
     * @see ActionBuilder#validate()
     */
    public void validate() throws ConfigurationException {
        if (hotspots.isEmpty()) {
            return;
        }
        for (Hotspot hotspot : hotspots) {
            String behavior = hotspot.getBehaviour();
            if (behavior != null && !configuration.getBehaviorNames().contains(behavior)) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "NoBehaviourFoundErrorMessage"), behavior));
            }
        }
    }

    /**
     * Builds this animation.
     *
     * @return the built animation
     * @throws AnimationInstantiationException if this animation's condition fails to be parsed into a {@link Variable}
     */
    public Animation buildAnimation() throws AnimationInstantiationException {
        try {
            return new Animation(condition == null ? null : Variable.parse(condition), poses, hotspots, turn, duration);
        } catch (final VariableException e) {
            throw new AnimationInstantiationException(Main.getInstance().getLanguageBundle().getString(
                    "FailedConditionEvaluationErrorMessage"), e);
        }
    }
}
