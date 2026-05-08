/*
 * Copyright © 2003 Maxim Stepin (maxst@hiend3d.com)
 *
 * Copyright © 2010 Cameron Zemek (grom@zeminvaders.net)
 *
 * Copyright © 2011 Tamme Schichler (tamme.schichler@googlemail.com)

 * Copyright © 2012 A. Eduardo García (arcnorj@gmail.com)
 *
 * This file is part of hqx-java.
 *
 * hqx-java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * hqx-java is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with hqx-java. If not, see <http://www.gnu.org/licenses/>.
 */

package hqx;

final class Util {
    private static final int MASK_2 = 0x0000FF00;
    private static final int MASK_13 = 0x00FF00FF;
    private static final int MASK_RGB = 0x00FFFFFF;
    private static final int MASK_ALPHA = 0xFF000000;

    private static final int MASK_Y = 0x00FF0000;
    private static final int MASK_U = 0x0000FF00;
    private static final int MASK_V = 0x000000FF;

    private static final int[] RGB_TO_YUV = new int[0x1000000];

    static {
        /* Initialize RGB-to-YUV lookup table */
        int r, g, b, y, u, v;
        for (int c = 0; c < RGB_TO_YUV.length; c++) {
            r = (c & 0xFF0000) >>> 16;
            g = (c & 0x00FF00) >>> 8;
            b = c & 0x0000FF;
            /*
            NOTE: This actually converts to YCbCr rather than YUV.
            The conversion formula matches with the one found here:
            https://en.wikipedia.org/wiki/YCbCr#JPEG_conversion
             */
            y = (int) (+0.299d * r + 0.587d * g + 0.114d * b);
            u = (int) (-0.169d * r - 0.331d * g + 0.500d * b) + 128;
            v = (int) (+0.500d * r - 0.419d * g - 0.081d * b) + 128;
            RGB_TO_YUV[c] = y << 16 | u << 8 | v;
        }
    }

    private Util() {
        throw new UnsupportedOperationException("Util is a static class and cannot be instantiated");
    }

    /**
     * Returns the 24-bit YUV equivalent of the provided 24-bit RGB color. <b>Any alpha component is dropped.</b>
     *
     * @param rgb the 24-bit RGB color to convert to YUV
     * @return the corresponding 24-bit YUV color
     */
    static int rgbToYuv(final int rgb) {
        return RGB_TO_YUV[rgb & MASK_RGB];
    }

    /**
     * Compares two ARGB colors according to the provided Y, U, V, and A thresholds.
     * The Y and U thresholds must be shifted left by 16 bits and 8 bits respectively.
     *
     * @param c1  the first ARGB color to compare
     * @param c2  the second ARGB color to compare
     * @param trY the Y (luminance) threshold, shifted left by 16 bits
     * @param trU the U (chrominance) threshold, shifted left by 8 bits
     * @param trV the V (chrominance) threshold
     * @param trA the A (transparency) threshold
     * @return {@code true} if the colors differ more than the thresholds permit
     */
    static boolean diff(final int c1, final int c2, final int trY, final int trU, final int trV, final int trA) {
        final int yuv1 = rgbToYuv(c1);
        final int yuv2 = rgbToYuv(c2);

        // Use unsigned comparisons because the YUV components are unsigned
        return Integer.compareUnsigned(Math.abs((yuv1 & MASK_Y) - (yuv2 & MASK_Y)), trY) > 0 ||
                Integer.compareUnsigned(Math.abs((yuv1 & MASK_U) - (yuv2 & MASK_U)), trU) > 0 ||
                Integer.compareUnsigned(Math.abs((yuv1 & MASK_V) - (yuv2 & MASK_V)), trV) > 0 ||
                Integer.compareUnsigned(Math.abs((c1 >>> 24) - (c2 >>> 24)), trA) > 0;
    }

    /* Interpolation methods */

    // Return statement format:
    //   Line 1: Green
    //   Line 2: Red and blue
    //   Line 3: Alpha

