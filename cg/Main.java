import java.awt.*;
import java.awt.event.*;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.math.VectorUtil;

/*
key control:

c - reset camera position and orientation

arrow-up 	- move forward
arrow-down 	- move backward
arrow-left 	- move left
arrow-right - move right
page-up 	- move up
page-down 	- move down

a - rotate left
d - rotate right
w - rotate up
s - rotate down

mouse rotation of the scene if shift is pressed


i - move light up
k - move light down
x - reset light position
*/

public class Main implements GLEventListener, KeyListener, MouseListener, MouseMotionListener  {

	//create an instance of GL Utility library and keep it
	private static final GLU glu = new GLU();
	//create an instance of GL Utility toolkit
	private static final GLUT glut = new GLUT();
	
	//Camera variables
	private static float camera_position[] = {0.0f, 1.0f, -10.0f};
	private static float center_position[] = {0.0f, 1.0f, 0.0f};
	private static float camera_orientation[] = {0.0f, 1.0f, 0.0f};
	
	//speed for camera movement
	private static float speed = 0.2f;
	//camera angle increment (left-right) in degrees
	private static float rot_speed = 1.0f;
	//camera angle increment (up-down) in degrees
	private static float updown_speed = 0.2f;
	//Bounds for looking up and down
	private static final float MAXUP = 4.0f;
	private static final float MAXDOWN = -4.0f;
	//left-right rotation
	private static float xzIncr = 0;
	//up-down rotation
	private static float yzIncr = 0;
	
	
	//Variables for cube
	//	Center of cube
	private static float cube_center[] = {0.0f, 0.75f, 2.0f};
	//	Cube rotation
	private static float cube_incr = 3.0f;
	
	//Rotation of the scene with the mouse
	private static int mouseX = 0, mouseY = 0;
	private static float scene_rotx = 0.0f, scene_roty = 0.0f;
	private static float scene_rotIncr = 2.0f;
	
	
	
	//Light calculation - variables for properties
	//	First light source: light0 (point light)
	//	Position: w=0->directed light source (position=direction) 
	private static float light0_ambient[] = {0.5f, 0.5f, 0.5f, 1.0f};		//RGBA
	private static float light0_diffuse[] = {1.0f, 1.0f, 1.0f, 1.0f};		//RGBA
	private static float light0_specular[] = {1.0f, 1.0f, 1.0f, 1.0f};		//RGBA
	private static float light0_emissive[] = {1.0f, 1.0f, 1.0f, 1.0f};		//RGBA
	private static float light0_position[] = {0.0f, 6.0f, 0.0f, 1.0f};		//XYZW (homogene Coord.)

	//	Speed for lightmovement
	private static float lightspeed = 0.2f;
	//	Radius for light movement
	private static float lightradius = light0_position[1] - cube_center[1];
	private static final float LIGHTMAXUP = 10.0f;
	private static final float LIGHTMAXDOWN = 3.0f;
	//	Rotation of the light
	private static float lightrot = 0.0f;

	
	
	private static float[] randomParameters;
	
	
	
	
	/*
	 * Main function
	 */
	public static void main(String[] args) {
		// create OpenGL window
		GLCanvas canvas = new GLCanvas();
		canvas.addGLEventListener(new Main());
		canvas.addKeyListener(new Main());
		canvas.addMouseListener(new Main());
		canvas.addMouseMotionListener(new Main());

		// animate the canvas with 60 frames per second
		FPSAnimator animator = new FPSAnimator(canvas, 60, true);
		animator.start();
		
		final int width = 800;
	    final int height = 600;
	    
		
		// create main window and insert the canvas into it
		JFrame frame = new JFrame("OpenGL Fenster");
		frame.setSize(width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(canvas);
		
		// show the main window
		frame.setVisible(true);
		
		// set input focus to the canvas
		canvas.setFocusable(true);
		canvas.requestFocus();
		
	}

	
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		
		// background color = dark grey
		gl.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
		
		// enable depthtest (default is off)
		gl.glEnable(GL2.GL_DEPTH_TEST);
		
		//For light calculation
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_NORMALIZE);
		initLights(gl);
	
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
		
