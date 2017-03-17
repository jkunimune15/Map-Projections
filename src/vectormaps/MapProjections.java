package vectormaps;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.complex.Complex;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import rastermaps.ProgressBarDialog;
import util.Vector;
import util.WinkelTripel;

/**
 * An application to make vector oblique aspects of map projections
 * 
 * @author Justin Kunimune
 */
public class MapProjections extends Application {

	private static final int CONT_WIDTH = 300;
	private static final int IMG_WIDTH = 500;
	
	
	private static final KeyCombination ctrlO = new KeyCodeCombination(KeyCode.O, KeyCodeCombination.CONTROL_DOWN);
	private static final KeyCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.CONTROL_DOWN);
	
	
	private static final String[] PROJ_ARR = { "Equirectangular", "Mercator", "Gall Stereographic",
			"Cylindrical Equal-Area", "Polar", "Stereographic", "Azimuthal Equal-Area", "Orthographic", "Gnomonic",
			"Lambert Conical", "Winkel Tripel", "Van der Grinten", "Mollweide", "Aitoff", "Hammer", "Sinusoidal",
			"Pierce Quincuncial", "Guyou", "TetraGraph", "Magnifier", "Experimental" };
	private static final String[] DESC = { "An equidistant cylindrical map", "A conformal cylindrical map",
			"A compromising cylindrical map", "An equal-area cylindrical map", "An equidistant azimuthal map",
			"A conformal azimuthal map", "An equal-area azimuthal map",
			"Represents earth viewed from an infinite distance",
			"Every straight line on the map is a straight line on the sphere", "A conformal conical map",
			"The compromise map used by National Geographic (caution: very slow)", "A circular compromise map",
			"An equal-area map shaped like an ellipse", "An equal-area map shaped like an elipse",
			"An equal-area map shaped like an elipse", "An equal-area map shaped like a sinusoid",
			"A conformal square map that uses complex math",
			"A reorganized version of Pierce Quincuncial and actually the best map ever",
			"A compromising knockoff of the AuthaGraph projection",
			"A novelty map that swells the center to disproportionate scale",
			"What happens when you apply a complex differentiable function to a stereographic projection?" };
	
	private static final String[] AXES = { "Standard", "Transverse", "Center of Mass", "Jerusalem", "Point Nemo",
			"Longest Line", "Longest Line Transverse", "Cylindrical", "Conical", "Quincuncial", "Antipode", "Random" };
	private static final double[] DEF_LATS = { 90, 0, 29.9792, 31.7833, 48.8767, -28.5217, -46.4883, -35, -10, 60 };
	private static final double[] DEF_LONS = { 0, 0, 31.1344, 35.216, 56.6067, 141.451, 16.5305, -13.6064, 65, -6 };
	private static final double[] DEF_THTS = { 0, 0, -32, -35, -45, 161.5, 137, 145, -150, -10 };
	private static final int DEF_MAX_VTX = 10000;
	
	
	private Stage stage;
	private FileChooser inputChooser, saver;
	private Text inputLabel;
	private Button changeInput;
	private ComboBox<String> projectionChooser;
	private Text projectionDesc;
	private Slider latSlider, lonSlider, thtSlider;
	private Button update;
	private Button saveMap;
	private List<String> format;
	private List<List<double[]>> input;
	private double minX, maxX, minY, maxY;
	private int numVtx;
	private Canvas viewer;
	
	
	
	public static void main(String[] args) {
		launch(args);
	}
	
	
	@Override
	public void start(Stage primaryStage) {
		stage = primaryStage;
		stage.setTitle("Map Designer");
		
		final VBox layout = new VBox();
		layout.setSpacing(5);
		layout.setAlignment(Pos.CENTER);
		layout.setPrefWidth(CONT_WIDTH);
		
		Label lbl = new Label("Current input:");
		inputLabel = new Text("None");
		layout.getChildren().add(new HBox(3, lbl, inputLabel));
		
		inputChooser = new FileChooser();
		inputChooser.setInitialDirectory(new File("input"));
		inputChooser.setTitle("Choose an input map");
		inputChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("SVG", "*.svg"));
		
		changeInput = new Button("Choose input...");
		changeInput.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				chooseInput();
			}
		});
		changeInput.setTooltip(new Tooltip(
				"Change the input image"));
		stage.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {	// ctrl-O opens
			public void handle(KeyEvent event) {
				if (ctrlO.match(event))	changeInput.fire();
			}
		});
		layout.getChildren().add(changeInput);
		
		layout.getChildren().add(new Separator());
		
		lbl = new Label("Projection:");
		ObservableList<String> items = FXCollections.observableArrayList(PROJ_ARR);
		projectionChooser = new ComboBox<String>(items);
		projectionChooser.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				for (int i = 0; i < PROJ_ARR.length; i ++) {
					if (PROJ_ARR[i].equals(projectionChooser.getValue())) {
						projectionDesc.setText(DESC[i]);
						break;
					}
				}
			}
		});
		projectionChooser.setPrefWidth(210);
		projectionChooser.setValue(PROJ_ARR[1]);
		layout.getChildren().add(new HBox(3, lbl, projectionChooser));
		
		projectionDesc = new Text(DESC[1]);
		projectionDesc.setWrappingWidth(CONT_WIDTH);
		layout.getChildren().add(projectionDesc);
		
		layout.getChildren().add(new Separator());
		
		final MenuButton defAxes = new MenuButton("Aspect Presets");
		for (String preset: AXES) {
			MenuItem m = new MenuItem(preset);
			m.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					setAxisByPreset(((MenuItem) event.getSource()).getText());
				}
			});
			defAxes.getItems().add(m);
		}
		defAxes.setTooltip(new Tooltip(
				"Set the aspect sliders based on a preset"));
		layout.getChildren().add(defAxes);
		
		latSlider = new Slider(-90, 90, 90);
		lonSlider = new Slider(-180,180,0);
		thtSlider = new Slider(-180,180,0);
		Tooltip aspTlTp = new Tooltip("Change the aspect of the map");
		latSlider.setTooltip(aspTlTp);
		lonSlider.setTooltip(aspTlTp);
		thtSlider.setTooltip(aspTlTp);
		
		GridPane grid = new GridPane();
		grid.addRow(0, new Text("Latitude:"), latSlider);
		grid.addRow(1, new Text("Longitude:"), lonSlider);
		grid.addRow(2, new Text("Orientation:"), thtSlider);
		GridPane.setHgrow(latSlider, Priority.ALWAYS);
		GridPane.setHgrow(lonSlider, Priority.ALWAYS);
		GridPane.setHgrow(thtSlider, Priority.ALWAYS);
		//GridPane.setFillWidth(latSlider, Boolean.valueOf(true));
		layout.getChildren().add(grid);
		
		layout.getChildren().add(new Separator());
		
		update = new Button("Update Map");
		update.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				updateMap();
			}
		});
		update.setTooltip(new Tooltip(
				"Update the current map with your parameters."));
		update.setDefaultButton(true);
		
		saver = new FileChooser();
		saver.setInitialDirectory(new File("output"));
		saver.setInitialFileName("myMap.svg");
		saver.setTitle("Save Map");
		saver.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("SVG", "*.svg"));
		
		saveMap = new Button("Save Map...");
		saveMap.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				startFinalizingMap();
			}
		});
		saveMap.setTooltip(new Tooltip("Save the map with current settings."));
		stage.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {	// ctrl-S saves
			public void handle(KeyEvent event) {
				if (ctrlS.match(event))	saveMap.fire();
			}
		});
		
		HBox box = new HBox(5, update, saveMap);
		box.setAlignment(Pos.CENTER);
		layout.getChildren().add(box);
		
		viewer = new Canvas(IMG_WIDTH, IMG_WIDTH);
		//viewer.setFitWidth(IMG_WIDTH);
		//viewer.setFitHeight(IMG_WIDTH);
		//viewer.setPreserveRatio(true);
		
		new Thread(() -> {
			setInput("basic.svg", "input/basic.svg");
			update.fire();
		}).start();
		
		final HBox gui = new HBox(layout, viewer);
		gui.setAlignment(Pos.CENTER);
		gui.setSpacing(10);
		StackPane.setMargin(gui, new Insets(10));
		stage.setScene(new Scene(new StackPane(gui)));
		stage.show();
	}
	
	
	private void chooseInput() {
		final File f = inputChooser.showOpenDialog(stage);
		if (f != null) {
			new Thread(() -> {
				setInput(f.getName(), f.getAbsolutePath());
			}).start();
		}
	}
	
	
	private void setInput(String name, String filename) {
		changeInput.setDisable(true);
		update.setDisable(true);
		saveMap.setDisable(true);
		
		try {
			input = loadSVG(filename);
			inputLabel.setText(name);
		} catch (IllegalArgumentException e) {
			final Alert alert = new Alert(Alert.AlertType.ERROR);//TODO: move this code to raster
			alert.setHeaderText("Unreadable file!");
			alert.setContentText("We couldn't read "+filename+". It may be corrupt or an unreadable format.");
		} catch (IOException e) {
			final Alert alert = new Alert(Alert.AlertType.ERROR);//TODO: move this code to raster
			alert.setHeaderText("File not found!");
			alert.setContentText("Couldn't find "+filename+".");
		}
		
		changeInput.setDisable(false);
		update.setDisable(false);
		saveMap.setDisable(false);
	}
	
	
	private List<List<double[]>> loadSVG(String filename) throws IOException { // this method is just awful.
		System.out.println("loading "+filename);
		input = new ArrayList<List<double[]>>();
		format = new ArrayList<String>();
		minX = minY = Integer.MAX_VALUE;
		maxX = maxY = Integer.MIN_VALUE;
		numVtx = 0;
		
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String formatStuff = "";
		int c = in.read();
		
		do {
			formatStuff += (char)c;
			if (formatStuff.length() >= 8 &&
					formatStuff.substring(formatStuff.length()-8).equals("path d=\"")) {
				format.add(formatStuff);
				formatStuff = "";
				List<double[]> currentShape = new ArrayList<double[]>();
				c = in.read();
				do {//TODO:figure out where to call readLine (probs at the end of loops)
					if (c == 'Z') {
						input.add(currentShape);
						currentShape = new ArrayList<double[]>();
						format.add("");
						c = in.read();
					}
					else if (isDigit((char) c)) {
						String num = Character.toString((char)c);
						while (isDigit((char) (c = in.read())))
							num += (char) c;
						double x = Double.parseDouble(num);
						if (x < minX)	minX = x;
						if (x > maxX)	maxX = x;
						c = in.read();
						num = Character.toString((char)c);
						while (isDigit((char) (c = in.read())))
							num += (char) c;
						double y = Double.parseDouble(num);
						if (y < minY)	minY = y;
						if (y > maxY)	maxY = y;
						currentShape.add(new double[] {x, y});
						numVtx ++;
					}
					else if (c == ' ' || c == ',' || c == '\n' ||
							c == 'M' || c == 'L' || c == 'B') {
						c = in.read();
					} //XXX This is jank; I should spiff it up once it works
					else {
						System.err.println("I don't know how to interpret "+(char)c);
						c = in.read();
					}
				} while (c != '"');
			}
			else {
				c = in.read();
			}
		} while (c >= 0);
		
		format.add(formatStuff);
		in.close();
		System.out.println("loaded.");
		return input;
	}
	
	
	private void setAxisByPreset(String preset) {
		if (preset.equals("Antipode")) {
			latSlider.setValue(-latSlider.getValue());
			lonSlider.setValue((lonSlider.getValue()+360)%360-180);
			thtSlider.setValue(-thtSlider.getValue());
			return;
		}
		if (preset.equals("Random")) {
			latSlider.setValue(Math.toDegrees(Math.asin(Math.random()*2-1)));
			lonSlider.setValue(Math.random()*360-180);
			thtSlider.setValue(Math.random()*360-180);
			return;
		}
		for (int i = 0; i < AXES.length; i ++) {
			if (AXES[i].equals(preset)) {
				latSlider.setValue(DEF_LATS[i]);
				lonSlider.setValue(DEF_LONS[i]);
				thtSlider.setValue(DEF_THTS[i]);
				break;
			}
		}
	}
	
	
	private void updateMap() {
		update.setDisable(true);
		new Thread(new Task<Void>() {
			protected Void call() {
				drawImage(map(), viewer);
				update.setDisable(false);
				return null;
			}
		}).start();
	}
	
	
	private void startFinalizingMap() {
		int p = 0;
		while (p < PROJ_ARR.length &&
				!PROJ_ARR[p].equals(projectionChooser.getValue()))
			p ++;
		
		ProgressBarDialog pBar = new ProgressBarDialog();
		pBar.show();
		new Thread(() -> {
			List<List<double[]>> map = map(0, pBar);
			Platform.runLater(() -> saveToSVG(map, pBar));
		}).start();
	}
	
	
	private void drawImage(List<List<double[]>> img, Canvas c) {
		GraphicsContext g = c.getGraphicsContext2D();
		g.clearRect(0, 0, c.getWidth(), c.getHeight());
		for (List<double[]> closedCurve: img) {
			g.beginPath();
			for (int i = 0; i < closedCurve.size(); i ++) {
				double[] p = closedCurve.get(i);
				if (i == 0)	g.moveTo(p[0], p[1]);
				else		g.lineTo(p[0], p[1]);
			}
			g.closePath();
			g.stroke();
		}
	}


	private void saveToSVG(List<List<double[]>> curves, ProgressBarDialog pBar) {	// call from the main thread!
		pBar.close();
		final File f = saver.showSaveDialog(stage);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			
			for (int i = 0; i < curves.size(); i ++) {
				out.write(format.get(i));
				String curveCode = "M";
				for (double[] p: curves.get(i))
					curveCode += p[0]+" "+p[1]+"L";
				out.write(curveCode.substring(0,curveCode.length()-2)+"Z");
				pBar.setProgress((double)i/curves.size());
			}
			out.write(format.get(curves.size()));
			out.close();
			
		} catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("File not found!");
			alert.setContentText("Could not find "+f+".");
		}
	}
	
	
	private List<List<double[]>> map() {
		return map(DEF_MAX_VTX, null);
	}
	
	private List<List<double[]>> map(int maxVtx, ProgressBarDialog pbar) {
		final String proj = projectionChooser.getValue();
		final double[] pole = {Math.toRadians(latSlider.getValue()),
							Math.toRadians(lonSlider.getValue()),
							Math.toRadians(thtSlider.getValue())};
		int step = maxVtx==0 ? 1 : numVtx/maxVtx+1;
		List<List<double[]>> output = new LinkedList<List<double[]>>();
		
		int i = 0;
		for (List<double[]> curve0: input) {
			if (curve0.size() < step*3)	continue;
			
			List<double[]> curve1 = new ArrayList<double[]>(curve0.size()/step);
			for (int j = 0; j < curve0.size(); j += step) {
				double[] radCoords = convCoordsToMathy(curve0.get(j));
				//System.out.println("Coast at "+radCoords[0]+","+radCoords[1]);
				double[] plnCoords = project(obliquify(pole, radCoords), proj);
				curve1.add(convCoordsToImg(plnCoords));
			}
			//System.out.println();
			output.add(curve1);
			
			if (pbar != null) {
				i ++;
				pbar.setProgress((double)i/input.size());
			}
		}
		
		return output;
	}
	
	
	private static double[] project(double[] latLon, String p) {
		double lat = latLon[0];
		double lon = latLon[1];
		if (p.equals("Pierce Quincuncial"))
			return quincuncial(lat, lon);
		else if (p.equals("Equirectangular"))
			return equirectangular(lat, lon);
		else if (p.equals("Mercator"))
			return mercator(lat, lon);
		else if (p.equals("Polar"))
			return polar(lat, lon);
		else if (p.equals("Gall Stereographic"))
			return gall(lat, lon);
		else if (p.equals("Sinusoidal"))
			return sinusoidal(lat, lon);
		else if (p.equals("Stereographic"))
			return stereographic(lat, lon);
		else if (p.equals("Gnomonic"))
			return gnomonic(lat, lon);
		else if (p.equals("Orthographic"))
			return orthographic(lat, lon);
		else if (p.equals("Cylindrical Equal-Area"))
			return eaCylindrical(lat, lon);
		else if (p.equals("Lambert Conical"))
			return lambert(lat, lon);
		else if (p.equals("Azimuthal Equal-Area"))
			return eaAzimuth(lat, lon);
		else if (p.equals("Guyou"))
			return quinshift(lat, lon);
		else if (p.equals("Mollweide"))
			return mollweide(lat, lon);
		else if (p.equals("Winkel Tripel"))
			return winkel_tripel(lat, lon);
		else if (p.equals("Van der Grinten"))
			return grinten(lat, lon);
		else if (p.equals("Magnifier"))
			return magnus(lat, lon);
		else if (p.equals("Aitoff"))
			return aitoff(lat, lon);
		else if (p.equals("Hammer"))
			return hammer(lat, lon);
		//else if (p.equals("TetraGraph"))
		//	return tetragraph(lat, lon);
		else if (p.equals("Experimental"))
			return experiment(lat, lon);
		else
			throw new IllegalArgumentException(p);
	}
	
	
	private double[] convCoordsToMathy(double[] coords) { // changes svg coordinates to radians
		final double NORTHMOST = 1.459095;
		final double SOUTHMOST = -1.4868809;
		final double EASTMOST = -Math.PI;
		final double WESTMOST = Math.PI;
		return new double[] {linMap(coords[1], minY,maxY, SOUTHMOST,NORTHMOST),
				linMap(coords[0], minX,maxX, EASTMOST,WESTMOST)};
	}
	
	
	private double[] convCoordsToImg(double[] coords) { // changes [-1,1] coordinates to image coordinates
		return new double[] {linMap(coords[0], -Math.PI,Math.PI, 0,IMG_WIDTH),
				linMap(coords[1], -Math.PI,Math.PI, IMG_WIDTH,0)};
	}
	
	
	private static final double[] obliquify(double[] pole, double[] coords) {
		final double lat0 = pole[0];
		final double lon0 = pole[1];
		final double tht0 = pole[2];
		double latF = coords[0];
		double lonF = coords[1];
		Vector r0 = new Vector (1, lat0, lon0);
		Vector rF = new Vector (1, latF, lonF);
		Vector r0XrF = r0.cross(rF);
		Vector r0Xk = r0.cross(Vector.K);
		
		double lat1 = Math.asin(r0.dot(rF)); // relative latitude
		double lon1;
		if (lat0 == Math.PI/2) // accounts for all the 0/0 errors at the poles
			lon1 = lonF-lon0;
		else if (lat0 == -Math.PI/2)
			lon1 = lon0-lonF+Math.PI;
		else {
			lon1 = Math.acos(r0XrF.dot(r0Xk)/(r0XrF.abs()*r0Xk.abs()))-Math.PI; // relative longitude
			if (Double.isNaN(lon1))
				lon1 = 0;
			else if (r0XrF.cross(r0Xk).dot(r0)/(r0XrF.abs()*r0Xk.abs()) > 0) // it's a plus-or-minus arccos.
				lon1 = 2*Math.PI-lon1;
		}
		lon1 = lon1-tht0;
		lon1 = mod(lon1+Math.PI, 2*Math.PI) - Math.PI;
		
		return new double[] {lat1, lon1};
		//return coords;
	}
	
	
	private static double[] quincuncial(double lat, double lon) { // a tessalatable square map
		double[] output = new double[2];
		
		final double wMag = Math.tan(lat/2+Math.PI/4);
		final Complex w = new Complex(wMag*Math.cos(lon), wMag*Math.sin(lon));
		final Complex k = new Complex(Math.sqrt(0.5));
		Complex z = F(w.acos(),k);
		if (z.isInfinite() || z.isNaN() || z.abs() > 10)
			z = new Complex(0);
				
		output[0] = -z.getReal();
		output[1] = z.getImaginary();
		return output;
	}
	
	private static double[] experiment(double lat, double lon) { // just some random complex plane stuff
		double[] output = new double[2];
		
		final double p = Math.tan(lat/2+Math.PI/4);
		final Complex w = new Complex(p*Math.cos(lon), p*Math.sin(lon));
		Complex z = w;
		if (z.isInfinite() || z.isNaN() || z.abs() > 10)
			z = new Complex(0);
				
		output[0] = -z.getReal();
		output[1] = z.getImaginary();
		return output;
	}
	
	private static double[] equirectangular(double lat, double lon) { // a linear scale
		return new double[] {lon, lat};
	}
	
	private static double[] mercator(double lat, double lon) { // a popular shape-preserving map
		return new double[] {lon, Math.log(Math.tan(Math.PI/4+lat/2))};
	}
	
	private static double[] polar(double lat, double lon) { // the projection used on the UN flag
		final double r = Math.PI/2 - lat;
		return new double[] {r*Math.sin(lon), -r*Math.cos(lon)};
	}
	
	private static double[] gall(double lat, double lon) { // a compromise map, similar to mercator
		return new double[] {lon, Math.tan(lat/2)*(1+Math.sqrt(2))};
	}
	
	private static double[] sinusoidal(double lat, double lon) { // a map shaped like a sinusoid
		return new double[] {Math.cos(lat)*lon, lat};
	}
	
	private static double[] stereographic(double lat, double lon) { // a shape-preserving infinite map
		final double r = 1.5/(Math.tan(lat/2 + Math.PI/4));
		return new double[] {r*Math.sin(lon), -r*Math.cos(lon)};
	}
	
	private static double[] gnomonic(double lat, double lon) { // map where straight lines are straight
		if (lat <= 0)	lat = 10e-5;
		final double r = Math.tan(Math.PI/2 - lat);
		return new double[] {r*Math.sin(lon), -r*Math.cos(lon)};
	}
	
	private static double[] orthographic(double lat, double lon) { // a map that mimics the view from space
		if (lat < 0)	lat = 0;
		final double r = Math.PI*Math.cos(lat);
		return new double[] {r*Math.sin(lon), -r*Math.cos(lon)};
	}
	
	private static double[] eaCylindrical(double lat, double lon) { // an equal-area cylindrical map
		return new double[] {lon, Math.sin(lat)*Math.PI/2};
	}
	
	private static double[] lambert(double lat, double lon) { // a conical projection
		final double r = 3*Math.sqrt(Math.tan(Math.PI/4-lat/2));
		return new double[] {r*Math.sin(lon/2), Math.PI-r*Math.cos(lon/2)};
	}
	
	private static double[] eaAzimuth(double lat, double lon) { // the lambert azimuthal equal area projection
		final double r = Math.PI*Math.cos((Math.PI/2+lat)/2);
		return new double[] {r*Math.sin(lon), -r*Math.cos(lon)};
	}
	
	private static double[] quinshift(double lat, double lon) { // a tessalatable rectangle map
		double[] output = new double[2];
		
		if (lat >= 0) {
			final double wMag = Math.tan(-lat/2+Math.PI/4);
			final Complex w = new Complex(wMag*Math.cos(lon), wMag*Math.sin(lon));
			final Complex k = new Complex(Math.sqrt(0.5));
			Complex z = F(w.acos(),k);
					
			output[0] = -z.getImaginary()*1000;
			output[1] = z.getReal()*1000;
			return output;
		}
		else {
			final double wMag = Math.tan(lat/2+Math.PI/4);
			final Complex w = new Complex(wMag*Math.cos(lon), wMag*Math.sin(lon));
			final Complex k = new Complex(Math.sqrt(0.5));
			Complex z = F(w.acos(),k);
					
			output[0] = -z.getReal();
			output[1] = z.getImaginary();
			return output;
		}
	}
	
	private static double[] mollweide(double lat, double lon) {
		double tht = lat;
		for (int i = 0; i < 10; i ++)
			tht -= (2*tht+Math.sin(2*tht)-Math.PI*Math.sin(lat))/
					(2+2*Math.cos(2*tht));
		return new double[] {
				3.2/Math.PI*lon*Math.cos(tht),
				1.6*Math.sin(tht)};
	}
	
	private static double[] winkel_tripel(double lat, double lon) {
		return new double[] {WinkelTripel.X(lat,lon)/1.65, WinkelTripel.Y(lat,lon)/1.65};
	}
	
	private static double[] grinten(double lat, double lon) {
		final double t = Math.asin(Math.abs(2*lat/Math.PI));
		
		if (lat == 0) // special case 1: equator
			return new double[] {lon, 0};
		if (lon == 0 || lat >= Math.PI/2 || lat <= -Math.PI/2) // special case 3: meridian
			return new double[] {0, Math.signum(lat)*Math.PI*Math.tan(t/2)};
		
		final double A = Math.abs(Math.PI/lon - lon/Math.PI)/2;
		final double G = Math.cos(t)/(Math.sin(t)+Math.cos(t)-1);
		final double P = G*(2/Math.sin(t) - 1);
		final double Q = A*A + G;
		
		return new double[] {
				Math.PI*Math.signum(lon)*(A*(G-P*P)+Math.sqrt(A*A*(G-P*P)*(G-P*P)-(P*P+A*A)*(G*G-P*P)))/(P*P+A*A),
				Math.PI*Math.signum(lat)*(P*Q-A*Math.sqrt((A*A+1)*(P*P+A*A)-Q*Q))/(P*P+A*A)};
	}
	
	private static double[] magnus(double lat, double lon) { // a novelty map that magnifies the center profusely
		final double p = 1/2.0+lat/Math.PI;
		final double fp = 1 - 0.1*p - 0.9*Math.pow(p,7);
		final double r = Math.PI*fp;
		return new double[] {r*Math.sin(lon), -r*Math.cos(lon)};
	}
	
	private static double[] hammer(double lat, double lon) { // similar to Mollweide, but moves distortion from the poles to the edges
		return new double[] {
				3.2*Math.cos(lat)*Math.sin(lon/2)/Math.sqrt(1+Math.cos(lat)*Math.cos(lon/2)),
				1.6*Math.sin(lat)/Math.sqrt(1+Math.cos(lat)*Math.cos(lon/2))};
	}
	
	private static double[] aitoff(double lat, double lon) { // similar to Mollweide, but moves distortion from the poles to the edges
		final double a = Math.acos(Math.cos(lat)*Math.cos(lon/2));
		return new double[] {
				6.4/Math.PI*Math.cos(lat)*Math.sin(lon/2)*a/Math.sin(a),
				3.2/Math.PI*Math.sin(lat)*a/Math.sin(a)};
	}
	
	
	/*private static double[] tetragraph(double lat, double lon) { // a tetrahedral compromise
		final double[] faceCenter = new double[3];
		final double rot, localX, localY;
		if (y < x-1) {
			faceCenter[0] = -Math.PI/2;
			faceCenter[1] = 0;
			rot = -Math.PI/2;
			localX = Math.sqrt(3)*(x-2/3.0);
			localY = y+1;
		}
		else if (y < -x-1) {
			faceCenter[0] = -Math.PI/2;
			faceCenter[1] = 0;
			rot = Math.PI/2;
			localX = Math.sqrt(3)*(x+2/3.0);
			localY = y+1;
		}
		else if (y > -x+1) {
			faceCenter[0] = Math.PI/2-Math.asin(Math.sqrt(8)/3);
			faceCenter[1] = Math.PI;
			rot = -Math.PI/2;
			localX = Math.sqrt(3)*(x-2/3.0);
			localY = y-1;
		}
		else if (y > x+1) {
			faceCenter[0] = Math.PI/2-Math.asin(Math.sqrt(8)/3);
			faceCenter[1] = Math.PI;
			rot = Math.PI/2;
			localX = Math.sqrt(3)*(x+2/3.0);
			localY = y-1;
		}
		else if (x < 0) {
			faceCenter[0] = Math.PI/2-Math.asin(Math.sqrt(8)/3);
			faceCenter[1] = -Math.PI/3;
			rot = Math.PI/6;
			localX = Math.sqrt(3)*(x+1/3.0);
			localY = y;
		}
		else {
			faceCenter[0] = Math.PI/2-Math.asin(Math.sqrt(8)/3);
			faceCenter[1] = Math.PI/3;
			rot = -Math.PI/6;
			localX = Math.sqrt(3)*(x-1/3.0);
			localY = y;
		}
		faceCenter[2] = 0;
		
		final double t = Math.atan2(localY, localX) + rot;
		final double t0 = Math.floor((t+Math.PI/2)/(2*Math.PI/3)+0.5)*(2*Math.PI/3) - Math.PI/2;
		final double dt = t-t0;
		
		double[] triCoords = {
				Math.PI/2 - Math.atan(Math.tan(1.654*Math.hypot(localX, localY)*Math.cos(dt))/Math.cos(dt)),
				Math.PI/2 + t0 + dt};
		return obliquify(faceCenter, triCoords);
	}*/
	
	
	
	private static final Complex F(Complex phi, final Complex k) { // series solution to incomplete elliptic integral of the first kind
		Complex sum = new Complex(0);
		Complex i_n = phi;
		
		for (int n = 0; n < 100; n++) {
			if (n > 0)
				i_n = i_n.multiply((2.0 * n - 1) / (2.0 * n))
						.subtract(phi.cos().multiply(phi.sin().pow(2.0 * n - 1)).divide(2.0 * n));
			sum = sum.add(i_n.multiply(Math.abs(combine(-.5, n))).multiply(k.pow(2.0 * n)));
		}
		
		return sum;
	}
	
	
	private static final double combine(double n, int k) {
		double output = 1;
		for (int i = k; i > 0; i --) {
			output *= (n+i-k)/i;
		}
		return output;
	}
	
	
	private static final double linMap(double x, double a0, double a1, double b0, double b1) {
		return (x-a0)*(b1-b0)/(a1-a0) + b0;
	}
	
	
	private static final boolean isDigit(char c) {
		return c >= '-' && c <= '9';
	}
	
	
	private static final double mod(double x, double y) {
		return x - Math.floor(x/y)*y;
	}
}
