/**
 * @author Selman OZTURK - 150112028
 */
package org.selmanozturk.roadmax;

import static org.anddev.andengine.extension.physics.box2d.util.constants.PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

import org.anddev.nilsson.UIElement;
import org.anddev.nilsson.UISprite;
import org.anddev.nilsson.StackPanel;
import org.anddev.nilsson.BitmapTextureAtlasEx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.BoundCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.WakeLockOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.primitive.Line;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.physics.box2d.FixedStepPhysicsWorld;
import org.anddev.andengine.extension.physics.box2d.PhysicsConnector;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.ui.activity.LayoutGameActivity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.res.AssetManager;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.KeyEvent;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJoint;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;

public class MainActivity extends LayoutGameActivity implements IOnSceneTouchListener, IAccelerometerListener {

	//Main Camera of the game
	private BoundCamera cameraBound;
	/*
	 * Set pre-calculated fixture definitions
	 * We will use them with game objects, later. 
	 */
	public static final FixtureDef IFixDef_concrete = PhysicsFactory.createFixtureDef(0, 0.5f, 0.8f, false, (short)1,(short)7, (short) 0),
								IFixDef_rectangle = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f, false, (short)2,(short)3, (short) 0),
								IFixDef_circle = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f, false, (short)4,(short)5, (short) 0),
								IFixDef_null = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f, false, (short)0,(short)0, (short) 0),
								IFixDef_wall = PhysicsFactory.createFixtureDef(1, 0.2f, 1f, false, (short)4,(short)1, (short) -1);
	/*
	 * Here, set unique final integers 
	 * to assign an ID to game scenes
	 */
	public static final int scene_null = 0,
					scene_main = 1,
					scene_options = 2,
					scene_help = 3,
					scene_author = 4,
					scene_levelSelect = 5,
					scene_pause = 6,
					scene_loading = 7,
					scene_vehicleSelect = 8;
	//This for storing current scene index
	private int currentScene;
	
	//Application Main Background(Screens)
	private Sprite globalBackground;
	
	//Updater instance of the scene used to later be unregistered
	private myUpdateHandler gameUpdateHandler = new myUpdateHandler(this);
			
	//stores updates on screen
	float preUpdates = 0;
	
	/*
	 * Set vehicles speed and torque
	 */
	private static final float torque_forward = 1000,
					speed_forward = 20,
					torque_brake = 2000,
					torque_reverse = 1000,
					speed_reverse = -10;
	/*
	 * Here, You can see 2 hash map 
	 * They map a string keyword with a TextureRegion 
	 * In this way, We can store game elements easily
	 * worldTextureRegionHashMap:	Stores level shapes
	 * gameTextureRegionHashMap:	Stores vehicle shapes
	 */
	private HashMap<String, TextureRegion> worldTextureRegionHashMap = new HashMap<String, TextureRegion>();
	private HashMap<String, TextureRegion> gameTextureRegionHashMap = new HashMap<String, TextureRegion>();
	
	/*
	 * variables to store number of vehicles and current vehicle's index
	 * You can see the levels in \gfx\levels\ folder
	 */
	int numOfvehicles = 3, selectedVehicleIndex=0;
	/*
	 * variables to store number of levels and current level's index
	 * You can see the levels in \gfx\levels\ folder
	 */
	int numOfLevels = 3, selectedLevelIndex = 0;
	/*
	 * These booleans store states of pedals
	 */
	boolean brakesPressed = false, gasPressed = false;
	
	//set this when the user failed
	private boolean isFailed = false;
	//check if a level successfully loaded or not
	private boolean isLevelStarted;
	//check if the user finished the level or not
	private boolean isFinished = false;
	//this is for game pause state
	private boolean isPaused;
	
	// Stores the value which comes from the sensor of the phone 
	private float accelerometerTiltAlpha = 0.5f;
	
	//This is the variable which stores the main scene along the game
	private Scene mainScene;
	/*
	 * Here, I created 6 sprite arrays
	 * to store each scene's elements in them.
	 * You can easily add the elements of the array list
	 * to load a scene to screen
	 * or remove for close that scene 
	 */
	private ArrayList<Sprite> mainScreen,
							loadingScreen,
							pauseScreen,
							aboutScreen,
							optionScreen,
							helpScreen,
							vehicleSelectScreen,
							levelSelectScreen;
	//vehicle sound
	private Sound vehicleExhaust;
	//path for vehicle sound
	private String engineSoundPath = "engine4.wav";
	/*
	 * Here, I used PhysicsEditorShapeLibrary which comes with PhysicsEditor Software
	 * This class reads shapes from pre-created ".xml" file
	 * and creates complex shapes for us easily.
	 */
	private PhysicsEditorShapeLibrary worldPhysicsEditorShapeLibrary;
	
	// this is for creating physics world
	private PhysicsWorld levelWorldPhysics;

	//Entities loaded in the level, use to release them from the scene when level finished
	private ArrayList<IEntity> levelEntities;
	private StackPanel pauseMenuButtons;
	
	
	
	//---------------------------------------------------------
	//Container of info for the level world to be loaded
	private XmlWorld currenLevelWorld;
	
	//background of current level
	private Sprite levelBackground;
	//back backgrounds for parallax
	private ArrayList<Sprite> runtimeBackgrounds = new ArrayList<Sprite>();
	
		
		//Textures to be unloaded whe needed to free memory
		private ArrayList<ITexture> currentLevelTextures = new ArrayList<ITexture>();
	
	//sprites for sliders
	private Sprite soundSlider,accelerometerSlider;
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Physics Entities
	RevoluteJointDef rearMotorJoint;
	private RevoluteJoint rearMotor,frontMotor;
	
	//jointdefinitions for springs
	PrismaticJointDef rearSpringJoint,frontSpringJoint;
	//spring joints
	PrismaticJoint rearSpring, frontSpring;
	//vehicle bodies
	Body vehicleBody,rearWheelBody,frontWheelBody,rearAxleBody,frontAxleBody;
	//vehicle body shape
	Shape vehicleBodyShape;
	//connection lines between wheel and vehicle body
	Line rearLine,frontLine;
	//
	List<XmlData> worldItems = new LinkedList<XmlData>();
	
	//shapes for dead areas
	LinkedList<Shape> deadAreaShapeList = new LinkedList<Shape>();
	//shape for finish area
	Shape finishAreaShape;
	
	//these store vehicle properties
	XmlData vehicleNode = new XmlData(),
					vehicleContainer = new XmlData(),
					rearWheelNode = new XmlData(),
					frontWheelNode = new XmlData(),
					rearSpringNode = new XmlData(),
					frontSpringNode = new XmlData();
	//sprites for wheels
	private Sprite rearWheelSprite,
			frontWheelSprite,
			rearAxleSprite,
			frontAxleSprite;	

	
	/*
	 * this listener checks if the game is finished or the user was failed
	 */
	ContactListener gameContactListener = new ContactListener() {
		@Override
		public void preSolve(Contact contact, Manifold oldManifold) {}
		@Override
		public void postSolve(Contact contact, ContactImpulse impulse) {}
		@Override
		public void endContact(Contact contact) {}
		@Override
		public void beginContact(Contact contact) {
			if (contact.getFixtureA().isSensor()) {
				if (contact.getFixtureA().getBody().getUserData().toString().equals("dead"))
					isFailed = true;
				if (contact.getFixtureA().getBody().getUserData().toString().equals("finish"))
					isFinished = true;
			}
			if (contact.getFixtureB().isSensor()) {
				if (contact.getFixtureB().getBody().getUserData().toString().equals("dead"))
					isFailed = true;
				if (contact.getFixtureB().getBody().getUserData().toString().equals("finish"))
					isFinished = true;
			}
		}
	};
	
	@Override
	public void onAccelerometerChanged(AccelerometerData pAccelerometerData) {
		float accelerometerData = pAccelerometerData.getX() / 4;
		//if the phone rotated right
		if (accelerometerData > 1)accelerometerData = 1;
		//if the phone rotated left
		if (accelerometerData < -1)accelerometerData = -1;
		if (vehicleBody != null && isLevelStarted)
			vehicleBody.applyTorque(accelerometerData * 900 * accelerometerTiltAlpha);
	}
	
	/*
     * Actions when back key pressed
     */
    @Override
	public void onBackPressed() {
		   switch (currentScene) {
		case scene_options:
			//if current scene is options return main menu
			closeScene(optionScreen);
			openScene(mainScreen);
			currentScene = scene_main;
			break;
		case scene_help:
			//if current scene is help menu return main menu
			closeScene(helpScreen);
			mainScene.setBackground(new SpriteBackground(globalBackground));
			openScene(mainScreen);
			currentScene = scene_main;
			break;
		case scene_author:
			//if current scene is author info return main menu
			closeScene(aboutScreen);
			openScene(mainScreen);
			currentScene = scene_main;
			break;
		//case scene_levelSelect:
		case scene_pause:
			//if current scene is pause return gameplay
			closeScene(pauseScreen); 
			generateUpdateHandler();
			isPaused = false;
			currentScene = scene_null;
			break;
		case scene_main:
			//if current scene is main terminate program
			finish();
		case scene_null:
			pauseGame();
		default:
			break;
		}
	}
	
	
	/* a method comes from AndEngine. Returns back an Engine Object and continues with the function onLoadResources
	 * Set settings of the game engine
	 * Set screen orientation to landscape
	 * Set the size of the game camera
	 * Ignore wake-lock option of screen
	 * Set engine to load music
	 */
	public Engine onLoadEngine() {
		final Display display = getWindowManager().getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight = display.getHeight();
		//Set bounds of the main camera of game
		cameraBound = new BoundCamera(0, 0, screenWidth, screenHeight, 0,screenWidth, 0, screenHeight);
		cameraBound.setBoundsEnabled(true);
		//Set engine options
		final EngineOptions engineOptions = new EngineOptions(true,
				ScreenOrientation.LANDSCAPE,
				new RatioResolutionPolicy(screenWidth, screenHeight), cameraBound)
				.setWakeLockOptions(WakeLockOptions.SCREEN_ON);
		engineOptions.getTouchOptions().setRunOnUpdateThread(true);
		//Set sound usage
		engineOptions.setNeedsMusic(true);
		engineOptions.setNeedsSound(true);
		return new Engine(engineOptions);
	}

	/*
	 * a mehtod comes from AndEngine. After this function end, it continue with the function onLoadScene
	 * I tried to load game sounds
	 * and inserted images of vehicles to my hash map.
	 */
	public void onLoadResources() {
		//Try to load the game music
		try {
			//get sound file from assets
			vehicleExhaust = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "sfx/" + engineSoundPath);
			// set looping enables
			vehicleExhaust.setLooping(true);
			//default level for volume
			vehicleExhaust.setVolume(0.5f);
		} catch (Exception e) {
			System.out.println("Error occured while initializing sound");
		}
		//Insert images of vehicle's bodies and wheels to hash map
		for(int i=0 ; i < numOfvehicles ; i++){
			BitmapTextureAtlasEx vehicleAtlas;
			TextureRegion vehicleBodyRegion;
			TextureRegion vehicleWheelRegion;
			vehicleAtlas = new BitmapTextureAtlasEx(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
			vehicleWheelRegion = vehicleAtlas.appendTextureAsset(this, "gfx/vehicle"+i+"/body.png");
			gameTextureRegionHashMap.put("vehicle_body_"+i, vehicleWheelRegion);
			vehicleBodyRegion = vehicleAtlas.appendTextureAsset(this, "gfx/vehicle"+i+"/wheel.png");
			gameTextureRegionHashMap.put("vehicle_wheel_"+i, vehicleBodyRegion);
			this.mEngine.getTextureManager().loadTexture(vehicleAtlas);
		}
	}
	
	/*
	 * a mehtod comes from AndEngine. Returns back an Scene Object and continues with the function onLoadComplete
	 * Here, I load a background image which will be used along the game-play
	 * After, called all scene creations here
	 * Lastly, started the main scene of the game
	 */
	@SuppressWarnings("deprecation")
	public Scene onLoadScene() {
		//get display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		mEngine.registerUpdateHandler(new FPSLogger());
		// set the main background of the game
		BitmapTextureAtlas backgroundAtlas;
		TextureRegion backgroundRegion ;
		backgroundAtlas = new BitmapTextureAtlas(2048, 2048,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		backgroundRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(backgroundAtlas, this, "gfx/menubackground.png", 0, 0);
		globalBackground = new Sprite(0, 0, backgroundRegion);
		globalBackground.setScaleCenter(0, 0);
		globalBackground.setScale(deviceDisplay.getWidth() / globalBackground.getWidth(), deviceDisplay.getHeight() / globalBackground.getHeight());
		mEngine.getTextureManager().loadTexture(backgroundAtlas);
		// Create an instance for main scene
		mainScene = new Scene();
		mainScene.setBackground(new SpriteBackground(globalBackground));
		mainScene.setOnSceneTouchListener(this);

		//Lets build all scenes here
		buildMainScene();
		buildOptionsScene();
		buildHelpScene();
		buildAuthorScene();
		buildPauseScene();
		buildLoadingScene();
		buildVehicleSelectScene();
		buildLevelSelectScene();
		
		// set bindings enabled
		mainScene.setTouchAreaBindingEnabled(true);
		// set current scene to main scene
		openScene(mainScreen);
		currentScene = scene_main;
		// return a scene for AngEngine
		return mainScene;
	}
	
	/*
	 * Last built in function of the andEngine
	 * Here we say all part for creating an AndEngine world is completed
	 */
	public void onLoadComplete() {
		System.out.println("Game Succesfully Loaded.");
	}

	/*
	 * This function let us to consider the direction of the vehicle by using touch points on screen
	 * We divide screen 3 parts like below
	 *            PHONE SCREEN
	 * #----------------------------------#
	 * #          |           |           #
	 * # BACKWARD |           |  FORWARD  #
	 * #   AREA   |           |    AREA   #
	 * #          |           |           #
	 * #----------------------------------#
	 */
	public boolean onSceneTouchEvent(final Scene scene,final TouchEvent touchEvent) {
		final Display display = getWindowManager().getDefaultDisplay();
		int screenWidth = display.getWidth();
		
		// If the game is not started yet, return
		if (!isLevelStarted)
			return false;
		//Check if the game is not in pause state 
		if (!isPaused)
			// if the user touch to screen check followings
			if (touchEvent.isActionDown()|| touchEvent.isActionMove()) {
				//if the forward area pressed set gas pressed
				if (touchEvent.getMotionEvent().getRawX() > screenWidth/ 2 + screenWidth / 5) {
					if (!gasPressed)	vehicleExhaust.play();
					gasPressed = true;
					brakesPressed = false;
				//if the backward area presssed set brakes pressed
				} else if (touchEvent.getMotionEvent().getRawX() < screenWidth/ 2 - screenWidth / 5) {
					brakesPressed = true;
					gasPressed = false;
					vehicleExhaust.stop();
				// if the user touchs an area between forward and backward set both non-pressed
				} else if(!isPaused && touchEvent.isActionMove()){
					brakesPressed = gasPressed = false;
					vehicleExhaust.stop();
				}
			} 
			// if the user does not touch the screen, do nothing. Set both pedals false
			else {
				brakesPressed = gasPressed = false;
				vehicleExhaust.stop();
			}

		return true;
	}
	
	/*
	 * This is for resuming game from pause state
	 */
	@Override
	public void onResumeGame() {
		super.onResumeGame();
	}
	/*
	 * This is for pausing game when the user clicked Back Key during game-play.
	 */
	@Override
	public void onPauseGame() {
		super.onPauseGame();
		finish();
	}


	private void generateUpdateHandler() {
		gameUpdateHandler = new myUpdateHandler(this);
		this.mEngine.registerUpdateHandler(gameUpdateHandler);
		this.mainScene.registerUpdateHandler(levelWorldPhysics);
	}
	
	//Update handler of the Entire application
	// update position and looking if you are in a dead area for restart or if
	// you reached the finish line
	// also here is the checking for the left and right pressed flags
	class myUpdateHandler implements IUpdateHandler {
		
		IAccelerometerListener c;
		public myUpdateHandler(IAccelerometerListener c){
			this.c = c;
		}
		
		public void onUpdate(float pSecondsElapsed) {

			if (preUpdates > 3) {
				preUpdates = -1;
				enableAccelerometerSensor(c);
				isLevelStarted = true;
				closeScene(loadingScreen);
				currentScene = scene_null;
				refreshEntities();
			} else if (preUpdates >= 0)
				preUpdates += pSecondsElapsed;

			final Display display = getWindowManager().getDefaultDisplay();
			int CAMERA_WIDTH = display.getWidth();
			
			if (gasPressed) {
				//accelerate forwards
				rearMotor.enableMotor(true);
				rearMotor.setMotorSpeed(speed_forward);
				rearMotor.setMaxMotorTorque(torque_forward);

				frontMotor.enableMotor(true);
				frontMotor.setMotorSpeed(speed_forward);
				frontMotor.setMaxMotorTorque(torque_forward);
				
			} else if (brakesPressed) {
				//brake if wheels going forwards, otherwise reverse
				
				if(rearMotor.getMotorSpeed() > 0){
					//brake
					rearMotor.enableMotor(true);
					rearMotor.setMotorSpeed(0);
					rearMotor.setMaxMotorTorque(torque_brake);
	
					frontMotor.enableMotor(true);
					frontMotor.setMotorSpeed(0);
					frontMotor.setMaxMotorTorque(torque_brake);
				}
				else{
					//reverse
					rearMotor.enableMotor(true);
					rearMotor.setMotorSpeed(speed_reverse);
					rearMotor.setMaxMotorTorque(torque_reverse);

					frontMotor.enableMotor(true);
					frontMotor.setMotorSpeed(speed_reverse);
					frontMotor.setMaxMotorTorque(torque_reverse);
				}
				
			} else {
				//coast (no acceleration/deceleration)
				rearMotor.setMotorSpeed(0);
				rearMotor.setMaxMotorTorque(0);
				frontMotor.setMotorSpeed(0);
				frontMotor.setMaxMotorTorque(0);
			}

			final Vector2 cartCenter = vehicleBody.getWorldCenter();
			final Vector2 wheel1Center = rearWheelBody.getWorldCenter();
			rearLine.setPosition(cartCenter.x
					* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
					cartCenter.y
							* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
					wheel1Center.x
							* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
					wheel1Center.y
							* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);

			final Vector2 wheel2Center = frontWheelBody.getWorldCenter();
			frontLine.setPosition(cartCenter.x
					* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
					cartCenter.y
							* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
					wheel2Center.x
							* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
					wheel2Center.y
							* PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
			// springs
			rearSpring.setMaxMotorForce((float) (40 + Math.abs(400 * Math.pow(
					rearSpring.getJointTranslation(), 2))));
			rearSpring
					.setMotorSpeed((float) ((rearSpring.getMotorSpeed() - 10 * rearSpring
							.getJointTranslation()) * 0.4));

			frontSpring.setMaxMotorForce((float) (40 + Math.abs(400 * Math.pow(
					frontSpring.getJointTranslation(), 2))));
			frontSpring
					.setMotorSpeed((float) ((frontSpring.getMotorSpeed() - 10 * frontSpring
							.getJointTranslation()) * 0.4));
			
			//If you are in a dead area then Restart
			if (isFailed)
				reloadCurrentLevel();
			isFailed = false;

			//If you reach the end, you will go to the next level in case there is one in other case
			//the main screen will be shown
			if (isFinished)
				if (selectedLevelIndex < numOfLevels - 1) {
					selectedLevelIndex++;
					destroyCurrentLevel();
					preUpdates = 0;
					openScene(loadingScreen);
					currentScene = scene_loading;
					Thread thread = new Thread(new LevelLoader());
					thread.start();
					mainScene.sortChildren();
				} else {
					destroyCurrentLevel();
					preUpdates = 0;
					mainScene.setBackground(new SpriteBackground(globalBackground));
					openScene(mainScreen);
					currentScene = scene_main;
				}
			isFinished = false;
			
			//Update for the different backs in the parallax
			for (Sprite sp : runtimeBackgrounds) {
				float val = ((levelBackground.getWidthScaled() - sp.getWidthScaled()) / (levelBackground.getWidthScaled() - CAMERA_WIDTH))
						* cameraBound.getMinX();
				sp.setPosition(val, 0);
			}
		}

		public void reset() {
		}
	}

	// This is for removing a scene from screen
	private void closeScene(ArrayList<Sprite> spriteArrayOfScene) {
		for (Sprite spriteToRemove : spriteArrayOfScene) {
			// delete sprites from scene
			mainScene.detachChild(spriteToRemove);
			// remove touch bindings areas
			mainScene.unregisterTouchArea(spriteToRemove);
			// remove texture from screen
			mEngine.getTextureManager().unloadTexture(spriteToRemove.getTextureRegion().getTexture());
		}
	}

	// This is for loading a scene to the screen
	private void openScene(ArrayList<Sprite> spriteArrayOfScene) {
		for (Sprite spriteToLoad : spriteArrayOfScene) {
			// add sprites to target scene
			mainScene.attachChild(spriteToLoad);
			// set touch bindings properties
			mainScene.registerTouchArea(spriteToLoad);
			// load texture of sprite
			mEngine.getTextureManager().loadTexture(spriteToLoad.getTextureRegion().getTexture());
		}
	}

	public float middleJointOf(float dimen1, float dimen2) {
		return (dimen1 - dimen2) / 2f;
	}

	/*
	 * There are 5 void function to create game scenes
	 * These are:
	 * 		mainScene
	 * 		pauseScene
	 * 		vehicleSelectScene
	 * 		levelSelectScene
	 * 		authorScene
	 * 		helpScene
	 * 		optionsScene
	 * 		loadingScene
	 */
	private void buildMainScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		//create objects to load sprites to scene
		mainScreen = new ArrayList<Sprite>();
		BitmapTextureAtlas tAtlas;
		TextureRegion tRegion ;
		Sprite spriteDef;
		UISprite tempISprite;
		
		// add game title image to screen
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/game_title.png", 0,0);
        spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 4 * tRegion.getHeight() + 20), tRegion);
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
        tempISprite.setDimens(0.5f , 1/5f , 0.26f , 0.01f);
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		mainScreen.add(tempISprite.sprite);

		//add the name of the author to the screen  
		tAtlas = new BitmapTextureAtlas(1024, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/author.png", 0, 0);
		spriteDef = new Sprite(screenWidth - tRegion.getWidth()- 10, screenHeight - tRegion.getHeight() + 5,tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(mainScreen);
					openScene(aboutScreen);
					currentScene = scene_author;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.width = 0.2f;
		tempISprite.height = 1/9f;
		tempISprite.bottomMargin = 0.0f;
		tempISprite.rightMargin = 0.0f;
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		mainScreen.add(tempISprite.sprite);
		
		/*
		 * Create a panel for game buttons
		 * There was 4 buttons:
		 * 		PLAY
		 * 		OPTIONS
		 * 		HELP
		 * 		EXIT
		 */
		StackPanel menuButtons = new StackPanel(screenWidth, screenHeight);	
		
		// add play button to menu
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/buttons/play.png", 0,0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 4 * tRegion.getHeight() + 20), tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(mainScreen);
						openScene(vehicleSelectScreen);
						currentScene = scene_vehicleSelect;
					return true;
				}
				return false;
            }
        };
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens(0.41f , 1/8f , 0.0f , 0.0f);
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		menuButtons.AddElement(tempISprite);
		
		// add options button to menu
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/buttons/options.png", 0,0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 4 * tRegion.getHeight() + 20)+ tRegion.getHeight() + 10, tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(mainScreen);
					openScene(optionScreen);
					currentScene = scene_options;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens( 0.41f , 1/8f , 0.0f , 0.0f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		menuButtons.AddElement(tempISprite);
		
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/buttons/help.png", 0, 0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 4 * tRegion.getHeight() + 20)+ 2 * tRegion.getHeight() + 20, tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(mainScreen);
					mainScene.setBackground(new ColorBackground(0, 0, 0));
					openScene(helpScreen);
					currentScene = scene_help;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens( 0.41f , 1/8f , 0.0f , 0.0f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		menuButtons.AddElement(tempISprite);
		
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/buttons/exit.png", 0, 0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 4 * tRegion.getHeight() + 20)+ 3 * tRegion.getHeight() + 30, tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					finish();
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens( 0.41f , 1/8f , 0.0f , 0.0f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		menuButtons.AddElement(tempISprite);
		
		// add all menu elements to scene
		for (UIElement buttonInPanel : menuButtons.elements) {
			mainScreen.add(((UISprite)buttonInPanel).sprite);
		}
	}

	private void buildPauseScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//create objects to load sprites to scene
		pauseScreen = new ArrayList<Sprite>();
		BitmapTextureAtlas tAtlas;
		TextureRegion tRegion ;
		Sprite spriteDef;
		UISprite tempISprite;
		pauseMenuButtons = new StackPanel(screenWidth, screenHeight);
		
		/*
		 * Add a continue button to menu
		 */
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/buttons/continue.png", 0, 0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 3 * tRegion.getHeight() + 20), tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(pauseScreen);
					currentScene = scene_null;
					generateUpdateHandler();
					isPaused = false;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens( 0.41f , 1/8f , 0.0f , 0.0f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		pauseMenuButtons.AddElement(tempISprite);
		
		/*
		 * Add a restart button to menu
		 */
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/buttons/restart.png", 0, 0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 3 * tRegion.getHeight() + 20)+ 2 * tRegion.getHeight() + 20, tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(pauseScreen);
					reloadCurrentLevel();
					isPaused = false;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens( 0.41f , 1/8f , 0.0f , 0.0f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		pauseMenuButtons.AddElement(tempISprite);

		/*
		 * Add an exit button to menu
		 */
		tAtlas = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/buttons/exit.png", 0, 0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, 3 * tRegion.getHeight() + 20)+ tRegion.getHeight() + 10, tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					preUpdates = 0;
					destroyCurrentLevel();
					closeScene(pauseScreen);
					mainScene.setBackground(new SpriteBackground(globalBackground));
					openScene(mainScreen);
					currentScene = scene_main;
					isPaused = false;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens( 0.41f , 1/8f , 0.0f , 0.0f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		pauseMenuButtons.AddElement(tempISprite);
		
		// add each button to scene
		for (UIElement elementInMenu : pauseMenuButtons.elements) {
			pauseScreen.add(((UISprite)elementInMenu).sprite);
		}
	}
	private void buildVehicleSelectScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//create objects to load sprites to scene
		vehicleSelectScreen = new ArrayList<Sprite>();
		StackPanel vehicleImages = new StackPanel(screenWidth, screenHeight);
		BitmapTextureAtlas tAtlas;
		TextureRegion tRegion;
		Sprite spriteDef;
		UISprite tempISprite;
		
		/*
		 * Here set the background of the vehicle select menu
		 */
		tAtlas = new BitmapTextureAtlas(1024, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this,"gfx/vehicleSelectBackground.png", 0, 0);
		spriteDef = new Sprite(0, 0, tRegion);
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens(1.00f , 1.00f , 0.00f , 0.00f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		vehicleSelectScreen.add(tempISprite.sprite);
		
		/*
		 * add and back key to return main menu
		 */
		tAtlas = new BitmapTextureAtlas(1024, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this,"gfx/backarrow.png", 0, 0);
		spriteDef = new Sprite(5, screenHeight- tRegion.getHeight(), tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t,final float x, final float y) {
				if (t.isActionDown()) {
					closeScene(vehicleSelectScreen);
					openScene(mainScreen);
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.width = 0.1f;
		tempISprite.height = 0.15f;
		tempISprite.leftMargin = 0.01f;
		tempISprite.bottomMargin= 0.03f;
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		vehicleSelectScreen.add(tempISprite.sprite);
		
		/*
		 * This loop adds images of vehicles to the menu  
		 */
		for(int i=0; i< numOfvehicles ; i++){
			tAtlas = new BitmapTextureAtlas(256, 128,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
			tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/vehicle"+i+"/embed.png", 0, 0);
			final int tempInt = i;
			spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, tRegion.getHeight()), tRegion) {
				@Override
				public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
					if (t.isActionDown()) {
						selectedVehicleIndex=tempInt;
						closeScene(vehicleSelectScreen);
						openScene(levelSelectScreen);
						currentScene = scene_levelSelect;
						return true;
					}
					return false;
				}
			};
			tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
	        tempISprite.width = 0.25f;
	        tempISprite.height = 0.25f;
			tempISprite.bottomMargin = 0.0f;
			tempISprite.rightMargin = 0.0f;
			tempISprite.SetProperties();
			mEngine.getTextureManager().loadTexture(tAtlas);
			vehicleImages.AddElement(tempISprite);
		}
		
		// this loop adds images of vehicles from panel to select menu
		for (UIElement vehicleSprite : vehicleImages.elements) {
			vehicleSelectScreen.add(((UISprite)vehicleSprite).sprite);
		}
		
	}
	
	private void buildLevelSelectScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//create objects to load sprites to scene
		levelSelectScreen = new ArrayList<Sprite>();
		UISprite tempUISprite;
		BitmapTextureAtlas tAtlas;
		TextureRegion tRegion;
		Sprite spriteDef;
		
		/*
		 * Here set the background of the level select menu
		 */
		tAtlas = new BitmapTextureAtlas(1024, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this,"gfx/levelBackground.png", 0, 0);
		spriteDef = new Sprite(0, 0, tRegion);
		tempUISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempUISprite.setDimens( 1.00f , 1.00f , 0.00f , 0.00f );
		tempUISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		levelSelectScreen.add(tempUISprite.sprite);
		
		/*
		 * add a back arrow key to return main menu
		 */
		tAtlas = new BitmapTextureAtlas(1024, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this,"gfx/backarrow.png", 0, 0);
		spriteDef = new Sprite(5, screenHeight- tRegion.getHeight(), tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t,final float x, final float y) {
				if (t.isActionDown()) {
					closeScene(levelSelectScreen);
					openScene(mainScreen);
					return true;
				}
				return false;
			}
		};
		tempUISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempUISprite.width = 0.1f;
		tempUISprite.height = 0.15f;
		tempUISprite.leftMargin = 0.01f;
		tempUISprite.bottomMargin= 0.03f;
		tempUISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		levelSelectScreen.add(tempUISprite.sprite);
		
		/*
		 * This function adds caption images of levels to the select screen  
		 */
		for(int i=0; i< numOfLevels ; i++){
			tAtlas = new BitmapTextureAtlas(256, 128,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
			BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("levels/level" + i + "/gfx/");
			tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "caption.png", 0, 0);
			final int tempInt = i;
			spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, tRegion.getHeight()), tRegion) {
				@Override
				public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
					if (t.isActionDown()) {
						closeScene(levelSelectScreen);
						openScene(loadingScreen);
						mainScene.setBackground(new ColorBackground(0,0,0));
						currentScene = scene_loading;
						selectedLevelIndex = tempInt;
						Thread thread = new Thread(
								new LevelLoader());
						thread.start();
						return true;
					}
					return false;
				}
			};
			tempUISprite = new UISprite(screenWidth, screenHeight, spriteDef);
	        tempUISprite.width = 0.25f;
	        tempUISprite.height = 0.25f;
			tempUISprite.topMargin = 0.15f;
			tempUISprite.leftMargin = 0.08f+i*0.30f;
			tempUISprite.SetProperties();
			mEngine.getTextureManager().loadTexture(tAtlas);
			/*
			 * Add all level capion images to screen
			 */
			levelSelectScreen.add((tempUISprite).sprite);
		}
		
	}
		
	private void buildAuthorScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//create objects to load sprites to scene

		aboutScreen = new ArrayList<Sprite>();
		
		/*
		 * add a title to scene
		 */
		BitmapTextureAtlas tempTexture = new BitmapTextureAtlas(256, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		TextureRegion tempTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tempTexture, this, "gfx/abouttitle.png", 0, 0);
		Sprite tempSprite = new Sprite(10, 5, tempTextureRegion);
		UISprite us = new UISprite(screenWidth, screenHeight, tempSprite);
		us.setDimens(0.4f , 0.25f , 0.05f , 0.05f );
		us.SetProperties();
		mEngine.getTextureManager().loadTexture(tempTexture);
		aboutScreen.add(us.sprite);

		
		tempTexture = new BitmapTextureAtlas(512, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tempTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tempTexture, this, "gfx/aboutcaption.png", 0,0);
		tempSprite = new Sprite(middleJointOf(screenWidth, tempTextureRegion.getWidth()), middleJointOf(screenHeight, tempTextureRegion.getHeight()), tempTextureRegion);
		us = new UISprite(screenWidth, screenHeight, tempSprite);
		us.width = 0.7f;
		us.height = 1/2f;		
		us.SetProperties();
		us.SetHorizontalAligmentCenter();
		us.SetVerticalAligmentCenter();
		mEngine.getTextureManager().loadTexture(tempTexture);
		aboutScreen.add(us.sprite);

		/*
		 * add a back arrow image to return main menu
		 */
		tempTexture = new BitmapTextureAtlas(512, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tempTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tempTexture, this, "gfx/backarrow.png", 0, 0);
		tempSprite = new Sprite(5, screenHeight- tempTextureRegion.getHeight(), tempTextureRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(aboutScreen);
					openScene(mainScreen);
					currentScene = scene_main;
					return true;
				}
				return false;
			}
		};
		us = new UISprite(screenWidth, screenHeight, tempSprite);
		us.width = 0.1f;
		us.height = 0.15f;
		us.leftMargin = 0.01f;
		us.bottomMargin = 0.01f;
		us.SetProperties();
		mEngine.getTextureManager().loadTexture(tempTexture);
		aboutScreen.add(us.sprite);
		
	}

	private void buildHelpScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//create objects to load sprites to scene
		helpScreen = new ArrayList<Sprite>();
		BitmapTextureAtlas tAtlas;
		TextureRegion tRegion;
		Sprite spriteDef;
		UISprite tempISprite;
		
		/*
		 * add title of the scene
		 */
		tAtlas = new BitmapTextureAtlas(256, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/helptitle.png", 0, 0);
		spriteDef = new Sprite(10, 5, tRegion);
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.setDimens( 0.4f , 0.23f , 0.05f , 0.02f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		helpScreen.add(tempISprite.sprite);

		/*
		 * add help image to scene
		 */
		tAtlas = new BitmapTextureAtlas(512, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/control_example.png",0, 0);
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, tRegion.getHeight()), tRegion);
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.width = 0.80f;
		tempISprite.height = 0.70f;
		tempISprite.SetProperties();
		tempISprite.SetHorizontalAligmentCenter();
		tempISprite.SetVerticalAligmentCenter();
		mEngine.getTextureManager().loadTexture(tAtlas);
		helpScreen.add(tempISprite.sprite);

		/*
		 * create an back arrow to return main menu 
		 */
		tAtlas = new BitmapTextureAtlas(256, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/backarrow.png", 0, 0);
		spriteDef = new Sprite(5, screenHeight- tRegion.getHeight(), tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(helpScreen);
					mainScene.setBackground(new SpriteBackground(globalBackground));
					openScene(mainScreen);
					currentScene = scene_main;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.width = 0.1f;
		tempISprite.height = 0.15f;
		tempISprite.leftMargin = 0.01f;
		tempISprite.bottomMargin = 0.01f;	
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		helpScreen.add(tempISprite.sprite);
	}

	private void buildOptionsScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//create objects to load sprites to scene
		optionScreen = new ArrayList<Sprite>();
		BitmapTextureAtlas tAtlas;
		TextureRegion tRegion ;
		Sprite spriteDef;
		UISprite tempISprite;
		StackPanel tempPanel;
		
		/*
		 * add title of the scene
		 */
		tAtlas = new BitmapTextureAtlas(512, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/optionstitle.png", 0,0);
		spriteDef = new Sprite(10, 5, tRegion);
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);	
		tempISprite.setDimens( 0.4f , 0.23f , 0.05f , 0.02f );
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		optionScreen.add(tempISprite.sprite);

		/*
		 * add a background for options menu
		 */
		tAtlas = new BitmapTextureAtlas(512, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/optionscontent.png",0, 0);
		final float cx, cy;
		cx = middleJointOf(screenWidth, tRegion.getWidth());
		cy = middleJointOf(screenHeight, tRegion.getHeight());
		spriteDef = new Sprite(middleJointOf(screenWidth, tRegion.getWidth()), middleJointOf(screenHeight, tRegion.getHeight()), tRegion);
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.width = 0.875f;
		tempISprite.height = 0.625f;	
		tempISprite.SetProperties();
		tempISprite.SetHorizontalAligmentCenter();
		tempISprite.SetVerticalAligmentCenter();
		mEngine.getTextureManager().loadTexture(tAtlas);
		optionScreen.add(tempISprite.sprite);

		/*
		 * add a back arrow to return main menu
		 */
		tAtlas = new BitmapTextureAtlas(256, 256,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/backarrow.png", 0, 0);
		spriteDef = new Sprite(5, screenHeight- tRegion.getHeight(), tRegion) {
			@Override
			public boolean onAreaTouched(final TouchEvent t, final float x,final float y) {
				if (t.isActionDown()) {
					closeScene(optionScreen);
					openScene(mainScreen);
					currentScene = scene_main;
					return true;
				}
				return false;
			}
		};
		tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.width = 0.1f;
		tempISprite.height = 0.15f;
		tempISprite.leftMargin = 0.01f;
		tempISprite.bottomMargin = 0.01f;		
		tempISprite.SetProperties();
		mEngine.getTextureManager().loadTexture(tAtlas);
		optionScreen.add(tempISprite.sprite);
		
		/*
		 * create a panel to store 2 slider:
		 * 		Sound Slider
		 * 		Accelerometer Slider
		 */
		tempPanel = new StackPanel(screenWidth, screenHeight);
		tempPanel.elementsMargin = 0.3f;
		
		/*
		 * create two background line for sliders
		 */
		for(int i=0 ; i<2 ;i++){
			tAtlas = new BitmapTextureAtlas(512, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
			tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/line.png", 0, 0);
			spriteDef = new Sprite(5, screenHeight- tRegion.getHeight(), tRegion) ;
			tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
			tempISprite.width = 0.78f;
			tempISprite.height = 0.05f;	
			tempISprite.SetProperties();
			mEngine.getTextureManager().loadTexture(tAtlas);
			tempPanel.AddElement(tempISprite);
		}
		
		/*
		 * add slider backgrounds to scene
		 */
		for (UIElement sliderBackground : tempPanel.elements) {
			optionScreen.add(((UISprite)sliderBackground).sprite);
		}
		
		/*
		 * get positions of sliders 
		 */
		final float sliderStartPoint = tempISprite.sprite.getX();
		final float sliderEndPoint = tempISprite.sprite.getX() + tempISprite.actualWidth - screenWidth * 0.07f;
		
		/*
		 * add a yellow line from start to center width of each slider
		 */
		tAtlas = new BitmapTextureAtlas(512, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/lineYellow.png", 0, 0);
		//load texture to main engine
		mEngine.getTextureManager().loadTexture(tAtlas);
		
		//sprite for sound slider
		soundSlider = new Sprite(sliderStartPoint, ((UISprite)tempPanel.elements.get(0)).sprite.getY(), tRegion) ;
		soundSlider.setScaleCenter(0, 0);
		//This is for set slider to center position
		soundSlider.setScale(((sliderEndPoint - sliderStartPoint)/2.0f) / soundSlider.getWidth(), tempISprite.scaleY);
		//add sound slider to scene
		optionScreen.add(soundSlider);
		
		//sprite for accelerometer slider
		accelerometerSlider = new Sprite(sliderStartPoint, ((UISprite)tempPanel.elements.get(1)).sprite.getY(), tRegion) ;
		accelerometerSlider.setScaleCenter(0, 0);
		//This is for set slider to center position
		accelerometerSlider.setScale(((sliderEndPoint - sliderStartPoint)/2.0f) / accelerometerSlider.getWidth(), tempISprite.scaleY);
		//add accelerometer slider to scene
		optionScreen.add(accelerometerSlider);
		
		/*
		 * create two panel for slider button
		 */
		tempPanel = new StackPanel(screenWidth, screenHeight);
		tempPanel.elementsMargin = 0.28f;
		
		for(int i=0 ; i<2 ;i++){
			tAtlas = new BitmapTextureAtlas(32, 32,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
			tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/thumb.png", 0, 0);
			mEngine.getTextureManager().loadTexture(tAtlas);
			final int index=i;
			spriteDef = new Sprite(sliderEndPoint, cy + 54 + i*93, tRegion) {
				@Override
				public boolean onAreaTouched(final TouchEvent t, final float x,
						final float y) {
					if (t.isActionMove() || t.isActionDown()) {
						// this case works for sound slider
						if(index==0){
							float sliderPosition = t.getX();
							//sliderPosition = Math.max(sliderStartPoint, sliderPosition);
							sliderPosition = Math.min(sliderEndPoint, sliderPosition);
							this.setPosition(sliderPosition, this.getY());
							//set the scale of slider
							vehicleExhaust.setVolume((sliderPosition - sliderStartPoint)/ (sliderEndPoint - sliderStartPoint));
							soundSlider.setScaleX((sliderPosition - sliderStartPoint)/soundSlider.getWidth());
						}
						//this case works for accelerometer slider
						else{
							float sliderPosition = t.getX();
							//sliderPosition = Math.max(sliderStartPoint, sliderPosition);
							sliderPosition = Math.min(sliderEndPoint, sliderPosition);
							this.setPosition(sliderPosition, this.getY());
							//set the scale of slider
							accelerometerSlider.setScaleX((sliderPosition - sliderStartPoint) / accelerometerSlider.getWidth() );
							accelerometerTiltAlpha = (sliderPosition - sliderStartPoint) / accelerometerSlider.getWidth();
						}
						return true;
					}
					return false;
				}
			};
			tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
			tempISprite.width = 0.07f;
			tempISprite.height = 0.07f;	
			tempISprite.SetProperties();
			tempPanel.AddElement(tempISprite);
		}
		
		for (UIElement sliderButtons : tempPanel.elements) {
			optionScreen.add(((UISprite)sliderButtons).sprite);
		}
	}
	private void buildLoadingScene() {
		//get device display properties
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		//get width and height of device display
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//create objects to load sprites to scene
		loadingScreen = new ArrayList<Sprite>();
		
		/*
		 * 
		 */
		BitmapTextureAtlas tAtlas = new BitmapTextureAtlas(512, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		TextureRegion tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, "gfx/loading.png", 0, 0);
		Sprite spriteDef = new Sprite(middleJointOf(screenWidth,tRegion.getWidth()), middleJointOf(screenHeight,tRegion.getHeight()), tRegion);
		UISprite tempISprite = new UISprite(screenWidth, screenHeight, spriteDef);
		tempISprite.width = 0.83f;
		tempISprite.height = 0.725f;
		tempISprite.SetProperties();
		tempISprite.SetHorizontalAligmentCenter();
		tempISprite.SetVerticalAligmentCenter();
		mEngine.getTextureManager().loadTexture(tAtlas);
		loadingScreen.add(tempISprite.sprite);
	}
	


	class LevelLoader extends Thread {
		public void run() {
			loadLevel(selectedLevelIndex);
		}
	}
	// Load the specified level
	private void loadLevel(int levelIndex) {
		isFinished = false;
		deadAreaShapeList = new LinkedList<Shape>();
		worldItems = new LinkedList<XmlData>();
		currenLevelWorld = new XmlWorld();
		selectedLevelIndex = levelIndex;
		levelEntities = new ArrayList<IEntity>();

		AssetManager assetManager = getAssets();
		try {
			InputStream xmlInputStream = assetManager.open("levels/level" + levelIndex+ "/world.xml");
			// if successfully opened
			if (xmlInputStream != null) {
				try {
					//create an instance of DocumentBuilderFactory 
					DocumentBuilderFactory docBuildFactory = DocumentBuilderFactory.newInstance();
					docBuildFactory.setValidating(false);
					docBuildFactory.setIgnoringComments(false);
					docBuildFactory.setIgnoringElementContentWhitespace(true);
					docBuildFactory.setNamespaceAware(true);
					//create an document builder instance
					DocumentBuilder docBuilder = null;
					docBuilder = docBuildFactory.newDocumentBuilder();
					//Create a document and parse the ".xml" file
					Document doc = docBuilder.parse(xmlInputStream);
					
					// the first element is "background-front"
					Element headElement = doc.getDocumentElement();
					currenLevelWorld.frontBackground = headElement.getAttribute("background-front");
					currenLevelWorld.backBackground = new ArrayList<String>();
					
					//get all variable names  from the string array : backgrounds
					NodeList nodeNames = headElement.getElementsByTagName("backgrounds");
					if (nodeNames.getLength() == 1) {
						/*
						 * get properties of vehicle : start coordinates
						 */
						Element vehicle = (Element) nodeNames.item(0);
						if (Integer.parseInt(vehicle.getAttribute("length")) > 0) {
							//get the list of nodes
							NodeList itemNodeList = vehicle.getElementsByTagName("background");
							int nodeCount = itemNodeList.getLength();
							// add nodes from ".xml" to level object
							for (int i = 0; i < nodeCount; i++) {
								Element node = (Element) itemNodeList.item(i);
								currenLevelWorld.backBackground.add(node.getAttribute("name"));
							}
						}
					}
					//get width and height of the level
					currenLevelWorld.width = Integer.parseInt(headElement.getAttribute("width"));
					currenLevelWorld.height = Integer.parseInt(headElement.getAttribute("height"));
					// set the level index
					currenLevelWorld.index = levelIndex;
					
					//get the vehicle node from xml
					NodeList carNodeList = headElement.getElementsByTagName("car");
					//set initial indexes of vehicle
					if (carNodeList.getLength() == 1) {
						Element carElement = (Element) carNodeList.item(0);
						this.vehicleNode.x = Integer.parseInt(carElement.getAttribute("x"));
						this.vehicleNode.y = Integer.parseInt(carElement.getAttribute("y"));
					}
					
					//get the properties of finish area
					float xIndex, yIndex, width, height;
					NodeList finishNodeList = headElement.getElementsByTagName("finish");
					//if there is only one finish node 
					if (finishNodeList.getLength() == 1) {
						// get the finish node
						Element finishNode = (Element) finishNodeList.item(0);
						//get start indexes from finish node
						xIndex = Float.parseFloat(finishNode.getAttribute("x"));
						yIndex = Float.parseFloat(finishNode.getAttribute("y"));
						width = Float.parseFloat(finishNode.getAttribute("width"));
						height = Float.parseFloat(finishNode.getAttribute("height"));
						//add finish area as a rectangle to the level
						finishAreaShape = new Rectangle(xIndex, yIndex, width, height);
					}

					//get deadareas container from xml file
					NodeList deadAreasNodeList = headElement.getElementsByTagName("deadareas");
					// if there is a node with name deadareas
					if (deadAreasNodeList.getLength() == 1) {
						//get the first deadareas container node
						Element deadAreas = (Element) deadAreasNodeList.item(0);
						//get the numbers of the deadarea nodes
						if (Integer.parseInt(deadAreas.getAttribute("length")) > 0) {
							//set the properties for all deadareas
							NodeList deadArea = deadAreas.getElementsByTagName("deadarea");
							int numOfAreas = deadArea.getLength();
							for (int i = 0; i < numOfAreas; i++) {
								Element deadAreaNode = (Element) deadArea.item(i);
								//set start indexes of dead area
								xIndex = Float.parseFloat(deadAreaNode.getAttribute("x"));
								yIndex = Float.parseFloat(deadAreaNode.getAttribute("y"));
								//set dimensions of dead areas
								width = Float.parseFloat(deadAreaNode.getAttribute("width"));
								height = Float.parseFloat(deadAreaNode.getAttribute("height"));
								//add deadareas to the scene as a rectangle
								deadAreaShapeList.add(new Rectangle(xIndex, yIndex, width, height));
							}
						}
					}
					
					// get entities container from xml
					NodeList spritesNodeList = headElement.getElementsByTagName("entities");
					//if there is a container named entities
					if (spritesNodeList.getLength() == 1) {
						//get the entities container
						Element spritesNode = (Element) spritesNodeList.item(0);
						//get all entities from xml
						NodeList spriteNodes = spritesNode.getElementsByTagName("entity");
						int numOfSprites = spriteNodes.getLength();
						for (int i = 0; i < numOfSprites; i++) {
							Element deadAreaNode = (Element) spriteNodes.item(i);
							XmlData nodeContainer = new XmlData();
							//set starting indexes of a entity node
							nodeContainer.x = Integer.parseInt(deadAreaNode.getAttribute("x"));
							nodeContainer.y = Integer.parseInt(deadAreaNode.getAttribute("y"));
							//set dimensions of an entity
							nodeContainer.sprite = deadAreaNode.getAttribute("sprite");
							nodeContainer.shape = deadAreaNode.getAttribute("shape");
							nodeContainer.visible = Boolean.getBoolean(deadAreaNode.getAttribute("visible"));
							// add the node to a list
							worldItems.add(nodeContainer);
						}
					}
					//close xml file
					xmlInputStream.close();

				} catch (Exception e) {
					System.out.println("Error getting values from xml file");
				}
			}
		} catch (Exception e) {
			System.out.println("Error Opening world.xml file");
		}
		

		//create the physics world for the level
		levelWorldPhysics = new FixedStepPhysicsWorld(100, new Vector2(0,SensorManager.GRAVITY_DEATH_STAR_I), false, 60, 60);
		levelWorldPhysics.setGravity(new Vector2(0, 0));
		
		createLevelPhysics(currenLevelWorld);
		
		initializeCriticalAreas();
		
		initializeVehicle(selectedVehicleIndex);

		this.enableAccelerometerSensor(this);
		mainScene.sortChildren();
			
		levelWorldPhysics.setContactListener(gameContactListener);

		generateUpdateHandler();
	}

	
	private void refreshEntities() {
		//add level entities to scene after update
		for (IEntity entity : levelEntities) {
			mainScene.attachChild(entity);
		}
		//set camera to view vehicle on center
		cameraBound.setChaseEntity(vehicleBodyShape);
		//refresh gravity
		levelWorldPhysics.setGravity(new Vector2(0, 15));
		mainScene.sortChildren();
	}
	
	/*
	 * this function creates dead and finish areas for the level
	 */
	private void initializeCriticalAreas() {
		//create a fixtureDef for sensing collision
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.isSensor = true;
		// create a box body to understand the finish line
		Body finisArea = PhysicsFactory.createBoxBody(levelWorldPhysics, finishAreaShape,BodyType.StaticBody, fixtureDef);
		//set the tag of area
		finisArea.setUserData("finish");
		
		//These are for dead areas
		ArrayList<Body> deadAreas = new ArrayList<Body>();
		for (Shape node : this.deadAreaShapeList) {
			//create a box body for all dead areas
			Body temp = PhysicsFactory.createBoxBody(levelWorldPhysics, node,BodyType.StaticBody, fixtureDef);
			//set the tag of area
			temp.setUserData("dead");
			//add each of them a list
			deadAreas.add(temp);
		}
	}

	/*
	 * This function creates a selected level
	 * loads all sprites to scene
	 * creates 2D ground shapes from xml file
	 * 
	 */
	private void createLevelPhysics(XmlWorld levelInstance) {
		currenLevelWorld = levelInstance;
		cameraBound.setBounds(0, levelInstance.width, 0, levelInstance.height);
		cameraBound.setBoundsEnabled(true);
		//create a PhysicsEditorShapeLibrary instance to read xml file
		this.worldPhysicsEditorShapeLibrary = new PhysicsEditorShapeLibrary();
		this.worldPhysicsEditorShapeLibrary.open(this, "levels/level" + levelInstance.index+ "/shapes.xml");
		
		BitmapTextureAtlas tAtlas;
		TextureRegion tRegion;

		BitmapTextureAtlasEx tAtlasEx;
		XmlData node;
		//reset base path
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("");
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("levels/level"+ levelInstance.index + "/");
		tAtlasEx = new BitmapTextureAtlasEx(4096,2048, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		
		//this loop adds all sprites from level folder to hash map 
		for (int i = 0; i < worldItems.size(); i++) {
			node = worldItems.get(i);
			//get the sprite from /levels/level[i]/gfx
			TextureRegion mTextureRegion = tAtlasEx.appendTextureAsset(this, "gfx/" + node.sprite);
			//add each sprite to hash map
			worldTextureRegionHashMap.put(node.sprite, mTextureRegion);
		}
		mEngine.getTextureManager().loadTexture(tAtlasEx);
		currentLevelTextures.add(tAtlasEx);
		
		
		/*
		 * this loop reads all shapes from hash map and adds them to the level entities
		 * and creates physical body to all sprites
		 */
		for (int i = 0; i < worldItems.size(); i++) {
			node = worldItems.get(i);
			Sprite tempSprite = new Sprite(node.x, node.y, worldTextureRegionHashMap.get(node.sprite));
			if (node.visible == false)tempSprite.setVisible(false);
			//create ground physics for the game
			Body groundShape = worldPhysicsEditorShapeLibrary.createBody(node.shape, tempSprite, levelWorldPhysics);
			levelEntities.add(tempSprite);
			//set the shape to physics
			levelWorldPhysics.registerPhysicsConnector(new PhysicsConnector(tempSprite, groundShape, true, true));
		}
		
		//reset base path
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("");
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("levels/level"+ levelInstance.index + "/gfx/");
		//add the level background to current level
		tAtlas = new BitmapTextureAtlas(2048, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, levelInstance.frontBackground, 0, 0);
		currentLevelTextures.add(tAtlas);
		mEngine.getTextureManager().loadTexture(tAtlas);
		
		//set globalLevel background to selected one
		levelBackground = new Sprite(0, 0, tRegion);
		levelBackground.setScaleCenter(0, 0);	
		
		//scale background for device screen
		float scaleX = levelInstance.width / levelBackground.getWidth();
		float scaleY = levelInstance.height / levelBackground.getHeight();
		levelBackground.setScale(scaleX, scaleY);
		
		
		//add each sprite to runtime background list to update them easily
		runtimeBackgrounds = new ArrayList<Sprite>();
		for (String spriteName : levelInstance.backBackground) {
			//create an atlas
			tAtlas = new BitmapTextureAtlas(2048, 512,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
			//get the region
			tRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(tAtlas, this, spriteName, 0, 0);
			//add it to level
			currentLevelTextures.add(tAtlas);
			//load on engine
			mEngine.getTextureManager().loadTexture(tAtlas);
			//add them to runtimeBackgrounds
			runtimeBackgrounds.add(new Sprite(0, 0, tRegion));
		}
		levelEntities.add(levelBackground);
		
		//add each node to levelEntities
		for (Sprite sprite : runtimeBackgrounds) {
			
			levelEntities.add(sprite);
			sprite.setScaleCenter(0, 0);
			sprite.setScale(scaleX, scaleY);
		}
		
		//create borders for the game
		createGameBorders(levelInstance);
	}
	
	public void createGameBorders(XmlWorld levelInstance){
		
		final Shape bottom = new Rectangle(0, levelInstance.height, levelInstance.width * 2, 1);
		PhysicsFactory.createBoxBody(levelWorldPhysics, bottom,BodyType.StaticBody, IFixDef_concrete);
		levelEntities.add(bottom);
		
		final Shape top = new Rectangle(0, 0, levelInstance.width, 1);
		PhysicsFactory.createBoxBody(levelWorldPhysics, top,BodyType.StaticBody, IFixDef_concrete);
		levelEntities.add(top);
		
		final Shape start = new Rectangle(-1, 0, 1, levelInstance.height);
		PhysicsFactory.createBoxBody(levelWorldPhysics, start,BodyType.StaticBody, IFixDef_concrete);
		levelEntities.add(start);
		
		final Shape end = new Rectangle(levelInstance.width, 0, 1, levelInstance.height);
		PhysicsFactory.createBoxBody(levelWorldPhysics, end,BodyType.StaticBody, IFixDef_concrete);
		levelEntities.add(end);
	}

	/*
	 * initialize selected car here
	 */
	private void initializeVehicle(int vehicle_index) {
		// body
		vehicleContainer.mass = 5;
		vehicleContainer.I = 10;

		// wheels
		rearWheelNode.relativeX = -80;
		frontWheelNode.relativeX = 60;
		rearWheelNode.relativeY = 60;
		frontWheelNode.relativeY = 60;
		rearWheelNode.mass = 80;
		frontWheelNode.I = 5;
		rearWheelNode.mass = 80;
		frontWheelNode.I = 5;

		// springs
		rearSpringNode.relativeX = 0;
		frontSpringNode.relativeX = 0;
		rearSpringNode.lowerTranslation = -25.5f;
		frontSpringNode.lowerTranslation = -25.5f;
		rearSpringNode.upperTranslation = 15.0f;
		frontSpringNode.upperTranslation = 15.0f;

		//get vehicle body sprite from hashmap
		Sprite tempSprite = new Sprite(0, 0, this.gameTextureRegionHashMap.get("vehicle_body_"+vehicle_index));
		tempSprite.setPosition(vehicleNode.x, vehicleNode.y);
		vehicleBodyShape = tempSprite;
		vehicleBodyShape.setZIndex(10);
		//set it as a box body
		this.vehicleBody = PhysicsFactory.createBoxBody(levelWorldPhysics, vehicleBodyShape,BodyType.DynamicBody, IFixDef_wall);
		
		//create a mass data object for the vehicle
		MassData massData = new MassData();
		massData.I = vehicleContainer.I;
		massData.mass = vehicleContainer.mass;
		
		//set mass data to vehicle body
		this.vehicleBody.setMassData(massData);
		levelEntities.add(vehicleBodyShape);
		levelWorldPhysics.registerPhysicsConnector(new PhysicsConnector(vehicleBodyShape, vehicleBody, true, true));
		
		//create rear wheel sprite
		rearWheelSprite = new Sprite((vehicleBody.getWorldCenter()).x* PIXEL_TO_METER_RATIO_DEFAULT + rearWheelNode.relativeX, vehicleBodyShape.getY()+ rearWheelNode.relativeY, this.gameTextureRegionHashMap.get("vehicle_wheel_"+vehicle_index));
		rearWheelSprite.setZIndex(11);
		//add a circle body to rear wheel
		rearWheelBody = PhysicsFactory.createCircleBody(levelWorldPhysics,rearWheelSprite, BodyType.DynamicBody, IFixDef_wall);
		//set mass data of rear wheel
		massData = new MassData();
		massData.I = 1;
		massData.mass = 5;
		rearWheelBody.setMassData(massData);
		//add rear wheel to level entities
		levelEntities.add(rearWheelSprite);
		
		//create front wheel sprite
		frontWheelSprite = new Sprite((vehicleBody.getWorldCenter()).x* PIXEL_TO_METER_RATIO_DEFAULT + frontWheelNode.relativeX, vehicleBodyShape.getY()+ frontWheelNode.relativeY, this.gameTextureRegionHashMap.get("vehicle_wheel_"+vehicle_index));
		frontWheelSprite.setZIndex(11);
		//add a circle body to front wheel
		frontWheelBody = PhysicsFactory.createCircleBody(levelWorldPhysics,frontWheelSprite, BodyType.DynamicBody, IFixDef_wall);
		frontWheelBody.setLinearDamping(1);
		frontWheelBody.setMassData(massData);
		//add front wheel to level entities
		levelEntities.add(frontWheelSprite);

		//create a sprite for rear axle
		rearAxleSprite = new Sprite((vehicleBody.getWorldCenter()).x* PIXEL_TO_METER_RATIO_DEFAULT + rearWheelNode.relativeX, vehicleBodyShape.getY()+ rearWheelNode.relativeY, this.gameTextureRegionHashMap.get("vehicle_wheel_"+vehicle_index));
		rearAxleSprite.setScale((float) 0.5);
		rearAxleSprite.setVisible(false);
		//set its physics to circle 
		rearAxleBody = PhysicsFactory.createCircleBody(levelWorldPhysics,rearAxleSprite, BodyType.DynamicBody, IFixDef_null);
		//add it to level entities
		levelEntities.add(rearAxleSprite);
		//set a physics connector for rear wheel
		levelWorldPhysics.registerPhysicsConnector(new PhysicsConnector(rearAxleSprite, rearAxleBody, true, true));
		
		//create a sprite for front axle
		frontAxleSprite = new Sprite((vehicleBody.getWorldCenter()).x* PIXEL_TO_METER_RATIO_DEFAULT + frontWheelNode.relativeX, vehicleBodyShape.getY()+ frontWheelNode.relativeY, this.gameTextureRegionHashMap.get("vehicle_wheel_"+vehicle_index));
		frontAxleSprite.setVisible(false);
		frontAxleSprite.setScale((float) 0.5);
		//set its physics to circle 
		frontAxleBody = PhysicsFactory.createCircleBody(levelWorldPhysics,frontAxleSprite, BodyType.DynamicBody, IFixDef_null);
		//add it to level entities
		levelEntities.add(frontAxleSprite);
		//set a physics connector for front wheel
		levelWorldPhysics.registerPhysicsConnector(new PhysicsConnector(frontAxleSprite, frontAxleBody, true, true));
		
		//initialize a connection line between body and rear wheel
		rearLine = new Line(10, 10, 11, 11);
		rearLine.setColor(0 / 255f, 0f / 255f, 0f / 255f);
		rearLine.setLineWidth(10.0f);
		rearLine.setZIndex(9);
		levelWorldPhysics.registerPhysicsConnector(new PhysicsConnector(rearWheelSprite, rearWheelBody, true, true));

		//initialize a connection line between body and front wheel
		frontLine = new Line(10, 10, 11, 11);
		frontLine.setColor(0 / 255f, 0f / 255f, 0f / 255f);
		frontLine.setLineWidth(4.0f);
		frontLine.setZIndex(9);
		levelWorldPhysics.registerPhysicsConnector(new PhysicsConnector(frontWheelSprite, frontWheelBody, true, true));
		
		//add connection lines to levelEntities
		levelEntities.add(rearLine);
		levelEntities.add(frontLine);

		//set joints for springs
		rearSpringJoint = new PrismaticJointDef();
		frontSpringJoint = new PrismaticJointDef();
		
		
		Float angleA = (float) (-(Math.cos(Math.PI / 3)));
		Float angleB = (float) ((Math.sin(Math.PI / 3)));
		
		//initialize motor spring for rear motor
		rearSpringJoint.initialize(vehicleBody, rearAxleBody, new Vector2(vehicleBody.getWorldCenter().x+ rearSpringNode.relativeX / PIXEL_TO_METER_RATIO_DEFAULT, vehicleBody.getWorldCenter().y), new Vector2(angleA, angleB));
		rearSpringJoint.lowerTranslation = (float) rearSpringNode.lowerTranslation/ PIXEL_TO_METER_RATIO_DEFAULT;
		rearSpringJoint.upperTranslation = (float) rearSpringNode.upperTranslation/ PIXEL_TO_METER_RATIO_DEFAULT;
		rearSpringJoint.motorSpeed = 10;
		rearSpringJoint.maxMotorForce = 20;
		rearSpringJoint.enableLimit = true;
		rearSpringJoint.enableMotor = true;
		

		//initialize motor spring for front motor
		angleA = (float) ((Math.cos(Math.PI / 1)));
		angleB = (float) ((Math.sin(Math.PI / 1)));
		
		frontSpringJoint.initialize(vehicleBody, frontAxleBody, new Vector2(vehicleBody.getWorldCenter().x+ frontSpringNode.relativeX / PIXEL_TO_METER_RATIO_DEFAULT, vehicleBody.getWorldCenter().y), new Vector2(angleA, angleB));
		frontSpringJoint.lowerTranslation = (float) frontSpringNode.lowerTranslation/ PIXEL_TO_METER_RATIO_DEFAULT;
		frontSpringJoint.upperTranslation = (float) frontSpringNode.upperTranslation/ PIXEL_TO_METER_RATIO_DEFAULT;
		frontSpringJoint.motorSpeed = 10;
		frontSpringJoint.maxMotorForce = 20;
		frontSpringJoint.enableLimit = true;
		frontSpringJoint.enableMotor = true;
		
		
		//create joints for springs
		rearSpring = (PrismaticJoint) levelWorldPhysics.createJoint(rearSpringJoint);
		frontSpring = (PrismaticJoint) levelWorldPhysics.createJoint(frontSpringJoint);

		// initialize rear motor
		rearMotorJoint = new RevoluteJointDef();
		rearMotorJoint.initialize(rearAxleBody, rearWheelBody, rearWheelBody.getWorldCenter());
		rearMotorJoint.motorSpeed = 1;
		rearMotorJoint.maxMotorTorque = 200;
		rearMotorJoint.enableMotor = true;
		
		
		//initialize front motor
		RevoluteJointDef frontMotorJoint = new RevoluteJointDef();
		frontMotorJoint.initialize(frontAxleBody, frontWheelBody, frontWheelBody.getWorldCenter());
		frontMotorJoint.motorSpeed = 1;
		frontMotorJoint.maxMotorTorque = 200;
		frontMotorJoint.enableMotor = false;
		
		
		//create joint for motors
		rearMotor = (RevoluteJoint) levelWorldPhysics.createJoint(rearMotorJoint);
		frontMotor = (RevoluteJoint) levelWorldPhysics.createJoint(frontMotorJoint);

	}

	
	/*
	 * this function destroys the current level
	 * for followings;
	 * 		Return main menu
	 * 		Restart level
	 * 		Go to next level
	 */
	private void destroyCurrentLevel() {
		//disable accerometer when level closed
		disableAccelerometerSensor();
		
		//stop checking collision detection for world physics
		mainScene.unregisterUpdateHandler(levelWorldPhysics);
		//stop handler for main engine
		mEngine.unregisterUpdateHandler(gameUpdateHandler);

		
		final Display deviceDisplay = getWindowManager().getDefaultDisplay();
		int screenWidth = deviceDisplay.getWidth();
		int screenHeight = deviceDisplay.getHeight();
		
		//reorganize camera bounds
		cameraBound.setChaseEntity(null);
		cameraBound.updateChaseEntity();
		cameraBound.reset();
		cameraBound.setCenter(screenWidth / 2.0f, screenHeight / 2.0f);

		//set state for new level loading
		isLevelStarted = false;
		//stop the vehicle sound
		vehicleExhaust.stop();
		
		//remove all current level entities from scene 
		for (IEntity entity : levelEntities) {
			mEngine.getTextureManager().unloadTexture((ITexture) entity.getUserData());
			mainScene.detachChild(entity);
		}

		//remove vehicle motors
		levelWorldPhysics.destroyJoint(rearMotor);
		levelWorldPhysics.destroyJoint(frontMotor);
		//remove vehicle springs
		levelWorldPhysics.destroyJoint(rearSpring);
		levelWorldPhysics.destroyJoint(frontSpring);

		//add all physics bodies to a list
		ArrayList<Body> physicsBodyList = new ArrayList<Body>();
		//get all bodies from current level physics
		Iterator<Body> physicsIterator = levelWorldPhysics.getBodies();
		while (physicsIterator.hasNext()) {
			physicsBodyList.add(physicsIterator.next());
		}
		
		//remove all bodies which are listed in previous array
		for (Body body : physicsBodyList) {
			levelWorldPhysics.destroyBody(body);
		}
		
		//destroy all physics connector
		PhysicsConnector[] physicssConnectors = new PhysicsConnector[levelWorldPhysics.getPhysicsConnectorManager().size()];
		//get physics connectors
		levelWorldPhysics.getPhysicsConnectorManager().toArray(physicssConnectors);
		for (PhysicsConnector physicsConnector : physicssConnectors) {
			levelWorldPhysics.unregisterPhysicsConnector(physicsConnector);
		}
		//set pedal states to default
		brakesPressed = gasPressed = false;
		
		//remove all textures from current level
		for (ITexture textures : currentLevelTextures) {
			mEngine.getTextureManager().unloadTexture(textures);
		}
		
		//reset physics
		levelWorldPhysics.reset();
	}

	private void reloadCurrentLevel() {
		//destroy current level
		destroyCurrentLevel();
		//reset update counter
		preUpdates = 0;
		//setcurrent scene to loading		
		openScene(loadingScreen);
		currentScene = scene_loading;
		
		//start new thread for new level
		Thread thread = new Thread(new LevelLoader());
		thread.start();
		//
		mainScene.sortChildren();
	}

    @Override
    protected int getLayoutID() {
        return R.layout.main;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.xmllayoutRenderSurfaceView;
    }
    
    /*
     * pause game within null state
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && currentScene == scene_null) {
            try {
                pauseGame();
            } catch (Exception e) {
                System.out.println("Error pausing game!");
            	e.printStackTrace();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void pauseGame() {
    	//set pause flag to true
        isPaused = true;
        
        //reset pedal states
        brakesPressed = gasPressed = false;
        //stop the engine sound
        vehicleExhaust.stop();
        
        //stop physics updaters
        mainScene.unregisterUpdateHandler(levelWorldPhysics);
        mEngine.unregisterUpdateHandler(gameUpdateHandler);
        
        /*
         * there was a problem with buttons of pause screen
         * to handle this problem reorganizing them here
         */
        reorganizePauseScreen();

        //set current scene to pause
        openScene(pauseScreen);
        currentScene = scene_pause;
    }
    /*
     * this function reorganize pause game buttons
     */
    public void reorganizePauseScreen(){
    	pauseMenuButtons.ArrangeElements();
    	for(int i=0 ; i<3 ;i++){
    		pauseScreen.get(i).setPosition(cameraBound.getMinX() + pauseScreen.get(i).getX(),cameraBound.getMinY() + +pauseScreen.get(i).getY());
    	}
    }
    

	
}
