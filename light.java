import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;


// Represents a tile on the game board
class GamePiece {

  // Position of the GamePiece on the board
  int row;
  int col;

  // Connection status to neighboring tiles
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;

  // Whether the power station is on this piece
  boolean powerStation;

  // Whether this piece is powered
  boolean powered;

  // Constructor
  GamePiece(int col, int row, boolean left, boolean right, boolean top, boolean bottom, boolean powerStation) {
    this.col = col;
    this.row = row;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = false;
  }


  public void rotate() {
    boolean right = this.right;
    this.right = this.top;
    this.top = this.left;
    this.left = this.bottom;
    this.bottom = right;
  }
  
  
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);
 
    if (this.top) image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    if (this.right) image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    if (this.bottom) image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    if (this.left) image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    if (hasPowerStation) {
      image = new OverlayImage(
                  new OverlayImage(
                      new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
                      new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
                  image);
    }
    return image;
  }
  
}


//Represents an edge connecting two vertices
class Edge {

// Vertices this edge connects
  GamePiece from;
  GamePiece to;



// Constructor
  Edge(GamePiece from, GamePiece to) {
    this.from = from;
    this.to = to;
  }
  

}

// Represents the LightEmAll game
class LightEmAll extends World {

  // Constants for the game
  int tileSize = 40;
  int boardWidth = 9;
  int boardHeight = 9;
  int powerRadius = 3;

  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  Random seed;
  ArrayList<Edge> edges;

  // Constructor to Play
  LightEmAll() {
    this.board = this.generateBoard();
    this.nodes = this.generateNodes();
    this.mst = new ArrayList<Edge>();
    this.powerRow = this.boardHeight / 2;
    this.powerCol = this.boardWidth / 2;
    this.powerRadius = 0;
    this.seed = new Random();
    this.edges = new ArrayList<Edge>();
    
    this.addNeighbors();
    


    this.krusk();
    this.genKruskBoard();
    this.scrambleBoard();
    this.powerUp(this.board.get(powerCol).get(powerRow));
    
  }
    
    // Constructor to Test
    
    LightEmAll(Random seed) {
      this.board = this.generateBoard();
      this.nodes = this.generateNodes();
      this.mst = new ArrayList<Edge>();
      this.powerRow = this.boardHeight / 2;
      this.powerCol = this.boardWidth / 2;
      this.powerRadius = 0;
      this.seed = seed;
      

      this.powerUp(this.board.get(powerCol).get(powerRow));
    

  }

  // Generates the initial game board
  ArrayList<ArrayList<GamePiece>> generateBoard() {
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<>();
    for (int col = 0; col < this.boardWidth; col++) {
      ArrayList<GamePiece> column = new ArrayList<>();
      for (int row = 0; row < this.boardHeight; row++) {        
        if (row == 0) {
          column.add(new GamePiece(col, row, false, false, false, true, false));
        }
        
        else if (row == this.boardHeight - 1) {
          column.add(new GamePiece(col, row, false, false, true, false, false));
        } 
        else if(col == 0) {
          
        if(row != this.boardHeight / 2) {  
        column.add(new GamePiece(col, row, false, false, true, true, false));
      }
        else {
          column.add(new GamePiece(col, row, false, true, true, true, false));
        }
      }
        else if(col == this.boardWidth - 1) {
          
        if(row != this.boardHeight / 2) {  
        column.add(new GamePiece(col, row, false, false, true, true, false));
      }
        else {
          column.add(new GamePiece(col, row, true, false, true, true, false));
        }
      }
        else {
          
        if(row != this.boardHeight / 2) {  
        column.add(new GamePiece(col, row, false, false, true, true, false));
      }
        else {
          if(col == this.boardWidth / 2) {
            column.add(new GamePiece(col, row, true, true, true, true, true));
          }
          else {
          column.add(new GamePiece(col, row, true, true, true, true, false));
        }
        }
      }
      }
      board.add(column);
    }
   
    return board;
  }

  // Generates a list of all nodes
  ArrayList<GamePiece> generateNodes() {
    ArrayList<GamePiece> nodes = new ArrayList<>();
    for (ArrayList<GamePiece> column : this.board) {
      for(GamePiece node : column) {
        nodes.add(node);
      }
    }
    return nodes;
  }

  
  // draws the game
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(this.boardWidth * this.tileSize, this.boardHeight * this.tileSize);

