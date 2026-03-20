package com.group_finity.mascot.sound;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Loads in new Clip objects into the Sounds collection. It will not duplicate
 * sounds already in the collection.
 *
 * @author Kilkakon
 * @since 1.0.9
 */
public class SoundLoader {
    public static void load(final String fileName, final float volume) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        String key = fileName + ":" + volume;
        if (Sounds.contains(key)) {
            return;
        }

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(fileName));
        final Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(volume);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                ((Clip) event.getLine()).stop();
            }
        });

        Sounds.put(key, clip);
    }
}
