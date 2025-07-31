package com.terrescalmes.core;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Game {

    private long window;
    private int width = 800;
    private int height = 600;

    // Game loop timing
    private final double TARGET_UPS = 30.0; // Updates per second (physique)
    private final double UPDATE_TIME = 1.0 / TARGET_UPS;

    // Player state
    private Player currentPlayer;
    private Player previousPlayer;

    // Performance counters
    private int fps = 0;
    private int ups = 0;
    private double fpsTimer = 0.0;
    private int fpsCounter = 0;
    private int upsCounter = 0;

    public void run() {
        init();
        gameLoop();
        cleanup();
    }

    private void init() {
        // Initialize GLFW
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create window
        window = glfwCreateWindow(width, height, "LWJGL Framerate Demo", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        glfwShowWindow(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Initialize game objects
        currentPlayer = new Player(400, 300, 50, 50);
        previousPlayer = new Player(400, 300, 50, 50);

        // Setup OpenGL for 2D rendering
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
    }

    private void gameLoop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;

        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;
            lastTime = currentTime;

            // Cap frame time to prevent spiral of death
            frameTime = Math.min(frameTime, 0.25);

            accumulator += frameTime;

            // Fixed timestep updates (physique à 30 UPS)
            while (accumulator >= UPDATE_TIME) {
                // Sauvegarder l'état précédent pour l'interpolation
                previousPlayer.copyFrom(currentPlayer);

                // Update game logic
                update();
                upsCounter++;

                accumulator -= UPDATE_TIME;
            }

            // Calculer le facteur d'interpolation
            double interpolationFactor = accumulator / UPDATE_TIME;

            // Render with interpolation
            render(interpolationFactor);
            fpsCounter++;

            // Update performance counters
            fpsTimer += frameTime;
            if (fpsTimer >= 1.0) {
                fps = fpsCounter;
                ups = upsCounter;
                fpsCounter = 0;
                upsCounter = 0;
                fpsTimer = 0.0;

                // Mettre à jour le titre de la fenêtre avec les FPS/UPS
                String title = String.format("LWJGL Framerate Demo | FPS: %d UPS: %d", fps, ups);
                glfwSetWindowTitle(window, title);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void update() {
        // Input handling
        boolean left = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS ||
                glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS;
        boolean right = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS ||
                glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS;
        boolean up = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS ||
                glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS;
        boolean down = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS ||
                glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS;

        // Player movement (physique à 30 UPS)
        float speed = 200.0f; // pixels par seconde
        float deltaSpeed = speed * (float) UPDATE_TIME;

        if (left && currentPlayer.x > 0) {
            currentPlayer.x -= deltaSpeed;
        }
        if (right && currentPlayer.x < width - currentPlayer.width) {
            currentPlayer.x += deltaSpeed;
        }
        if (up && currentPlayer.y > 0) {
            currentPlayer.y -= deltaSpeed;
        }
        if (down && currentPlayer.y < height - currentPlayer.height) {
            currentPlayer.y += deltaSpeed;
        }

        // Keep player in bounds
        currentPlayer.x = Math.max(0, Math.min(width - currentPlayer.width, currentPlayer.x));
        currentPlayer.y = Math.max(0, Math.min(height - currentPlayer.height, currentPlayer.y));
    }

    private void render(double interpolationFactor) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Interpoler la position du joueur pour un rendu fluide
        float renderX = (float) (previousPlayer.x + (currentPlayer.x - previousPlayer.x) * interpolationFactor);
        float renderY = (float) (previousPlayer.y + (currentPlayer.y - previousPlayer.y) * interpolationFactor);

        // Dessiner le joueur (carré rouge)
        glColor3f(1.0f, 0.3f, 0.3f);
        glBegin(GL_QUADS);
        glVertex2f(renderX, renderY);
        glVertex2f(renderX + currentPlayer.width, renderY);
        glVertex2f(renderX + currentPlayer.width, renderY + currentPlayer.height);
        glVertex2f(renderX, renderY + currentPlayer.height);
        glEnd();
    }

    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public static void main(String[] args) {
        new Game().run();
    }

    // Classe Player simple
    private static class Player {
        float x, y;
        float width, height;

        public Player(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void copyFrom(Player other) {
            this.x = other.x;
            this.y = other.y;
            this.width = other.width;
            this.height = other.height;
        }
    }
}
