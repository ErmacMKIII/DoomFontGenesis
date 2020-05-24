/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.dfg.fonts;

import com.sun.jimi.core.util.Packbits;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BigFont extends DoomFont {

    private boolean constantWidth = false;
    private boolean usesKerning = false;
    private int kerning = 0;

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS 
    //--------------------------------------------------------------------------
    // A1 - CONSTRUCTOR USED WHEN READING FROM THE BINARY FONT FILE
    public BigFont(byte[] buffer) {
        super(buffer);
        this.loadFont(); // loads font by creating char data about it
    }

    // A2 - CONSTRUCTOR USED WHEN MAKING BIG FONT FROM PRE EXISTING INSTALLED FONT
    public BigFont(BufferedImage image, DoomFontChar[] charVector) {
        super(image, charVector);
        this.unloadFont(); // unloads font to the buffer
    }

    //--------------------------------------------------------------------------
    // B - IMPLEMENTED METHODS FOR INITIALIZATION
    //--------------------------------------------------------------------------
    @Override
    protected String initFontType() {
        return "FON2";
    }

    @Override
    protected Color initTransparentColor() {
        return new Color(35, 0, 60);
    }

    @Override
    protected boolean initVerticalOffsets() {
        return false;
    }

    //--------------------------------------------------------------------------
    // C - PRIVATE METHODS FOR CONSTRUCTORS
    //--------------------------------------------------------------------------
    // priv method for the constructor A1 (Big Font) -> Buffer to Font
    private void loadFont() {
        if (buffer[0] == 'F' && buffer[1] == 'O' && buffer[2] == 'N' && buffer[3] == '2') { // The characters 'F', 'O', 'N', and '2'.
            // -- READING FIRST DATA AFTER THE HEADER                                           
            // Technically used as a boolean, since there is only one flag. 
            // 1 if the font kerning information, 0 otherwise.                        
            this.maxheight = (buffer[5] & 0xFF) << 8 | (buffer[4] & 0xFF); // Unsigned value for character height.
            int firstChar = buffer[6] & 0xFF; // ASCII number for the first character described in the font.
            int lastChar = buffer[7] & 0xFF; // ASCII number for the last character described in the font.
            int numchars = lastChar - firstChar + 1;
            this.constantWidth = (buffer[8] != 0x00); // 1 if the characters are of constant width, 0 otherwise.
            int p = buffer[10] & 0xFF; // Amount of active colors in the palette.
            // The true palette size is one greater, 
            // as the last palette entry is for the inactive color.
            p++;
            this.usesKerning = (buffer[11] != 0x00);
            // -- READ POSSIBLE KERNING VALUE
            this.pos = 12;
            if (this.usesKerning) {
                this.kerning = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
            }
            // -- READ CHARACTER WIDTHS    
            int[] chWidths = new int[(this.constantWidth) ? 1 : numchars];

            for (int i = 0; i < chWidths.length; i++) {
                chWidths[i] = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
            }
            // -- READ FONT PALETTE                                                                                  
            for (int i = 0; i < p; i++) {
                int red = buffer[pos + i * 3] & 0xFF;
                int green = buffer[pos + i * 3 + 1] & 0xFF;
                int blue = buffer[pos + i * 3 + 2] & 0xFF;
                Color color = new Color(red, green, blue);
                if (!this.palette.contains(color) && this.palette.size() <= PAL_MAX_SIZE) {
                    this.palette.add(color);
                }

            }
            this.transparentColor = this.palette.get(0);
            pos += 3 * p;
            // -- END
            // -- AND THE NEW BEGINNING
            this.chars = new DoomFontChar[numchars];
            // -- READING CHAR ITSELF, ONE BY ONE 
            for (int i = 0; i < numchars; i++) {
                char c = (char) (firstChar + i);
                int w = chWidths[i];
                if (w > 0) {
                    int h = maxheight;
                    int numpixels = w * h;
                    this.chars[i] = new DoomFontChar(c, w, h);
                    int e = 0; // encoded data index
                    int len = 0; // length
                    byte code;
                    while (numpixels > 0) { // THANKS SLADE FOR HELP!
                        code = buffer[pos++];
                        if (code > 0) {
                            len = code + 1;
                            // Overflows shouldn't happen!    
                            if (len > numpixels) {
                                this.chars = null;
                                this.error = true;
                                this.errorMsg = "Error - Character Overflow!";
                                return;
                            }
                            System.arraycopy(buffer, pos, this.chars[i].getData(), e, len);
                            e += len;
                            pos += len;
                            numpixels -= len;
                        } else if (code != -128) {
                            len = (-code) + 1;
                            // Overflows shouldn't happen!
                            if (len > numpixels) {
                                this.chars = null;
                                this.error = true;
                                this.errorMsg = "Error - Character Overflow!";
                                return;
                            }
                            code = buffer[pos++];
                            Arrays.fill(this.chars[i].getData(), e, e + len, code);
                            e += len;
                            numpixels -= len;
                        }
                    }
                    this.chars[i].setOffset(totalwidth);
                    this.totalwidth += w;
                }
            }
        }
        this.error = (this.maxheight <= 0 || this.palette.isEmpty() || this.chars.length <= 0 || this.chars.length > 256);
        if (this.maxheight <= 0) {
            this.errorMsg = "Error - Negative or zero font height!";
        } else if (this.palette.isEmpty()) {
            this.errorMsg = "Error - This font has no colors!";
        } else if (this.chars.length <= 0) {
            this.errorMsg = "Error - This font has no characters!";
        } else if (this.chars.length > 256) {
            this.errorMsg = "Error - This font has over 256 characters!";
        }
        // error is not successful
    }

    // priv method for the constructor A2 (Big Font) -> Font to Buffer
    private void unloadFont() {
        // -- WRITING HEADER OF THE BIG FONT
        this.buffer[0] = 'F'; // The characters 'F', 'O', 'N', and '2'.   
        this.buffer[1] = 'O';
        this.buffer[2] = 'N';
        this.buffer[3] = '2';
        this.buffer[4] = (byte) (this.maxheight & 0xFF); // Unsigned value for character height.
        this.buffer[5] = (byte) ((this.maxheight >> 8) & 0xFF); // In Little-endian format                        
        this.buffer[6] = (byte) (this.chars[0].getC()); // ASCII number for the first character described in the font.
        this.buffer[7] = (byte) (this.chars[this.chars.length - 1].getC()); // ASCII number for the last character described in the font.
        this.buffer[8] = (byte) (this.constantWidth ? 0x01 : 0x00); // Constant width : 1 if the characters are of constant width, 0 otherwise.            
        this.buffer[9] = (byte) 0x00; // Shading type : Seems unused.
        // -- WRITING PALETTE SIZE
        // Palette size : Amount of active colors in the palette. The true palette size is one greater, 
        // as the last palette entry is for the inactive color.
        int p = Math.min(this.palette.size() - 1, 0xFF);
        this.buffer[10] = (byte) p;

        this.buffer[11] = (byte) (this.usesKerning ? 0x01 : 0x00); // Whether or not this font uses kerning
        this.pos = 12;
        if (this.usesKerning) { // if there is kerning information, 
            this.buffer[pos] = (byte) (this.kerning & 0xFF); // the header is followed by a signed two-byte value for it.
            this.buffer[pos + 1] = (byte) ((this.kerning >> 8) & 0xFF);
            pos += 2;
        }
        if (this.constantWidth && this.chars != null) {
            buffer[pos] = (byte) (this.chars[0].getW() & 0xFF);
            buffer[pos + 1] = (byte) ((this.chars[0].getW() >> 8) & 0xFF);
            pos += 2;
        } else {
            for (DoomFontChar ch : this.chars) {
                buffer[pos] = (byte) (ch.getW() & 0xFF);
                buffer[pos + 1] = (byte) ((ch.getW() >> 8) & 0xFF);
                pos += 2;
            }
        }
        // -- WRITING PALETTE ITSELF                        
        for (int i = 0; i < this.palette.size(); i++) {
            buffer[pos] = (byte) (this.palette.get(i).getRed() & 0xFF);
            buffer[pos + 1] = (byte) (this.palette.get(i).getGreen() & 0xFF);
            buffer[pos + 2] = (byte) (this.palette.get(i).getBlue() & 0xFF);
            pos += 3;
        }
        // -- LOOPING THROUGH ALL THE CHARS -- COMPRESSING DATA -- FINALIZING        
        for (DoomFontChar ch : this.chars) {
            if (ch != null) {
                byte[] temp = new byte[ch.getData().length];
                // compress the char data and return the compressed size (len)
                int len = Packbits.packbits(ch.getData(), temp);
                System.arraycopy(temp, 0, buffer, pos, len);
                this.pos += len;
            }
        }
    }
    //--------------------------------------------------------------------------
    // D - GETTERS
    //--------------------------------------------------------------------------

    public boolean isConstantWidth() {
        return constantWidth;
    }

    public boolean isUsesKerning() {
        return usesKerning;
    }

    public int getKerning() {
        return kerning;
    }

}
