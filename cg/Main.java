
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;


public class Main implements GLEventListener, KeyListener {


	/*
	 * Global Constants & Variables
	 */
	//Constants for framesize
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	//GLU object
	private GLU glu = new GLU();


	//Variables for rotation of cube
	private float rotx = 0.0f, roty = 0.0f;


	//Constants for the background color of the window (2.)
	//Name			Hex		R		G		B
	//DodgerBlue	1E90FF	0.118	0.565	1.000
	private static final float
		BACK_R = 0.118f, BACK_G = 0.565f, BACK_B = 1.0f, BACK_A = 1.0f;


	//Variable for time management
	long timerstart;




	/*
	 * Overwritten Methods from the interface GLEventListener
	 */

	@Override
	public void init(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub
		GL2 gl = drawable.getGL().getGL2();

		//Set the background color according to the in 'Constants'
		//defined values in RGBA format
		gl.glClearColor(BACK_R, BACK_G, BACK_B, BACK_A);

		//enable depth test
		gl.glEnable(GL2.GL_DEPTH_TEST);

		//set the timer start
		timerstart = System.currentTimeMillis();
	}


	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}


	@Override
	public void display(GLAutoDrawable drawable) {

		GL2 gl = drawable.getGL().getGL2();

		//Clear color and depth buffer
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);



		//for the 1st cube
		gl.glLoadIdentity();
		gl.glPushMatrix();		//push matrix on the stack

		//Translation into the depth (neg. Z-axis)
		gl.glTranslatef( 0.0f, 0.0f, -3.0f);

		//Rotate with angle defined by rotx or roty,
		//rotate around x-axis and y-axis simultaneously
		gl.glRotatef(rotx, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(roty, 0.0f, 1.0f, 0.0f);

		//draw it
		drawCube(gl);
		gl.glPopMatrix();		//pop matrix from the stack


		//for the 2nd cube
		//only if runtime is between 5-10s, 15-20s, 25-30s,...
		long runtime = System.currentTimeMillis() - timerstart;
		if  (runtime / 1000.0 >= 5) {

			gl.glLoadIdentity();	//reset matrix
			gl.glPushMatrix();		//push matrix on the stack

			//Translation to a point behind the 1st cube
			gl.glTranslatef( 3.0f, -1.5f, -7.0f);

			//Rotate with angle defined by rotx or roty, rotate around
			//x-axis and y-axis simultaneously (rotx / 2 -> slower)
			gl.glRotatef(rotx / 2, 1.0f, -1.0f, 0.0f);

			//scale it smaller
			gl.glScaled(0.5, 0.5, 0.5);

			//draw it
			drawCube(gl);
			gl.glPopMatrix();
		}

		//reset timerstart if >= 10s
		if (runtime / 1000.0 >= 10)
			timerstart = System.currentTimeMillis();

		//finish
		gl.glFlush();


		//Increment rotx and roty
		rotx += 0.5f;
		roty += 0.6f;

	}


	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {

		final GL2 gl = drawable.getGL().getGL2();

		//Aspect ratio of perspective frame
		final float h = (float) width / (float) height;

		gl.glViewport(0, 0, width, height);		//viewport size
		gl.glMatrixMode(GL2.GL_PROJECTION);		//use projection matrix
		gl.glLoadIdentity();					//reset projection matrix

		//set the camera parameter:
		//(Y-axis), aspect ratio, near & far clipping plane
		glu.gluPerspective(45.0f, h, 1.0f, 20.0f);

		gl.glMatrixMode(GL2.GL_MODELVIEW);		//use modelview matrix
		gl.glLoadIdentity();					//reset modelview matrix
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


		//draw cube
		gl.glBegin(GL2.GL_QUADS);
			//front
			gl.glColor3f(0, 0, 0);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);

			//left side
			gl.glColor3f(1, 0, 0);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);

			//right side
			gl.glColor3f(0, 1, 0);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);

			//back side
			gl.glColor3f(0, 0, 1);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);

			//top side
			gl.glColor3f(0, 1, 1);
			gl.glVertex3f(LEFT_X, UPPER_Y, FRONT_Z);
			gl.glVertex3f(LEFT_X, UPPER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, UPPER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, UPPER_Y, FRONT_Z);

			//bottom side
			gl.glColor3f(1, 0, 1);
			gl.glVertex3f(LEFT_X, LOWER_Y, FRONT_Z);
			gl.glVertex3f(LEFT_X, LOWER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, LOWER_Y, BACK_Z);
			gl.glVertex3f(RIGHT_X, LOWER_Y, FRONT_Z);
		gl.glEnd();	//Done drawing

	}



	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	/*
	 * Main Method
	 */
	public static void main(String[] args) {

		//instantiate OpenGL functionality
		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		GLCapabilities gcaps = new GLCapabilities(profile);

		//create glcanvas
		final GLCanvas glcanvas = new GLCanvas(gcaps);

		//construct object Main
		Main m = new Main();
		glcanvas.addGLEventListener(m);

		//create frame & add GLCanvas
		final JFrame frame = new JFrame("OpenGL");
		frame.getContentPane().add(glcanvas);

		//set frame properties & make it visible
		frame.setSize(WIDTH, HEIGHT);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		//instantiate FPSAnimator for animation support
		//Params: GLAutoDrawable object, int fps, boolean schedule at fixed rate
		final FPSAnimator animator = new FPSAnimator( glcanvas, 300, true);
		animator.start();	//start animation
	}




}
