package com.terrescalmes;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {
    // Identifiants OpenGL
    private int vaoId;
    private int vboId;
    private int shaderProgram;

    // Handle de la fenêtre
    private long window;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Initialisation GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Impossible d'initialiser GLFW");
        }

        // Configuration GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Création de la fenêtre
        window = glfwCreateWindow(800, 600, "Triangle OpenGL", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Échec de la création de la fenêtre GLFW");
        }

        // Configuration des callbacks
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        // Centrage de la fenêtre
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

        // Création du contexte OpenGL
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // VSync
        glfwShowWindow(window);

        // Chargement des bindings OpenGL
        GL.createCapabilities();

        // Configuration OpenGL
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);

        // Création du triangle
        setupTriangle();
        setupShaders();
    }

    private void setupTriangle() {
        // Vertices du triangle (x, y, z)
        float[] vertices = {
                -0.5f, -0.5f, 0.0f, // Coin gauche
                0.5f, -0.5f, 0.0f, // Coin droit
                0.0f, 0.5f, 0.0f // Haut
        };

        // Création du VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Création du VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        // Conversion des vertices en buffer
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        // Transfert des données vers la GPU
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);

        // Configuration des attributs de vertex
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // Désactivation du VAO
        glBindVertexArray(0);
    }

    private void setupShaders() {
        // Vertex Shader
        String vertexShaderSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 aPos;\n" +
                "void main() {\n" +
                "   gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0);\n" +
                "}";

        // Fragment Shader
        String fragmentShaderSource = "#version 330 core\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "   FragColor = vec4(1.0f, 0.0f, 0.0f, 1.0f);\n" + // Couleur rouge
                "}";

        // Compilation des shaders
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        checkCompileErrors(vertexShader, "VERTEX");

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        checkCompileErrors(fragmentShader, "FRAGMENT");

        // Création du programme shader
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        checkCompileErrors(shaderProgram, "PROGRAM");

        // Nettoyage des shaders
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private void checkCompileErrors(int object, String type) {
        if (type.equals("PROGRAM")) {
            int status = glGetProgrami(object, GL_LINK_STATUS);
            if (status == GL_FALSE) {
                String log = glGetProgramInfoLog(object);
                System.err.println("Erreur de linkage du shader:\n" + log);
            }
        } else {
            int status = glGetShaderi(object, GL_COMPILE_STATUS);
            if (status == GL_FALSE) {
                String log = glGetShaderInfoLog(object);
                System.err.println("Erreur de compilation du " + type + " shader:\n" + log);
            }
        }
    }

    private void loop() {
        // Boucle de rendu principale
        while (!glfwWindowShouldClose(window)) {
            // Nettoyage de l'écran
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Activation du shader
            glUseProgram(shaderProgram);

            // Dessin du triangle
            glBindVertexArray(vaoId);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);

            // Échange des buffers
            glfwSwapBuffers(window);

            // Gestion des événements
            glfwPollEvents();
        }
    }

    private void cleanup() {
        // Libération des ressources OpenGL
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteProgram(shaderProgram);

        // Libération des ressources GLFW
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
