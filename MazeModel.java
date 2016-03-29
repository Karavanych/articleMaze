package labyrinth.d3D.maze;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Random;


public class MazeModel {
	int[][] mazeArray;
	
	public static final int ELLER_ALGORITHM = 1; // http://habrahabr.ru/post/176671/
	public static final int RECURSIVE_BACKTRACKING = 2; // http://habrahabr.ru/post/262345/
	
	public static final int NO_WALL		= 0x0000;
	public static final int WALL_LEFT 	= 0x0001;
	public static final int WALL_RIGHT 	= 0x0002;
	public static final int WALL_TOP 	= 0x0004;
	public static final int WALL_BOTTOM = 0x0008;

	public int width;
	public int height;

	public int entranceLenght;
	public int entranceX;
	public int entranceY;

	public int exitLenght;
	public int exitX;
	public int exitY;

	public MazeModel(int width, int height, int algorithm) {
		this(width, height);
		switch (algorithm) {
			case ELLER_ALGORITHM:
				this.generate();
				break;
			case RECURSIVE_BACKTRACKING:
				this.generateRecursive();
				break;
		}
	}
	
	public MazeModel(LevelGeneration instance) {
		this(instance.getWidth(),instance.getHeight(),instance.getAlgorithm());
	}

	public void generateRecursive() {
		
		Random random = new Random();
		ArrayDeque<Cell> hasNeighbors=new ArrayDeque<Cell>();
		LinkedList<Cell> tempUnvisited=new LinkedList<Cell>();
		
		int[][] visitedArray=new int[height][width];
		
		for (int y=0;y<height;y++) {	
			for (int x = 0; x < width; x++) {	
				mazeArray[y][x]=WALL_LEFT|WALL_RIGHT|WALL_TOP|WALL_BOTTOM;
				visitedArray[y][x]=1;
			}
		}						
		
		Cell currentCell=new Cell(0,0);
		Cell nextCell;
		
		while (true) {					
			currentCell.setVisited(visitedArray);
			
			if (currentCell.hasUnvisited(visitedArray,tempUnvisited)) {
				hasNeighbors.push(currentCell);
				
				int nextCellIdx = random.nextInt(tempUnvisited.size());
				nextCell = tempUnvisited.get(nextCellIdx);				
				tempUnvisited.clear();
				removeWall(currentCell,nextCell);
				currentCell=nextCell;
			} else if (hasNeighbors.size()>0) {
				currentCell=hasNeighbors.pop();					
			} else { //  generation complite
				break;
			}
		}
		
		
		
	}
	
	
	private void removeWall(Cell currentCell, Cell nextCell) {
		if (currentCell.x>nextCell.x) {
			mazeArray[currentCell.y][currentCell.x] &= (~WALL_LEFT);
			mazeArray[nextCell.y][nextCell.x] &= (~WALL_RIGHT);
		}
		
		else if (currentCell.x<nextCell.x) {
			mazeArray[currentCell.y][currentCell.x] &= (~WALL_RIGHT);
			mazeArray[nextCell.y][nextCell.x] &= (~WALL_LEFT);
		}
		
		else if (currentCell.y>nextCell.y) {
			mazeArray[currentCell.y][currentCell.x] &= (~WALL_TOP);
			mazeArray[nextCell.y][nextCell.x] &= (~WALL_BOTTOM);
		}
		
		else if (currentCell.y<nextCell.y) {
			mazeArray[currentCell.y][currentCell.x] &= (~WALL_BOTTOM);
			mazeArray[nextCell.y][nextCell.x] &= (~WALL_TOP);
		}		
		
	}


	private class Cell {
		public int x;
		public int y;				
		
		public Cell(int y,int x) {
			this.x=x;
			this.y=y;			
		}
		public void setVisited(int[][] visitedArray) {
			visitedArray[y][x]=0;			
		}
		
		public boolean hasUnvisited(int[][] visitedArray, LinkedList<Cell> tempUnvisited) {
			
			if (this.x>0 && visitedArray[y][x-1]!=0) {				
				tempUnvisited.add(new Cell(y,x-1));				
			}
			
			if (this.y>0 && visitedArray[y-1][x]!=0) {				
				tempUnvisited.add(new Cell(y-1,x));
			}
			
			if (this.x<visitedArray[0].length-1 && visitedArray[y][x+1]!=0) {				
				tempUnvisited.add(new Cell(y,x+1));
			}
			
			if (this.y<visitedArray.length-1 && visitedArray[y+1][x]!=0) {				
				tempUnvisited.add(new Cell(y+1,x));
			}
			
			return tempUnvisited.size()>0;
		}
	}

