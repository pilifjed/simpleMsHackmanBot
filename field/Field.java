/*
 * Copyright 2017 riddles.io (developers@riddles.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package field;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import move.MoveType;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
 * field.Field
 *
 * Stores all information about the playing field and
 * contains methods to perform calculations about the field
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public class Field {

    protected final String EMTPY_FIELD = ".";
    protected final String BLOCKED_FIELD = "x";

    private String myId;
    private String opponentId;
    private int width;
    private int height;

    private String[][] field;
    private Point myPosition;
    private Point opponentPosition;
    private ArrayList<Point> enemyPositions;
    private ArrayList<Point> prevEnemyPositions;
    private ArrayList<Snippet> snippets;
    private ArrayList<Point> bombPositions;
    private ArrayList<Point> tickingBombPositions;
    public MoveType nextMove;

    private ArrayList<Point> unsafePositions;
    private Snippet forbiddenSnippet;
    private int[][] myDistances;
    private int[][] opponentDistances;

    public Field() {
        this.enemyPositions = new ArrayList<>();
        this.prevEnemyPositions = new ArrayList<>();
        this.unsafePositions = new ArrayList<>();
        //this.snippetPositions = new ArrayList<>();
        this.snippets = new ArrayList<>();
        this.bombPositions = new ArrayList<>();
        this.tickingBombPositions = new ArrayList<>();
    }


    /**
     * Initializes field
     * @throws Exception: exception
     */
    public void initField() throws Exception {
        try {
            this.field = new String[this.width][this.height];
        } catch (Exception e) {
            throw new Exception("Error: trying to initialize field while field "
                    + "settings have not been parsed yet.");
        }
        clearField();
    }

    /**
     * Clears the field
     */
    public void clearField() {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.field[x][y] = "";
            }
        }
        this.prevEnemyPositions=this.enemyPositions;
        this.myPosition = null;
        this.opponentPosition = null;
        this.forbiddenSnippet = null;
        this.enemyPositions.clear();
        this.unsafePositions.clear();
        //this.snippetPositions.clear();
        this.snippets.clear();
        this.bombPositions.clear();
        this.tickingBombPositions.clear();
    }

    /**
     * Parses input string from the engine and stores it in
     * this.field. Also stores several interesting points.
     * @param input String input from the engine
     */
    public void parseFromString(String input) {
        clearField();

        String[] cells = input.split(",");
        int x = 0;
        int y = 0;

        for (String cellString : cells) {
            this.field[x][y] = cellString;

            for (String cellPart : cellString.split(";")) {
                switch (cellPart.charAt(0)) {
                    case 'P':
                        parsePlayerCell(cellPart.charAt(1), x, y);
                        break;
                    case 'e':
                        // TODO: store spawn points
                        break;
                    case 'E':
                        parseEnemyCell(cellPart.charAt(1), x, y);
                        break;
                    case 'B':
                        parseBombCell(cellPart, x, y);
                        break;
                    case 'C':
                        parseSnippetCell(x, y);
                        break;
                }
            }

            if (++x == this.width) {
                x = 0;
                y++;
            }
        }
        setUnsafePositions();
    }

    /**
     * Stores the position of one of the players, given by the id
     * @param id Player ID
     * @param x X-position
     * @param y Y-position
     */
    private void parsePlayerCell(char id, int x, int y) {
        if (id == this.myId.charAt(0)) {
            this.myPosition = new Point(x, y);
        } else if (id == this.opponentId.charAt(0)) {
            this.opponentPosition = new Point(x, y);
        }
    }

    /**
     * Stores the position of an enemy. The type of enemy AI
     * is also given, but not stored in the starterbot.
     * @param type Type of enemy AI
     * @param x X-position
     * @param y Y-position
     */
    private void parseEnemyCell(char type, int x, int y) {
        this.enemyPositions.add(new Point(x, y));
    }

    private void setUnsafePositions(){
        for (Point enemy: enemyPositions) {
            if(abs(enemy.x-myPosition.x)+abs(enemy.y-myPosition.y)<=4)
                this.unsafePositions.add(new Point(enemy.x+1,enemy.y));
                this.unsafePositions.add(new Point(enemy.x-1,enemy.y));
                this.unsafePositions.add(new Point(enemy.x,enemy.y+1));
                this.unsafePositions.add(new Point(enemy.x,enemy.y-1));
        }
        //this.unsafePositions.removeAll(this.prevEnemyPositions);
    }

    /**
     * Stores the position of a bomb that can be collected or is
     * about to explode. The amount of ticks is not stored
     * in this starterbot.
     * @param cell The string that represents a bomb, if only 1 letter it
     *             can be collected, otherwise it will contain a number
     *             2 - 5, that means it's ticking to explode in that amount
     *             of rounds.
     * @param x X-position
     * @param y Y-position
     */
    private void parseBombCell(String cell, int x, int y) {
        if (cell.length() <= 1) {
            this.bombPositions.add(new Point(x, y));
        } else {
            this.tickingBombPositions.add(new Point(x, y));
        }
    }

    /**
     * Stores the position of a snippet
     * @param x X-position
     * @param y Y-position
     */
    private void parseSnippetCell(int x, int y) {
        this.snippets.add(new Snippet(new Point(x, y)));
    }

    private boolean closerToMe(Snippet s){
        return this.myDistances[s.position.x][s.position.y] <= this.opponentDistances[s.position.x][s.position.y];
    }

    private boolean isOneSnippetOnField(){
        return this.snippets.size()==1;
    }

    public MoveType getMyMoveType(){
        setDistances(); //IMPORTANT: You need to do this first! Otherwise it does'nt make sense.
        Snippet myChosen = closestSnippet(this.myDistances);
        Snippet opponentChosen = closestSnippet(this.opponentDistances);
        if(myChosen!=opponentChosen) {
            erlog();
            return towardsSnippet(myChosen);
        }
        else{
            if(!closerToMe(myChosen) && !isOneSnippetOnField()){
                forbiddenSnippet=myChosen;
                this.myDistances = BFS(this.myPosition.x,this.myPosition.y);
                erlog();
                return towardsSnippet(closestSnippet(this.myDistances));
            }
            else{
                erlog();
                return towardsSnippet(myChosen);
            }
        }
    }

    private void erlog(){
        for(int y=0;y<this.height;y++){
            for(int x=0;x<this.width;x++){
                if(myDistances[x][y]==0)
                    System.err.print("\t");
                else
                    System.err.print(myDistances[x][y]+"\t");
            }
            System.err.print("\n");
        }
        System.err.print("("+myPosition.x+", "+myPosition.y +"); ");
        System.err.print("[");
        for (Point pos: enemyPositions) {
            System.err.print("("+pos.x+ ","+pos.y+")");
        }
        System.err.print("]\n");
    }

    private void setDistances(){
        this.myDistances = BFS(this.myPosition.x,this.myPosition.y);
        this.opponentDistances = BFS(this.opponentPosition.x,this.opponentPosition.y);
    }

    private Snippet closestSnippet(int[][] distances){
        int minVal = 100;
        Snippet p = null;
        if(this.snippets.isEmpty()) {
            p=new Snippet (new Point(0,7));
        }
        else {
            for (Snippet s : this.snippets) {
                s.value = distances[s.position.x][s.position.y];
                for (Snippet ns : this.snippets) {
                    int[] minDist=new int[3];
                    minDist[0]=abs(s.position.x - ns.position.x) + abs(s.position.y - ns.position.y);
                    minDist[1]=abs(-1 - s.position.x) + abs(7 - s.position.y) +abs(ns.position.x - 18) + abs(ns.position.y - 7);
                    minDist[2]=abs(19 - s.position.x) + abs(7 - s.position.y) +abs(s.position.x - 0) + abs(s.position.y - 7);
                    minDist[0]=min(minDist[2],min(minDist[0],minDist[1]));
                    if (minDist[0] != 0) {
                        s.value -= (2 / minDist[0]);
                    }
                }
            }
            for (Snippet s : this.snippets) {
                if (s.value < minVal && s.value != 0 &&!s.equals(forbiddenSnippet)) {
                    minVal = s.value;
                    p = s;
                }
            }
        }
        if(p == null)
            return new Snippet (new Point(0,7));
        return p;
    }


    private MoveType towardsSnippet(Snippet s){
        Point p = s.position;
        System.err.print("\nTarget snippet: ("+p.x+","+p.y+")\n");
        int prevX = p.x; int prevY=p.y;
        while(myDistances[prevX][prevY]>2){
            if((prevY-1>=0 && myDistances[prevX][prevY-1] !=0) && myDistances[prevX][prevY-1]<myDistances[prevX][prevY]) {
                if (myDistances[prevX][prevY] == 4)
                    this.nextMove = MoveType.DOWN;
                prevY--;
            }
            else if(prevX+1<this.width && myDistances[prevX+1][prevY] !=0 && myDistances[prevX+1][prevY]<myDistances[prevX][prevY]) {
                if (myDistances[prevX][prevY] == 4)
                    this.nextMove = MoveType.LEFT;
                prevX++;
            }
            else if(prevY+1<this.height && myDistances[prevX][prevY+1] !=0 && myDistances[prevX][prevY+1]<myDistances[prevX][prevY]) {
                if (myDistances[prevX][prevY] == 4)
                    this.nextMove = MoveType.UP;
                prevY++;
            }
            else if(prevX-1>=0 && myDistances[prevX-1][prevY] !=0 && myDistances[prevX-1][prevY]<myDistances[prevX][prevY]) {
                if (myDistances[prevX][prevY] == 4)
                    this.nextMove = MoveType.RIGHT;
                prevX--;
            }
            else if(prevX==18 && prevY==7 && myDistances[0][7]<myDistances[prevX][prevY]) {
                if (myDistances[prevX][prevY] == 4)
                    this.nextMove = MoveType.RIGHT;
                prevX = 0;
            }
            else if(prevX==0 && prevY==7 && myDistances[18][7]<myDistances[prevX][prevY]) {
                if (myDistances[prevX][prevY] == 4)
                    this.nextMove = MoveType.LEFT;
                prevX = 18;
            }

        }
        if((prevX - this.myPosition.x)==0) {
            if ((prevY - this.myPosition.y)>0)
                return MoveType.DOWN;
            else if ((prevY - this.myPosition.y)<0)
                return MoveType.UP;
        }
        if((prevX - this.myPosition.x)==-18)
            return MoveType.RIGHT;
        if((prevX - this.myPosition.x)==18)
            return MoveType.LEFT;
        else if((prevX - this.myPosition.x)>0)
            return MoveType.RIGHT;
        else if((prevX - this.myPosition.x)<0)
            return MoveType.LEFT;
        return MoveType.PASS;
    }


    public int [][] BFS(int x, int y){
        int [][] bfs = new int[this.width][this.height];
        Queue<queueElement> queue = new LinkedList<>();
        queue.offer(new queueElement(1,x,y));
        queueElement curr;
        while(!queue.isEmpty()){
            curr = queue.poll();
            if(bfs[curr.x][curr.y]==0) {
                bfs[curr.x][curr.y] = curr.i;
                ArrayList<MoveType> moves = getPositionMoveTypes(curr.x, curr.y);
                for (MoveType move : moves) {
                    switch(move){
                        case UP:
                            queue.add(new queueElement(curr.i+1,curr.x,curr.y-1));
                            break;
                        case DOWN:
                            queue.add(new queueElement(curr.i+1,curr.x,curr.y+1));
                            break;
                        case LEFT:
                            if(curr.x==0 && curr.y ==7)
                                queue.add(new queueElement(curr.i+1,18,7));
                            else
                                queue.add(new queueElement(curr.i+1,curr.x-1,curr.y));
                            break;
                        case RIGHT:
                            if(curr.x==18 && curr.y ==7)
                                queue.add(new queueElement(curr.i+1,0,7));
                            else
                                queue.add(new queueElement(curr.i+1,curr.x+1,curr.y));
                            break;
                    }
                }
            }
        }
        return bfs;
    }

    private ArrayList<MoveType> getPositionMoveTypes(int x, int y) {
        ArrayList<MoveType> validMoveTypes = new ArrayList<>();
        int myX = x;
        int myY = y;

        Point up = new Point(myX, myY - 1);
        Point down = new Point(myX, myY + 1);
        Point left = new Point(myX - 1, myY);
        Point right = new Point(myX + 1, myY);

        if (isPointValid(up) && isPointSafe(up)) validMoveTypes.add(MoveType.UP);
        if (isPointValid(down) && isPointSafe(down)) validMoveTypes.add(MoveType.DOWN);
        if (isPointValid(left) && isPointSafe(left)) validMoveTypes.add(MoveType.LEFT);
        if (isPointValid(right) && isPointSafe(right)) validMoveTypes.add(MoveType.RIGHT);
        if(myX == 0 && myY == 7) validMoveTypes.add(MoveType.LEFT);
        if(myX == 18 && myY == 7) validMoveTypes.add(MoveType.RIGHT);
        return validMoveTypes;
    }

    /**
     * Return a list of valid moves for my bot, i.e. moves does not bring
     * player outside the field or inside a wall
     * @return A list of valid moves
     */

    public ArrayList<MoveType> getValidMoveTypes() {
        return getPositionMoveTypes(this.myPosition.x,this.myPosition.y);
    }

    /**
     * Returns whether a point on the field is valid to stand on.
     * @param point Point to test
     * @return True if point is valid to stand on, false otherwise
     */
    public boolean isPointValid(Point point) {
        int x = point.x;
        int y = point.y;

        return x >= 0 && x < this.width && y >= 0 && y < this.height &&
                !this.field[x][y].contains(BLOCKED_FIELD);
    }

    public boolean isPointSafe(Point point) {
        if(this.forbiddenSnippet != null)
            return !this.enemyPositions.contains(point) && !this.forbiddenSnippet.position.equals(point) && !this.unsafePositions.contains(point);
        return !this.enemyPositions.contains(point) && !this.unsafePositions.contains(point) ;
    }


    public void setMyId(int id) {
        this.myId = id + "";
    }

    public void setOpponentId(int id) {
        this.opponentId = id + "";
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Point getMyPosition() {
        return this.myPosition;
    }

    public Point getOpponentPosition() {
        return this.opponentPosition;
    }

    public ArrayList<Point> getEnemyPositions() {
        return this.enemyPositions;
    }

    public ArrayList<Snippet> getsnippets() {
        return this.snippets;
    }

    public ArrayList<Point> getBombPositions() {
        return this.bombPositions;
    }

    public ArrayList<Point> getTickingBombPositions() {
        return this.tickingBombPositions;
    }
}
