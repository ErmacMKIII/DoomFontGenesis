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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Coa
 */
public class DoomFont {

    // Font type in set {FON1, FON2, BMF}
    protected String type;

    // Doom color comparator (taken from ZDoom)
    protected static Comparator<Color> doomComp = new Comparator<Color>() {
        @Override
        public int compare(Color o1, Color o2) {
            return (299 * (o1.getRed() - o2.getRed()) + 587 * (o1.getGreen() - o2.getGreen()) + 114 * (o1.getBlue() - o2.getBlue()));
        }
    };

    // Array List of RGB entries - palette of colors
    protected ArrayList<Color> palette = new ArrayList<Color>();

    // Index 0 is transparent and not described,
    // Index 1 is where the whole thing with palette starts,
    // There's a reason though we cannot allow palettes larger than 256.
    public static final int PAL_MAX_SIZE = 255;

    // Setting this color is important
    protected Color transparentColor = new Color(35, 0, 60);

    // Setting this color is also important (it must be added as the last color)
    // for some things to work
    protected Color unusedColor = new Color(167, 107, 107);

    // Characters of the Doom Font 
    // (it's length is all the chars that can be displayed)
    // and containts only the chars that can be displayed, not outside the range
    protected DoomFontChar[] chars;

    // Values for display
    protected int totalwidth = 0;
    protected int maxheight = 0;

    protected byte[] buffer = new byte[65536]; // buffer is 64K size
    protected int pos = 0; // position in the buffer;

    // Are offsets vertical (for BigFont and BMF offsets are horizontal 
    // but for ConsoleFont offsets are vertical)
    protected boolean verticalOffsets = false;

    // Helping the user figure out where error occurred
    protected boolean error = false;
    protected String errorMsg;

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS
    //--------------------------------------------------------------------------     
    // A1 - CONSTRUCTOR USED WHEN READING FROM THE BINARY FONT FILE
    public DoomFont(byte[] buffer, int pos) {
        this.buffer = buffer;
        this.pos = pos;
    }

    // A2 - CONSTRUCTOR USED WHEN MAKING "BMF" FONT FROM PRE EXISTING INSTALLED FONT
    public DoomFont(BufferedImage image, DoomFontChar[] charVector) {
        this.chars = charVector;
        if (image != null && charVector != null) {
            this.palette.add(transparentColor);
            for (DoomFontChar ch : charVector) {
                for (int x = 0; x < ch.getW(); x++) {
                    for (int y = 0; y < ch.getH(); y++) {
                        int e = ch.getW() * y + x;
                        int px, py;
                        if (this.verticalOffsets) {
                            px = x;
                            py = y + ch.getOffset();
                        } else {
                            px = x + ch.getOffset();
                            py = y;
                        }
                        if (px >= 0 && px < image.getWidth() && py >= 0 && py < image.getHeight()) {
                            Color color = new Color(image.getRGB(px, py));
                            if (!color.equals(Color.BLACK)) {
                                if (palette.size() < PAL_MAX_SIZE) { // if pallete size is less then MAX ALLOWED SIZE
                                    if (!palette.contains(color)) { // if it doesn't contain color
                                        palette.add(color); // add the color
                                    }
                                    ch.getData()[e] = (byte) (Math.min(palette.indexOf(color), 0xFF));
                                } else if (palette.size() == PAL_MAX_SIZE) { // if pallete is MAXED OUT, use approximation
                                    if (!palette.contains(color)) {
                                        int mindeviation = 255000;
                                        int mindevindex = -1;
                                        for (Color palColor : palette) { // by finding color with minimal absolute deviation
                                            int deviation = Math.abs(doomComp.compare(color, palColor));
                                            if (deviation < mindeviation) {
                                                mindeviation = deviation;
                                                mindevindex = palette.indexOf(palColor);
                                            }
                                        }
                                        if (mindevindex != -1) { // and parsing that color index into the character data
                                            ch.getData()[e] = (byte) (Math.min(mindevindex, 0xFF));
                                        }
                                    } else {
                                        ch.getData()[e] = (byte) (Math.min(palette.indexOf(color), 0xFF));
                                    }
                                }
                            }
                        }
                    }
                }

                this.totalwidth += ch.getW();
                if (ch.getH() > this.maxheight) {
                    this.maxheight = ch.getH();
                }
            }
            this.palette.add(unusedColor); // TRICK FOR ZDOOM :) - cuz one-color palette won't work
        }
    }

    //--------------------------------------------------------------------------
    // B - METHODS
    //--------------------------------------------------------------------------
    // search all the characters in the font and returns one which holds the value same as the key
    protected DoomFontChar giveChar(char key) {
        for (DoomFontChar ch : this.chars) {
            if (ch != null && ch.getC() == key) {
                return ch;
            }
        }
        return null;
    }