    static int mix3To1(final int c1, final int c2) {
        // return (c1*3+c2) >> 2;
        if (c1 == c2) {
            return c1;
        }
        return (c1 & MASK_2) * 3 + (c2 & MASK_2) >>> 2 & MASK_2 |
                (c1 & MASK_13) * 3 + (c2 & MASK_13) >>> 2 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 2) * 3 + ((c2 & MASK_ALPHA) >>> 2) & MASK_ALPHA;
    }

    static int mix2To1To1(final int c1, final int c2, final int c3) {
        // return (c1*2+c2+c3) >> 2;
        return (c1 & MASK_2) * 2 + (c2 & MASK_2) + (c3 & MASK_2) >>> 2 & MASK_2 |
                (c1 & MASK_13) * 2 + (c2 & MASK_13) + (c3 & MASK_13) >>> 2 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 2) * 2 + ((c2 & MASK_ALPHA) >>> 2) + ((c3 & MASK_ALPHA) >>> 2) & MASK_ALPHA;
    }

    static int mix7To1(final int c1, final int c2) {
        // return (c1*7+c2)/8;
        if (c1 == c2) {
            return c1;
        }
        return (c1 & MASK_2) * 7 + (c2 & MASK_2) >>> 3 & MASK_2 |
                (c1 & MASK_13) * 7 + (c2 & MASK_13) >>> 3 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 3) * 7 + ((c2 & MASK_ALPHA) >>> 3) & MASK_ALPHA;
    }

    static int mix2To7To7(final int c1, final int c2, final int c3) {
        // return (c1*2+(c2+c3)*7)/16;
        return (c1 & MASK_2) * 2 + (c2 & MASK_2) * 7 + (c3 & MASK_2) * 7 >>> 4 & MASK_2 |
                (c1 & MASK_13) * 2 + (c2 & MASK_13) * 7 + (c3 & MASK_13) * 7 >>> 4 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 4) * 2 + ((c2 & MASK_ALPHA) >>> 4) * 7 + ((c3 & MASK_ALPHA) >>> 4) * 7 & MASK_ALPHA;
    }

    static int mixEven(final int c1, final int c2) {
        // return (c1+c2) >> 1;
        if (c1 == c2) {
            return c1;
        }
        return (c1 & MASK_2) + (c2 & MASK_2) >>> 1 & MASK_2 |
                (c1 & MASK_13) + (c2 & MASK_13) >>> 1 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 1) + ((c2 & MASK_ALPHA) >>> 1) & MASK_ALPHA;
    }

    static int mix4To2To1(final int c1, final int c2, final int c3) {
        // return (c1*5+c2*2+c3)/8;
        return (c1 & MASK_2) * 5 + (c2 & MASK_2) * 2 + (c3 & MASK_2) >>> 3 & MASK_2 |
                (c1 & MASK_13) * 5 + (c2 & MASK_13) * 2 + (c3 & MASK_13) >>> 3 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 3) * 5 + ((c2 & MASK_ALPHA) >>> 3) * 2 + ((c3 & MASK_ALPHA) >>> 3) & MASK_ALPHA;
    }

    static int mix6To1To1(final int c1, final int c2, final int c3) {
        // return (c1*6+c2+c3)/8;
        return (c1 & MASK_2) * 6 + (c2 & MASK_2) + (c3 & MASK_2) >>> 3 & MASK_2 |
                (c1 & MASK_13) * 6 + (c2 & MASK_13) + (c3 & MASK_13) >>> 3 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 3) * 6 + ((c2 & MASK_ALPHA) >>> 3) + ((c3 & MASK_ALPHA) >>> 3) & MASK_ALPHA;
    }

    static int mix5To3(final int c1, final int c2) {
        // return (c1*5+c2*3)/8;
        if (c1 == c2) {
            return c1;
        }
        return (c1 & MASK_2) * 5 + (c2 & MASK_2) * 3 >>> 3 & MASK_2 |
                (c1 & MASK_13) * 5 + (c2 & MASK_13) * 3 >>> 3 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 3) * 5 + ((c2 & MASK_ALPHA) >>> 3) * 3 & MASK_ALPHA;
    }

    static int mix2To3To3(final int c1, final int c2, final int c3) {
        // return (c1*2+(c2+c3)*3)/8;
        return (c1 & MASK_2) * 2 + (c2 & MASK_2) * 3 + (c3 & MASK_2) * 3 >>> 3 & MASK_2 |
                (c1 & MASK_13) * 2 + (c2 & MASK_13) * 3 + (c3 & MASK_13) * 3 >>> 3 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 3) * 2 + ((c2 & MASK_ALPHA) >>> 3) * 3 + ((c3 & MASK_ALPHA) >>> 3) * 3 & MASK_ALPHA;
    }

    static int mix14To1To1(final int c1, final int c2, final int c3) {
        // return (c1*14+c2+c3)/16;
        return (c1 & MASK_2) * 14 + (c2 & MASK_2) + (c3 & MASK_2) >>> 4 & MASK_2 |
                (c1 & MASK_13) * 14 + (c2 & MASK_13) + (c3 & MASK_13) >>> 4 & MASK_13 |
                ((c1 & MASK_ALPHA) >>> 4) * 14 + ((c2 & MASK_ALPHA) >>> 4) + ((c3 & MASK_ALPHA) >>> 4) & MASK_ALPHA;
    }
}
