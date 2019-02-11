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

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 *
 * @author Coa
 */
public class BMF extends DoomFont {

    // THIS WAS INSPIRED BY C library by Tomas Dvorak
    // SO ALL CREDITS TO HIM FOR DOING SUCH A FANTASTIC JOB
    // This version is always 0x11, irrelevant
    private int version = 0x11;
    // Some information about the font
    private String info;

    // Index 0 is transparent and not described,
    // Index 1 is where the whole thing with palette starts,
    // There's a reason though we cannot allow palettes larger than 256.
    public static final int PAL_MAX_SIZE = 256;

    // As said - line height..
    private int line_height;
    // Size over the base line
    private int size_over;
    // Size under the base line	Signed value
    private int size_under;
    // Space after each character, in addition to each character's own shift
    private int add_space;
    // Inner size (UNKNOWN PURPOSE)
    private int size_inner;
    // Count of used colors (not the same as palette size)
    private int colors;
    // Highest used color index
    private int highest_color;

    // Characters of Bytemap Font 
    // (it's length is all the chars that can be displayed)
    // and containts only the chars that can be displayed, not outside the range       
    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS 
    //--------------------------------------------------------------------------
    // A1 - CONSTRUCTOR USED WHEN READING FROM THE BINARY "BMF" FILE
    public BMF(byte[] buffer, int pos) {
        super(buffer, pos);
        this.loadFont(); // loads font by creating char data about it
        this.type = "BMF";
    }

    // A2 - CONSTRUCTOR USED WHEN MAKING "BMF" FONT FROM PRE EXISTING INSTALLED FONT - NEW SCHOOL VARIANT
    public BMF(String info, int spacing,
            int line_height, int size_over, int size_under,
            BufferedImage image, BMFChar[] charVector) {
        super(image, charVector);
        this.info = info;
        this.add_space = spacing;
        this.line_height = line_height;
        this.size_over = size_over;
        this.size_under = size_under;
        this.unloadFont();
        this.type = "BMF";
    }

    //--------------------------------------------------------------------------
    // B - METHODS
    //--------------------------------------------------------------------------            
    private void loadFont() {
        if (buffer[0] == (byte) 0xE1 && buffer[1] == (byte) 0xE6 && buffer[2] == (byte) 0xD5 && buffer[3] == (byte) 0x1A) { // BMF Magic Header
            // -- READING FIRST DATA AFTER THE HEADER                                                 
            this.version = buffer[4] & 0xFF; // version (currently 11h) 
            this.line_height = buffer[5] & 0xFF; // line-height
            this.size_over = buffer[6]; // size-over the base line (-128…127)
            this.size_under = buffer[7]; // size-under the base line (-128…127)
            this.add_space = buffer[8]; // add-space after each char (-128…127)
            this.size_inner = buffer[9]; // size-inner (non-caps level) (-128…127)
            this.colors = buffer[10] & 0xFF; // count of used colors (should be <= 32) 
            this.highest_color = buffer[11] & 0xFF; // highest used color attribute  
            // -- RESERVED            
            // -- READ NUMBER OF RGB ENTRIES (P)            
            int p = buffer[16] & 0xFF; // number of RGB entries (P)                                      
            // -- READ FONT PALETTE              
            int pos = 17; // is position
            // -- FONT PALETTE (RGB bytes, max=63)
            this.palette.add(transparentColor);
            for (int i = 0; i < p; i++) {
                int red = buffer[pos + i * 3] & 0xFF;
                int green = buffer[pos + i * 3 + 1] & 0xFF;
                int blue = buffer[pos + i * 3 + 2] & 0xFF;
                Color color = new Color(
                        Math.min(red << 2 | red >> 4, 0xFF),
                        Math.min(green << 2 | green >> 4, 0xFF),
                        Math.min(blue << 2 | blue >> 4, 0xFF));
                if (!color.equals(transparentColor) && !this.palette.contains(color) && this.palette.size() < PAL_MAX_SIZE) {
                    this.palette.add(color);
                }

            }
            pos += 3 * p;
            /* in bmf, color 0 is meant to be transparent. i start
                    * reading palette 3 bytes after and put pink (rgb = 0xff00ff)
                    * in the first color. */
            // -- READ INFO LENGTH                    
            int l = buffer[pos++] & 0xFF;
            // -- READ INFO STRING            
            char[] info = new char[l];
            for (int i = 0; i < l; i++) {
                info[i] = (char) buffer[pos++];
            }
            this.info = String.valueOf(info);
            // -- READING NUMBER OF CHARACTERS IN THE FONT                                                     
            int numchars = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
            pos += 2;
            // -- END
            // -- AND THE NEW BEGINNING
            this.chars = new BMFChar[numchars];
            // -- READING CHAR ITSELF, ONE BY ONE                                                                                                   
            for (int i = 0; i < numchars; i++) {
                char c = (char) buffer[pos];
                int w = buffer[pos + 1] & 0xFF;
                int h = buffer[pos + 2] & 0xFF;
                this.chars[i] = new BMFChar(c, w, h);
                BMFChar ch = (BMFChar) this.chars[i];
                if (h > this.maxheight) {
                    this.maxheight = h;
                }
                ch.setRelx((int) buffer[pos + 3]);
                ch.setRely((int) buffer[pos + 4]);
                ch.setShift(buffer[pos + 5]);
                ch.setOffset(this.totalwidth);
                pos += 6;
                this.totalwidth += w + this.add_space;
                System.arraycopy(buffer, pos, ch.getData(), 0, w * h);
                pos += w * h;
            }
        }
        this.error = (this.line_height <= 0 || this.palette.isEmpty() || this.chars.length <= 0 || this.chars.length > 256);
        if (this.line_height <= 0) {
            this.errorMsg = "Error - Negative or zero line height!";
        } else if (this.palette.isEmpty()) {
            this.errorMsg = "Error - This font has no colors!";
        } else if (this.chars.length <= 0) {
            this.errorMsg = "Error - This font has no characters!";
        } else if (this.chars.length > 256) {
            this.errorMsg = "Error - This font has over 256 characters!";
        }
        // error is not successful        
    }