    for (int i = 0; i < nodes.size(); i++) {
      Color color;
      int x = (nodes.get(i).col * this.tileSize) + (this.tileSize / 2);
      int y = (nodes.get(i).row * this.tileSize) + (this.tileSize / 2);
      if (nodes.get(i).powered) {
        color = Color.YELLOW;
      }
      else {
        color = Color.LIGHT_GRAY;
      }
      WorldImage drawnTile = nodes.get(i).tileImage(this.tileSize, 5, color,
          nodes.get(i).powerStation);
      scene.placeImageXY(drawnTile, x, y);
    }  
    return scene;
  }

  // Handles key events (moves power station)
  public void onKeyEvent(String key) {
    GamePiece curr = this.board.get(powerCol).get(powerRow);
    if (key.equals("up") && this.idxCheck(powerCol, powerRow - 1)) {
      GamePiece next = this.board.get(curr.col).get(curr.row - 1);
        if (next.bottom && curr.top) {
          this.powerRow -= 1;
          curr.powerStation = false;
          next.powerStation = true;
        }
    }
    else if (key.equals("down") && this.idxCheck(powerCol, powerRow + 1)) {
      GamePiece next = this.board.get(curr.col).get(curr.row + 1);
        if (next.top && curr.bottom) {
          this.powerRow += 1;
          curr.powerStation = false;
          next.powerStation = true;
        }
    }
    else if (key.equals("right") && this.idxCheck(powerCol + 1, powerRow)) {
      GamePiece next = this.board.get(curr.col + 1).get(curr.row);
        if (next.left && curr.right) {
          this.powerCol += 1;
          curr.powerStation = false;
          next.powerStation = true;
        }
    }
    else if (key.equals("left") && this.idxCheck(powerCol - 1, powerRow)) {
      GamePiece next = this.board.get(curr.col - 1).get(curr.row);
        if (next.right && curr.left) {
          this.powerCol -= 1;
          curr.powerStation = false;
          next.powerStation = true;
        }
    }
  }
  
  

  // handles clicks
  public void onMousePressed(Posn pos, String buttonName) {
    for (int i = 0; i < this.board.size(); i++) {
      if (i * this.tileSize <= pos.x && pos.x < (i + 1) * this.tileSize) {
        for (int j = 0; j < this.board.get(i).size(); j++) {
          if (j * this.tileSize <= pos.y && pos.y < (j + 1) * this.tileSize) {
            GamePiece currPiece = this.board.get(i).get(j);
            currPiece.rotate();
            this.powerUp(this.board.get(powerCol).get(powerRow));
          }
        }
      }
    }
      
          
  }
 
   
  public void powerUp(GamePiece from) {
    for (GamePiece piece : this.nodes) { 
      piece.powered = false;
      from.powered = true;
    }
    ArrayList<GamePiece> workList = new ArrayList<GamePiece>();
    ArrayList<GamePiece> visited = new ArrayList<GamePiece>();
    workList.add(from);
    while (!workList.isEmpty()) {
      GamePiece next = workList.remove(0);
      if (!visited.contains(next)) {
        if(this.idxCheck(next.col, next.row + 1)
            && next.bottom && this.board.get(next.col).get(next.row+1).top) {
          workList.add(this.board.get(next.col).get(next.row+1));
          this.board.get(next.col).get(next.row+1).powered = true;
        }
        if(this.idxCheck(next.col, next.row - 1)
            && next.top && this.board.get(next.col).get(next.row-1).bottom) {
          workList.add(this.board.get(next.col).get(next.row-1));
          this.board.get(next.col).get(next.row-1).powered = true;
        }
        if(this.idxCheck(next.col + 1, next.row)
            && next.right && this.board.get(next.col+1).get(next.row).left) {
          workList.add(this.board.get(next.col+1).get(next.row));
          this.board.get(next.col+1).get(next.row).powered = true;
        }
        if(this.idxCheck(next.col - 1, next.row)
            && next.left && this.board.get(next.col-1).get(next.row).right) {
          workList.add(this.board.get(next.col-1).get(next.row));
          this.board.get(next.col-1).get(next.row).powered = true;
        }
        visited.add(next);
      }
    }
  }
  
  public boolean idxCheck(int posx, int posy) {
    return posy >= 0 
        && posy < this.boardHeight
        && posx >= 0 && posx < this.boardWidth;
    
  }
  
  public void scrambleBoard() {
    for(int i = 0; i < this.board.size(); i++) {
      for(int k = 0; k < this.board.get(i).size(); k++ ) {
       GamePiece curr = this.board.get(i).get(k);
       for(int j = 0; j < seed.nextInt(4); j++) {
         curr.rotate();
       }
      }
    }
  }
  

  // Checks if the game is won
  public WorldEnd worldEnds() {
   for (GamePiece node : nodes) {
      if (!node.powered) {
        return new WorldEnd(false, this.makeScene());
      }
    }
    return new WorldEnd(true, this.makeAFinalScene());
  }
  
  public WorldScene makeAFinalScene() {
    WorldScene scene = this.makeScene();
        scene.placeImageXY(new TextImage("You Win", 25, Color.GREEN),
            this.boardWidth / 2 * this.tileSize, this.boardHeight / 2 * this.tileSize);    
        
        return scene;
    }
  
  void addNeighbors() {
    ArrayList<Edge> allEdges = new ArrayList<Edge>();
    
    for (int col = 0; col < this.board.size(); col++) {
      for (int row = 0; row < this.board.get(col).size(); row++) {
       
        GamePiece current =  this.board.get(col).get(row);
        if (row < this.board.get(col).size() - 1) {      
          allEdges.add(new Edge(current, this.board.get(col).get(row + 1)));

        }
         
        
        if (col < this.board.size() - 1) {          
          allEdges.add(new Edge(current, this.board.get(col + 1).get(row)));
        }
            
      }
    }
    
    Collections.shuffle(allEdges);
    
    this.edges = allEdges;
  }
  
  
  public void krusk() {
   UnionFind og = new UnionFind(this.nodes);
    for(int i = 0; i < this.edges.size(); i++) {
     Edge current = this.edges.get(i);
      if(og.find(current.to) != og.find(current.from)) {
       this.mst.add(current);
       og.union(current.to, current.from);
     }

   }
  } 
  
  public void genKruskBoard() {
    boolean right = false;
    boolean left = false;
    boolean top = false;
    boolean bot = false;
    
    for(int i = 0; i < this.mst.size(); i++) {
      Edge curr = this.mst.get(i);
      
      if (curr.from.col + 1 == curr.to.col) {
        right = true;
        
      }
      
      if (curr.from.col - 1 == curr.to.col) {
        left = true;
        
      }
      
      if (curr.from.row + 1 == curr.to.row) {
        bot = true;
        
      }
      
      if (curr.from.row - 1 == curr.to.row) {
        top = true;
        
      }
      
      if (right) {
        curr.from.right = true;
      }
      
      if (left) {
        curr.from.left = true;
      }
      
      if (top) {
        curr.from.top = true;
      }
      
      if (bot) {
        curr.from.bottom = true;
      }
    }  
    
  }
  
}



