package field;

import java.awt.*;
import java.util.ArrayList;

public class Enemy {
    Point currentPosition;
    Point previousPosition;
    ArrayList<Point> dangerousFields;
    public Enemy(Point currentPosition){
        this.currentPosition=currentPosition;
    }
}
