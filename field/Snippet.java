package field;

import java.awt.*;

public class Snippet {
    public final Point position;
    public int value=0;

    public Snippet(Point position){
        this.position=position;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