	public MazeModel(int width, int height) {
		this.width = width;
		this.height = height;
		this.mazeArray = new int[height][width];	
	}

	public void generate() {				
		
		int[] sets=new int[width];
		int[] setsSize=new int[width+1];
		
		ArrayDeque<Integer> freeSetsId=new ArrayDeque<Integer>();
		for (int i=1;i<=width;i++) {
			freeSetsId.push(i);			
		}
				
		
		Random random = new Random();
		
		/*Step 1: Create the first row

		This will just be an empty row.

			 ___ ___ ___ ___ ___ ___ ___ ___
			|                               |*/
		//int y=0;
		
		for (int y=0;y<height;y++) {
		
			/*		 		  
			Step 2:Join any cells not members of a set to their own unique set
	
			 ___ ___ ___ ___ ___ ___ ___ ___
			| 1   2   3   4   5   6   7   8 |*/
			
			
			// my sample
			//y = 1 sets =[1, 2, 0, 3, 0, 0, 3, 8, 9, 10]
					
			for (int x = 0; x < width; x++) {				
				if (sets[x]==0) {				
					sets[x]=freeSetsId.pop();
					setsSize[sets[x]]=1;
				}
			}						
			
			/* Step 3:Create right walls
		 	___ ___ ___ ___ ___ ___ ___ ___
			|(1   2)  3   4   5   6   7   8 |
		
			If we choose not to add a wall, union the sets
		 	___ ___ ___ ___ ___ ___ ___ ___
			| 1  (1   3)  4   5   6   7   8 |
		
			 ___ ___ ___ ___ ___ ___ ___ ___
			| 1   1  (1 | 4)  5   6   7   8 |
		
			... snip ...
		
			 ___ ___ ___ ___ ___ ___ ___ ___
			| 1   1   1 | 4   4 | 6   6   6 | */
			
			for (int x = 0;x<width;x++) {
				
				boolean addWall=(y==height-1)?true:random.nextBoolean();
				
				if (addWall) {
					mazeArray[y][x]|=WALL_RIGHT;				
				} else { // no_right_wall
					if (x<width-1) {
						
						setsSize[sets[x+1]]--; // убираем сет из
						setsSize[sets[x]]++;
						
						sets[x+1]=sets[x];
					}
				}
			}						
						
			
			/* Step 4:Create bottom walls.
	
			Ensure that each set has at least one cell with a down passage (i.e. without a bottom wall). Failure to do so will create an isolation.
	
				 ___ ___ ___ ___ ___ ___ ___ ___
				| 1  _1_ _1_| 4  _4_| 6   6  _6_|
				*/
			int currentSet=0;	
			//boolean wasSpaceInCurrentSet=false;
			for (int x=0;x<width;x++) {					
				
				boolean canAddWall=false;
				
				currentSet=sets[x];
				if (setsSize[currentSet]>1) {
					canAddWall=true;
				}
				
				if (canAddWall) {
					boolean addWall=random.nextBoolean();
					if (addWall) {
						mazeArray[y][x]|=WALL_BOTTOM;
						setsSize[currentSet]--;
					} else {
						setsSize[currentSet]=32000;// just big int value
					}							
				}
			}	
			
			/*Step 5: Decide to keep adding rows, or stop and complete the maze
			 * A)
				If you decide to add another row:
					a) Output the current row
					b) Remove all right walls
					c) Remove cells with a bottom-wall from their set
					d) Remove all bottom walls
					e) Continue from Step 2 */
			
			//a ) Output the current row & b) remove all right walls
			
			if (y<height-1) {
			
				for (int x=0;x<width;x++) {
					mazeArray[y+1][x] = mazeArray[y][x] & (~(WALL_RIGHT|WALL_LEFT)); // исключаем все правые и левые стороны
					
					// c) Remove cells with a bottom-wall from their set
					if (bottomWall(y+1,x)) {
						sets[x]=0;
						//d) Remove all bottom walls, and all another walls
						mazeArray[y+1][x] = NO_WALL;
					}
				}
								
			} else { // last
				/* 5. If you decide to complete the maze
					B.
					  	a) Add a bottom wall to every cell
						b) Moving from left to right:
						 -If the current cell and the cell to the right are members of a different set:
						 -Remove the right wall
						 -Union the sets to which the current cell and cell to the right are members.
						 -Output the final row*/
				
				for (int x=0;x<width-1;x++) {
					
					//If the current cell and the cell to the right are members of a different set:
					if (sets[x]!=sets[x+1]) {
						//Remove the right wall
						mazeArray[y][x] = mazeArray[y][x] & (~WALL_RIGHT);
						//Union the sets to which the current cell and cell to the right are members.
						int tempSets=sets[x+1];
						for (int i=0;i<width;i++) {
							if (sets[i]==tempSets) {
								sets[i]=sets[x];
							}							
						}
					}
				}
				break;
			}
						
			
			// counting free id		
			for (int i=1;i<=width;i++) {			
				if (!isValueInSets(i,sets)) {
					freeSetsId.push(i);
				}
				
				//clear current setsSize
				setsSize[i]=0;
			}
			
			// counting current sets size
			for (int x=0;x<sets.length;x++) {
				if (sets[x]!=0) {
					setsSize[sets[x]]++;
				}
			}
		}
		
		
		//add LEFT & TOP wall in cells
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {	
				if (x<width-1 && rightWall(y,x)) {
					mazeArray[y][x+1]|=WALL_LEFT;
				}
				if ((y<height-1) && bottomWall(y,x)) {
					mazeArray[y+1][x]|=WALL_TOP;
				}
			}
		}
		
		
		// add borders TOP & BOTTOM
		for (int x = 0; x < width; x++) {
			mazeArray[0][x]|=WALL_TOP;
			mazeArray[height-1][x]|=WALL_BOTTOM;
		}
		