    private void unloadFont() {
        // -- WRITING MAGIC HEADER            
        this.buffer[0] = (byte) 0xE1;
        this.buffer[1] = (byte) 0xE6;
        this.buffer[2] = (byte) 0xD5;
        this.buffer[3] = (byte) 0x1A;
        // -- WRITING MORE METADATA
        this.buffer[4] = (byte) this.version;
        this.buffer[5] = (byte) this.line_height;
        this.buffer[6] = (byte) this.size_over;
        this.buffer[7] = (byte) this.size_under;
        this.buffer[8] = (byte) this.add_space;
        this.buffer[9] = (byte) this.size_inner;
        this.buffer[10] = (byte) this.colors;
        this.buffer[11] = (byte) this.highest_color;
        // -- WRITING ZEROS AS RESERVED - NOT IMPORTANT
        this.buffer[12] = 0x00;
        this.buffer[13] = 0x00;
        this.buffer[14] = 0x00;
        this.buffer[15] = 0x00;
        // -- WRITING PALETTE SIZE
        int p = Math.min(this.palette.size() - 1, 0xFF);
        this.buffer[16] = (byte) p;
        this.pos = 17;
        // -- WRITING PALETTE ITSELF                        
        for (int i = 1; i < this.palette.size(); i++) {
            this.buffer[pos] = (byte) ((this.palette.get(i).getRed() & 0xFF) >> 2);
            this.buffer[pos + 1] = (byte) ((this.palette.get(i).getGreen() & 0xFF) >> 2);
            this.buffer[pos + 2] = (byte) ((this.palette.get(i).getBlue() & 0xFF) >> 2);
            pos += 3;
        }
        // -- WRITING INFO AND ITS CONTENT        
        this.buffer[pos++] = (byte) this.info.length();
        for (int i = 0; i < this.info.getBytes().length; i++) {
            this.buffer[pos++] = this.info.getBytes()[i];
        }
        // -- WRITING NUMBER OF CHARS IN THE FONT
        if (this.chars.length < 256) {
            this.buffer[pos] = (byte) this.chars.length;
            this.buffer[pos + 1] = 0x00;
        } else {
            this.buffer[pos] = 0x00;
            this.buffer[pos + 1] = 0x01;
        }
        this.pos += 2;
        // -- FOR EACH CHAR WRITE THE SPECIFICS
        for (DoomFontChar ch : this.chars) {
            BMFChar bmfCh = (BMFChar) ch;
            this.buffer[pos] = (byte) ch.getC();
            this.buffer[pos + 1] = (byte) ch.getW();
            this.buffer[pos + 2] = (byte) ch.getH();
            this.buffer[pos + 3] = (byte) bmfCh.getRelx();
            this.buffer[pos + 4] = (byte) bmfCh.getRely();
            this.buffer[pos + 5] = (byte) bmfCh.getShift();
            this.pos += 6;
            System.arraycopy(ch.getData(), 0, buffer, pos, ch.getData().length);
            this.pos += ch.getData().length;
        }
    }

