package labyrinth.d3D.maze;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

import labyrinth.d3D.maze.LevelGeneration.LevelParams;
import labyrinth.maze.materials.MazeMaterial;
import labyrinth.maze.materials.MazePlayerMaterial;

import draziw.gles.engine.Texture;
import draziw.gles.game.ResourceManager;
import draziw.gles.materials.Material;
import draziw.gles.objects.Custom3D;

public class MazeView extends Custom3D {
	
	public static final String MODEL_KEY="fullmaze";
	
	public float uoffset;
	public float voffset;

	private boolean unlocked;
	
	public boolean visible=false;
	
	
	private Texture planebasetexture;

	private Texture planenormalstexture;

	private float tileStartX;

	private float tileStartY;

	private int indexCountWalls;

	private int indexCountPlanes;
	
	public final static float PLANE_TILE_SIZE_X=0.25f;
	public final static float PLANE_TILE_SIZE_Y=0.25f;

	
	public MazeView(Texture texture, Texture normalMap,Texture planebase,Texture planenormals,Material material,
			ResourceManager resources, MazeModel mazeModel,boolean createPlane) {
		super(texture, normalMap, material, resources, createVBO(mazeModel,resources,MODEL_KEY,1f,1f,1f,createPlane,true));
		
		this.planebasetexture=planebase;
		this.planenormalstexture=planenormals;
		
		setGeometry(mazeModel.width*MazeGame.MAZE_CELL, 0.5f*MazeGame.MAZE_CELL, mazeModel.height*MazeGame.MAZE_CELL);
		
		indexCountPlanes=(createPlane?6:0)+(mazeModel.entranceLenght>0?12:0);
		indexCountWalls=indexCount-indexCountPlanes;
						
	}

	public MazeView(Texture texture, Texture normalMap,Texture planebase,Texture planenormals,Material material,
			ResourceManager resources, MazeModel mazeModel, String resModelKey,float scaleFactorX,float scaleFactorY,float scaleFactorZ,boolean createPlane,boolean override) {
		super(texture, normalMap, material, resources, createVBO(mazeModel,resources,resModelKey,scaleFactorX,scaleFactorY,scaleFactorZ,createPlane,override));
		setGeometry(mazeModel.width*MazeGame.MAZE_CELL*scaleFactorX, 0.5f*MazeGame.MAZE_CELL*scaleFactorY, mazeModel.height*MazeGame.MAZE_CELL*scaleFactorZ);
		
		this.planebasetexture=planebase;
		this.planenormalstexture=planenormals;
				
		indexCountPlanes=(createPlane?6:0)+(mazeModel.entranceLenght>0?12:0);
		indexCountWalls=indexCount-indexCountPlanes;
				
	}
	
