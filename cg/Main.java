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

import javax.swing.JFrame;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

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
*/

public class Main implements GLEventListener, KeyListener, MouseListener, MouseMotionListener  {

	// create an instance of GL Utility library and keep it
	private static final GLU glu = new GLU();
	
	//Camera variables
	private static float camera_position[] = {0.0f, 1.0f, -10.0f};
	private static float center_position[] = {0.0f, 1.0f, 0.0f};
	private static float camera_orientation[] = {0.0f, 1.0f, 0.0f};
	
	// speed for camera movement
	private static float speed = 0.2f;
	// camera angle increment in degrees
	private static float angle_incr = 1.0f;
	private static float rotx = 0.0f, roty = 0.0f;
	
	//Cube rotation
	private static float cube_incr = 3.0f;
	private static float cube_rotx = 0.0f, cube_roty = 0.0f, cube_rotz = 0.0f;
	private static int mouseX = 0, mouseY = 0;
	
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
	    Label label = new Label("Press shift and drag the mouse to move the cube");
	    
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
			//System.out.println("time="+timeDiff+", fps="+fps);

			//Update variables
			lastTime = currentTime;
			frames = 0;
			
		}
		
		
		
		// clear the framebuffer
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		// place object into the scene
		gl.glLoadIdentity();
		
		gl.glRotatef(rotx, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(roty, 0.0f, 1.0f, 0.0f);
		
		glu.gluLookAt(	camera_position[0], camera_position[1], camera_position[2],
						center_position[0], center_position[1], center_position[2],
						camera_orientation[0], camera_orientation[1], camera_orientation[2]);
		
		//Ground
		gl.glPushMatrix();
			gl.glTranslatef(0.0f, 0.0f, -2.0f);
			drawGround(gl);
		gl.glPopMatrix();
	 
		//Cube
		gl.glPushMatrix();
			gl.glTranslatef(0.0f, 1.0f, -2.0f);
			gl.glRotatef(cube_rotx, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(cube_roty, 0.0f, 1.0f, 0.0f);
			gl.glRotatef(cube_rotz, 0.0f, 0.0f, 1.0f);
			drawCube(gl);
		gl.glPopMatrix();
		
		
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
	
	/**
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
		glu.gluPerspective(45.0f, (float) width / (float) height, 1.0f, 20.0f);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}


	
	// draw a ground
	void drawGround(GL2 gl) {
		
		if (!toggleGround) {
			gl.glBegin(GL2.GL_QUADS);
				gl.glColor3f(1.0f, 0.0f, 0.0f);		//Red
				gl.glVertex3f(-5.0f, 0.0f, 5.0f);
				gl.glColor3f(1.0f, 0.5f, 0.0f);		//Orange
				gl.glVertex3f( 5.0f, 0.0f, 5.0f);
				gl.glColor3f(0.5f, 0.0f, 0.5f);		//Purple
				gl.glVertex3f( 5.0f, 0.0f,-5.0f);
				gl.glColor3f(0.0f, 1.0f, 0.5f);		//Green
				gl.glVertex3f(-5.0f, 0.0f,-5.0f);
			gl.glEnd();
		} else {
			float 	x1 = -1000.f, x2 = 1000.f, z1 = -1000.f, z2 = 1000.f;
			float width = 0.5f;
			
			//Draw grid
			gl.glBegin(GL2.GL_LINES);
				gl.glColor3f(0.f, 0.f, 0.f);
				
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
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		
		// move backward
		if (keyCode == KeyEvent.VK_DOWN) {
			camera_position[2] -= speed;
			center_position[2] -= speed;
		}
		
		else if (keyCode == KeyEvent.VK_UP) {
			camera_position[2] += speed;
			center_position[2] += speed;
		}
		
		// move left
		else if (keyCode == KeyEvent.VK_LEFT) {
			camera_position[0] += speed;
			center_position[0] += speed;
		}
		
		// move right
		else if (keyCode == KeyEvent.VK_RIGHT) {
			camera_position[0] -= speed;
			center_position[0] -= speed;
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
			System.out.print("left: ");
			roty -= angle_incr;
			System.out.println(roty);
			if (roty < -360.0f) roty += 360.0f;
		}
		
		// turn right
		else if (keyCode == KeyEvent.VK_D) {
			roty += angle_incr;
			if (roty > 360.0f) roty -= 360.0f;
		}
		
		// turn up
		else if (keyCode == KeyEvent.VK_W) {
			rotx -= angle_incr;
			if (rotx < -360.0f) rotx += 360.0f;
		}
		
		// turn down
		else if (keyCode == KeyEvent.VK_S) {
			rotx += angle_incr;
			if (rotx > 360.0f) rotx -= 360.0f;
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
			
			rotx = 0.0f;
			roty = 0.0f;
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