    // generates image displaying all the characters in the font
    public BufferedImage generateImage(boolean transparency) {
        BufferedImage image = null;
        image = new BufferedImage(this.totalwidth + 2, this.maxheight + 2,
                transparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        for (DoomFontChar ch : this.chars) {
            if (ch != null) {
                for (int x = 0; x < ch.getW(); x++) {
                    for (int y = 0; y < ch.getH(); y++) {
                        int e = ch.getW() * y + x;
                        int index = ch.getData()[e] & 0xFF;
                        if (index >= 0 && index < this.palette.size()) {
                            Color color = this.palette.get(index);
                            if (!color.equals(transparentColor)) {
                                int px, py;
                                if (this.verticalOffsets) {
                                    px = x;
                                    py = y + ch.getOffset();
                                } else {
                                    px = x + ch.getOffset();
                                    py = y;
                                }
                                if (px >= 0 && px < this.totalwidth && py >= 0 && py < this.maxheight) {
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

    // generaters image displaying text, it allows typing in the font
    public BufferedImage generateImage(boolean transparency, String text) {
        // 1. initializing
        BufferedImage image = null;
        if (text.isEmpty()) {
            return null;
        }
        int totalwidth = 0;
        int[] offsets = new int[text.length()];
        // 2. calculating
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            offsets[i] = totalwidth;
            DoomFontChar ch = this.giveChar(c);
            if (ch != null) {
                totalwidth += ch.getW();
            }
        }
        int maxheight = (this.verticalOffsets) ? this.chars[0].getH() : this.maxheight;
        // 3. creating image        
        image = new BufferedImage(totalwidth + 2, maxheight + 2,
                transparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        // 4. writing to pixels of the image
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            DoomFontChar ch = this.giveChar(c);
            if (ch != null) {
                for (int x = 0; x < ch.getW(); x++) {
                    for (int y = 0; y < ch.getH(); y++) {
                        int e = ch.getW() * y + x;
                        int index = ch.getData()[e] & 0xFF;
                        if (index >= 0 && index < this.palette.size()) {
                            Color color = this.palette.get(index);
                            if (!color.equals(transparentColor)) {
                                int px, py;
                                px = x + offsets[i];
                                py = y;
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

    // saves the font to the file
    public boolean saveToFile(File file) {
        boolean success = false;
        if (file.exists()) { // if file exists delete it
            file.delete();
        }   // if someone chosen to save file without extension add proper extension
        if (!file.getName().contains(".lmp") && !file.getName().contains(".bmf")) {
            switch (this.type) {
                case "FON1":
                    file = new File(file.getAbsolutePath() + ".lmp");
                    break;
                case "FON2":
                    file = new File(file.getAbsolutePath() + ".lmp");
                    break;
                case "BMF":
                    file = new File(file.getAbsolutePath() + ".bmf");
                    break;
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(this.buffer, 0, this.pos); // write buffer to the targeted file
            success = true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DoomFont.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DoomFont.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(DoomFont.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return success;
    }

    //--------------------------------------------------------------------------
    // C - STATIC METHODS
    //--------------------------------------------------------------------------
    // polymorphic way of loading the file, it returns font based on the it's header, that's why it's static
    public static DoomFont loadFromFile(File file) {
        DoomFont doomFont = null;
        byte[] buffer = new byte[65536]; // 64K buffer oh-yeah!
        if (file != null) {
            if (file.exists() && (file.getName().contains(".lmp") || file.getName().contains(".bmf"))) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    fis.read(buffer);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(DoomFont.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(DoomFont.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ex) {
                            Logger.getLogger(BMF.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                // POLYMORPHIC WAY OF MAKING FONTS
                // depending on the header {FON1, FON2 or BMF} 
                // it returns one font in that set or it stays null
                if (buffer[0] == 'F' && buffer[1] == 'O' && buffer[2] == 'N' && buffer[3] == '1') {
                    // The characters 'F', 'O', 'N', and '1'.
                    doomFont = new ConsoleFont(buffer, 0);
                } else if (buffer[0] == 'F' && buffer[1] == 'O' && buffer[2] == 'N' && buffer[3] == '2') {
                    // The characters 'F', 'O', 'N', and '2'.
                    doomFont = new BigFont(buffer, 0);
                } else if (buffer[0] == (byte) 0xE1 && buffer[1] == (byte) 0xE6 && buffer[2] == (byte) 0xD5 && buffer[3] == (byte) 0x1A) {
                    // BMF Magic Header
                    doomFont = new BMF(buffer, 0);
                }
            }
        }
        return doomFont;
    }

    //--------------------------------------------------------------------------
    // D - GETTERS AND SETTERS
    //--------------------------------------------------------------------------
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<Color> getPalette() {
        return palette;
    }

    public void setPalette(ArrayList<Color> palette) {
        this.palette = palette;
    }

    public Color getTransparentColor() {
        return transparentColor;
    }

    public void setTransparentColor(Color transparentColor) {
        this.transparentColor = transparentColor;
    }

    public Color getUnusedColor() {
        return unusedColor;
    }

    public void setUnusedColor(Color unusedColor) {
        this.unusedColor = unusedColor;
    }

    public DoomFontChar[] getChars() {
        return chars;
    }

    public void setChars(DoomFontChar[] chars) {
        this.chars = chars;
    }

    public int getTotalwidth() {
        return totalwidth;
    }

    public void setTotalwidth(int totalwidth) {
        this.totalwidth = totalwidth;
    }

    public int getMaxheight() {
        return maxheight;
    }

    public void setMaxheight(int maxheight) {
        this.maxheight = maxheight;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isVerticalOffsets() {
        return verticalOffsets;
    }

    public void setVerticalOffsets(boolean verticalOffsets) {
        this.verticalOffsets = verticalOffsets;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

}