class UnionFind {
  HashMap<GamePiece, GamePiece> map;
  
  UnionFind(ArrayList<GamePiece> nodes) {
     this.map = new HashMap<GamePiece, GamePiece>();
     
     for(int i = 0; i < nodes.size(); i++) {
       map.put(nodes.get(i), nodes.get(i));
     }
     
     }
  
  GamePiece find(GamePiece g) {
    GamePiece curr = g;
    while (curr != map.get(curr)) {
     curr = map.get(curr);
    }
    
    return curr;
  }
  
  void union(GamePiece g1, GamePiece g2) {
    map.replace(this.find(g1), g2);
    
  }
}



class ExamplesLight {
  ExamplesLight() {}
  
  // to play
  LightEmAll w1;
  // to test
  LightEmAll w2;
  
  GamePiece cp;
  GamePiece lr;
  GamePiece tl;
  GamePiece tbl;
  
  
  
  // tiles for world 2 testing
  
  GamePiece g1c;
  GamePiece g1tb;
  GamePiece g1b;
  GamePiece g1lr;
  GamePiece g1;
  
  
  
  
  
  
  void init() {
    
    w1 = new LightEmAll();
    w2 = new LightEmAll(new Random(5));
    cp = new GamePiece(0, 0, true, true, true, true, false);
    lr = new GamePiece(50, 50, true, true, false, false, false);
    tl = new GamePiece(100, 100, true, false, true, false, false);
    tbl = new GamePiece(100, 100, true, false, true, true, false);
  }
  
  
  void testBigBang2(Tester t) {
    this.init();
    int worldWidth = 1000;
    int worldHeight = 1000;
    double tickRate = 0.1;
    this.w1.bigBang(worldWidth, worldHeight, tickRate);
  }
  
  
  void testIdxCheck(Tester t) {
    this.init();
    t.checkExpect(this.w1.idxCheck(9, 0), false);
    t.checkExpect(this.w1.idxCheck(0, 9), false);
    t.checkExpect(this.w1.idxCheck(3, 3), true);
    t.checkExpect(this.w1.idxCheck(3, 4), true);
    t.checkExpect(this.w1.idxCheck(-2, 0), false);
    t.checkExpect(this.w1.idxCheck(0, -1), false);
  }
  
