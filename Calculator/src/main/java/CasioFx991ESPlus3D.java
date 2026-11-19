import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main JavaFX Application assembling the 3D Casio fx-991ES PLUS (2nd Edition)
 * hardware, studio lighting, 360° camera orbit/pan/zoom, and keyboard bindings.
 */
public class CasioFx991ESPlus3D extends Application {

    private final Rotate rotateX = new Rotate(-16, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(14, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);
    private final Translate cameraTranslate = new Translate(0, 0, -560);
    private final Translate panTranslate = new Translate(0, 0, 0);

    private double mouseOldX, mouseOldY, mousePressX, mousePressY;
    private boolean isDragging = false;

    private final Canvas lcdCanvas = new Canvas(LcdRenderer.LCD_W, LcdRenderer.LCD_H);
    private final WritableImage lcdImage = new WritableImage(LcdRenderer.LCD_W, LcdRenderer.LCD_H);
    private final PhongMaterial lcdMaterial = new PhongMaterial();
    private boolean cursorVisible = true;

    private final CalculatorEngine engine = new CalculatorEngine(this::refreshLCD);
    private final Map<String, Group> keyNodeMap = new HashMap<>();
    private final List<KeyDef> allKeys = KeyDef.createAllKeys();

    @Override
    public void start(Stage stage) {
        Group calculatorGroup = buildCalculator3D();
        calculatorGroup.getTransforms().addAll(panTranslate, rotateY, rotateX, rotateZ);

        Group worldRoot = new Group();
        worldRoot.getChildren().add(calculatorGroup);
        setupStudioLighting(worldRoot);

        SubScene subScene = new SubScene(worldRoot, 1150, 860, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.rgb(16, 19, 25));

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(45.0);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(34.0);
        camera.getTransforms().add(cameraTranslate);
        subScene.setCamera(camera);

        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: radial-gradient(center 50% 45%, radius 75%, #242a36, #0e1117);");
        subScene.widthProperty().bind(rootPane.widthProperty());
        subScene.heightProperty().bind(rootPane.heightProperty());

        rootPane.getChildren().add(subScene);

        Scene scene = new Scene(rootPane, 1150, 860, true);
        setupMouseAndKeyboardControls(scene, subScene);

        Timeline blinkTimer = new Timeline(new KeyFrame(Duration.millis(520), e -> {
            cursorVisible = !cursorVisible;
            refreshLCD();
        }));
        blinkTimer.setCycleCount(Timeline.INDEFINITE);
        blinkTimer.play();

        refreshLCD();

        stage.setTitle("calc");
        stage.setScene(scene);
        stage.show();
    }

    private void refreshLCD() {
        LcdRenderer.render(lcdCanvas, lcdImage, engine, cursorVisible);
    }

    private Group buildCalculator3D() {
        Group root = new Group();

        // 1. Front Sculpted Housing & Silkscreened Faceplate
        WritableImage faceplateTex = TextureFactory.createFaceplateTexture(allKeys);
        PhongMaterial chassisFrontMat = new PhongMaterial(Color.WHITE);
        chassisFrontMat.setDiffuseMap(faceplateTex);
        chassisFrontMat.setSpecularColor(Color.rgb(42, 46, 54));
        chassisFrontMat.setSpecularPower(26.0);

        MeshView frontHousing = new MeshView(
                ProceduralMeshFactory.createFrontHousingMesh(
                        ProceduralMeshFactory.BODY_W, ProceduralMeshFactory.BODY_H));
        frontHousing.setMaterial(chassisFrontMat);
        frontHousing.setCullFace(CullFace.BACK);

        // 2. Modeled Back Cover
        WritableImage backTex = TextureFactory.createBackCoverTexture();
        PhongMaterial chassisBackMat = new PhongMaterial(Color.WHITE);
        chassisBackMat.setDiffuseMap(backTex);
        chassisBackMat.setSpecularColor(Color.rgb(40, 44, 52));
        chassisBackMat.setSpecularPower(24.0);

        MeshView backHousing = new MeshView(
                ProceduralMeshFactory.createBackHousingMesh(
                        ProceduralMeshFactory.BODY_W,
                        ProceduralMeshFactory.BODY_H,
                        ProceduralMeshFactory.BODY_D));
        backHousing.setMaterial(chassisBackMat);
        backHousing.setCullFace(CullFace.BACK);

        // 3. Recessed Solar Panel, LCD Assembly, Back Hardware & D-Pad Center Hub
        Group solarAssembly = buildSolarPanelAssembly();
        Group lcdAssembly = buildLcdAssembly();
        Group backHardware = buildBackHardwareDetails();

        PhongMaterial brushedSilverMat = TextureFactory.createBrushedSilverMaterial();
        MeshView dpadCenterHub = new MeshView(
                ProceduralMeshFactory.createDpadCenterHubMesh(6.4));
        dpadCenterHub.setMaterial(brushedSilverMat);
        dpadCenterHub.setTranslateX(0);
        dpadCenterHub.setTranslateY(-23.0);

        root.getChildren().addAll(
                frontHousing,
                backHousing,
                solarAssembly,
                lcdAssembly,
                backHardware,
                dpadCenterHub
        );

        // 4. All 50 Sculpted 3D Keys & Keywell Collars
        PhongMaterial keywellMat = new PhongMaterial(Color.rgb(12, 14, 18));
        keywellMat.setSpecularColor(Color.rgb(24, 26, 30));
        keywellMat.setSpecularPower(16.0);

        for (KeyDef kd : allKeys) {
            Group keyGroup = new Group();
            MeshView keyMeshView;

            switch (kd.category) {
                case SILVER_ROUND -> {
                    keyMeshView = new MeshView(
                            ProceduralMeshFactory.createCircularButtonMesh(kd.w * 0.5, 3.6));
                    keyMeshView.setMaterial(brushedSilverMat);
                    keyGroup.setTranslateX(kd.x);
                    keyGroup.setTranslateY(kd.y);

                    MeshView collar = new MeshView(
                            ProceduralMeshFactory.createKeywellRingMesh(kd.w, kd.h, kd.w * 0.5));
                    collar.setMaterial(keywellMat);
                    collar.setTranslateX(kd.x);
                    collar.setTranslateY(kd.y);
                    root.getChildren().add(collar);
                }
                case DPAD_ROCKER -> {
                    keyMeshView = new MeshView(
                            ProceduralMeshFactory.createDpadRockerMesh(kd.dpadDir, 6.4, 18.0));
                    keyMeshView.setMaterial(TextureFactory.createDpadRockerMaterial(kd.dpadDir));
                    keyGroup.setTranslateX(kd.x);
                    keyGroup.setTranslateY(kd.y);
                }
                case SCI_PILL -> {
                    keyMeshView = new MeshView(
                            ProceduralMeshFactory.createSculptedKeyMesh(kd.w, kd.h, 3.6, 3.9));
                    keyMeshView.setMaterial(TextureFactory.createKeycapMaterial(
                            kd, Color.web("#252830"), Color.rgb(48, 52, 60)));
                    keyGroup.setTranslateX(kd.x);
                    keyGroup.setTranslateY(kd.y);

                    MeshView collar = new MeshView(
                            ProceduralMeshFactory.createKeywellRingMesh(kd.w, kd.h, 3.8));
                    collar.setMaterial(keywellMat);
                    collar.setTranslateX(kd.x);
                    collar.setTranslateY(kd.y);
                    root.getChildren().add(collar);
                }
                case MAIN_GREY -> {
                    keyMeshView = new MeshView(
                            ProceduralMeshFactory.createSculptedKeyMesh(kd.w, kd.h, 3.2, 4.6));
                    keyMeshView.setMaterial(TextureFactory.createKeycapMaterial(
                            kd, Color.web("#585d68"), Color.rgb(62, 66, 74)));
                    keyGroup.setTranslateX(kd.x);
                    keyGroup.setTranslateY(kd.y);

                    MeshView collar = new MeshView(
                            ProceduralMeshFactory.createKeywellRingMesh(kd.w, kd.h, 3.4));
                    collar.setMaterial(keywellMat);
                    collar.setTranslateX(kd.x);
                    collar.setTranslateY(kd.y);
                    root.getChildren().add(collar);
                }
                case MAIN_GREEN -> {
                    keyMeshView = new MeshView(
                            ProceduralMeshFactory.createSculptedKeyMesh(kd.w, kd.h, 3.2, 4.6));
                    keyMeshView.setMaterial(TextureFactory.createKeycapMaterial(
                            kd, Color.web("#7ab843"), Color.rgb(75, 100, 52)));
                    keyGroup.setTranslateX(kd.x);
                    keyGroup.setTranslateY(kd.y);

                    MeshView collar = new MeshView(
                            ProceduralMeshFactory.createKeywellRingMesh(kd.w, kd.h, 3.4));
                    collar.setMaterial(keywellMat);
                    collar.setTranslateX(kd.x);
                    collar.setTranslateY(kd.y);
                    root.getChildren().add(collar);
                }
                default -> throw new IllegalStateException();
            }

            keyMeshView.setCullFace(CullFace.BACK);
            keyGroup.getChildren().add(keyMeshView);
            keyGroup.setCursor(Cursor.HAND);

            keyGroup.setOnMouseReleased(e -> {
                if (e.getButton() == MouseButton.PRIMARY && !isDragging) {
                    triggerKeyPress(kd);
                    e.consume();
                }
            });

            keyNodeMap.put(kd.id, keyGroup);
            root.getChildren().add(keyGroup);
        }

        return root;
    }

    private Group buildSolarPanelAssembly() {
        Group solarGroup = new Group();
        double recessZ = 2.0;

        PhongMaterial frameMat = new PhongMaterial(Color.rgb(15, 17, 21));
        frameMat.setSpecularColor(Color.rgb(145, 150, 160));
        frameMat.setSpecularPower(68.0);

        MeshView bezel = new MeshView(
                ProceduralMeshFactory.createRecessedBezelMesh(
                        ProceduralMeshFactory.SOLAR_OUTER_W + 2.4,
                        ProceduralMeshFactory.SOLAR_OUTER_H + 2.4,
                        3.2, 3.6, 0.0, recessZ));
        bezel.setMaterial(frameMat);
        bezel.setTranslateX(ProceduralMeshFactory.SOLAR_CENTER_X);
        bezel.setTranslateY(ProceduralMeshFactory.SOLAR_CENTER_Y);

        PhongMaterial cellMat = TextureFactory.createSolarPanelMaterial();
        MeshView cellSurface = new MeshView(
                ProceduralMeshFactory.createRoundedPanelMesh(
                        ProceduralMeshFactory.SOLAR_OUTER_W - 2.2,
                        ProceduralMeshFactory.SOLAR_OUTER_H - 2.2,
                        1.5, recessZ));
        cellSurface.setMaterial(cellMat);
        cellSurface.setTranslateX(ProceduralMeshFactory.SOLAR_CENTER_X);
        cellSurface.setTranslateY(ProceduralMeshFactory.SOLAR_CENTER_Y);

        solarGroup.getChildren().addAll(bezel, cellSurface);
        return solarGroup;
    }

    private Group buildLcdAssembly() {
        Group lcdGroup = new Group();
        double depthZ = 3.8;

        PhongMaterial bezelMat = new PhongMaterial(Color.rgb(20, 23, 28));
        bezelMat.setSpecularColor(Color.rgb(48, 52, 60));
        bezelMat.setSpecularPower(28.0);

        MeshView bezelMesh = new MeshView(
                ProceduralMeshFactory.createRecessedBezelMesh(
                        ProceduralMeshFactory.LCD_OUTER_W + 2.8,
                        ProceduralMeshFactory.LCD_OUTER_H + 2.8,
                        6.0, 6.8, 0.0, depthZ));
        bezelMesh.setMaterial(bezelMat);
        bezelMesh.setTranslateX(ProceduralMeshFactory.LCD_CENTER_X);
        bezelMesh.setTranslateY(ProceduralMeshFactory.LCD_CENTER_Y);

        lcdMaterial.setDiffuseColor(Color.WHITE);
        lcdMaterial.setDiffuseMap(lcdImage);
        lcdMaterial.setSpecularColor(Color.rgb(45, 52, 45));
        lcdMaterial.setSpecularPower(38.0);

        MeshView screenMesh = new MeshView(
                ProceduralMeshFactory.createRoundedPanelMesh(
                        ProceduralMeshFactory.LCD_OUTER_W - 7.6,
                        ProceduralMeshFactory.LCD_OUTER_H - 7.6,
                        2.0, depthZ));
        screenMesh.setMaterial(lcdMaterial);
        screenMesh.setTranslateX(ProceduralMeshFactory.LCD_CENTER_X);
        screenMesh.setTranslateY(ProceduralMeshFactory.LCD_CENTER_Y);

        lcdGroup.getChildren().addAll(bezelMesh, screenMesh);
        return lcdGroup;
    }

    private Group buildBackHardwareDetails() {
        Group hwGroup = new Group();
        PhongMaterial screwMat = TextureFactory.createScrewMaterial();
        double[][] screwCoords = {
                {-59.0, -135.0}, {59.0, -135.0},
                {-61.0, -18.0},  {61.0, -18.0},
                {-59.0, 134.0},  {59.0, 134.0}
        };
        for (double[] sc : screwCoords) {
            MeshView screw = new MeshView(ProceduralMeshFactory.createScrewHeadMesh(2.5));
            screw.setMaterial(screwMat);
            screw.setTranslateX(sc[0]);
            screw.setTranslateY(sc[1]);
            screw.setTranslateZ(ProceduralMeshFactory.BODY_D + 0.08);
            hwGroup.getChildren().add(screw);
        }

        PhongMaterial rubberMat = new PhongMaterial(Color.rgb(16, 18, 21));
        rubberMat.setSpecularColor(Color.rgb(22, 24, 28));
        rubberMat.setSpecularPower(10.0);

        for (double fy : new double[]{-142.0, 142.0}) {
            for (double fx : new double[]{-34.0, 34.0}) {
                MeshView foot = new MeshView(
                        ProceduralMeshFactory.createBackRubberFootMesh(16.0, 4.5, 1.8, 0.9));
                foot.setMaterial(rubberMat);
                foot.setTranslateX(fx);
                foot.setTranslateY(fy);
                foot.setTranslateZ(ProceduralMeshFactory.BODY_D + 0.05);
                hwGroup.getChildren().add(foot);
            }
        }

        return hwGroup;
    }

    private void setupStudioLighting(Group worldRoot) {
        AmbientLight ambient = new AmbientLight(Color.rgb(92, 96, 104));

        PointLight keyLight = new PointLight(Color.rgb(168, 165, 158));
        keyLight.setTranslateX(-250);
        keyLight.setTranslateY(-320);
        keyLight.setTranslateZ(-440);

        PointLight fillLight = new PointLight(Color.rgb(78, 86, 102));
        fillLight.setTranslateX(290);
        fillLight.setTranslateY(250);
        fillLight.setTranslateZ(-390);

        PointLight backRimLight = new PointLight(Color.rgb(138, 144, 154));
        backRimLight.setTranslateX(40);
        backRimLight.setTranslateY(-140);
        backRimLight.setTranslateZ(460);

        worldRoot.getChildren().addAll(ambient, keyLight, fillLight, backRimLight);
    }

    private void triggerKeyPress(KeyDef kd) {
        Group keyNode = keyNodeMap.get(kd.id);
        if (keyNode != null) {
            Timeline anim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(keyNode.translateZProperty(), 0.0)),
                    new KeyFrame(Duration.millis(55),
                            new KeyValue(keyNode.translateZProperty(), 2.2)),
                    new KeyFrame(Duration.millis(145),
                            new KeyValue(keyNode.translateZProperty(), 0.0))
            );
            anim.play();
        }
        cursorVisible = true;
        engine.handleKey(kd.id);
    }

