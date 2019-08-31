/*
 * Copyright (C) 2019 Coa
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
 * @author Coa
 */
public class ConsoleFont extends DoomFont {

    private static final float LUMA_RED_COEFF = 0.2126f;
    private static final float LUMA_GREEN_COEFF = 0.7152f;
    private static final float LUMA_BLUE_COEFF = 0.0722f;

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS
    //--------------------------------------------------------------------------     
    // A1 - CONSTRUCTOR USED WHEN READING FROM THE BINARY FONT FILE
    public ConsoleFont(byte[] buffer) {
        super(buffer);
        this.loadFont();
    }

    // A2 - CONSTRUCTOR USED WHEN MAKING "BMF" FONT FROM PRE EXISTING INSTALLED FONT    
    public ConsoleFont(BufferedImage image, DoomFontChar[] charVector) {
        super(image, charVector);
        this.unloadFont();
    }

    //--------------------------------------------------------------------------
    // B - IMPLEMENTED METHODS FOR INITIALIZATION
    //--------------------------------------------------------------------------
    @Override
    protected String initFontType() {
        return "FON1";
    }

    @Override
    protected Color initTransparentColor() {
        return Color.BLACK;
    }

    @Override
    protected boolean initVerticalOffsets() {
        return true;
    }

    //--------------------------------------------------------------------------
    // C - PRIVATE METHODS FOR CONSTRUCTORS
    //--------------------------------------------------------------------------
    // priv method for the constructor A1 (Console Font) -> Buffer to Font
    private void loadFont() {
        if (buffer[0] == 'F' && buffer[1] == 'O' && buffer[2] == 'N' && buffer[3] == '1') { // The characters 'F', 'O', 'N', and '1'.
            // -- READING FIRST DATA AFTER THE HEADER                                           
            // Technically used as a boolean, since there is only one flag. 
            // 1 if the font kerning information, 0 otherwise.                        
            int w = (buffer[5] & 0xFF) << 8 | (buffer[4] & 0xFF); // Unsigned value for character width.
            int h = (buffer[7] & 0xFF) << 8 | (buffer[6] & 0xFF); // Unsigned value for character height.            
            this.totalwidth = w;
            this.pos = 8;
            this.chars = new DoomFontChar[256];
            for (int i = 0; i < this.chars.length; i++) {
                int numpixels = w * h;
                byte[] temp = new byte[numpixels]; // temp data
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
                        System.arraycopy(buffer, pos, temp, e, len);
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
                        Arrays.fill(temp, e, e + len, code);
                        e += len;
                        numpixels -= len;
                    }
                }

                this.chars[i] = new DoomFontChar((char) i, w, h);
                this.chars[i].setOffset(this.maxheight);
                this.maxheight += this.chars[i].getH();

                // Converting grayscale temp info to indexed color info of char data
                for (int j = 0; j < temp.length; j++) {
                    Color color = new Color(temp[j] & 0xFF, temp[j] & 0xFF, temp[j] & 0xFF);
                    if (!this.palette.contains(color)) {
                        this.palette.add(color);
                    }
                    if (!color.equals(transparentColor)) {
                        this.chars[i].getData()[j] = (byte) palette.indexOf(color);
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
        }
    }

    // priv method for the constructor A2 (Console Font) -> Font to Buffer
    private void unloadFont() {
        // -- WRITING HEADER OF THE BIG FONT
        this.buffer[0] = 'F'; // The characters 'F', 'O', 'N', and '1'.   
        this.buffer[1] = 'O';
        this.buffer[2] = 'N';
        this.buffer[3] = '1';
        this.buffer[4] = (byte) (this.chars[0].getW() & 0xFF); // Unsigned value for character height.
        this.buffer[5] = (byte) (((this.chars[0].getW()) >> 8) & 0xFF); // In Little-endian format                        
        this.buffer[6] = (byte) (this.chars[0].getH() & 0xFF); // Unsigned value for character width.
        this.buffer[7] = (byte) ((this.chars[0].getH() >> 8) & 0xFF); // In Little-endian format.
        this.pos = 8;
        // -- LOOPING THROUGH ALL THE CHARS -- COMPRESSING DATA -- FINALIZING        
        for (DoomFontChar ch : this.chars) {
            if (ch != null) {
                byte[] grayscale = new byte[ch.getData().length];
                for (int i = 0; i < ch.getData().length; i++) {
                    int index = ch.getData()[i] & 0xFF;
                    Color col = this.palette.get(index);
                    grayscale[i] = (byte) (col.getRed() * LUMA_RED_COEFF + col.getGreen() * LUMA_GREEN_COEFF + col.getBlue() * LUMA_BLUE_COEFF);
                }
                byte[] temp = new byte[grayscale.length];
                // compress the char data and return the compressed size (len)
                int len = Packbits.packbits(grayscale, temp);
                System.arraycopy(temp, 0, buffer, pos, len);
                this.pos += len;
            }
        }
    }
}
