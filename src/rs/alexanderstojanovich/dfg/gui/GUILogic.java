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
package rs.alexanderstojanovich.dfg.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import rs.alexanderstojanovich.dfg.fonts.BMF;
import rs.alexanderstojanovich.dfg.fonts.BMFChar;
import rs.alexanderstojanovich.dfg.fonts.BigFont;
import rs.alexanderstojanovich.dfg.fonts.ConsoleFont;
import rs.alexanderstojanovich.dfg.fonts.DoomFont;
import rs.alexanderstojanovich.dfg.fonts.DoomFontChar;
import rs.alexanderstojanovich.dfg.util.ColorSample;
import rs.alexanderstojanovich.dfg.util.Palette;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GUILogic {

    // tells us did we initialize the GUI_Logic
    private boolean initialized = false;
    // font used to crate images   
    private Font myFont = new Font("Courier New", Font.PLAIN, 12);
    // text whibmfCharVector[i] contains all the characters in the font
    private String myText;
    // info about the author and the font
    private String myInfo = "";
    // this text is a test which user wrote in order to test the font
    private String myTest = "";
    // spacing between the characters (default is 0)
    private int spacing = 0;

    // base line height for BMF font
    private int line_height = 0;
    // size over the base line (often negative number)
    private int size_over = 0;
    // size under the base line (often positive number)
    private int size_under = 0;

    // BMF font either from loading or derived from prexising font
    private DoomFont fontLoad, fontDer;

    // Image which contains only rendered character data
    private BufferedImage imageRender;

    // Foreground (main or primary) color
    private Color fgColor = Color.YELLOW;
    // Background (secondary) color 
    private Color bgColor = Color.CYAN;
    // Outlining color
    private Color outlineColor = Color.BLUE;

    // shadow color (if shadow has been selected by the user)
    private Color shadowColor = Color.GRAY;

    // Way of displaying colors on the layered pane
    // via several labels coloured differently
    private JLabel[] colorVector = new JLabel[256];
    // Color panel which holds all the color labels
    private JPanel colorPanel; //= new JPanel(new GridLayout(16, 16, 1, 1));
    // Color vector for displaying the color map
    private DoomFontChar[] charVector;
    // Doom Font Format
    private String fontFormat = "FON1";

    // Image zoom factor (in percentage)
    private int zoom = 100;

    //--------------------------------------------------------------------------
    // A - CONSTRUCTORS 
    //--------------------------------------------------------------------------    
    public GUILogic(JPanel colorPanel) {
        this.colorPanel = colorPanel;
        initColorVectors();
        this.initialized = true;
    }

    //--------------------------------------------------------------------------
    // B - METHODS
    //--------------------------------------------------------------------------
    // init Palette display (it's called Color Vector)
    private void initColorVectors() {
        for (int i = 0; i < colorVector.length; i++) {
            colorVector[i] = new JLabel();
            colorVector[i].setBackground(Color.BLACK);
            colorVector[i].setOpaque(true);
            colorVector[i].setSize(9, 9);
            colorVector[i].setBorder(new BevelBorder(BevelBorder.RAISED));
            colorPanel.add(colorVector[i], new Integer(i));
        }
    }

    // open the file and load the font
    public boolean fileOpen(File file) {
        boolean ok = false;
        if (file != null) {
            fontLoad = DoomFont.loadFromFile(file);
            ok = ((fontLoad != null) && !fontLoad.isError());
        }
        return ok;
    }

    // saving loaded font to file
    public boolean fileSaveFontLoad(File file) {
        boolean ok = false;
        if (fontLoad != null && file != null) {
            ok = fontLoad.saveToFile(file);
        }
        return ok;
    }

    // saving derived font to file
    public boolean fileSaveFontDer(File file) {
        boolean ok = false;
        if (file != null) {
            switch (fontFormat) {
                case "FON1":
                    fontDer = new ConsoleFont(imageRender, charVector);
                    break;
                case "FON2":
                    fontDer = new BigFont(imageRender, charVector);
                    break;
                case "BMF":
                    fontDer = new BMF(myInfo, spacing, line_height, size_over, size_under, imageRender, (BMFChar[]) charVector);
                    break;
            }
            ok = fontDer.saveToFile(file);
        }
        return ok;
    }

    // make icon for loaded font
    public ImageIcon giveFontLoadIcon(boolean transparency) {
        ImageIcon imageIcon = null;
        if (fontLoad != null) {
            if (!myTest.isEmpty()) {
                imageRender = fontLoad.generateImage(transparency, myTest);
            } else {
                imageRender = fontLoad.generateImage(transparency);
            }
            if (imageRender != null) {
                AffineTransform xform = new AffineTransform();
                xform.scale(zoom / 100.0, zoom / 100.0);
                AffineTransformOp atOp = new AffineTransformOp(xform, null);
                BufferedImage destImage = atOp.filter(imageRender, null);
                imageIcon = new ImageIcon(destImage);
            }
            colorPanel.setEnabled(true);

            for (int i = 0; i < fontLoad.getPalette().size(); i++) {
                Color col = fontLoad.getPalette().get(i);
                colorVector[i].setBackground(col);
                colorVector[i].setToolTipText("Red = " + col.getRed()
                        + ", Green = " + col.getGreen() + ", Blue = " + col.getBlue());
            }

            for (int j = fontLoad.getPalette().size(); j < colorVector.length; j++) {
                colorVector[j].setBackground(Color.BLACK);
                colorVector[j].setToolTipText(null);
            }
        }
        return imageIcon;
    }

    // get Max String Bounds if Console Font is selected (it's gonna convert font to monospace)
    private static Rectangle2D maxStrBounds(Font font, FontRenderContext frc, String text) {
        double maxWidth = 0.0;
        Rectangle2D maxStrBounds = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Rectangle2D bounds = font.getStringBounds(String.valueOf(c), frc);
            if (bounds.getWidth() > maxWidth) {
                maxStrBounds = bounds;
                maxWidth = bounds.getWidth();
            }
        }
        return maxStrBounds;
    }

    // make icon in case for Big Font and BMF, boolean monospace is true in case of Console Font
    public ImageIcon giveFontDerIcon(boolean monospace, boolean transparency, boolean antialiasing, boolean useGradient, int outlineWidth, boolean shadow, int shadowAngle, double multiplier) {
        ImageIcon imageIcon = null;
        if (myFont != null && myText != null) {
            // define sampler
            double sampler = multiplier;
            if (outlineWidth > 0) {
                sampler *= 2.0 * outlineWidth;
            }
            if (shadow) {
                sampler *= 2.0;
            }
            // create the FontRenderContext object which helps us to measure the text             
            FontRenderContext frc = new FontRenderContext(null, antialiasing, true);
            Rectangle2D maxCharBounds = maxStrBounds(myFont, frc, myText);
            Rectangle2D globalBounds = myFont.getStringBounds(myText, frc);
            if (monospace) {
                maxCharBounds.setRect(globalBounds.getX(), globalBounds.getY(), (sampler + spacing + maxCharBounds.getWidth()) * myText.length(), maxCharBounds.getHeight() + sampler);
            } else {
                globalBounds.setRect(globalBounds.getX(), globalBounds.getY(), globalBounds.getWidth() + (sampler + spacing) * myText.length(), globalBounds.getHeight() + sampler);
            }

            Rectangle2D bounds = (monospace) ? maxCharBounds : globalBounds;

            // determining the three parameters for Byte Map Font                        
            line_height = (int) Math.round(bounds.getHeight());
            size_over = (int) Math.floor(bounds.getMinY());
            size_under = (int) Math.floor(bounds.getMaxY());

            // calculating with and height and adding +1 to be correctly displayed
            int w = (int) Math.round(bounds.getWidth()) + 2; // + 2 is used so borders around chars're correctly displayed
            int h = (int) Math.round(bounds.getHeight()) + 2; // + 2 is used so borders around chars're correctly displayed                        

            // create BufferedImage objects
            BufferedImage imageResult; // -- which is used later..
            BufferedImage imageOverlay; // -- which is used for rendering char boundaries

            // creating buffered images for display
            imageRender = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            imageOverlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            imageResult = new BufferedImage(w, h, transparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

            // calling createGraphics() to get the Graphics2D for each
            Graphics2D graphicsOverlay = imageOverlay.createGraphics();
            Graphics2D graphicsRender = imageRender.createGraphics();

            // do the first necessary translation for each
            graphicsOverlay.translate(0, -bounds.getY());
            graphicsRender.translate(0, -bounds.getY());

            // set the font for both
            graphicsOverlay.setFont(myFont);
            graphicsRender.setFont(myFont);

            if (antialiasing) {
                graphicsRender.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                graphicsRender.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            } else {
                graphicsRender.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);

                graphicsRender.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_DEFAULT);
            }

            graphicsRender.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // looping troughout the characters of the font
            if (fontFormat.equals("BMF")) {
                charVector = new BMFChar[myText.length()];
            } else {
                charVector = new DoomFontChar[myText.length()];
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < myText.length(); i++) {
                char c = myText.charAt(i);
                sb.append(c);
                // 1. drawing char bounds first
                Rectangle2D cb = (monospace) ? maxStrBounds(myFont, frc, myText) : myFont.getStringBounds(String.valueOf(c), frc);
                cb.setRect(cb.getX(), cb.getY(), cb.getWidth() + sampler, cb.getHeight() + sampler);

                graphicsOverlay.setColor(transparency ? Color.BLACK : Color.MAGENTA);
                graphicsOverlay.draw(cb);
                graphicsOverlay.translate(cb.getWidth() + spacing, 0);

                Rectangle2D cbx;
                int offset;
                if (monospace) {
                    offset = ((int) Math.round(i * cb.getWidth()) + i * spacing);
                } else {
                    cbx = myFont.getStringBounds(String.valueOf(sb), frc);
                    cbx.setRect(cbx.getX(), cbx.getY(), cbx.getWidth() + sampler, cbx.getHeight() + sampler);
                    offset = (int) Math.round(cbx.getWidth() - cb.getWidth() + i * (spacing + sampler));
                }

                if (fontFormat.equals("BMF")) {
                    charVector[i] = new BMFChar(c, (int) Math.round(cb.getWidth()), (int) Math.round(cb.getHeight()));
                    BMFChar bmfCh = (BMFChar) charVector[i];
                    bmfCh.setShift((int) Math.round(cb.getWidth()));
                    bmfCh.setOffset(offset);
                } else {
                    charVector[i] = new DoomFontChar(c, (int) Math.round(cb.getWidth()), (int) Math.round(cb.getHeight()));
                    charVector[i].setOffset(offset);
                }
                // 2. then if gradient is selected we choose paint or simple color -> for drawing                
                if (useGradient) {
                    TextLayout chLayout = new TextLayout(String.valueOf(c), myFont, frc);
                    Rectangle2D gb = chLayout.getBounds();
                    GradientPaint gp = new GradientPaint(
                            0.0f, (float) gb.getMinY() - (float) (0.5f * sampler),
                            fgColor,
                            0.0f, (float) gb.getMaxY() + (float) (0.5f * sampler),
                            bgColor, false);
                    graphicsRender.setPaint(gp);
                } else {
                    graphicsRender.setColor(fgColor);
                }
                graphicsRender.drawString(String.valueOf(c), (float) (0.5f * sampler), (float) (0.5f * sampler));

                // 3. necessary translation                
                graphicsRender.translate(cb.getWidth() + spacing, 0);
            }

            //if antialiasing is selected multiply color with it's alpha
            if (antialiasing) {
                for (int px = 0; px < imageRender.getWidth(); px++) {
                    for (int py = 0; py < imageRender.getHeight(); py++) {
                        Color srcCol = new Color(imageRender.getRGB(px, py), true);
                        if (srcCol.getAlpha() > 0) { // this if is in order to not ruin the borders around the chars                            
                            Color dstCol = new Color( // constructor with the three floats is called
                                    (srcCol.getAlpha() / 255.0f) * (srcCol.getRed() / 255.0f),
                                    (srcCol.getAlpha() / 255.0f) * (srcCol.getGreen() / 255.0f),
                                    (srcCol.getAlpha() / 255.0f) * (srcCol.getBlue() / 255.0f)
                            );
                            imageRender.setRGB(px, py, dstCol.getRGB());
                        }
                    }
                }
            }
            // if outline is selected;                                                
            if (outlineWidth > 0) {
                // Copy of raster of unaltered image is needed!!
                WritableRaster wr = imageRender.copyData(null);
                for (int px = 0; px < imageRender.getWidth(); px++) {
                    for (int py = 0; py < imageRender.getHeight(); py++) {
                        Color pixCol = new Color(imageRender.getRGB(px, py), true);
                        // writtable raster must be associated with ARGB image!!
                        ColorSample cs = ColorSample.getSample(wr, px, py, outlineWidth);
                        if (pixCol.getAlpha() == 0 && cs.getAlpha() > 0) {
                            imageRender.setRGB(px, py, outlineColor.getRGB());
                        }
                    }
                }
            }

            // if user selected shadow; this is for shadow effect                        
            if (shadow) {
                WritableRaster wr = imageRender.copyData(null);
                for (DoomFontChar ch : charVector) {
                    for (int px = ch.getOffset(); px < ch.getW() + ch.getOffset(); px++) {
                        for (int py = 0; py < ch.getH(); py++) {
                            // calculate dx an dy cuz they are one square away from px and py
                            float cos = (float) Math.cos(Math.toRadians(shadowAngle));
                            float sin = (float) Math.sin(Math.toRadians(shadowAngle));

                            float dx = px + cos;
                            float dy = py + sin;
                            // constrain dx and dy to the image bounds    
                            if (dx < ch.getOffset()) {
                                dx = ch.getOffset();
                            } else if (dx > ch.getW() + ch.getOffset() - 1) {
                                dx = (float) (ch.getW() + ch.getOffset() - 1);
                            }

                            if (dy < 0) {
                                dy = 0;
                            } else if (dy > ch.getH() - 1) {
                                dy = (float) (ch.getH() - 1);
                            }

                            Color dstPixCol = new Color(imageRender.getRGB(Math.round(dx), Math.round(dy)), true);
                            ColorSample csg = ColorSample.getGaussianBlurSample(wr, px, py); // calc gauss blur
                            float csa = csg.getAlpha() / 255.0f; // reason behind this value used is to prevent "too many wrong" pixels
                            if (dstPixCol.getAlpha() == 0 && csa >= 0.195346f) {
                                // to create nice shadow effect multiply shadow color components with alpha_sqrt
                                float alpha_sqrt = (float) Math.sqrt(csa);
                                float red = alpha_sqrt * shadowColor.getRed() / 255.0f;
                                float green = alpha_sqrt * shadowColor.getGreen() / 255.0f;
                                float blue = alpha_sqrt * shadowColor.getBlue() / 255.0f;
                                Color fineCol = new Color(red, green, blue);
                                imageRender.setRGB(Math.round(dx), Math.round(dy), fineCol.getRGB());
                            }
                        }
                    }
                }
            }

            // finalizing - merging overlay with rendered
            Graphics2D graphicsResult = imageResult.createGraphics();
            graphicsResult.drawImage(imageOverlay, 0, 0, null);
            // if user chose palette in the image, make conversion..
            if (Palette.isLoaded()) {
                IndexColorModel icm = new IndexColorModel(8, Palette.getColors().length, Palette.getColBuff(), 0, true);
                BufferedImage imageIndexed = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, icm);
                imageIndexed.createGraphics().drawImage(imageRender, 0, 0, null);
                imageRender = imageIndexed;
            }

            graphicsResult.drawImage(imageRender, 0, 0, null);

            AffineTransform xform = new AffineTransform();
            xform.scale(zoom / 100.0, zoom / 100.0);
            AffineTransformOp atOp = new AffineTransformOp(xform, null);
            BufferedImage destImage = atOp.filter(imageResult, null);

            imageIcon = new ImageIcon(destImage);
        }

        return imageIcon;
    }

    // Is wrapper for loading 6-bit RGB palette and displaying it's colors
    public void load6bitRGBPalette() {
        Palette.load6bitRGB();
        if (initialized) {
            for (int i = 0; i < Palette.getColors().length; i++) {
                Color col = new Color(Palette.getColors()[i]);
                colorVector[i].setBackground(col);
                colorVector[i].setToolTipText("Red = " + col.getRed()
                        + ", Green = " + col.getGreen() + ", Blue = " + col.getBlue());
            }

            for (int i = Palette.getColors().length; i < colorVector.length; i++) {
                colorVector[i].setBackground(Color.BLACK);
                colorVector[i].setToolTipText(null);
            }
        }
    }

    // Is wrapper for loading 8-bit RGB palette and displaying it's colors
    public void load8bitRGBPalette() {
        Palette.load8bitRGB();
        if (initialized) {
            for (int i = 0; i < Palette.getColors().length; i++) {
                Color col = new Color(Palette.getColors()[i]);
                colorVector[i].setBackground(col);
                colorVector[i].setToolTipText("Red = " + col.getRed()
                        + ", Green = " + col.getGreen() + ", Blue = " + col.getBlue());
            }
        }
    }

    // Loads palette from the file if palette Name is not null otherwise resets the palette
    // If palette is loaded it displays the colors otherwise it displays black squares
    public void loadPalette(String paletteName) {
        if (paletteName == null) {
            Palette.reset();
        } else {
            Palette.load(paletteName);
        }
        if (initialized && Palette.isLoaded()) {
            for (int i = 0; i < Palette.getColors().length; i++) {
                Color col = new Color(Palette.getColors()[i]);
                colorVector[i].setBackground(col);
                colorVector[i].setToolTipText("Red = " + col.getRed()
                        + ", Green = " + col.getGreen() + ", Blue = " + col.getBlue());
            }
        } else if (initialized) {
            for (JLabel label : colorVector) {
                label.setBackground(Color.BLACK);
                label.setToolTipText(null);
            }
        }
    }

    // Asynchronous reset  - returns the logic into initial state
    public void reset() {
        myFont = new Font("Courier New", Font.PLAIN, 12);
        myText = null;
        myInfo = "";
        myTest = "";
        spacing = 0;

        fontLoad = null;
        fontDer = null;

        imageRender = null;

        fgColor = Color.YELLOW;
        bgColor = Color.CYAN;

        outlineColor = Color.BLUE;
        shadowColor = Color.GRAY;

        charVector = null;

        fontFormat = "FON1";

        zoom = 100; // resetting zoom; damn forgot this
        Palette.reset();

        for (JLabel label : colorVector) {
            label.setBackground(Color.BLACK);
            label.setToolTipText("Red = " + Color.BLACK.getRed()
                    + ", Green = " + Color.BLACK.getGreen() + ", Blue = " + Color.BLACK.getBlue());
        }
    }

    //--------------------------------------------------------------------------
    // C - GETTERS AND SETTERS  
    //--------------------------------------------------------------------------
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public Font getMyFont() {
        return myFont;
    }

    public void setMyFont(Font myFont) {
        this.myFont = myFont;
    }

    public String getMyText() {
        return myText;
    }

    public void setMyText(String myText) {
        this.myText = myText;
    }

    public String getMyInfo() {
        return myInfo;
    }

    public void setMyInfo(String myInfo) {
        this.myInfo = myInfo;
    }

    public String getMyTest() {
        return myTest;
    }

    public void setMyTest(String myTest) {
        this.myTest = myTest;
    }

    public int getSpacing() {
        return spacing;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
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

    public DoomFont getFontLoad() {
        return fontLoad;
    }

    public void setFontLoad(DoomFont fontLoad) {
        this.fontLoad = fontLoad;
    }

    public DoomFont getFontDer() {
        return fontDer;
    }

    public void setFontDer(DoomFont fontDer) {
        this.fontDer = fontDer;
    }

    public BufferedImage getImageRender() {
        return imageRender;
    }

    public void setImageRender(BufferedImage imageRender) {
        this.imageRender = imageRender;
    }

    public Color getFgColor() {
        return fgColor;
    }

    public void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
    }

    public Color getBgColor() {
        return bgColor;
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
    }

    public Color getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(Color outlineColor) {
        this.outlineColor = outlineColor;
    }

    public JLabel[] getColorVector() {
        return colorVector;
    }

    public void setColorVector(JLabel[] colorVector) {
        this.colorVector = colorVector;
    }

    public JPanel getColorPanel() {
        return colorPanel;
    }

    public void setColorPanel(JPanel colorPanel) {
        this.colorPanel = colorPanel;
    }

    public DoomFontChar[] getCharVector() {
        return charVector;
    }

    public void setCharVector(DoomFontChar[] charVector) {
        this.charVector = charVector;
    }

    public String getFontFormat() {
        return fontFormat;
    }

    public void setFontFormat(String fontFormat) {
        this.fontFormat = fontFormat;
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
    }

}
