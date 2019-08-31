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

/**
 *
 * @author Coa
 */
public class DoomFontChar {

    // Which character it is 
    protected char c;
    // Character image width and height
    protected int w, h;
    // Character image as raw pixels in row-major format
    // Data says on which pixel is which entry in the palette of the font
    protected byte[] data;
    // Used for remembering the offset of the char
    // When rendering the chars from left to right 
    // (or from up to down for Console font)
    // into single image this offset increases
    protected int offset;

    //--------------------------------------------------------------------------
    // A - CONSTRUCTOR 
    //--------------------------------------------------------------------------
    public DoomFontChar(char c, int w, int h) {
        this.c = c;
        this.w = w;
        this.h = h;
        this.data = new byte[w * h];
    }

    //--------------------------------------------------------------------------
    // B - GETTERS AND SETTERS (TRIVIAL)
    //--------------------------------------------------------------------------
    public char getC() {
        return c;
    }

    public void setC(char c) {
        this.c = c;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}