  void testRotation(Tester t) {
    this.init();
    
    // check rotating crosspipe
    t.checkExpect(cp.left && cp.right && cp.top && cp.bottom, true);
    
    cp.rotate();
    
    t.checkExpect(cp.left && cp.right && cp.top && cp.bottom, true);
    
    // checking rotating a left right pipe
    t.checkExpect(lr.left && lr.right, true);
    
    lr.rotate();
    t.checkExpect(lr.top && lr.bottom, true);
    
    lr.rotate();
    t.checkExpect(lr.left && lr.right, true);
    
    // checking rotating a corner pipe
    
    t.checkExpect(tl.left && tl.top, true);
    
    tl.rotate();
    t.checkExpect(tl.right&& tl.top, true);
    
    tl.rotate();
    t.checkExpect(tl.bottom && tl.right, true);
    
    tl.rotate();
    t.checkExpect(tl.left && tl.bottom, true);
    
    tl.rotate();
    t.checkExpect(tl.left && tl.top, true);
    
    // checking rotating a 3 way pipe
    t.checkExpect(tbl.left && tbl.bottom && tbl.top, true);
    
    tbl.rotate();
    t.checkExpect(tbl.left && tbl.right && tbl.top, true);
    
    tbl.rotate();
    t.checkExpect(tbl.bottom && tbl.right && tbl.top, true);
    
    tbl.rotate();
    t.checkExpect(tbl.bottom && tbl.right && tbl.left, true);
    
    tbl.rotate();
    t.checkExpect(tbl.left && tbl.top && tbl.bottom, true);
    
    
  }
  
  
  void testMakeScene(Tester t) {
    this.init();
    WorldScene ws = new WorldScene(w2.boardWidth, w2.boardHeight);
    
    ws.placeImageXY(null, 0, 0);
  }
  
  void testScramble(Tester t) {
    this.init();
    
    GamePiece p1 = w2.board.get(0).get(0);
    GamePiece p2 = w2.board.get(0).get(2);
    GamePiece p3 = w2.board.get(0).get(4);
    GamePiece p4 = w2.board.get(2).get(4);
    
    t.checkExpect(p1.bottom, true);
    t.checkExpect(p2.bottom && p2.top, true);
    t.checkExpect(p3.bottom && p3.right && p3.top, true);
    t.checkExpect(p4.bottom && p4.right && p4.top && p4.left, true);
    
    w2.scrambleBoard();

    
    t.checkExpect(p1.bottom, false);
    t.checkExpect(p1.left , true);
    
    t.checkExpect(p2.bottom && p2.top, false);
    t.checkExpect(p2.left && p2.right, true);
    
    t.checkExpect(p3.bottom && p3.right && p3.top, false);
    t.checkExpect(p3.bottom && p3.right && p3.left, true);
    
    t.checkExpect(p4.bottom && p4.right && p4.top && p4.left, true);
    
    
  }
  
  void testPowerUp(Tester t) {
    this.init();
    this.w2.powerRow = 0;
    this.w2.powerCol = 0;
    this.w2.powerRadius = 1;
    this.w2.powerUp(this.w2.board.get(0).get(0));

    GamePiece upLeft = this.w2.board.get(0).get(1);
    GamePiece top = this.w2.board.get(1).get(0);
    GamePiece left = this.w2.board.get(0).get(0);

    t.checkExpect(upLeft.powered, true);
    t.checkExpect(top.powered, true);
    t.checkExpect(left.powered, true);

    this.w2.powerRow = this.w2.boardHeight / 2;
    this.w2.powerCol = this.w2.boardWidth / 2;
    this.w2.powerRadius = 2;
    this.w2.powerUp(this.w2.board.get(this.w2.powerCol).get(this.w2.powerRow));

    GamePiece up = this.w2.board.get(this.w2.powerCol).get(this.w2.powerRow - 1);
    GamePiece down = this.w2.board.get(this.w2.powerCol).get(this.w2.powerRow + 1);
    GamePiece right = this.w2.board.get(this.w2.powerCol + 1).get(this.w2.powerRow);
    GamePiece left1 = this.w2.board.get(this.w2.powerCol - 1).get(this.w2.powerCol);

    t.checkExpect(up.powered, true);
    t.checkExpect(down.powered, true);
    t.checkExpect(right.powered, true);
    t.checkExpect(left1.powered, true);
  
  }
  
