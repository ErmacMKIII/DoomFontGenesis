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

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BMFChar extends DoomFontChar {

    // Character x and y offset relative to the corresponding cursor
    private int relx, rely;
    // Horizontal cursor shift after drawing the character 
    // (used instead of width) in addition to the global font value of space 
    // added after each character
    private int shift;

    //--------------------------------------------------------------------------
    // A - CONSTRUCTOR 
    //--------------------------------------------------------------------------
    public BMFChar(char c, int w, int h) {
        super(c, w, h);
    }

    //--------------------------------------------------------------------------
    // B - GETTERS AND SETTERS (TRIVIAL)
    //--------------------------------------------------------------------------    
    public int getRelx() {
        return relx;
    }

    public void setRelx(int relx) {
        this.relx = relx;
    }

    public int getRely() {
        return rely;
    }

    public void setRely(int rely) {
        this.rely = rely;
    }

    public int getShift() {
        return shift;
    }

    public void setShift(int shift) {
        this.shift = shift;
    }

}