	public static String createVBO(MazeModel mazeModel, ResourceManager resources, String resModelKey,float scaleFactorX,float scaleFactorY,float scaleFactorZ,boolean createPlane, boolean override) {		
		
		if (override || !resources.hasModel(resModelKey)) {
		
			FloatBuffer leftwall = resources.loadBuffer("leftwall");		
			int leftwallIndexCount = (int) (leftwall.remaining()*4);
			
			FloatBuffer doubleleft = resources.loadBuffer("doubleleft");
			int doubleleftIndexCount = (int) (doubleleft.remaining()*4);
			
			FloatBuffer bottomwall = resources.loadBuffer("bottomwall");
			int bottomwallIndexCount = (int) (bottomwall.remaining()*4);
			
			FloatBuffer doublebottom = resources.loadBuffer("doublebottom");
			int doublebottomIndexCount = (int) (doublebottom.remaining()*4);
			
			FloatBuffer plane = resources.loadBuffer("plane");
			int planeIndexCount = (int)(plane.remaining()*4);
			
			int bufferLenght=0;
			
			//считаем длинну буфера первым проходом по лабиринту
			// сам лабиринт		
			for (int y = 0; y < mazeModel.height; y++) {
				for (int x = 0; x < mazeModel.width; x++) {
					if (x==0 && mazeModel.leftWall(y,x)) {
						bufferLenght+=leftwallIndexCount;
						//bufferLenght+=doubleleftIndexCount;
					}
					
					if (x==mazeModel.width-1 && mazeModel.rightWall(y,x)) {
						bufferLenght+=leftwallIndexCount;
						//bufferLenght+=doubleleftIndexCount;
					}
					
					if (y==0 && mazeModel.topWall(y,x)) {
						bufferLenght+=bottomwallIndexCount;
						//bufferLenght+=doublebottomIndexCount;
					}
					
					if (y==mazeModel.height-1 && mazeModel.bottomWall(y,x)) {
						bufferLenght+=bottomwallIndexCount;
						//bufferLenght+=doublebottomIndexCount;
					}	
					
					if (x>0 && mazeModel.leftWall(y,x)) {
						bufferLenght+=doubleleftIndexCount;
					}
					
					if (y<mazeModel.height-1 && mazeModel.bottomWall(y,x)) {
						bufferLenght+=doublebottomIndexCount;
					}				
				}
			}		
			
			// увеличиваем буферы если есть выходы в лабиринте
			//bufferLenght+=2*mazeModel.entranceLenght*doubleleftIndexCount;
			//bufferLenght+=2*mazeModel.exitLenght*doubleleftIndexCount;
			if (mazeModel.entranceLenght>0) {
				bufferLenght+=(2*mazeModel.entranceLenght+7)*leftwallIndexCount;
				bufferLenght+=planeIndexCount;
			}
			if (mazeModel.exitLenght>0) {
				bufferLenght+=(2*mazeModel.exitLenght+7)*leftwallIndexCount;
				bufferLenght+=planeIndexCount;
			}
			
			if (createPlane) {
				bufferLenght+=planeIndexCount;
			}
			
			ByteBuffer pointVBB = ByteBuffer.allocateDirect(bufferLenght);		
			pointVBB.order(ByteOrder.nativeOrder());
			
			FloatBuffer sumBuffer = pointVBB.asFloatBuffer();		
			
			// сам лабиринт		
			for (int y = 0; y < mazeModel.height; y++) {
				for (int x = 0; x < mazeModel.width; x++) {
					if (x==0 && mazeModel.leftWall(y,x)) {
						addtranslateFB(sumBuffer,leftwall,x*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY,scaleFactorZ);
						//addtranslateFB(sumBuffer,doubleleft,x*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactor);
					}
					
					if (x==mazeModel.width-1 && mazeModel.rightWall(y,x)) {
						addtranslateFB(sumBuffer,leftwall,(x+1)*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY,scaleFactorZ);
						//addtranslateFB(sumBuffer,doubleleft,(x+1)*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 0.0f, y*MazeGame.MAZE_CELL,scaleFactor);
					}
					
					if (y==0 && mazeModel.topWall(y,x)) {
						addtranslateFB(sumBuffer,bottomwall,x*MazeGame.MAZE_CELL, 0.0f, (y-1)*MazeGame.MAZE_CELL+MazeGame.WALL_WIDTH,scaleFactorX,scaleFactorY,scaleFactorZ);
						//addtranslateFB(sumBuffer,doublebottom,x*MazeGame.MAZE_CELL, 0.0f, (y-1)*MazeGame.MAZE_CELL+MazeGame.WALL_WIDTH,scaleFactor);
					}
					
					if (y==mazeModel.height-1 && mazeModel.bottomWall(y,x)) {
						addtranslateFB(sumBuffer,bottomwall,x*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY,scaleFactorZ);
						//addtranslateFB(sumBuffer,doublebottom,x*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactor);
					}	
					
					if (x>0 && mazeModel.leftWall(y,x)) {
						addtranslateFB(sumBuffer,doubleleft,x*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY,scaleFactorZ);
					}
					
					if (y<mazeModel.height-1 && mazeModel.bottomWall(y,x)) {
						addtranslateFB(sumBuffer,doublebottom,x*MazeGame.MAZE_CELL, 0.0f,y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY,scaleFactorZ);
					}				
				}
			}
			
			// добавим выходы			
			if (mazeModel.entranceLenght>0) {
				int x=mazeModel.entranceX;
				for (int y=mazeModel.entranceY+1;y<=mazeModel.entranceY+mazeModel.entranceLenght;y++) {
					//addtranslateFB(sumBuffer,doubleleft,x*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactor);
					//addtranslateFB(sumBuffer,doubleleft,(x+1)*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactor);
					addtranslateFB(sumBuffer,leftwall,x*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY*0.5f,scaleFactorZ);
					addtranslateFB(sumBuffer,leftwall,(x+1)*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY*0.5f,scaleFactorZ);
				}
				// строим портал
				addTranslateRotateX(sumBuffer, leftwall, x*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 0.0f, (mazeModel.entranceY+mazeModel.entranceLenght)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				addTranslateRotateX(sumBuffer, leftwall, x*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 2.0f, (mazeModel.entranceY+mazeModel.entranceLenght)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				addTranslateRotateX(sumBuffer, leftwall, (x+1)*MazeGame.MAZE_CELL, 0.0f, (mazeModel.entranceY+mazeModel.entranceLenght)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				addTranslateRotateX(sumBuffer, leftwall, (x+1)*MazeGame.MAZE_CELL, 2.0f, (mazeModel.entranceY+mazeModel.entranceLenght)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				
				//верхняя перекладина
				addTranslateRotateX(sumBuffer,bottomwall,x*MazeGame.MAZE_CELL+0.46f, 4.48f, (mazeModel.entranceY+mazeModel.entranceLenght)*MazeGame.MAZE_CELL,0.54f,0.5f,0.92f,1.5708f);
				
				//правая (куда смотрим)
				addTranslateRotateZ(sumBuffer, bottomwall, x*MazeGame.MAZE_CELL-0.12f, 3.88f,0.01f+ (mazeModel.entranceY+mazeModel.entranceLenght)*MazeGame.MAZE_CELL+MazeGame.WALL_WIDTH*2, 0.5f, 0.18f, 2f, 0.785398f);
				
				//левая
				addTranslateRotateZ(sumBuffer, bottomwall, (x+1)*MazeGame.MAZE_CELL+MazeGame.WALL_WIDTH, 4f,0.01f+ (mazeModel.entranceY+mazeModel.entranceLenght)*MazeGame.MAZE_CELL+MazeGame.WALL_WIDTH*2, 0.5f, 0.18f, 2f, 2.3562f);
								
			}
					
			if (mazeModel.exitLenght>0) {								
				
				int x=mazeModel.exitX;
				for (int y=mazeModel.exitY-mazeModel.exitLenght;y<mazeModel.exitY;y++) {
					//addtranslateFB(sumBuffer,doubleleft,x*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactor);
					//addtranslateFB(sumBuffer,doubleleft,(x+1)*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactor);
					addtranslateFB(sumBuffer,leftwall,x*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY*0.5f,scaleFactorZ);
					addtranslateFB(sumBuffer,leftwall,(x+1)*MazeGame.MAZE_CELL, 0.0f, y*MazeGame.MAZE_CELL,scaleFactorX,scaleFactorY*0.5f,scaleFactorZ);
				}
				
				// строим портал
				addTranslateRotateX(sumBuffer, leftwall, x*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 0.0f, (-mazeModel.exitLenght-1.25f)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				addTranslateRotateX(sumBuffer, leftwall, x*MazeGame.MAZE_CELL-MazeGame.WALL_WIDTH, 2.0f, (-mazeModel.exitLenght-1.25f)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				addTranslateRotateX(sumBuffer, leftwall, (x+1)*MazeGame.MAZE_CELL, 0.0f, (-mazeModel.exitLenght-1.25f)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				addTranslateRotateX(sumBuffer, leftwall, (x+1)*MazeGame.MAZE_CELL, 2.0f, (-mazeModel.exitLenght-1.25f)*MazeGame.MAZE_CELL, 1f, 0.5f, 1f, 1.5708f);
				
				//верхняя перекладина
				addTranslateRotateX(sumBuffer,bottomwall,x*MazeGame.MAZE_CELL+0.46f, 4.48f, (-mazeModel.exitLenght-1.25f)*MazeGame.MAZE_CELL,0.54f,0.5f,0.92f,1.5708f);
				
				//правая (куда смотрим)
				addTranslateRotateZ(sumBuffer, bottomwall, x*MazeGame.MAZE_CELL-0.12f, 3.88f,-0.01f-(mazeModel.exitLenght+1f)*MazeGame.MAZE_CELL, 0.5f, 0.18f, 2f, 0.785398f);
				
				//левая
				addTranslateRotateZ(sumBuffer, bottomwall, (x+1)*MazeGame.MAZE_CELL+MazeGame.WALL_WIDTH, 4f,-0.01f-(mazeModel.exitLenght+1f)*MazeGame.MAZE_CELL, 0.5f, 0.18f, 2f, 2.3562f);
			}
			
			// добавляем plane
			if (createPlane) {
				MazePlane mpl = new MazePlane();
				//addtranslatePlane(sumBuffer,plane,0.0f, 0.0f,(mazeModel.width-1)*MazeGame.MAZE_CELL*scaleFactorZ,mazeModel.width*MazeGame.MAZE_CELL*scaleFactorX,mazeModel.height*MazeGame.MAZE_CELL*scaleFactorZ,0f,true);
				mpl.createPlane(sumBuffer,0.0f, 0.0f,(mazeModel.height-1)*MazeGame.MAZE_CELL*scaleFactorZ,mazeModel.width*MazeGame.MAZE_CELL*scaleFactorX,mazeModel.height*MazeGame.MAZE_CELL*scaleFactorZ,0f,true);
				if (mazeModel.entranceLenght>0) {
					//addtranslatePlane(sumBuffer,plane,mazeModel.width*MazeGame.MAZE_CELL*0.5f, 0.0f,(mazeModel.height+5.1f)*MazeGame.MAZE_CELL,MazeGame.MAZE_CELL*scaleFactorX,6.1f*MazeGame.MAZE_CELL,0f,false);
					mpl.createPlane(sumBuffer,mazeModel.entranceX*MazeGame.MAZE_CELL, 0.0f,(mazeModel.height+mazeModel.entranceLenght-0.9f)*MazeGame.MAZE_CELL,MazeGame.MAZE_CELL*scaleFactorX,(mazeModel.entranceLenght+0.1f)*MazeGame.MAZE_CELL,0f,false);
				}
				if (mazeModel.exitLenght>0) {
					//addtranslatePlane(sumBuffer,plane,mazeModel.width*MazeGame.MAZE_CELL*0.5f, 0.0f,-MazeGame.MAZE_CELL,MazeGame.MAZE_CELL*scaleFactorX,6.1f*MazeGame.MAZE_CELL,0.14f,false);
					mpl.createPlane(sumBuffer,mazeModel.exitX*MazeGame.MAZE_CELL, 0.0f,-MazeGame.MAZE_CELL,MazeGame.MAZE_CELL*scaleFactorX,(mazeModel.exitLenght+0.1f)*MazeGame.MAZE_CELL,0.14f,false);
				}
			}
			
			sumBuffer.position(0);			
			
			sumBuffer.position(0);
			
			resources.putSingleModelData(resModelKey,sumBuffer);
		}
		
		return resModelKey;		
	}
	
	public static void addtranslateFB(FloatBuffer sumBuffer,FloatBuffer append,float x,float y,float z,float scaleFactorX,float scaleFactorY,float scaleFactorZ) {
		
		int bufferStride = ResourceManager.VNT_STRIDE/4;
		
		//в буфере идут сначала вертексы 3*float, затем нормали 3*float, и всякие текстуры, тангенты битангенты
		// сдвигать нужно только первые 3 float
		// остальное сохраняем как есть
		
		append.position(0);
		
		while (append.hasRemaining()) {			
			sumBuffer.put((append.get()+x)*scaleFactorX);
			sumBuffer.put((append.get()+y)*scaleFactorY);
			sumBuffer.put((append.get()+z)*scaleFactorZ);
											
			for (int i=0;i<bufferStride-3;i++) {// оставшиеся складываем в буфер без изменений
				sumBuffer.put(append.get());
			}
		}      
	}
	
	public static void addTranslateRotateX(FloatBuffer sumBuffer,FloatBuffer append,float addx,float addy,float addz,float scaleFactorX,float scaleFactorY,float scaleFactorZ,float angle) {
		
		int bufferStride = ResourceManager.VNT_STRIDE/4;
		
		//в буфере идут сначала вертексы 3*float, затем нормали 3*float, и всякие текстуры, тангенты битангенты
		// сдвигать нужно только первые 3 float
		// остальное сохраняем как есть
		
		append.position(0);
		
		float cosA=(float) Math.cos(angle);
		float sinA=(float) Math.sin(angle);
		
		while (append.hasRemaining()) {				
			float x=append.get()*scaleFactorX;
			float y=append.get()*scaleFactorY;
			float z=append.get()*scaleFactorZ;
			
			sumBuffer.put(x+addx);
			sumBuffer.put(y*cosA-z*sinA+addy);
			sumBuffer.put(y*sinA+z*cosA+addz);
			
			// повернем нормали
			x=append.get();
			y=append.get();
			z=append.get();
			
			sumBuffer.put(x);
			sumBuffer.put(y*cosA-z*sinA);
			sumBuffer.put(y*sinA+z*cosA);
			
			// текстуры положим как есть
			sumBuffer.put(append.get());
			sumBuffer.put(append.get());
			
			// повернем тангент
			x=append.get();
			y=append.get();
			z=append.get();
			
			sumBuffer.put(x);
			sumBuffer.put(y*cosA-z*sinA);
			sumBuffer.put(y*sinA+z*cosA);
			
			
			// повернем бинормаль
			
			x=append.get();
			y=append.get();
			z=append.get();
			
			sumBuffer.put(x);
			sumBuffer.put(y*cosA-z*sinA);
			sumBuffer.put(y*sinA+z*cosA);
			
		}      
	}
	
	public static void addTranslateRotateZ(FloatBuffer sumBuffer,FloatBuffer append,float addx,float addy,float addz,float scaleFactorX,float scaleFactorY,float scaleFactorZ,float angle) {
		
		int bufferStride = ResourceManager.VNT_STRIDE/4;
		
		//в буфере идут сначала вертексы 3*float, затем нормали 3*float, и всякие текстуры, тангенты битангенты
		// сдвигать нужно только первые 3 float
		// остальное сохраняем как есть
		
		append.position(0);
		
		float cosA=(float) Math.cos(angle);
		float sinA=(float) Math.sin(angle);
		
		while (append.hasRemaining()) {				
			float x=append.get()*scaleFactorX;
			float y=append.get()*scaleFactorY;
			float z=append.get()*scaleFactorZ;
			
			sumBuffer.put(x*cosA-y*sinA+addx);
			sumBuffer.put(x*sinA+y*cosA+addy);
			sumBuffer.put(z+addz);
			
			// повернем нормали
			x=append.get();
			y=append.get();
			z=append.get();
			
			sumBuffer.put(x*cosA-y*sinA);
			sumBuffer.put(x*sinA+y*cosA);
			sumBuffer.put(z);
			
			// текстуры положим как есть
			sumBuffer.put(append.get());
			sumBuffer.put(append.get());
			
			// повернем тангент
			x=append.get();
			y=append.get();
			z=append.get();
			
			sumBuffer.put(x*cosA-y*sinA);
			sumBuffer.put(x*sinA+y*cosA);
			sumBuffer.put(z);
			
			
			// повернем бинормаль
			
			x=append.get();
			y=append.get();
			z=append.get();
			
			sumBuffer.put(x*cosA-y*sinA);
			sumBuffer.put(x*sinA+y*cosA);
			sumBuffer.put(z);
			
		}      
	}
	
	
	public static void addtranslatePlane(FloatBuffer sumBuffer,FloatBuffer append,float x,float y,float z,float sizeX,float sizeZ,float textureOffsetZ,boolean limitTextureSize) {
		
		int bufferStride = ResourceManager.VNT_STRIDE/4;
		
		//в буфере идут сначала вертексы 3*float, затем нормали 3*float, и всякие текстуры, тангенты битангенты
		// сдвигать нужно только первые 3 float
		// остальное сохраняем как есть
		
		append.position(0);
		float textureScaleX = sizeX/20f;
		float textureScaleZ = sizeZ/20f;
		if (limitTextureSize) {
			textureScaleX = Math.max(textureScaleX,0.25f);
			textureScaleZ = Math.max(textureScaleX,0.25f);
		} 
		
		while (append.hasRemaining()) {			
			sumBuffer.put(append.get()*sizeX+x);
			sumBuffer.put(append.get()+y);
			sumBuffer.put(append.get()*sizeZ+z);
			
			// normals
			sumBuffer.put(append.get());
			sumBuffer.put(append.get());
			sumBuffer.put(append.get());
			
			//textures			
			sumBuffer.put(append.get()*textureScaleX);
			sumBuffer.put(append.get()*textureScaleZ+textureOffsetZ);
			
			for (int i=0;i<bufferStride-8;i++) {// оставшиеся складываем в буфер без изменений
				sumBuffer.put(append.get());
			}
		}
		      	
	}
	
	@Override
	public void draw(float[] viewMatrix, float[] projectionMatrix, float timer) {
		if (visible) {
			GLES20.glUniform2f(((MazePlayerMaterial)material).uVec2TextureOffset, uoffset, voffset);						
			
			Matrix.setIdentityM(mMVMatrix,0);				
			 
			 GLES20.glUniformMatrix4fv(material.um, 1, false, mObjectMatrix, 0);//передаем матрицу M в шейдер
			 
			 Matrix.multiplyMM(mMVMatrix, 0, viewMatrix, 0, mObjectMatrix, 0);
			 
			 setNormalMatrix();
			 
				
			 GLES20.glUniformMatrix3fv(material.uNormalMatrix, 1, false, tmpNormalMatrix, 0);

			 
			 
			 Matrix.multiplyMM(mObjectMVPMatrix, 0, projectionMatrix, 0, mMVMatrix, 0);
			 
			 GLES20.glUniformMatrix4fv(material.umvp, 1, false, mObjectMVPMatrix, 0);//передаем кумулятивную матрицы MVP в шейдер
			 
			 mTexture.use(material.uBaseMap);
			 //GLES20.glActiveTexture(mTexture.slot); // активируем текстуру, которой собрались рисовать		 
			 //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture.id); // прикрепляем текстуру, которой собираемся сейчас рисовать		 
			 //GLES20.glUniform1i(uSamplerHolder,mTexture.slot);//передаем индекс текстуры в шейдер... index текстуры и id текстуры различаются, я хз пока почему
			 
			 GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vntBufferHolder);
			 GLES20.glEnableVertexAttribArray(material.aPosition);
			 GLES20.glVertexAttribPointer(material.aPosition, 3, GLES20.GL_FLOAT, false, ResourceManager.VNT_STRIDE, 0);
			 
			 
			 GLES20.glEnableVertexAttribArray(material.aNormal);
			 GLES20.glVertexAttribPointer(material.aNormal, 3, GLES20.GL_FLOAT, false, ResourceManager.VNT_STRIDE,ResourceManager.NORMAL_OFFSET); // 12 - offset 3*4float
			 
			 
			 GLES20.glEnableVertexAttribArray(material.aTextureCoord);
			 GLES20.glVertexAttribPointer(material.aTextureCoord, 2, GLES20.GL_FLOAT, false, ResourceManager.VNT_STRIDE,ResourceManager.TEXTURE_OFFSET); // 24 - offset (3+3)*4float
			 						 		 
			 GLES20.glEnableVertexAttribArray(material.aTangent);
			 GLES20.glVertexAttribPointer(material.aTangent, 3, GLES20.GL_FLOAT, false, ResourceManager.VNT_STRIDE,ResourceManager.TANGENT_OFFSET); // 24 - offset (3+3)*4float

				 
			 GLES20.glEnableVertexAttribArray(material.aBitangent);
			 GLES20.glVertexAttribPointer(material.aBitangent, 3, GLES20.GL_FLOAT, false, ResourceManager.VNT_STRIDE,ResourceManager.BITANGENT_OFFSET); // 24 - offset (3+3)*4float			 			 			 					
				 
			 normalMap.use(material.uNormalMap);

			 
			 GLES20.glUniform1i(((MazeMaterial)material).uDrawplane,0);	 	     //
		     GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,indexCountWalls);	
		     
		     // рисуем plane другой текстурой
		     planebasetexture.use(material.uBaseMap);
		     planenormalstexture.use(material.uNormalMap);
		     
		     GLES20.glUniform2f(((MazePlayerMaterial)material).uVec2TextureOffset, 0f, 0f);
		     GLES20.glUniform1i(((MazeMaterial)material).uDrawplane,1);
		     GLES20.glUniform2f(((MazeMaterial)material).uTileSize,MazeView.PLANE_TILE_SIZE_X,MazeView.PLANE_TILE_SIZE_Y);
		     GLES20.glUniform2f(((MazeMaterial)material).uTileStart,tileStartX,tileStartY);
		     
		     GLES20.glDrawArrays(GLES20.GL_TRIANGLES,indexCountWalls,indexCountPlanes);	
		     
		    // Если буфер неотключить - то его начинают использовать другие объекты - например cubeMap
			// Надо подумать куда это вставлять
			// Clear the currently bound buffer (so future OpenGL calls do not use this buffer).
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		}
	}

	public void setLevelParams(LevelParams levelParams) {	
		if (levelParams!=null) {
			this.uoffset=levelParams.uOffset;
			this.voffset=levelParams.vOffset;
			this.unlocked=levelParams.unlocked;
			this.tileStartX=levelParams.planeTileX;
			this.tileStartY=levelParams.planeTileY;
			this.visible=true;
		} else {
			this.unlocked=true;
			this.visible=false;
		}
	}
	
	public boolean isUnlocked() {
		return unlocked;
	}

}