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

public final class Hq3x {
    private Hq3x() {
        throw new UnsupportedOperationException("Hq3x is a static class and cannot be instantiated");
    }

    /**
     * Upscales the provided image data by a factor of 3 using the hq3x algorithm.
     * <b>The destination image must be exactly 3 times as large in both dimensions as the source image.</b>
     * The Y, U, V, and A thresholds are defaulted to 48, 7, 6, and 0, respectively. Wrapping is defaulted to false.
     *
     * @param sp   the source image data array in ARGB format
     * @param dp   the destination image data array in ARGB format
     * @param xRes the horizontal resolution of the source image
     * @param yRes the vertical resolution of the source image
     * @see #scale3(int[], int[], int, int, int, int, int, int, boolean, boolean)
     */
    public static void scale3(
            final int[] sp, final int[] dp,
            final int xRes, final int yRes) {
        scale3(sp, dp, xRes, yRes, 48, 7, 6, 0, false, false);
    }

    /**
     * Upscales the provided image data by a factor of 3 using the hq3x algorithm.
     * <b>The destination image must be exactly 3 times as large in both dimensions as the source image.</b>
     *
     * @param sp    the source image data array in ARGB format
     * @param dp    the destination image data array in ARGB format
     * @param xRes  the horizontal resolution of the source image
     * @param yRes  the vertical resolution of the source image
     * @param trY   the Y (luminance) threshold
     * @param trU   the U (chrominance) threshold
     * @param trV   the V (chrominance) threshold
     * @param trA   the A (transparency) threshold
     * @param wrapX whether the source image can be seamlessly repeated horizontally
     * @param wrapY whether the source image can be seamlessly repeated vertically
     */
    public static void scale3(
            final int[] sp, final int[] dp,
            final int xRes, final int yRes,
            int trY, int trU, final int trV, final int trA,
            final boolean wrapX, final boolean wrapY) {
        int spIdx = 0, dpIdx = 0;
        // Don't shift trA, as it uses shift right instead of a mask for comparisons.
        trY <<= 16;
        trU <<= 8;
        final int dpL = xRes * 3;

        int prevLine, nextLine;
        final int[] w = new int[9];

        //   +----+----+----+
        //   | w0 | w1 | w2 |
        //   +----+----+----+
        //   | w3 | w4 | w5 |
        //   +----+----+----+
        //   | w6 | w7 | w8 |
        //   +----+----+----+

        for (int j = 0; j < yRes; j++) {
            prevLine = j > 0
                    ? -xRes
                    : wrapY
                      ? xRes * (yRes - 1)
                      : 0;
            nextLine = j < yRes - 1
                    ? xRes
                    : wrapY
                      ? -(xRes * (yRes - 1))
                      : 0;
            for (int i = 0; i < xRes; i++) {
                w[1] = sp[spIdx + prevLine];
                w[4] = sp[spIdx];
                w[7] = sp[spIdx + nextLine];

                if (i > 0) {
                    w[0] = sp[spIdx + prevLine - 1];
                    w[3] = sp[spIdx - 1];
                    w[6] = sp[spIdx + nextLine - 1];
                } else {
                    if (wrapX) {
                        w[0] = sp[spIdx + prevLine + xRes - 1];
                        w[3] = sp[spIdx + xRes - 1];
                        w[6] = sp[spIdx + nextLine + xRes - 1];
                    } else {
                        w[0] = w[1];
                        w[3] = w[4];
                        w[6] = w[7];
                    }
                }

                if (i < xRes - 1) {
                    w[2] = sp[spIdx + prevLine + 1];
                    w[5] = sp[spIdx + 1];
                    w[8] = sp[spIdx + nextLine + 1];
                } else {
                    if (wrapX) {
                        w[2] = sp[spIdx + prevLine - xRes + 1];
                        w[5] = sp[spIdx - xRes + 1];
                        w[8] = sp[spIdx + nextLine - xRes + 1];
                    } else {
                        w[2] = w[1];
                        w[5] = w[4];
                        w[8] = w[7];
                    }
                }

                int pattern = 0;
                int flag = 1;

                for (int k = 0; k < w.length; k++) {
                    if (k == 4) {
                        continue;
                    }

                    if (w[k] != w[4]) {
                        if (Util.diff(w[4], w[k], trY, trU, trV, trA)) {
                            pattern |= flag;
                        }
                    }
                    flag <<= 1;
                }
                switch (pattern) {
                    case 0:
                    case 1:
                    case 4:
                    case 5:
                    case 32:
                    case 33:
                    case 36:
                    case 37:
                    case 128:
                    case 129:
                    case 132:
                    case 133:
                    case 160:
                    case 161:
                    case 164:
                    case 165: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 2:
                    case 34:
                    case 130:
                    case 162: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 16:
                    case 17:
                    case 48:
                    case 49: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 64:
                    case 65:
                    case 68:
                    case 69: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 8:
                    case 12:
                    case 136:
                    case 140: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 3:
                    case 35:
                    case 131:
                    case 163: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 6:
                    case 38:
                    case 134:
                    case 166: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 20:
                    case 21:
                    case 52:
                    case 53: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 144:
                    case 145:
                    case 176:
                    case 177: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 192:
                    case 193:
                    case 196:
                    case 197: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 96:
                    case 97:
                    case 100:
                    case 101: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 40:
                    case 44:
                    case 168:
                    case 172: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 9:
                    case 13:
                    case 137:
                    case 141: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 18:
                    case 50: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 80:
                    case 81: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 72:
                    case 76: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 10:
                    case 138: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 66: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 24: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 7:
                    case 39:
                    case 135:
                    case 167: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 148:
                    case 149:
                    case 180:
                    case 181: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 224:
                    case 225:
                    case 228:
                    case 229: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 41:
                    case 45:
                    case 169:
                    case 173: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 22:
                    case 54: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 208:
                    case 209: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 104:
                    case 108: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 11:
                    case 139: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 19:
                    case 51: {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 146:
                    case 178: {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        } else {
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 84:
                    case 85: {
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mixEven(w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        break;
                    }
                    case 112:
                    case 113: {
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mixEven(w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        break;
                    }
                    case 200:
                    case 204: {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        } else {
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 73:
                    case 77: {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 42:
                    case 170: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 14:
                    case 142: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 67: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 70: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 28: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 152: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 194: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 98: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 56: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 25: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 26:
                    case 31: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 82:
                    case 214: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 88:
                    case 248: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 74:
                    case 107: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 27: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 86: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 216: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 106: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 30: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 210: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 120: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 75: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 29: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 198: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 184: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 99: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 57: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 71: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 156: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 226: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 60: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 195: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 102: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 153: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 58: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 83: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 92: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 202: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 78: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 154: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 114: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 89: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 90: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 23:
                    case 55: {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 150:
                    case 182: {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        } else {
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 212:
                    case 213: {
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mixEven(w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        break;
                    }
                    case 240:
                    case 241: {
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mixEven(w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        break;
                    }
                    case 232:
                    case 236: {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        } else {
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 105:
                    case 109: {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 43:
                    case 171: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 15:
                    case 143: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 124: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 203: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 62: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 211: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 118: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 217: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 110: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 155: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 188: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 185: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 61: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 157: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 103: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 227: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 230: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 199: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 220: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 158: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 234: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 242: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 59: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 121: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 87: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 79: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 122: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 94: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 218: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 91: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 186: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 115: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 93: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 206: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 201:
                    case 205: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 46:
                    case 174: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 147:
                    case 179: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 116:
                    case 117: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 189: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 231: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 126: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 219: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 125: {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 221: {
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mixEven(w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        break;
                    }
                    case 207: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 238: {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        } else {
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 190: {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        } else {
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 187: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 243: {
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mixEven(w[5], w[7]);
                        }
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        break;
                    }
                    case 119: {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 233:
                    case 237: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 47:
                    case 175: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        break;
                    }
                    case 151:
                    case 183: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 244:
                    case 245: {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 250: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 123: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 95: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 222: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 252: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 249: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 235: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 111: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 63: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 159: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 215: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 246: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 254: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 253: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 251: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 239: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[5]);
                        break;
                    }
                    case 127: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To7To7(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To7To7(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        break;
                    }
                    case 191: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                        break;
                    }
                    case 223: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To7To7(w[4], w[3], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[3]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To7To7(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 247: {
                        dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                    case 255: {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix2To1To1(w[4], w[5], w[7]);
                        }
                        break;
                    }
                }
                spIdx++;
                dpIdx += 3;
            }
            dpIdx += dpL * 2;
        }
    }
}
