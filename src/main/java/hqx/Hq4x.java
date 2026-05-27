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

public final class Hq4x {
    private Hq4x() {
        throw new UnsupportedOperationException("Hq4x is a static class and cannot be instantiated");
    }

    /**
     * Upscales the provided image data by a factor of 4 using the hq4x algorithm.
     * <b>The destination image must be exactly 4 times as large in both dimensions as the source image.</b>
     * The Y, U, V, and A thresholds are defaulted to 48, 7, 6, and 0, respectively. Wrapping is defaulted to false.
     *
     * @param sp   the source image data array in ARGB format
     * @param dp   the destination image data array in ARGB format
     * @param xRes the horizontal resolution of the source image
     * @param yRes the vertical resolution of the source image
     * @see #scale4(int[], int[], int, int, int, int, int, int, boolean, boolean)
     */
    public static void scale4(
            final int[] sp, final int[] dp,
            final int xRes, final int yRes) {
        scale4(sp, dp, xRes, yRes, 48, 7, 6, 0, false, false);
    }

    /**
     * Upscales the provided image data by a factor of 4 using the hq4x algorithm.
     * <b>The destination image must be exactly 4 times as large in both dimensions as the source image.</b>
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
    public static void scale4(
            final int[] sp, final int[] dp,
            final int xRes, final int yRes,
            int trY, int trU, final int trV, final int trA,
            final boolean wrapX, final boolean wrapY) {
        int spIdx = 0, dpIdx = 0;
        // Don't shift trA, as it uses shift right instead of a mask for comparisons.
        trY <<= 16;
        trU <<= 8;
        final int dpL = xRes * 4;

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
                    case 0, 1, 4, 5, 32, 33, 36, 37, 128, 129, 132, 133, 160, 161, 164, 165 -> case0(dp, dpIdx, dpL, w);
                    case 2, 34, 130, 162 -> case2(dp, dpIdx, dpL, w);
                    case 16, 17, 48, 49 -> case16(dp, dpIdx, dpL, w);
                    case 64, 65, 68, 69 -> case64(dp, dpIdx, dpL, w);
                    case 8, 12, 136, 140 -> case8(dp, dpIdx, dpL, w);
                    case 3, 35, 131, 163 -> case3(dp, dpIdx, dpL, w);
                    case 6, 38, 134, 166 -> case6(dp, dpIdx, dpL, w);
                    case 20, 21, 52, 53 -> case20(dp, dpIdx, dpL, w);
                    case 144, 145, 176, 177 -> case144(dp, dpIdx, dpL, w);
                    case 192, 193, 196, 197 -> case192(dp, dpIdx, dpL, w);
                    case 96, 97, 100, 101 -> case96(dp, dpIdx, dpL, w);
                    case 40, 44, 168, 172 -> case40(dp, dpIdx, dpL, w);
                    case 9, 13, 137, 141 -> case9(dp, dpIdx, dpL, w);
                    case 18, 50 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 80, 81 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 72, 76 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 10, 138 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 66 -> case66(dp, dpIdx, dpL, w);
                    case 24 -> case24(dp, dpIdx, dpL, w);
                    case 7, 39, 135, 167 -> case7(dp, dpIdx, dpL, w);
                    case 148, 149, 180, 181 -> case148(dp, dpIdx, dpL, w);
                    case 224, 225, 228, 229 -> case224(dp, dpIdx, dpL, w);
                    case 41, 45, 169, 173 -> case41(dp, dpIdx, dpL, w);
                    case 22, 54 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 208, 209 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 104, 108 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 11, 139 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 19, 51 -> {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mix5To3(w[1], w[5]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix2To1To1(w[5], w[4], w[1]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 146, 178 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[1], w[4], w[5]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix5To3(w[5], w[1]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                    }
                    case 84, 85 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[5], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix2To1To1(w[7], w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 112, 113 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix2To1To1(w[5], w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[7], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                    }
                    case 200, 204 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[3], w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 73, 77 -> {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mix5To3(w[3], w[7]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix2To1To1(w[7], w[4], w[3]);
                        }
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 42, 170 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                            dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix2To1To1(w[1], w[4], w[3]);
                            dp[dpIdx + dpL] = Util.mix5To3(w[3], w[1]);
                            dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 14, 142 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix5To3(w[1], w[3]);
                            dp[dpIdx + 2] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix2To1To1(w[3], w[4], w[1]);
                            dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 67 -> case67(dp, dpIdx, dpL, w);
                    case 70 -> case70(dp, dpIdx, dpL, w);
                    case 28 -> case28(dp, dpIdx, dpL, w);
                    case 152 -> case152(dp, dpIdx, dpL, w);
                    case 194 -> case194(dp, dpIdx, dpL, w);
                    case 98 -> case98(dp, dpIdx, dpL, w);
                    case 56 -> case56(dp, dpIdx, dpL, w);
                    case 25 -> case25(dp, dpIdx, dpL, w);
                    case 26, 31 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 82, 214 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 88, 248 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                    }
                    case 74, 107 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 27 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 86 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 216 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 106 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 30 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 210 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 120 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 75 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 29 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 198 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 184 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 99 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 57 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 71 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 156 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 226 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 60 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 195 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 102 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 153 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 58 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 83 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 92 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 202 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 78 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 154 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 114 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                    }
                    case 89 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 90 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 23, 55 -> {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mix5To3(w[1], w[5]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix2To1To1(w[5], w[4], w[1]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 150, 182 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[1], w[4], w[5]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix5To3(w[5], w[1]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                    }
                    case 212, 213 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[5], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix2To1To1(w[7], w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 240, 241 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix2To1To1(w[5], w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[7], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                    }
                    case 232, 236 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[3], w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 105, 109 -> {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mix5To3(w[3], w[7]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix2To1To1(w[7], w[4], w[3]);
                        }
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 43, 171 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix2To1To1(w[1], w[4], w[3]);
                            dp[dpIdx + dpL] = Util.mix5To3(w[3], w[1]);
                            dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 15, 143 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix5To3(w[1], w[3]);
                            dp[dpIdx + 2] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix2To1To1(w[3], w[4], w[1]);
                            dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 124 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 203 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 62 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 211 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 118 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 217 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 110 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 155 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 188 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 185 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 61 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 157 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 103 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 227 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 230 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 199 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 220 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                    }
                    case 158 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 234 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 242 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                    }
                    case 59 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 121 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 87 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 79 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 122 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 94 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 218 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                    }
                    case 91 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 186 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 115 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                    }
                    case 93 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 206 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 201, 205 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 46, 174 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                            dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL + 1] = w[4];
                        }
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 147, 179 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                            dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        } else {
                            dp[dpIdx + 2] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 116, 117 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                    }
                    case 189 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 231 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 126 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 219 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 125 -> {
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix3To1(w[4], w[3]);
                            dp[dpIdx + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL] = Util.mix5To3(w[3], w[7]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix2To1To1(w[7], w[4], w[3]);
                        }
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 221 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix3To1(w[4], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[5], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix2To1To1(w[7], w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 207 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix5To3(w[1], w[3]);
                            dp[dpIdx + 2] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + dpL] = Util.mix2To1To1(w[3], w[4], w[1]);
                            dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        }
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 238 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mix2To1To1(w[3], w[4], w[7]);
                            dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix3To1(w[4], w[7]);
                        }
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 190 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                        } else {
                            dp[dpIdx + 2] = Util.mix2To1To1(w[1], w[4], w[5]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix5To3(w[5], w[1]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix3To1(w[4], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                    }
                    case 187 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                            dp[dpIdx + dpL + 1] = w[4];
                            dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mix2To1To1(w[1], w[4], w[3]);
                            dp[dpIdx + dpL] = Util.mix5To3(w[3], w[1]);
                            dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                            dp[dpIdx + dpL + dpL] = Util.mix3To1(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix3To1(w[4], w[3]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 243 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                            dp[dpIdx + dpL + dpL + 3] = Util.mix2To1To1(w[5], w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix3To1(w[4], w[7]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[7], w[5]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                    }
                    case 119 -> {
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                            dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 2] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix3To1(w[4], w[1]);
                            dp[dpIdx + 1] = Util.mix3To1(w[1], w[4]);
                            dp[dpIdx + 2] = Util.mix5To3(w[1], w[5]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                            dp[dpIdx + dpL + 3] = Util.mix2To1To1(w[5], w[4], w[1]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 233, 237 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
                        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
                        dp[dpIdx + dpL + dpL] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 47, 175 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                    }
                    case 151, 183 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 244, 245 -> {
                        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 250 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                    }
                    case 123 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 95 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 222 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 252 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 249 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                    }
                    case 235 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
                        dp[dpIdx + dpL + dpL] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 111 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 63 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 159 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 215 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 246 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 254 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
                        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 253 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
                        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
                        dp[dpIdx + dpL + dpL] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 3] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 251 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
                        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
                        dp[dpIdx + dpL + dpL] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                    }
                    case 239 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        dp[dpIdx + dpL + dpL] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
                    }
                    case 127 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 2] = w[4];
                            dp[dpIdx + 3] = w[4];
                            dp[dpIdx + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + 2] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + 3] = Util.mixEven(w[1], w[5]);
                            dp[dpIdx + dpL + 3] = Util.mixEven(w[5], w[4]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL] = Util.mixEven(w[3], w[4]);
                            dp[dpIdx + dpL + dpL + dpL] = Util.mixEven(w[7], w[3]);
                            dp[dpIdx + dpL + dpL + dpL + 1] = Util.mixEven(w[7], w[4]);
                        }
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
                    }
                    case 191 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
                        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
                    }
                    case 223 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                            dp[dpIdx + 1] = w[4];
                            dp[dpIdx + dpL] = w[4];
                        } else {
                            dp[dpIdx] = Util.mixEven(w[1], w[3]);
                            dp[dpIdx + 1] = Util.mixEven(w[1], w[4]);
                            dp[dpIdx + dpL] = Util.mixEven(w[3], w[4]);
                        }
                        dp[dpIdx + 2] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + 3] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + 3] = Util.mixEven(w[5], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 2] = Util.mixEven(w[7], w[4]);
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mixEven(w[7], w[5]);
                        }
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
                    }
                    case 247 -> {
                        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + 2] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
                        dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                    case 255 -> {
                        if (Util.diff(w[3], w[1], trY, trU, trV, trA)) {
                            dp[dpIdx] = w[4];
                        } else {
                            dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
                        }
                        dp[dpIdx + 1] = w[4];
                        dp[dpIdx + 2] = w[4];
                        if (Util.diff(w[1], w[5], trY, trU, trV, trA)) {
                            dp[dpIdx + 3] = w[4];
                        } else {
                            dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
                        }
                        dp[dpIdx + dpL] = w[4];
                        dp[dpIdx + dpL + 1] = w[4];
                        dp[dpIdx + dpL + 2] = w[4];
                        dp[dpIdx + dpL + 3] = w[4];
                        dp[dpIdx + dpL + dpL] = w[4];
                        dp[dpIdx + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + 2] = w[4];
                        dp[dpIdx + dpL + dpL + 3] = w[4];
                        if (Util.diff(w[7], w[3], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
                        }
                        dp[dpIdx + dpL + dpL + dpL + 1] = w[4];
                        dp[dpIdx + dpL + dpL + dpL + 2] = w[4];
                        if (Util.diff(w[5], w[7], trY, trU, trV, trA)) {
                            dp[dpIdx + dpL + dpL + dpL + 3] = w[4];
                        } else {
                            dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
                        }
                    }
                }
                spIdx++;
                dpIdx += 4;
            }
            dpIdx += dpL * 3;
        }
    }

    /**
     * This and the next caseXXX methods were used to reduce the code size of the main
     * {@link #scale4(int[], int[], int, int, int, int, int, int, boolean, boolean)} method because of the Java 65K bytecode limit.
     * Only the necessary methods were created, to leave the maximum code on the original one to avoid excessive calling.
     * However, this is a very bad design (too much code in the same method)
     */
    private static void case0(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case2(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case16(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case64(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case8(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case3(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case6(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case20(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case144(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
    }

    private static void case192(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
    }

    private static void case96(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case40(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case9(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case66(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case24(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case7(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case148(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix2To1To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
    }

    private static void case224(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix2To1To1(w[4], w[1], w[3]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[3]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix6To1To1(w[4], w[3], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
    }

    private static void case41(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[5]);
        dp[dpIdx + 3] = Util.mix2To1To1(w[4], w[1], w[5]);
        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix6To1To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix2To1To1(w[4], w[7], w[5]);
    }

    private static void case67(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case70(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case28(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
        dp[dpIdx + 2] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 3] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case152(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[7]);
    }

    private static void case194(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix4To2To1(w[4], w[3], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix7To1(w[4], w[5]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[5]);
    }

    private static void case98(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + 2] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix4To2To1(w[4], w[3], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix4To2To1(w[4], w[5], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix7To1(w[4], w[3]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case56(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[0]);
        dp[dpIdx + 1] = Util.mix4To2To1(w[4], w[1], w[0]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix3To1(w[4], w[0]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[0]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[7]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix5To3(w[4], w[7]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }

    private static void case25(final int[] dp, final int dpIdx, final int dpL, final int[] w) {
        dp[dpIdx] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 1] = Util.mix5To3(w[4], w[1]);
        dp[dpIdx + 2] = Util.mix4To2To1(w[4], w[1], w[2]);
        dp[dpIdx + 3] = Util.mix5To3(w[4], w[2]);
        dp[dpIdx + dpL] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 1] = Util.mix7To1(w[4], w[1]);
        dp[dpIdx + dpL + 2] = Util.mix7To1(w[4], w[2]);
        dp[dpIdx + dpL + 3] = Util.mix3To1(w[4], w[2]);
        dp[dpIdx + dpL + dpL] = Util.mix3To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 1] = Util.mix7To1(w[4], w[6]);
        dp[dpIdx + dpL + dpL + 2] = Util.mix7To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + 3] = Util.mix3To1(w[4], w[8]);
        dp[dpIdx + dpL + dpL + dpL] = Util.mix5To3(w[4], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 1] = Util.mix4To2To1(w[4], w[7], w[6]);
        dp[dpIdx + dpL + dpL + dpL + 2] = Util.mix4To2To1(w[4], w[7], w[8]);
        dp[dpIdx + dpL + dpL + dpL + 3] = Util.mix5To3(w[4], w[8]);
    }
}
