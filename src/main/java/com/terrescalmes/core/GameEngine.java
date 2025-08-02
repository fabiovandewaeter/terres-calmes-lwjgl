package com.terrescalmes.core;

import com.terrescalmes.MouseInput;
import com.terrescalmes.Window;
import com.terrescalmes.Window.WindowOptions;
import com.terrescalmes.core.graphics.Render;
import com.terrescalmes.core.graphics.Scene;
import com.terrescalmes.core.graphics.SkyBox;
import com.terrescalmes.core.graphics.GUI.IGuiInstance;
import com.terrescalmes.core.graphics.GUI.LightControls;
import com.terrescalmes.core.graphics.lights.AmbientLight;
import com.terrescalmes.core.graphics.lights.DirLight;
import com.terrescalmes.core.graphics.lights.SceneLights;
import com.terrescalmes.core.graphics.Camera;
import com.terrescalmes.core.graphics.Fog;
import com.terrescalmes.core.graphics.Model;
import com.terrescalmes.core.graphics.ModelLoader;
import com.terrescalmes.entities.Entity;
import com.terrescalmes.entities.Player;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GameEngine implements IGuiInstance {
    // Game loop timing
    public static final double TARGET_UPS = 30.0; // Updates per second (physique)
    public static final double UPDATE_TIME = 1.0 / TARGET_UPS;
    private static final String WINDOW_TITLE = "Terres Calmes";
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;

    // private long window;
    private Window window;
    private Render render;
    private Scene scene;

    // Performance counters
    private int fps = 0;
    private int ups = 0;
    private double fpsTimer = 0.0;
    private int fpsCounter = 0;
    private int upsCounter = 0;

    // Player state
    private Player player;
    private Entity cubeEntity;
    private Vector4f displInc = new Vector4f();
    private float rotation;

    private boolean running;
    private LightControls lightControls;
    private static final int NUM_CHUNKS = 40;
    private Entity[][] terrainEntities;

    public GameEngine() {
        WindowOptions opts = new WindowOptions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        window = new Window(WINDOW_TITLE, opts, () -> {
            resize();
            return null;
        });
        render = new Render(window);
        scene = new Scene(window.getWidth(), window.getHeight());
        init(window, scene, render);
        player = new Player(400, 300, 50, 50);
        running = true;
    }

    public void run() {
        init(window, scene, render);
        gameLoop();
        cleanup();
    }

    public void init(Window window, Scene scene, Render render) {
        String terrainModelId = "terrain";
        Model terrainModel = ModelLoader.loadModel(terrainModelId, "resources/models/terrain/terrain.obj",
                scene.getTextureCache());
        scene.addModel(terrainModel);
        Entity terrainEntity = new Entity("terrainEntity", terrainModelId);
        terrainEntity.setScale(100.0f);
        terrainEntity.updateModelMatrix();
        scene.addEntity(terrainEntity);

        SceneLights sceneLights = new SceneLights();
        AmbientLight ambientLight = sceneLights.getAmbientLight();
        ambientLight.setIntensity(0.5f);
        ambientLight.setColor(0.3f, 0.3f, 0.3f);

        DirLight dirLight = sceneLights.getDirLight();
        dirLight.setPosition(0, 1, 0);
        dirLight.setIntensity(1.0f);
        scene.setSceneLights(sceneLights);

        SkyBox skyBox = new SkyBox("resources/models/skybox/skybox.obj", scene.getTextureCache());
        skyBox.getSkyBoxEntity().setScale(50);
        scene.setSkyBox(skyBox);

        scene.setFog(new Fog(true, new Vector3f(0.5f, 0.5f, 0.5f), 0.95f));

        scene.getCamera().moveUp(0.1f);
    }

    private void gameLoop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;
        IGuiInstance iGuiInstance = scene.getGuiInstance();

        while (running && !window.shouldClose()) {
            window.pollEvents();
            window.getMouseInput().input();
            boolean inputConsumed = iGuiInstance != null && iGuiInstance.handleGuiInput(scene, window);
            input(window, scene, (long) (glfwGetTime() * 1000 - lastTime * 1000), inputConsumed);

            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;

            // Cap frame time to prevent spiral of death
            frameTime = Math.min(frameTime, 0.25);

            accumulator += frameTime;

            // Fixed timestep updates (physique à 30 UPS)
            while (accumulator >= UPDATE_TIME) {
                // Sauvegarder l'état précédent pour l'interpolation
                player.saveState();

                // Update game logic
                update();
                upsCounter++;

                accumulator -= UPDATE_TIME;
            }

            double interpolationFactor = accumulator / UPDATE_TIME;
            render(interpolationFactor);

            // Update performance counters
            fpsCounter++;
            fpsTimer += frameTime;
            if (fpsTimer >= 1.0) {
                fps = fpsCounter;
                ups = upsCounter;
                fpsCounter = 0;
                upsCounter = 0;
                fpsTimer = 0.0;

                // Mettre à jour le titre de la fenêtre avec les FPS/UPS
                String title = String.format("Terres Calmes | FPS: %d UPS: %d", fps, ups);
                glfwSetWindowTitle(window.getWindowHandle(), title);
            }

            lastTime = currentTime;
        }
    }

    private void resize() {
        int width = window.getWidth();
        int height = window.getHeight();
        scene.resize(width, height);
        render.resize(width, height);
    }

    public void drawGui() {
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.showDemoWindow();
        ImGui.endFrame();
        ImGui.render();
    }

    public boolean handleGuiInput(Scene scene, Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        MouseInput mouseInput = window.getMouseInput();
        Vector2f mousePos = mouseInput.getCurrentPos();
        imGuiIO.addMousePosEvent(mousePos.x, mousePos.y);
        imGuiIO.addMouseButtonEvent(0, mouseInput.isLeftButtonPressed());
        imGuiIO.addMouseButtonEvent(1, mouseInput.isRightButtonPressed());

        return imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
    }

    public void input(Window window, Scene scene, long diffTimeMillis, boolean inputConsumed) {
        if (inputConsumed) {
            return;
        }

        float move = diffTimeMillis * MOVEMENT_SPEED;
        Camera camera = scene.getCamera();
        if (window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(move);
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackwards(move);
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(move);
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(move);
        }
        if (window.isKeyPressed(GLFW_KEY_UP)) {
            camera.moveUp(move);
        } else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
            camera.moveDown(move);
        }

        MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            Vector2f displVec = mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(-displVec.x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(-displVec.y * MOUSE_SENSITIVITY));
        }
    }

    private void update() {
        // updateTerrain(scene);
    }

    public void updateTerrain(Scene scene) {
        int cellSize = 10;
        Camera camera = scene.getCamera();
        Vector3f cameraPos = camera.getPosition();
        int cellCol = (int) (cameraPos.x / cellSize);
        int cellRow = (int) (cameraPos.z / cellSize);

        int numRows = NUM_CHUNKS * 2 + 1;
        int numCols = numRows;
        int zOffset = -NUM_CHUNKS;
        float scale = cellSize / 2.0f;
        for (int j = 0; j < numRows; j++) {
            int xOffset = -NUM_CHUNKS;
            for (int i = 0; i < numCols; i++) {
                Entity entity = terrainEntities[j][i];
                entity.setScale(scale);
                entity.setPosition((cellCol + xOffset) * 2.0f, 0, (cellRow + zOffset) * 2.0f);
                entity.getModelMatrix().identity().scale(scale).translate(entity.getPosition());
                xOffset++;
            }
            zOffset++;
        }
    }

    private void render(double interpolationFactor) {
        render.render(window, scene);
        window.update();
    }

    private void cleanup() {
        render.cleanup();
        scene.cleanup();
        window.cleanup();
    }
}
