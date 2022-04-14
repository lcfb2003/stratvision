package br.edu.utfpr.stratvision.patlan;

import java.awt.Color;

/**
 *
 * @author Luis C. F. Bueno 
 * Defines a enumeration of semantic relationships between chess pieces.
 * Also allows a translation to natural language 
 */
    public enum PatternActionType {
        ATTACKS("attacks", 0, "=>", false, Color.RED, true),
        DEFENDS("defends", 1, "=<", false, Color.GREEN, true),
        BLOCKS("blocks", 2, "||", false, Color.GRAY, true),
        MOVES("moves to", 3, ">>", false, Color.BLUE, true),
        RIGHTSIDE("right",4, "|>", true, Color.YELLOW, true),
        LEFTSIDE("left", 5, "<|", true, Color.YELLOW, true),
        UPPERSIDE("above", 6, "|^", true, Color.YELLOW, true),
        BOTTOMSIDE("under", 7, "|v", true, Color.YELLOW, true),
        SE("southeast",8,"\\>", true, Color.YELLOW, true),
        NW("northwest",9,"<\\", true, Color.YELLOW, true),
        NE("northeast",10,"/>", true, Color.YELLOW, true),
        SW("southwest",11,"</", true, Color.YELLOW, true),
        IATTACK("attacks [X] through [Y]",12,"->",false,Color.RED, false),
        IDEFENSE("defends [X] through [Y]",13,"-<",false,Color.GREEN, false),
        PATTACK("can threat [X] through [Y]",14,":>",false,Color.RED, false),
        PDEFENSE("can protect [X] through [Y]",15,":<",false,Color.GREEN, false),
        CHECKMATE("checkmates through [Y]",16,"#>",false,Color.RED, true),
        DECLARATION("Actor declaration",17,"  ", false, Color.WHITE,true);
        
        private final String description;
        private final int id;
        private final String operator;
        private final boolean isPositional; //is a positional operator
        private final Color color;
        private final boolean isDirect; // is a direct relationship

        private PatternActionType(String descr, int cod, String ope, boolean pos, Color lineColor, boolean dir) {
            description = descr;
            id = cod;
            operator = ope;
            isPositional = pos;
            color = lineColor;
            isDirect = dir; 
        }

        public int id() {
            return id;
        }
        
        public String operator() {
            return operator;
        }

        @Override
        public String toString() {
            return description + "(" + operator + ")";
        }
        
        public boolean positional() {
            return isPositional;
        }
        
        public Color getColor() {
            return color;
        }
        
        public boolean getDirect() {
            return isDirect;
        }
        
        public String toEnglish(boolean notRelation) {
            return " " + (notRelation?"NOT ":"") + description + " ";
        }
    }