		//Enable fog
		gl.glEnable(GL2.GL_FOG);
		
		
		//Random parameter calculation
		Random r = new Random();
		//amount of rectangles (min:5, max:25)
		int amount = 5 + (int) (r.nextFloat() * 20);
		randomParameters = new float[amount * 4];	//each rect has 4 parameters
		System.out.println("Amount="+amount);
		
		//Calculate the 4 parameters for random position & size (used later)	
		for (int i = 0; i < amount * 4; i += 4) {
			//get random position in the area [-10,-10]x[10,10]
			randomParameters[i] = -10 + r.nextFloat() * 20;
			randomParameters[i+1] = -10 + r.nextFloat() * 20;
			
			//get random size of the rectangle
			randomParameters[i+2] = 0.5f + r.nextFloat() * 1.5f;
			randomParameters[i+3] = 0.5f + r.nextFloat() * 1.5f;
		}
		
		System.out.println(Arrays.toString(randomParameters));
	}
	
	
	public void display(GLAutoDrawable drawable) {

		GL2 gl = drawable.getGL().getGL2();
	
		// clear the framebuffer
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		// place object into the scene
		gl.glLoadIdentity();
		
		
		glu.gluLookAt(	camera_position[0], camera_position[1], camera_position[2],
						center_position[0], center_position[1]+yzIncr, center_position[2],
						camera_orientation[0], camera_orientation[1], camera_orientation[2]);

		
		//for help purposes
		//drawCoordinateLines(gl);
		
		
		//rotate the scene
		gl.glRotatef(scene_rotx, 1, 0, 0);
		gl.glRotatef(scene_roty, 0, 1, 0);
		
		
		//Ground
		gl.glPushMatrix();
			gl.glTranslatef(0.0f, 0.0f, 2.0f);
			drawGround(gl);
		gl.glPopMatrix();
			
		
		//Bridge over troubled water
		gl.glPushMatrix();
			gl.glTranslatef(-4, 0, 0);
			gl.glRotatef(90, 0, 1, 0);
			drawBridge(gl);
		gl.glPopMatrix();
		
		
		//TrafficLight
		gl.glPushMatrix();
			gl.glTranslatef(2, 0, -2);
			gl.glRotatef(-90, 1, 0, 0);		//stand upwards
			drawTrafficLight(gl);
		gl.glPopMatrix();
		
		
		//spread some rectangles
		gl.glPushMatrix();
			drawRects(gl, randomParameters);
		gl.glPopMatrix();
		
		
		//blending for transparent objects in the following block
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
			//Visualize light0 with a solid sphere
			visualize(gl);
		
		gl.glDisable(GL2.GL_BLEND);
		
		
		//set light position
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0_position, 0);
		
		
		//create the fog
		someFog(gl);
		
		//Finish
		gl.glFlush();
		
		//increase light rotation factor & update it's position
		lightrot += lightspeed;
		if (lightrot >= 360.0f)
			lightrot -= 360.0f;
		updateLightPosition();
	}
	
	
	/*
	 * When the window changes its position or shape the projection transformation
	 * and viewport dimensions have to be adjusted accordingly.
	 */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
		
		// set viewport to window dimensions
		gl.glViewport(0, 0, width, height);
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		
		// set perspective projection
		glu.gluPerspective(45.0f, (float) width / (float) height, 1.0f, 100.0f);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	
	/*
	 * Help method to draw coordinate lines
	 * x-axis is red
	 * y-axis is green
	 * z-axis is blue
	 */
	private void drawCoordinateLines(GL2 gl) {
		
		float min = -5.0f;
		float max = 5.0f;
		
		//thicker lines
		gl.glLineWidth(3.0f);
		
		gl.glColor3f(1.0f,0.0f,0.0f); 		//red
	    gl.glBegin(GL2.GL_LINES);
	    	//x-axis
	    	gl.glVertex3f(max, 0, 0);
	    	gl.glVertex3f(min, 0, 0);
	 
	    	// arrow
	    	gl.glVertex3f(max, 0, 0);
	    	gl.glVertex3f(max-1, 1, 0);
	    	gl.glVertex3f(max, 0, 0);
	    	gl.glVertex3f(max-1, -1, 0);
	    gl.glEnd();
	    	 
	  
	    gl.glColor3f(0.0f, 1.0f, 0.0f); 	//green
	    gl.glBegin(GL2.GL_LINES);
	    	//y-axis
	    	gl.glVertex3f(0, max, 0);
	    	gl.glVertex3f(0, min, 0);
	    	
	    	// arrow
	    	gl.glVertex3f(0, max, 0);
	    	gl.glVertex3f(1, max-1, 0);
	    	gl.glVertex3f(0, max, 0);
	    	gl.glVertex3f(-1, max-1, 0);
	    	
	    gl.glEnd();
	 
	 
	    gl.glColor3f(0.0f, 0.0f, 1.0f); 	//blue
	    gl.glBegin(GL2.GL_LINES);
	    	//z-axis
	    	gl.glVertex3f(0, 0, max);
	    	gl.glVertex3f(0, 0, min);
	    	
	    	// arrow
	    	gl.glVertex3f(0, 0, max);
	    	gl.glVertex3f(1, 0, max-1);
	    	gl.glVertex3f(0, 0, max);
	    	gl.glVertex3f(-1, 0, max-1);
	    	
	    gl.glEnd();
	    //reset line width
	    gl.glLineWidth(1.0f);
	}
	
	
	
	/*
	 * Init all the needed light sources.
	 */
	private void initLights(GL2 gl) {
		
		//Enable the light(s)
		gl.glEnable(GL2.GL_LIGHT0);
		
		//Init Light0
		//	Ambient, diffuse, specular and emissive part of the light
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, light0_ambient, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light0_diffuse, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, light0_specular, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_EMISSION, light0_emissive, 0);
		
		//	Position of the light
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0_position, 0);
	}
	
	
	/*
	 *  Draw a grid ground
	 */
	public void drawGround(GL2 gl) {

		//Grid ground
		float 	x1 = -1000.f, x2 = 1000.f, z1 = -1000.f, z2 = 1000.f;
		float width = 0.5f;
		
		//Draw black grid
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glBegin(GL2.GL_LINES);
			gl.glColor3f(0.f, 0.f, 0.f);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);	//Normal
			
			//Draw parallel to the z-axis
			for (float x = x1; x <= x2; x += width) {
				gl.glVertex3f(x, 0.f, z1);
				gl.glVertex3f(x, 0.f, z2);
			}
			
			//Draw parallel to the x-axis
			for (float z = z1; z <= z2; z += width) {
				gl.glVertex3f(x1, 0.f, z);
				gl.glVertex3f(x2, 0.f, z);
			}
		gl.glEnd();
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
		
	}

	
	/*
	 * Draw a cube with side length 1 around the point of origin (0,0,0).
	 */
	public void drawCube(GL2 gl) {

		//constants for horizontal (x) constraints
		final float LEFT_X = -0.5f, RIGHT_X = 0.5f;
		//constants for vertical (y) constraints
		final float UPPER_Y = 0.5f, LOWER_Y = -0.5f;
		//constants for depth (z) constraints
		final float FRONT_Z = 0.5f, BACK_Z = -0.5f;

		//material for the cube: jade
		float ambient[] = {0.14f, 0.16f, 0.16f, 0.9f};
		float diffuse[] = {0.54f, 0.89f, 0.63f, 0.9f};
		float specular[] = {0.32f, 0.32f, 0.32f, 0.32f};
		float shininess = 12.8f;
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambient, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuse, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specular, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, shininess);
		
		
		//draw cube with normal vectors
		gl.glBegin(GL2.GL_QUADS);
			//front
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);

			//left side
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);

			//right side
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);

			//back side
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);

			//top side
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);

			//bottom side
			gl.glNormal3f(0.0f, 0.0f, -1.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 0.0f, -1.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, -1.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, -1.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);
		gl.glEnd();	//Done drawing

	}
	
	
	
	/*
	 * Draw a traffic light. The base is at origin, standing in positive
	 * y direction (upvector).
	 */
	private void drawTrafficLight(GL2 gl) {
		
		//Material: dark green plastic
		float amb[]={0.0f, 0.11f, 0.0f, 1.0f};
		float diff[]={0.05f, 0.3f, 0.05f, 1.0f};
		float spec[]={0.4f, 0.55f, 0.4f, 1.0f};
		float shine = 80f;
		gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, amb, 0);
		gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, diff, 0);
		gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, spec, 0);
		gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shine);
		
		//base of traffic light
		glut.glutSolidCylinder(0.4f, 0.1f, 100, 100);
		//stake of traffic light
		glut.glutSolidCylinder(0.075f, 2.0f, 100, 100);
		
		
		//top of traffic light => 3 stacked cubes (colored)		
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glPushMatrix();
			//1st cube (green)
			gl.glTranslatef(0.0f, 0.0f, 2.0f);		//set on top of stake
			//gl.glScalef(0.5f, 0.5f, 1.0f);
			gl.glColor3f(0, 1, 0);
			glut.glutSolidCube(0.5f);
			
			//2nd cube (yellow)
			gl.glTranslatef(0.0f, 0.0f, 0.5f);		//stack on top of last cube
			gl.glColor3f(1, 1, 0);
			glut.glutSolidCube(0.5f);				
			
			//3rd cube (red)
			gl.glTranslatef(0.0f, 0.0f, 0.5f);		//stack on top of last cube
			gl.glColor3f(1, 0, 0);
			glut.glutSolidCube(0.5f);	
		gl.glPopMatrix();
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
	}
	
	
	
	/*
	 * Draw a bridge starting at origin in direction of z-axis.
	 * The width (along x-axis) is default set to 2 (from x=-1 to x=1).
	 * The length (along z-axis) is default set to 2 (from z=0 to z=2).
	 */
	private void drawBridge(GL2 gl) {
		
		//Controll points for Bezier-Grid
		float[] ctrlPoints = {
				//First line segment
				-1.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				//Second line segment
				-1.0f, 1.0f, 1.0f,
				0.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				//Third line segment
				-1.0f, 0.0f, 2.0f,
				0.0f, 0.0f, 2.0f,
				1.0f, 0.0f, 2.0f,
		};
		
		//Transform into FloatBuffer
		FloatBuffer buffer = Buffers.newDirectFloatBuffer(ctrlPoints);
		
		//Activate & set 2D-Evaluator for creation of the Bezier-Grid
		gl.glEnable(GL2.GL_MAP2_VERTEX_3);
		gl.glMap2f(GL2.GL_MAP2_VERTEX_3, 0.0f, 1.0f, 3, 3, 0.0f, 1.0f, 9, 3, buffer);

		
		
		//Create two-dimensional point-grid & fill it
		int n = 20;
		gl.glMapGrid2f(n, 0.0f, 1.0f, n, 0.0f, 1.0f);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glColor3f(0.55f, 0.0f, 0.0f);
		gl.glEvalMesh2(GL2.GL_FILL, 0, n, 0, n);
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
	}
	
	
	
	/*
	 * Draw a random amount of randomly distributed, random size rectangles,
	 * which are orthogonal to the ground. The parameter randoms[] defines
	 * all the randomized values.
	 */
	private void drawRects(GL2 gl, float[] randoms) {
		
		//draw a random orthogonal rectangle
		for (int i = 0; i < randoms.length; i += 4) {
			gl.glPushMatrix();
				//translate to random pos
				gl.glTranslatef(randoms[i], 0, randoms[i+1]);
				//scale to random size
				gl.glScalef(randoms[i+2], randoms[i+3], 0);
				drawOrthQuad(gl);
			gl.glPopMatrix();
		}
				
	}
	
	
	/*
	 * Draw a quad orthogonal to the ground.
	 */
	private void drawOrthQuad(GL2 gl) {
		
		float p[] = {
			1.0f, 0.0f, 0.0f,
			1.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,
			0.0f, 0.0f, 0.0f
		};
		
		gl.glBegin(GL2.GL_QUADS);
			gl.glVertex3fv(p, 0);
			gl.glVertex3fv(p, 3);
			gl.glVertex3fv(p, 6);
			gl.glVertex3fv(p, 9);
		gl.glEnd();
	}
	
	
	/*
	 * Renders some fog in the scene.
	 */
	private void someFog(GL2 gl) {
		
		//fog parameters: color, start & end (only lin. fog), density (only exp. fog)
		float col[] = {0.9f, 0.9f, 0.9f, 1.0f};
		float start = 7.0f;
		float end = 15.0f;
		float density = 0.1f;
		
		//set fog color
		gl.glFogfv(GL2.GL_FOG_COLOR, col, 0);
		
		//set start and end of linear fog
		gl.glFogf(GL2.GL_FOG_START, start);
		gl.glFogf(GL2.GL_FOG_END, end);
		
		//set density of exponential fog
		gl.glFogf(GL2.GL_FOG_DENSITY, density);
		
		//choose fog mode: GL_LINEAR/EXP/EXP2
		gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);
		
	}
	
	
	/*
	 * Visualize the light source GL2.GL_LIGHT0 with a sphere ("sun"). 
	 */
	private void visualize(GL2 gl) {
		
		//Glut Solid Sphere to visualize the light source (light0)
		float sphere_radius = 0.3f;
		int sphere_slices = 100;
		int sphere_stacks = 100;
		
		
		//Material for the sphere
		float ambAndDiff[] = {1.0f, 1.0f, 0.65f, 0.7f};
		
		
		gl.glPushMatrix();
			//translation of the sphere to light0's position
			gl.glTranslatef(light0_position[0], light0_position[1], light0_position[2]);
			
			//set the material
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, ambAndDiff, 0);
			
			//draw the sphere
			glut.glutSolidSphere(sphere_radius, sphere_slices, sphere_stacks);
			
		gl.glPopMatrix();
	}
	
	/*
	 * Calculates the new center position (which is looked at) and sets it.
	 * Needed for the movement through the scene.
	 */
	private void updateCenterPosition() {
		
		//Calculate the radius
		float viewdir[] = new float[3];
		VectorUtil.subVec3(viewdir, center_position, camera_position);
		float radius = VectorUtil.normVec3(viewdir);
		
		//Calculate the angles
		double phi = xzIncr * Math.PI / 180.0f;
		//double theta = yzIncr * Math.PI / 180.0f;
		
		//Update the center position
		center_position[0] = camera_position[0];
		center_position[0] += radius * (float)Math.sin(phi);
		center_position[2] = camera_position[2];
		center_position[2] += radius * (float)Math.cos(phi);
	}
	
	
	
	/*
	 * Update the light position according to the rotation
	 */
	private void updateLightPosition() {
		//rotation angle for xy-plane
		double angle;
		
		//mirror the light at yz-plane
		if (lightrot >= 80 && lightrot <= 280)
			lightrot = 280;
			
		angle = lightrot * Math.PI / 180.0f;
		
		//set light0's new positions
		light0_position[1] = cube_center[1] + lightradius * (float)Math.cos(angle);
		light0_position[0] = cube_center[0] + lightradius * (float)Math.sin(angle);
	}
	
	
	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		
		// move backward
		if (keyCode == KeyEvent.VK_DOWN) {
			//get the moving direction (= from center_pos to camera_pos)
			float movdir[] = new float[3];
			VectorUtil.subVec3(movdir, camera_position, center_position);
			VectorUtil.normalizeVec3(movdir);
			
			//calculate how far to move
			VectorUtil.scaleVec3(movdir, movdir, speed);
			
			//move camera & center
			VectorUtil.addVec3(camera_position, camera_position, movdir);
			VectorUtil.addVec3(center_position, center_position, movdir);
		}
		
		//move forward 
		else if (keyCode == KeyEvent.VK_UP) {
			//get the moving direction (= from camera_pos to center_pos)
			float movdir[] = new float[3];
			VectorUtil.subVec3(movdir, center_position, camera_position);
			VectorUtil.normalizeVec3(movdir);
			
			//calculate how far to move
			VectorUtil.scaleVec3(movdir, movdir, speed);
			
			//move camera & center
			VectorUtil.addVec3(camera_position, camera_position, movdir);
			VectorUtil.addVec3(center_position, center_position, movdir);
		}
		
		// move left
		else if (keyCode == KeyEvent.VK_LEFT) {
			//get the viewing direction (= from camera_pos to center_pos)
			float viewdir[] = new float[3];
			float movdir[] = new float[3];
			VectorUtil.subVec3(viewdir, center_position, camera_position);
			VectorUtil.normalizeVec3(viewdir);
			
			//calculate moving direction with crossproduct of viewdir and v
			//right-hand-rule!
			float v[] = {0.0f, 1.0f, 0.0f};
			VectorUtil.crossVec3(movdir, v, viewdir);
			VectorUtil.normalizeVec3(movdir);
			
			//calculate how far to move
			VectorUtil.scaleVec3(movdir, movdir, speed);
			
			//move camera & center
			VectorUtil.addVec3(camera_position, camera_position, movdir);
			VectorUtil.addVec3(center_position, center_position, movdir);
		}
		
		// move right
		else if (keyCode == KeyEvent.VK_RIGHT) {
			//get the viewing direction (= from camera_pos to center_pos)
			float viewdir[] = new float[3];
			float movdir[] = new float[3];
			VectorUtil.subVec3(viewdir, center_position, camera_position);
			VectorUtil.normalizeVec3(viewdir);
			
			//calculate moving direction with crossproduct of viewdir and v
			//right-hand-rule!
			float v[] = {0.0f, 1.0f, 0.0f};
			VectorUtil.crossVec3(movdir, viewdir, v);
			VectorUtil.normalizeVec3(movdir);
			
			//calculate how far to move
			VectorUtil.scaleVec3(movdir, movdir, speed);
			
			//move camera & center
			VectorUtil.addVec3(camera_position, camera_position, movdir);
			VectorUtil.addVec3(center_position, center_position, movdir);
		}
		
		// move up
		else if (keyCode == KeyEvent.VK_PAGE_UP) {
			camera_position[1] += speed;
			center_position[1] += speed;
		}
		
		// move down
		else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
			camera_position[1] -= speed;
			center_position[1] -= speed;
		}
		
		// turn left
		else if (keyCode == KeyEvent.VK_A) {
			//rotate by rot_speed degrees to the left
			//update left-right rotation offset
			xzIncr += rot_speed;
			if (xzIncr >= 360.0f) xzIncr -= 360.0f;
			
			//calculate new center position
			updateCenterPosition();
		}
		
		// turn right
		else if (keyCode == KeyEvent.VK_D) {
			//rotate by rot_speed degrees to the right
			//update the left-right rotation offset
			xzIncr -= rot_speed;
			if (xzIncr <= -360.0f) xzIncr += 360.0f;
			
			//calculate new center position
			updateCenterPosition();
		}
		
		// turn up
		else if (keyCode == KeyEvent.VK_W) {
			//rotate by updown_speed degrees up
			//update the up-down rotation offset
			if (yzIncr <= MAXUP - updown_speed)
				yzIncr += updown_speed;

			//calculate new center position
			updateCenterPosition();
		}
		
		// turn down
		else if (keyCode == KeyEvent.VK_S) {
			//rotate by updown_speed degrees down
			//update the up-down rotation offset
			if (yzIncr >= MAXDOWN + updown_speed)
				yzIncr -= updown_speed;

			//calculate new center position
			updateCenterPosition();
		}
		
		//toggle ground
		else if (keyCode == KeyEvent.VK_B) {
			
		}
		
		// reset the camera position and orientation
		else if (keyCode == KeyEvent.VK_C) {
			camera_position[0] = 0.0f;
			camera_position[1] = 1.0f;
			camera_position[2] = -10.0f;
			
			center_position[0] = 0.0f;
			center_position[1] = 1.0f;
			center_position[2] = 0.0f;
			
			camera_orientation[0] = 0.0f;
			camera_orientation[1] = 1.0f;
			camera_orientation[2] = 0.0f;
			
			xzIncr = 0.0f;
			yzIncr = 0.0f;
		}
		
		//move light0 up
		else if (keyCode == KeyEvent.VK_I) {
			//increment lightradius if in bounds
			if (lightradius <= LIGHTMAXUP + lightspeed)
				lightradius += lightspeed;
			
			updateLightPosition();
		}
		
		//move light0 left - disabled 
		else if (keyCode == KeyEvent.VK_J) {
			//lightrot += lightspeed;
			//updateLightPosition();
		}
		
		//move light0 down
		else if (keyCode == KeyEvent.VK_K) {
			//decrement lightradius if in bounds
			if (lightradius >= LIGHTMAXDOWN - lightspeed)
				lightradius -= lightspeed;
			
			updateLightPosition();
		}
		
		//move light0 right - disabled
		else if (keyCode == KeyEvent.VK_L) {
			//lightrot -= lightspeed;
			//updateLightPosition();
		}
		
		//reset light to default position
		else if (keyCode == KeyEvent.VK_X) {
			light0_position[0] = 0.0f;
			light0_position[1] = 6.0f;
			light0_position[2] = 0.0f;
			
			lightradius = light0_position[1] - cube_center[1];
			lightrot = 0.0f;
		}
	}	
	
	@Override
	public void keyReleased(KeyEvent arg0) {
		int keyCode = arg0.getKeyCode();
		
		if (keyCode == KeyEvent.VK_DOWN) {
		}
	}
	
	@Override
	public void keyTyped(KeyEvent arg1) {
		int keyCo = arg1.getKeyCode();
		if (keyCo == KeyEvent.VK_A) {
		}
		
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		 
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		//only do scene rotation if shift is down
		if (e.isShiftDown()) {
			
			//save old mouse coordinates
			int oldMouseX = mouseX;
			int oldMouseY = mouseY;
			
			//Get mouse coordinates
			mouseX = e.getX();
			mouseY = e.getY();
			
			int diff = mouseX - oldMouseX;
			if (diff > 0) 						//Mouse moved to the right
				scene_roty += scene_rotIncr;	//Rotate scene to the right (around y-axis)
			else if (diff < 0)					//Mouse moved to the left
				scene_roty -= scene_rotIncr;	//Rotate scene to the left (around y-axis)
			
			diff = mouseY - oldMouseY;
			if (diff > 0)						//Mouse moved up
				scene_rotx -= scene_rotIncr;	//Rotate scene up (around x-axis)
			else if (diff < 0)					//Mouse moved down
				scene_rotx += scene_rotIncr;	//Rotate scene down (around x-axis)
		}
	}
}
