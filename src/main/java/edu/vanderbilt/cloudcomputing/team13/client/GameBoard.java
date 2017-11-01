/**
 * Created by Killian on 10/31/17.
 */
package edu.vanderbilt.cloudcomputing.team13.client;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import sun.jvm.hotspot.HelloWorld;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;



public class GameBoard {
    // use this bi-directional interface to communicate with client
    GraphicInterface graphicInterface;

    // game specs
    private boolean isDrawer = true;

    // The window handle
    private long window;
    private int windowWidth = 800;
    private int windowHeight = 600;

    // isMouseClicked
    private boolean isMouseClicked = false;

    private double smoothThreshold = 1;

    // a set of points that the player has drawn
    private List<Double> drawnPoints = new ArrayList<>();

    public GameBoard(GraphicInterface graphicInterface){
        this.graphicInterface = graphicInterface;
    }

    public void run() {
        System.out.println("Game Board started!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(windowWidth, windowHeight, "Let's Draw And Guess!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Setup a cursor position callback, it will be called every time the cursor is moved
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if(isDrawer && isMouseClicked){
                addPointToDrawnList(xpos, ypos);
                reportDrawnPoint(xpos, ypos);
                //System.out.printf("%f, %f\n", xpos, ypos);
            }
        });

        // Setup a cursor button input callback, it will be called every time the cursor is clicked
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if(isDrawer){
                if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS){
                    isMouseClicked = true;
                }else if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE){
                    isMouseClicked = false;
                    drawnPoints.add(null);
                    drawnPoints.add(null);
                }
            }
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        glOrtho( 0, windowWidth, 0, windowHeight, -1, 1);

        // Set the clear color
        glClearColor( 1f, 1f, 1f, 1.0f);
        drawnPoints.clear();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            renderGameFrame();

            renderDrawing();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    public void addPointToDrawnList(double x, double y){
        if(drawnPoints.size() >= 2){
            Double prevX = drawnPoints.get(drawnPoints.size() - 2);
            Double prevY = drawnPoints.get(drawnPoints.size() - 1);
            if(prevX != null && prevY != null) addMakeupPoints(prevX, prevY, x, y);
        }
        drawnPoints.add(x);
        drawnPoints.add(y);
    }

    private void addMakeupPoints(double prevX, double prevY, double curX, double curY){
        int num = 0;
        double dist = Math.sqrt(Math.pow(Math.abs(prevX - curX),2) + Math.pow(Math.abs(prevY - curY), 2));
        if(dist > smoothThreshold){
            num = (int) Math.floor(dist / smoothThreshold);
        }
        if(num == 0) return;
        double diffX = (curX - prevX) / num;
        double diffY = (curY - prevY) / num;
        for(int i=1; i <= num -1; i++){
            drawnPoints.add(prevX + diffX * i);
            drawnPoints.add(prevY + diffY * i);
        }
    }

    private void reportDrawnPoint(double x, double y){
        graphicInterface.reportDrawnPoint(x, y);
    }

    private void renderOnePoint(double x, double y){
        double centerX = x;
        double centerY = windowHeight - y;
        int offSet = 2; // size of the point
        glBegin(GL_QUADS);
        glVertex2d(centerX - offSet, centerY + offSet);
        glVertex2d(centerX + offSet, centerY + offSet);
        glVertex2d(centerX + offSet, centerY - offSet);
        glVertex2d(centerX - offSet, centerY - offSet);
        glEnd();
    }

    private void renderDrawing(){
        for(int i =0; i<drawnPoints.size(); i+=2){
            if(drawnPoints.get(i) == null) continue;
            renderOnePoint(drawnPoints.get(i), drawnPoints.get(i+1));
        }
    }

    private void renderGameFrame(){
        float offset = 20;
        glColor4f(0, 0, 0, 0);
        glBegin(GL_LINE_LOOP);
        glVertex2f( 0+offset, 0+offset );
        glVertex2f( 0+offset, windowHeight-offset );
        glVertex2f( windowWidth-offset, windowHeight-offset );
        glVertex2f( windowWidth-offset, 0+offset );
        glEnd();
    }

    public void setThisDrawer() {
        isDrawer = true;
    }

    public void cancelDrawer(){
        isDrawer = false;
    }

    public void clearCanvas(){
        drawnPoints.clear();
    }

    public static void main(String[] args) {
    }

}