    private void triggerKeyById(String id) {
        for (KeyDef kd : allKeys) {
            if (kd.id.equals(id)) {
                triggerKeyPress(kd);
                return;
            }
        }
    }

    private void setupMouseAndKeyboardControls(Scene scene, SubScene subScene) {
        subScene.setOnMousePressed(e -> {
            mouseOldX = mousePressX = e.getSceneX();
            mouseOldY = mousePressY = e.getSceneY();
            isDragging = false;
        });

        subScene.setOnMouseDragged(e -> {
            double dxTotal = e.getSceneX() - mousePressX;
            double dyTotal = e.getSceneY() - mousePressY;
            if (Math.hypot(dxTotal, dyTotal) > 4.0) {
                isDragging = true;
            }

            double dx = e.getSceneX() - mouseOldX;
            double dy = e.getSceneY() - mouseOldY;
            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();

            if (e.isPrimaryButtonDown()) {
                rotateY.setAngle(rotateY.getAngle() + dx * 0.42);
                rotateX.setAngle(rotateX.getAngle() - dy * 0.42);
            } else if (e.isSecondaryButtonDown() || e.isMiddleButtonDown()) {
                panTranslate.setX(panTranslate.getX() + dx * 0.45);
                panTranslate.setY(panTranslate.getY() + dy * 0.45);
            }
        });

        subScene.setOnScroll(e -> {
            double newZ = cameraTranslate.getZ() + e.getDeltaY() * 0.65;
            cameraTranslate.setZ(Math.max(-1200.0, Math.min(-210.0, newZ)));
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode code = e.getCode();
            String text = e.getText();

            if (code == KeyCode.ENTER) { triggerKeyById("EQ"); e.consume(); return; }
            if (code == KeyCode.BACK_SPACE) { triggerKeyById("DEL"); e.consume(); return; }
            if (code == KeyCode.ESCAPE || code == KeyCode.DELETE) { triggerKeyById("AC"); e.consume(); return; }
            if (code == KeyCode.UP) { triggerKeyById("UP"); e.consume(); return; }
            if (code == KeyCode.DOWN) { triggerKeyById("DOWN"); e.consume(); return; }
            if (code == KeyCode.LEFT) { triggerKeyById("LEFT"); e.consume(); return; }
            if (code == KeyCode.RIGHT) { triggerKeyById("RIGHT"); e.consume(); return; }

            if (e.isShiftDown() && code == KeyCode.SHIFT) { return; }

            switch (text) {
                case "0" -> triggerKeyById("0");
                case "1" -> triggerKeyById("1");
                case "2" -> triggerKeyById("2");
                case "3" -> triggerKeyById("3");
                case "4" -> triggerKeyById("4");
                case "5" -> triggerKeyById("5");
                case "6" -> triggerKeyById("6");
                case "7" -> triggerKeyById("7");
                case "8" -> triggerKeyById("8");
                case "9" -> triggerKeyById("9");
                case "." -> triggerKeyById("DOT");
                case "+" -> triggerKeyById("ADD");
                case "-" -> triggerKeyById("SUB");
                case "*" -> triggerKeyById("MUL");
                case "/" -> triggerKeyById("DIV");
                case "(" -> triggerKeyById("LPAREN");
                case ")" -> triggerKeyById("RPAREN");
                case "^" -> triggerKeyById("POW");
                case "=" -> triggerKeyById("EQ");
                case "!" -> { triggerKeyById("SHIFT"); triggerKeyById("INV"); }
                case "%" -> { triggerKeyById("SHIFT"); triggerKeyById("LPAREN"); }
                case "s", "S" -> {
                    if (e.isControlDown()) triggerKeyById("SHIFT");
                    else triggerKeyById("SIN");
                }
                case "c", "C" -> triggerKeyById("COS");
                case "t", "T" -> triggerKeyById("TAN");
                case "l", "L" -> triggerKeyById("LOG");
                case "n", "N" -> triggerKeyById("LN");
                case "r", "R" -> triggerKeyById("SQRT");
                case "d", "D" -> triggerKeyById("SD");
                case "p", "P" -> { triggerKeyById("SHIFT"); triggerKeyById("EXP"); }
                case "e", "E" -> triggerKeyById("EXP");
                case "a", "A" -> {
                    if (e.isControlDown()) triggerKeyById("ALPHA");
                    else triggerKeyById("ANS");
                }
                case "m", "M" -> triggerKeyById("MPLUS");
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}