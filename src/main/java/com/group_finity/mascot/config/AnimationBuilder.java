package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.image.Filter;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.script.Variable;
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
import java.util.ArrayList;
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
    private final String condition;
    private String imageSet = "";
    private final List<Pose> poses = new ArrayList<>();
    private final List<Hotspot> hotspots = new ArrayList<>();
    private final Configuration configuration;
    private final boolean turn;

    public AnimationBuilder(final Configuration configuration, final Entry animationNode, final String imageSet) throws ConfigurationException {
        if (!imageSet.isEmpty()) {
            this.imageSet = imageSet;
        }

        this.configuration = configuration;
        ResourceBundle schema = configuration.getSchema();
        condition = animationNode.hasAttribute(schema.getString("Condition")) ? animationNode.getAttribute(schema.getString("Condition")) : "true";
        turn = animationNode.hasAttribute(schema.getString("IsTurn")) && Boolean.parseBoolean(animationNode.getAttribute(schema.getString("IsTurn")));

        log.debug("Loading an animation");

        try {
            // Verify that the condition can be parsed
            Variable.parse(condition);
        } catch (final VariableException e) {
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }

        for (final Entry poseNode : animationNode.selectChildren(schema.getString("Pose"))) {
            try {
                poses.add(loadPose(poseNode));
            } catch (IOException | RuntimeException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadPoseErrorMessage"), poseNode.getAttributes()), e);
            }
        }

        for (final Entry hotspotNode : animationNode.selectChildren(schema.getString("Hotspot"))) {
            try {
                hotspots.add(loadHotspot(hotspotNode));
            } catch (RuntimeException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadHotspotErrorMessage"), hotspotNode.getAttributes()), e);
            }
        }
    }

    private Pose loadPose(final Entry poseNode) throws IOException {
        log.debug("Loading pose: {}", poseNode.getAttributes());

        ResourceBundle schema = configuration.getSchema();

        final Path imagePath = poseNode.hasAttribute(schema.getString("Image")) ? Path.of(imageSet, poseNode.getAttribute(schema.getString("Image"))) : null;
        final Path imageRightPath = poseNode.hasAttribute(schema.getString("ImageRight")) ? Path.of(imageSet, poseNode.getAttribute(schema.getString("ImageRight"))) : null;
        final String anchorText = poseNode.getAttribute(schema.getString("ImageAnchor"));
        final String velocityText = poseNode.getAttribute(schema.getString("Velocity"));
        final String durationText = poseNode.getAttribute(schema.getString("Duration"));
        final String soundText = poseNode.getAttribute(schema.getString("Sound"));
        final String volumeText = poseNode.hasAttribute(schema.getString("Volume")) ? poseNode.getAttribute(schema.getString("Volume")) : "0";

        final double opacity = Main.getInstance().getSettings().opacity;
        final double scaling = Main.getInstance().getSettings().scaling;
        final Filter filter = Main.getInstance().getSettings().filter;

        String imageKey = null;
        if (imagePath != null) {
            final String[] anchorCoordinates = anchorText.split(",");
            final Point anchor = new Point(Integer.parseInt(anchorCoordinates[0]), Integer.parseInt(anchorCoordinates[1]));

            try {
                imageKey = ImagePairs.load(imagePath, imageRightPath, anchor, scaling, filter, opacity);
            } catch (IOException | NumberFormatException e) {
                String imagePairString = imagePath.toString();
                if (imageRightPath != null) {
                    imagePairString += ", " + imageRightPath;
                }
                throw new IOException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadImageErrorMessage"), imagePairString), e);
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
                if (Files.exists(Main.SOUND_DIRECTORY.resolve(soundText))) {
                    soundPath = Main.SOUND_DIRECTORY.resolve(soundText);
                } else if (Files.exists(Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText))) {
                    soundPath = Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText);
                } else {
                    soundPath = Main.IMAGE_DIRECTORY.resolve(imageSet).resolve(Main.SOUND_DIRECTORY).resolve(soundText);
                }

                float volume = Float.parseFloat(volumeText);
                soundKey = Sounds.load(soundPath.toString(), volume);
                Sounds.addUsage(soundKey, imageSet);
            } catch (IOException | NumberFormatException | UnsupportedAudioFileException | LineUnavailableException e) {
                throw new IOException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadSoundErrorMessage"), soundText), e);
            }
        }

        final Pose pose = new Pose(imageKey, scaledDx, scaledDy, duration, soundKey);

        log.debug("Finished loading pose: {}", pose);

        return pose;
    }

    private Hotspot loadHotspot(final Entry hotspotNode) {
        log.debug("Loading hotspot: {}", hotspotNode.getAttributes());

        ResourceBundle schema = configuration.getSchema();

        final String shapeText = hotspotNode.getAttribute(schema.getString("Shape"));
        final String originText = hotspotNode.getAttribute(schema.getString("Origin"));
        final String sizeText = hotspotNode.getAttribute(schema.getString("Size"));
        final String behaviourText = hotspotNode.getAttribute(schema.getString("Behaviour"));

        final double scaling = Main.getInstance().getSettings().scaling;

        final String[] originCoordinates = originText.split(",");
        final String[] sizeCoordinates = sizeText.split(",");

        final Point origin = new Point((int) Math.round(Integer.parseInt(originCoordinates[0]) * scaling),
                (int) Math.round(Integer.parseInt(originCoordinates[1]) * scaling));
        final Dimension size = new Dimension((int) Math.round(Integer.parseInt(sizeCoordinates[0]) * scaling),
                (int) Math.round(Integer.parseInt(sizeCoordinates[1]) * scaling));

        Shape shape;
        if (shapeText.equalsIgnoreCase("Rectangle")) {
            shape = new Rectangle(origin, size);
        } else if (shapeText.equalsIgnoreCase("Ellipse")) {
            shape = new Ellipse2D.Float(origin.x, origin.y, size.width, size.height);
        } else {
            throw new IllegalArgumentException(String.format(Main.getInstance().getLanguageBundle().getString("HotspotShapeNotSupportedErrorMessage"), shapeText));
        }

        final Hotspot hotspot = new Hotspot(behaviourText, shape);

        log.debug("Finished loading hotspot: {}", hotspot);

        return hotspot;
    }

    public void validate() throws ConfigurationException {
        for (Hotspot hotspot : hotspots) {
            String behavior = hotspot.getBehaviour();
            if (behavior != null && !configuration.getBehaviorNames().contains(behavior)) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage"), behavior));
            }
        }
    }

    public Animation buildAnimation() throws AnimationInstantiationException {
        try {
            return new Animation(Variable.parse(condition), poses.toArray(new Pose[0]), hotspots.toArray(new Hotspot[0]), turn);
        } catch (final VariableException e) {
            throw new AnimationInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }
    }
}