  void testOnMouse(Tester t) {
    this.init();
    GamePiece g = w2.board.get(0).get(0);
    
    
    t.checkExpect(g.bottom, true);
    
    w2.onMousePressed(new Posn(0, 0), "LeftButton");
    
    t.checkExpect(g.bottom, false);
    t.checkExpect(g.left, true);

    
    this.init();
    
    GamePiece g2 = w2.board.get(2).get(2);
    
    t.checkExpect(g2.bottom && g2.top, true);
    
    w2.onMousePressed(new Posn(110, 110), "LeftButton");
    
    t.checkExpect(g2.bottom && g2.top, false);
    t.checkExpect(g2.right && g2.left, true);
    
    this.init();
    
    GamePiece g3 = w2.board.get(4).get(4);
    
    t.checkExpect(g3.bottom && g3.top && g3.right && g3.left, true);
    
    w2.onMousePressed(new Posn(185, 185), "LeftButton");
    
    t.checkExpect(g3.bottom && g3.top && g3.right && g3.left, true);
    
    this.init();
    
    GamePiece g4 = w2.board.get(0).get(4);
    
    t.checkExpect(g4.bottom && g4.top && g4.right, true);
    
    w2.onMousePressed(new Posn(0, 185), "LeftButton");
    
    t.checkExpect(g4.bottom && g4.top && g4.right, false);
    t.checkExpect(g4.bottom && g4.left && g4.right, true);
    
  }
  
  void testOnKey(Tester t) {
    
  }
  
  
  void testWorldEnd(Tester t) {
    this.init();
    
    t.checkExpect(w2.worldEnds(), new WorldEnd(true, w2.makeAFinalScene()));
    
    
    
    t.checkExpect(w1.worldEnds(), new WorldEnd(false, w1.makeScene()));
    
  }
  
  void testFinalScene(Tester t) {
    this.init();
    WorldScene test = w2.makeScene();
    
    test.placeImageXY(new TextImage("You Win", 25, Color.GREEN),
        160, 160);
    
    t.checkExpect(w2.makeAFinalScene(), test);
  }
  
//Tests if all the corners of the board have a GamePiece with two connections
  void testGenBoard(Tester t) {
    this.init();
    GamePiece topLeft = this.w2.board.get(0).get(0);
    GamePiece topRight = this.w2.board.get(this.w2.boardWidth - 1).get(0);
    GamePiece botLeft = this.w2.board.get(0).get(this.w2.boardHeight - 1);
    GamePiece botRight = this.w2.board.get(this.w2.boardWidth - 1).get(this.w2.boardHeight - 1);

    t.checkExpect(topLeft.left && topLeft.top && !topLeft.right && !topLeft.bottom, false);
    t.checkExpect(!topRight.left && topRight.top && topRight.right && !topRight.bottom, false);
    t.checkExpect(botLeft.left && !botLeft.top && !botLeft.right && botLeft.bottom, false);
    t.checkExpect(!botRight.left && !botRight.top && botRight.right && botRight.bottom, false);

    GamePiece topEdge = this.w2.board.get(1).get(0);
    t.checkExpect(topEdge.left && topEdge.top && topEdge.right && !topEdge.bottom, false);
    GamePiece botEdge = this.w2.board.get(1).get(this.w2.boardHeight - 1);
    t.checkExpect(botEdge.left && !botEdge.top && botEdge.right && botEdge.bottom, false);
    GamePiece leftEdge = this.w2.board.get(0).get(1);
    t.checkExpect(leftEdge.left && leftEdge.top && !leftEdge.right && leftEdge.bottom, false);
    GamePiece rightEdge = this.w2.board.get(this.w2.boardWidth - 1).get(1);
    t.checkExpect(!rightEdge.left && rightEdge.top && rightEdge.right && rightEdge.bottom, false);
    GamePiece center = this.w2.board.get(this.w2.boardWidth / 2).get(this.w2.boardHeight / 2);
    t.checkExpect(center.left && center.right && center.top && center.bottom, true);

  }

  void testGenNode(Tester t) {
    this.init();
    int expectedLength = this.w2.boardWidth * this.w2.boardHeight;
    t.checkExpect(this.w2.nodes.size(), expectedLength);
    t.checkExpect(this.w2.nodes.isEmpty(), false);
    int randomIndex = (int) (Math.random() * this.w2.nodes.size()); 
    GamePiece randomNode = this.w2.nodes.get(randomIndex);
    t.checkExpect(this.w2.nodes.contains(randomNode), true);
  }

  

}
