package br.edu.utfpr.stratvision.patlan;

/**
 *
 * @author Luis C. F. Bueno
 */
public class Bits {
    public long WP = 0L, WN = 0L, WB = 0L, WR = 0L, WQ = 0L, WK = 0L, 
                BP = 0L, BN = 0L, BB = 0L, BR = 0L, BQ = 0L, BK = 0L,
                EP = 0L;
    public long MP = 0L, MN = 0L, MB = 0L, MR = 0L, MQ = 0L, MK = 0L, // M - pieces of the player conducting the search
                SP = 0L, SN = 0L, SB = 0L, SR = 0L, SQ = 0L, SK = 0L; // S - pieces of the opponent player
    public boolean CWK=true,CWQ=true,CBK=true,CBQ=true,WhiteToMove=true;

    public long BLACK_PIECES;
    public long WHITE_PIECES;
    public long NOT_MY_PIECES;
    public long MY_PIECES;
    public long OCCUPIED;
    public long EMPTY;
    public long YOUR_PIECES;
    
    public long getOccupied() {
         return WP|WN|WB|WR|WQ|WK|BP|BN|BB|BR|BQ|BK;
    }
    
    public Bits Clone() {
        Bits clone = new Bits();
        clone.BB = this.BB;
        clone.BK = this.BK;
        clone.BN = this.BN;
        clone.BP = this.BP;
        clone.BQ = this.BQ;
        clone.BR = this.BR;
        clone.CBK = this.CBK;
        clone.CBQ = this.CBQ;
        clone.CWK = this.CWK;
        clone.CWQ = this.CWQ;
        clone.EP = this.EP;
        clone.WB = this.WB;
        clone.WK = this.WK;
        clone.WN = this.WN;
        clone.WP = this.WP;
        clone.WQ = this.WQ;
        clone.WR = this.WR;
        clone.MB = this.MB;
        clone.MK = this.MK;
        clone.MN = this.MN;
        clone.MP = this.MP;
        clone.MQ = this.MQ;
        clone.MR = this.MR;
        clone.SB = this.SB;
        clone.SK = this.SK;
        clone.SN = this.SN;
        clone.SP = this.SP;
        clone.SQ = this.SQ;
        clone.SR = this.SR;
        clone.WhiteToMove = this.WhiteToMove;
        clone.EMPTY = this.EMPTY;
        clone.NOT_MY_PIECES = this.NOT_MY_PIECES;
        clone.MY_PIECES = this.MY_PIECES;
        clone.OCCUPIED = this.OCCUPIED;
        clone.YOUR_PIECES = this.YOUR_PIECES;
        clone.BLACK_PIECES = this.BLACK_PIECES;
        clone.WHITE_PIECES = this.WHITE_PIECES;
        return clone;
    }
}
