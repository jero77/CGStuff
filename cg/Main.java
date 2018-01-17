import static javax.swing.JFrame.EXIT_ON_CLOSE;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Label;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import javax.swing.JFrame;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
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

b - toggle ground
capslock - toggle mouse rotation (capslock on -> mouse rotation off)

Mouse rotation (mouse move) of the cube only when shift is down
Mouse rotation (mouse dragged) of cube only when control is down


i - move light up
j - move light left
k - move light down
l - move light right
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
	//camera angle increment in degrees
	private static float rot_speed = 1.0f;
	//left-right rotation
	private static float xzIncr = 0;
	//up-down rotation
	private static float yzIncr = 0;
	
	
	//Text field
	private static TextField tf;
	
	//Boolean flag for toggling
	private static boolean toggleGround = false;
	
	//Renderer
	private static TextRenderer renderer; 
	private static boolean rendererActivated = false;
	private static int rendererX, rendererY;
	private static int canvasHeight;
	
	//FPS calculation
	private static long initialTime;
	private static long lastTime;
	private static int frames, fps;
	
	
	//Light calculation - variables for properties
	//	First light source: light0 (spotlight directed to center of cube)
	//	Position: w=0->directed light source (position=direction) 
	private static float light0_ambient[] = {0.2f, 0.2f, 0.2f, 1.0f};		//RGBA
	private static float light0_diffuse[] = {1.0f, 1.0f, 1.0f, 1.0f};		//RGBA
	private static float light0_specular[] = {0.75f, 0.75f, 0.75f, 1.0f};	//RGBA
	private static float light0_emissive[] = {0.8f, 0.8f, 0.8f, 1.0f};		//RGBA
	private static float light0_position[] = {0.0f, 4.0f, 2.0f, 1.0f};		//XYZW (homogene Coord.)
	private static float light0_direction[] = {0.0f, 0.0f, 0.0f};			//XYZ
	private static float light0_cutoff = 25.0f;
	private static float light0_exponent = 2.0f;
	private static float lightspeed = 0.5f;		//Speed for cameramovement
	
	//Variables for cube
	//	Center of cube
	private static float cube_center[] = {0.0f, 0.75f, 2.0f};
	//	Cube rotation
	private static float cube_incr = 3.0f;
	private static float cube_rotx = 0.0f, cube_roty = 0.0f, cube_rotz = 0.0f;
	private static int mouseX = 0, mouseY = 0;
	
	
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
	    tf = new TextField(30);
	    Label label = new Label("Press shift & move the mouse or press ctrl"
	    					+ "& drag the mouse to move the cube");
	    
	    //Init renderer
	    Font font = new Font("TimesRoman", Font.BOLD, 16);
	    renderer = new TextRenderer(font);
		
		// create main window and insert the canvas into it
		JFrame frame = new JFrame("OpenGL Fenster");
		frame.setSize(width, height);
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.getContentPane().add(canvas);
		frame.add(tf, BorderLayout.SOUTH);
		frame.add(label, BorderLayout.NORTH);
		
		// show the main window
		frame.setVisible(true);
		
		// set input focus to the canvas
		canvas.setFocusable(true);
		canvas.requestFocus();
		
		//init fps calculation
		initialTime = System.currentTimeMillis();
		lastTime = initialTime;
		
		//Needed for Renderer drawing point
		canvasHeight = canvas.getHeight();
	}

	
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		
		// background color
		gl.glClearColor(0.1f, 0.5f, 0.5f, 1.0f);
		
		// enable backface culling (default is off)
		//gl.glEnable(GL2.GL_CULL_FACE);
		
		// enable z-buffer (default is off)
		gl.glEnable(GL2.GL_DEPTH_TEST);
		
		//For light calculation
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_NORMALIZE);
		initLights(gl);
		
		//For materials
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
	}
	
	
	public void display(GLAutoDrawable drawable) {

		GL2 gl = drawable.getGL().getGL2();
		
		
		//Calculate time since start of simulation
		long currentTime = System.currentTimeMillis();
		float timeDiff = (currentTime - initialTime) / 1000.f;
		
		frames++;
		
		//Measure FPS once per sec
		if (currentTime - lastTime > 1000) {
		
			fps = (int) (frames / ((currentTime - lastTime) / 1000.f));

			//Update variables
			lastTime = currentTime;
			frames = 0;
			
		}
		
		
		
		// clear the framebuffer
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		// place object into the scene
		gl.glLoadIdentity();
		
		glu.gluLookAt(	camera_position[0], camera_position[1], camera_position[2],
						center_position[0], center_position[1], center_position[2],
						camera_orientation[0], camera_orientation[1], camera_orientation[2]);

		
		//Ground
		gl.glPushMatrix();
			gl.glTranslatef(0.0f, 0.0f, 2.0f);
			drawGround(gl);
		gl.glPopMatrix();
	 
		//Cube (Material Ruby, blending -> ruby is transparent)
		gl.glPushMatrix();
			gl.glTranslatef(cube_center[0], cube_center[1], cube_center[2]);
			gl.glRotatef(cube_rotx, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(cube_roty, 0.0f, 1.0f, 0.0f);
			gl.glRotatef(cube_rotz, 0.0f, 0.0f, 1.0f);
			drawCube(gl);
		gl.glPopMatrix();
		
		
		//Visualize light0 with a solid cone (also updates light0's pos. & dir.)
		visualize(gl);
		
		
		//Renderer activated? Then draw fps on screen
		if (rendererActivated) {
			renderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
			renderer.setColor(1.0f, 0.0f, 0.0f, 0.8f);
			renderer.draw("FPS: "+fps, rendererX, rendererY);
			renderer.endRendering();
		}
		
		
		//Finish
		gl.glFlush();
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
	 * Init all the needed light sources.
	 */
	private void initLights(GL2 gl) {
		
		//Enable the light(s)
		gl.glEnable(GL2.GL_LIGHT0);
		
		//Init Light0
		//Ambient, diffuse, specular and emissive part of the light
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, light0_ambient, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light0_diffuse, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, light0_specular, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_EMISSION, light0_emissive, 0);
		
		
		//Position of the light
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0_position, 0);
		
		//Cutoff and exponent of the light
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, light0_cutoff);
		gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_EXPONENT, light0_exponent);
		
		//Direction of the light: towards the center of the cube
		getLight0Dir();
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPOT_DIRECTION, light0_direction, 0);
	}
	
	
	/*
	 *  draw a ground
	 */
	public void drawGround(GL2 gl) {

		//Material for ground: chrome
		float ambient[] = {0.25f, 0.25f, 0.25f, 1.0f};
		float diffuse[] = {0.4f, 0.4f, 0.4f, 1.0f};
		float specular[] = {0.77f, 0.77f, 0.77f, 1.0f};
		float shininess = 76.8f;
		
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambient, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuse, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specular, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, shininess);
		
		
		if (!toggleGround) {
			gl.glBegin(GL2.GL_QUADS);
				gl.glNormal3f(0.0f, 1.0f, 0.0f);	//Normal of the ground
				gl.glVertex3f(-5.0f, 0.0f, 5.0f);
				gl.glVertex3f( 5.0f, 0.0f, 5.0f);
				gl.glVertex3f( 5.0f, 0.0f,-5.0f);
				gl.glVertex3f(-5.0f, 0.0f,-5.0f);
			gl.glEnd();
		} else {
			
			//Grid ground
			float 	x1 = -1000.f, x2 = 1000.f, z1 = -1000.f, z2 = 1000.f;
			float width = 0.5f;
			
			//Draw grid
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
		}
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
			gl.glColor3f(0, 0, 0);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);

			//left side
			gl.glColor3f(1, 0, 0);
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);

			//right side
			gl.glColor3f(0, 1, 0);
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);

			//back side
			gl.glColor3f(0, 0, 1);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);

			//top side
			gl.glColor3f(0, 1, 1);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glNormal3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);

			//bottom side
			gl.glColor3f(1, 0, 1);
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
	 * Calculate the direction for light0 (from light0_position to center of the cube).
	 * Saves the calculated directions in the float array light0_direction.
	 */
	private void getLight0Dir() {
		//Calculate vector from the position of the light to the center of the cube
		//light0_direction = cube_center - light0_position
		for (int i = 0; i < 3; i++)
			light0_direction[i] = cube_center[i] - light0_position[i];
		VectorUtil.normalizeVec3(light0_direction);
	}
	
	
	/*
	 * Visualize the light source GL2.GL_LIGHT0 with a cone. The base of the cone
	 * is orthogonal to light0's direction. 
	 */
	private void visualize(GL2 gl) {
		
		//Glut Solid Cone (white) to visualize the light source (light0)
		float cone_radius = 0.4f;
		float cone_height = 1.0f;
		int cone_slices = 70;
		int cone_stacks = 70;
		
		
		//Material: something transparent
		float ambient[] = {0.0f, 0.0f, 0.0f, 0.0f};
		float diffuse[] = {0.4f, 0.4f, 0.4f, 0.4f};
		float specular[] = {0.3f, 0.3f, 0.3f, 0.4f};
		float shininess = 32.0f;
		
		
		gl.glPushMatrix();
		
			//update light0's position & direction
			getLight0Dir();
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0_position, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPOT_DIRECTION, light0_direction, 0);
			
			//translation to light0's position
			gl.glTranslatef(light0_position[0], light0_position[1], light0_position[2]);
			
			//rotate the cone so that its base area is orthogonal to light0_direction
			//"direction" of cone (from base to tip), normalized
			float cone_direction[] = {0.0f, 0.0f, 1.0f};
			
			//angle between the vectors = acos(dotproduct of both vectors) in rad!
			float angle = VectorUtil.angleVec3(cone_direction, light0_direction);
			angle = angle * 180.0f / (float) Math.PI;
			
			//cross product yields (orthogonal) rotation vector, normalized
			float[] rotVec = new float[3];
			VectorUtil.crossVec3(rotVec, cone_direction, light0_direction);
			VectorUtil.normalizeVec3(rotVec);
			
			//now rotate around the rotation vector
			gl.glRotatef(-angle, rotVec[0], rotVec[1], rotVec[2]);
			
			//set the material
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambient, 0);
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuse, 0);
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specular, 0);
			gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, shininess);
			
			//draw the cone
			glut.glutSolidCone(cone_radius, cone_height, cone_slices, cone_stacks);
		gl.glPopMatrix();
		
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
		
		//TODO adapt to sphere calculations (3D)
		
		// turn left
		else if (keyCode == KeyEvent.VK_A) {
			//rotate by rot_speed degrees to the left
			//get the radius of the rotation circle
			float viewdir[] = new float[3];
			VectorUtil.subVec3(viewdir, center_position, camera_position);
			float radius = VectorUtil.normVec3(viewdir);
			
			//update left-right rotation offset
			xzIncr += rot_speed;
			if (xzIncr > 360.0f) xzIncr -= 360.0f;
			float angle = xzIncr * (float) Math.PI / 180.0f;
			
			//calculate new center position
			center_position[0] = camera_position[0] + radius * (float) Math.sin(angle);
			center_position[2] = camera_position[2] + radius * (float) Math.cos(angle);
		}
		
		// turn right
		else if (keyCode == KeyEvent.VK_D) {
			//rotate by rot_speed degrees to the right
			//get the radius of the rotation circle
			float viewdir[] = new float[3];
			VectorUtil.subVec3(viewdir, center_position, camera_position);
			float radius = VectorUtil.normVec3(viewdir);
			
			//update the left-right rotation offset
			xzIncr -= rot_speed;
			if (xzIncr < -360.0f) xzIncr += 360.0f;
			float angle = xzIncr * (float) Math.PI / 180.0f;
			
			//calculate new center position
			center_position[0] = camera_position[0] + radius * (float) Math.sin(angle);
			center_position[2] = camera_position[2] + radius * (float) Math.cos(angle);
		}
		
		// turn up
		else if (keyCode == KeyEvent.VK_W) {
			//rotate by rot_speed degrees up
			//get the radius of the rotation circle
			float viewdir[] = new float[3];
			VectorUtil.subVec3(viewdir, center_position, camera_position);
			float radius = VectorUtil.normVec3(viewdir);
			
			//update the up-down rotation offset
			yzIncr += rot_speed;
			if (yzIncr > 360.0f) yzIncr -= 360.0f;
			float angle = yzIncr * (float) Math.PI / 180.0f;
			
			//calculate new center position
			center_position[1] = camera_position[1] + radius * (float) Math.sin(angle);
			center_position[2] = camera_position[2] + radius * (float) Math.cos(angle);
		}
		
		// turn down
		else if (keyCode == KeyEvent.VK_S) {
			//rotate by rot_speed degrees down
			//get the radius of the rotation circle
			float viewdir[] = new float[3];
			VectorUtil.subVec3(viewdir, center_position, camera_position);
			float radius = VectorUtil.normVec3(viewdir);
			
			//update the up-down rotation offset
			yzIncr -= rot_speed;
			if (yzIncr < -360.0f) yzIncr += 360.0f;
			float angle = yzIncr * (float) Math.PI / 180.0f;
			
			//calculate new center position
			center_position[1] = camera_position[1] + radius * (float) Math.sin(angle);
			center_position[2] = camera_position[2] + radius * (float) Math.cos(angle);
		}
		
		//toggle ground
		else if (keyCode == KeyEvent.VK_B) {
			toggleGround = !toggleGround;
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
			light0_position[1] += lightspeed;
		}
		
		//move light0 left 
		else if (keyCode == KeyEvent.VK_J) {
			
		}
		
		//move light0 down
		else if (keyCode == KeyEvent.VK_K) {
			light0_position[1] -= lightspeed;
		}
		
		//move light0 right
		else if (keyCode == KeyEvent.VK_L) {
	
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
		//Activate the renderer once, prints the fps at the location clicked at
		if (!rendererActivated) {
			rendererActivated = true;
			rendererX = e.getX();
			//Need to flip y coordinate (TextRenderer starts at lowerleft corner,
			//  Canvas starts at upperleft corner!)
			rendererY = canvasHeight - e.getY();
		}
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
		
		//only do cube rotation if ctrl is down & capslock is NOT on
		boolean capsOn = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
		if (e.isControlDown() && !capsOn) {
			
			//save old mouse coordinates
			int oldMouseX = mouseX;
			int oldMouseY = mouseY;
			
			//Get mouse coordinates
			mouseX = e.getX();
			mouseY = e.getY();
			
			int diff = mouseX - oldMouseX;
			if (diff > 0) 						//Mouse moved to the right
				cube_roty += cube_incr;			//Rotate cube to the right
			else if (diff < 0)					//Mouse moved to the left
				cube_roty -= cube_incr;			//Rotate cube to the left
			
			diff = mouseY - oldMouseY;
			if (diff > 0)						//Mouse moved up
				cube_rotx -= cube_incr;			//Rotate cube up
			else if (diff < 0)					//Mouse moved down
				cube_rotx += cube_incr;			//Rotate cube down
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		//only do cube rotation if shift is down & capslock is NOT on
		boolean capsOn = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
		if (e.isShiftDown() && ! capsOn) {
			
			//save old mouse coordinates
			int oldMouseX = mouseX;
			int oldMouseY = mouseY;
			
			//Get mouse coordinates
			mouseX = e.getX();
			mouseY = e.getY();
			
			int diff = mouseX - oldMouseX;
			if (diff > 0) 						//Mouse moved to the right
				cube_roty += cube_incr;			//Rotate cube to the right
			else if (diff < 0)					//Mouse moved to the left
				cube_roty -= cube_incr;			//Rotate cube to the left
			
			diff = mouseY - oldMouseY;
			if (diff > 0)						//Mouse moved up
				cube_rotx -= cube_incr;			//Rotate cube up
			else if (diff < 0)					//Mouse moved down
				cube_rotx += cube_incr;			//Rotate cube down
		}
	}
}