		for (int y = 0; y < height; y++) {
			mazeArray[y][0]|=WALL_LEFT;
			mazeArray[y][width-1]|=WALL_RIGHT;
		}
		
		
	}	

	protected void allRandom() {
		Random random = new Random();		
		
		int maxRandom=WALL_LEFT|WALL_RIGHT|WALL_TOP|WALL_BOTTOM;		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				mazeArray[y][x] = random.nextInt(maxRandom);
			}
		}		
	}
	
	private boolean isValueInSets(int value,int sets[]) {
		
		for (int o=0;o<sets.length;o++) {
			if (sets[o]==value) {
				return true;
			}			
		}
		return false;
		
	}

	public boolean leftWall(int y, int x) {
		return isWall(y,x,WALL_LEFT);	
	}

	public boolean rightWall(int y, int x) {
		return isWall(y,x,WALL_RIGHT);	
	}

	public boolean topWall(int y, int x) {
		return isWall(y,x,WALL_TOP);
	}

	public boolean bottomWall(int y, int x) {
		return isWall(y,x,WALL_BOTTOM);
	}
	
	public boolean isWall(int y,int x, int wall) {
		return ((mazeArray[y][x] & wall) == wall);
	}

	public void clear() {
		for (int[] each:mazeArray) {
			for (int i=0;i<each.length;i++) {
				each[i]=NO_WALL;
			}
		}			
	}

	public void addEntrance(int y, int x,int lenght) {
		//TODO нужно достраивать лабиринт, на 1 ячейку в длинну дальше и 2 в ширину, для соединения с мостом
		mazeArray[y][x] = mazeArray[y][x] & (~WALL_BOTTOM);
		/*if (x<width-1) {
			mazeArray[y][x+1] = mazeArray[y][x+1] & (~WALL_BOTTOM);
		}	*/	
		this.entranceLenght=lenght;
		this.entranceX=x;
		this.entranceY=y;
	}

	public void addExit(int y, int x,int lenght) {		
		//TODO нужно достраивать лабиринт, на 1 ячейку в длинну дальше и 2 в ширину, для соединения с мостом
		mazeArray[y][x] = mazeArray[y][x] & (~WALL_TOP);
		/*if (x<width-1) {
			mazeArray[y][x+1] = mazeArray[y][x+1] & (~WALL_TOP);
		}*/
		this.exitLenght=lenght;
		this.exitX=x;
		this.exitY=y;
	}

}
