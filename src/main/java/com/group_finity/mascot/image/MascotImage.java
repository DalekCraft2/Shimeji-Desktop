package com.group_finity.mascot.image;

import com.group_finity.mascot.NativeFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
@AllArgsConstructor
@Getter
public class MascotImage {
    private final NativeImage image;

    private final Point center;

    private final Dimension size;

    public MascotImage(final BufferedImage image, final Point center) {
        this(NativeFactory.getInstance().newNativeImage(image), center, new Dimension(image.getWidth(), image.getHeight()));
    }
}