    @Override
    public BufferedImage generateImage(boolean transparency) {
        BufferedImage image = null;
        if (verticalOffsets == true) {
            return null;
        }
        image = new BufferedImage(this.totalwidth + 2, this.maxheight + 2,
                transparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        for (DoomFontChar ch : this.chars) {
            BMFChar bmfCh = (BMFChar) ch;
            for (int x = 0; x < ch.getW(); x++) {
                for (int y = 0; y < ch.getH(); y++) {
                    int e = ch.getW() * y + x;
                    int index = ch.getData()[e];
                    if (index >= 0 && index < this.palette.size()) {
                        Color color = this.palette.get(index);
                        if (!color.equals(transparentColor)) {
                            int px = x + bmfCh.getRelx() + ch.getOffset();
                            int py = y + bmfCh.getRely();
                            if (px >= 0 && px < this.totalwidth && py >= 0 && py < this.maxheight) {
                                image.setRGB(px, py, color.getRGB());
                            }
                        }
                    }
                }
            }
        }
        return image;
    }

    @Override
    public BufferedImage generateImage(boolean transparency, String text) {
        // 1. initializing
        BufferedImage image = null;
        if (text.isEmpty() || verticalOffsets == true) {
            return null;
        }
        int totalwidth = 0;
        int[] offsets = new int[text.length()];
        // 2. calculating
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            offsets[i] = totalwidth;
            BMFChar ch = (BMFChar) this.giveChar(c);
            if (ch != null) {
                totalwidth += ch.getShift() + this.add_space;
            }
        }
        // 3. creating image        
        image = new BufferedImage(totalwidth + 2, this.maxheight + 2,
                transparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        // 4. writing to pixels of the image
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            BMFChar ch = (BMFChar) this.giveChar(c);
            if (ch != null) {
                for (int x = 0; x < ch.getW(); x++) {
                    for (int y = 0; y < ch.getH(); y++) {
                        int e = ch.getW() * y + x;
                        int index = ch.getData()[e];
                        if (index >= 0 && index < this.palette.size()) {
                            Color color = this.palette.get(index);
                            if (!color.equals(transparentColor)) {
                                int px = x + ch.getRelx() + offsets[i];
                                int py = y + ch.getRely();
                                if (px >= 0 && px < totalwidth && py >= 0 && py < this.maxheight) {
                                    image.setRGB(px, py, color.getRGB());
                                }
                            }
                        }
                    }
                }
            }
        }
        return image;
    }

    //--------------------------------------------------------------------------
    // C - GETTERS AND SETTERS (TRIVIAL) 
    //--------------------------------------------------------------------------
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getLine_height() {
        return line_height;
    }

    public void setLine_height(int line_height) {
        this.line_height = line_height;
    }

    public int getSize_over() {
        return size_over;
    }

    public void setSize_over(int size_over) {
        this.size_over = size_over;
    }

    public int getSize_under() {
        return size_under;
    }

    public void setSize_under(int size_under) {
        this.size_under = size_under;
    }

    public int getAdd_space() {
        return add_space;
    }

    public void setAdd_space(int add_space) {
        this.add_space = add_space;
    }

    public int getSize_inner() {
        return size_inner;
    }

    public void setSize_inner(int size_inner) {
        this.size_inner = size_inner;
    }

    public int getColors() {
        return colors;
    }

    public void setColors(int colors) {
        this.colors = colors;
    }

    public int getHighest_color() {
        return highest_color;
    }

    public void setHighest_color(int highest_color) {
        this.highest_color = highest_color;
    }

    public void setChars(BMFChar[] chars) {
        this.chars = chars;
    }

}
