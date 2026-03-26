package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.image.ImagePairLoader.Filter;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.sound.SoundLoader;
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
    private final ResourceBundle schema;
    private final Boolean turn;

    public AnimationBuilder(final ResourceBundle schema, final Entry animationNode, final String imageSet) throws ConfigurationException {
        if (!imageSet.isEmpty()) {
            this.imageSet = imageSet;
        }

        this.schema = schema;
        condition = animationNode.getAttribute(schema.getString("Condition")) == null ? "true" : animationNode.getAttribute(schema.getString("Condition"));
        turn = animationNode.getAttribute(schema.getString("IsTurn")) != null && Boolean.parseBoolean(animationNode.getAttribute(schema.getString("IsTurn")));

        log.debug("Loading animations");

        try {
            // Verify that the condition can be parsed
            Variable.parse(condition);
        } catch (final VariableException e) {
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }

        for (final Entry frameNode : animationNode.selectChildren(schema.getString("Pose"))) {
            try {
                poses.add(loadPose(frameNode));
            } catch (IOException | RuntimeException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadPoseErrorMessage"), frameNode.getAttributes()), e);
            }
        }

        for (final Entry frameNode : animationNode.selectChildren(schema.getString("Hotspot"))) {
            try {
                hotspots.add(loadHotspot(frameNode));
            } catch (RuntimeException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadHotspotErrorMessage"), frameNode.getAttributes()), e);
            }
        }

        log.debug("Finished loading animations");
    }

    private Pose loadPose(final Entry frameNode) throws IOException {
        final Path imagePath = frameNode.getAttribute(schema.getString("Image")) != null ? Path.of(imageSet, frameNode.getAttribute(schema.getString("Image"))) : null;
        final Path imageRightPath = frameNode.getAttribute(schema.getString("ImageRight")) != null ? Path.of(imageSet, frameNode.getAttribute(schema.getString("ImageRight"))) : null;
        final String anchorText = frameNode.getAttribute(schema.getString("ImageAnchor"));
        final String moveText = frameNode.getAttribute(schema.getString("Velocity"));
        final String durationText = frameNode.getAttribute(schema.getString("Duration"));
        String soundText = frameNode.getAttribute(schema.getString("Sound"));
        final String volumeText = frameNode.getAttribute(schema.getString("Volume")) != null ? frameNode.getAttribute(schema.getString("Volume")) : "0";

        final double opacity = Double.parseDouble(Main.getInstance().getProperties().getProperty("Opacity", "1.0"));
        final double scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        String filterText = Main.getInstance().getProperties().getProperty("Filter", "false");
        Filter filter = Filter.NEAREST_NEIGHBOUR;
        if (filterText.equalsIgnoreCase("true") || filterText.equalsIgnoreCase("hqx")) {
            filter = ImagePairLoader.Filter.HQX;
        } else if (filterText.equalsIgnoreCase("bicubic")) {
            filter = ImagePairLoader.Filter.BICUBIC;
        }

        if (imagePath != null) {
            final String[] anchorCoordinates = anchorText.split(",");
            final Point anchor = new Point(Integer.parseInt(anchorCoordinates[0]), Integer.parseInt(anchorCoordinates[1]));

            try {
                ImagePairLoader.load(imagePath, imageRightPath, anchor, scaling, filter, opacity);
            } catch (IOException | NumberFormatException e) {
                String imagePairString = imagePath.toString();
                if (imageRightPath != null) {
                    imagePairString += ", " + imageRightPath;
                }
                throw new IOException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadImageErrorMessage"), imagePairString), e);
            }
        }

        final String[] moveCoordinates = moveText.split(",");
        int moveX = Integer.parseInt(moveCoordinates[0]);
        int moveY = Integer.parseInt(moveCoordinates[1]);
        moveX = Math.abs(moveX) > 0 && Math.abs(moveX * scaling) < 1 ? moveX > 0 ? 1 : -1 : (int) Math.round(moveX * scaling);
        moveY = Math.abs(moveY) > 0 && Math.abs(moveY * scaling) < 1 ? moveY > 0 ? 1 : -1 : (int) Math.round(moveY * scaling);

        final int duration = Integer.parseInt(durationText);

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
                soundText = soundPath.toString();

                float volume = Float.parseFloat(volumeText);
                SoundLoader.load(soundText, volume);
                soundText += ":" + volume;
                Sounds.addUsage(soundText, imageSet);
            } catch (IOException | NumberFormatException | UnsupportedAudioFileException | LineUnavailableException e) {
                throw new IOException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadSoundErrorMessage"), soundText), e);
            }
        }

        final Pose pose = new Pose(imagePath, imageRightPath, moveX, moveY, duration, soundText);

        log.debug("Finished loading pose: {}", pose);

        return pose;
    }

    private Hotspot loadHotspot(final Entry frameNode) {
        final String shapeText = frameNode.getAttribute(schema.getString("Shape"));
        final String originText = frameNode.getAttribute(schema.getString("Origin"));
        final String sizeText = frameNode.getAttribute(schema.getString("Size"));
        final String behaviourText = frameNode.getAttribute(schema.getString("Behaviour"));
        final double scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

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

    public Animation buildAnimation() throws AnimationInstantiationException {
        try {
            return new Animation(Variable.parse(condition), poses.toArray(new Pose[0]), hotspots.toArray(new Hotspot[0]), turn);
        } catch (final VariableException e) {
            throw new AnimationInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }
    }
}
