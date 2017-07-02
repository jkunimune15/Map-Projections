/**
 * MIT License
 * 
 * Copyright (c) 2017 Justin Kunimune
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class MapConfigurationDialog extends Dialog<Void> {

	public final double DEF_SIZE = 1000;
	
	
	private final double defaultRatio;
	private final VBox gui;
	private final CheckBox maintainRatio;
	private final Spinner<Integer> widthBox, heightBox;
	private final ComboBox<String> smoothBox;
	
	private boolean realEdit; // is a real edit happening, or is it just me?
	
	public MapConfigurationDialog(double defAsp) {
		this.defaultRatio = defAsp;
		DialogPane pane = this.getDialogPane();
		
		this.maintainRatio = new CheckBox("Maintain aspect ratio");	// instantiate the components
		this.maintainRatio.setSelected(true);
		
		this.widthBox = new Spinner<Integer>(1,12000,
				10*(int)Math.round(DEF_SIZE*Math.sqrt(defaultRatio)/10));
		this.widthBox.setEditable(true);
		this.widthBox.setMaxWidth(Double.MAX_VALUE);
		
		this.heightBox = new Spinner<Integer>(1,12000,
				10*(int)Math.round(this.widthBox.getValue()/defaultRatio/10));
		this.heightBox.setEditable(true);
		this.widthBox.setMaxWidth(Double.MAX_VALUE);
		
		this.widthBox.getEditor().textProperty().addListener((ov, pv, nv) -> {	// link the Spinners
			if (widthBox.getEditor().textProperty().isEmpty().get())	return;
			try {
				widthBox.increment(0);	// forces the spinner to commit its value
			} catch (NumberFormatException e) {
				widthBox.getEditor().textProperty().set(pv);
				widthBox.increment(0);
			}
			if (realEdit) {
				realEdit = false;
				if (maintainRatio.isSelected()) {
					int prefHeight = (int)Math.floor(widthBox.getValue()/defaultRatio);
					heightBox.getValueFactory().setValue(prefHeight);
				}
				realEdit = true;
			}
		});
		
		this.heightBox.getEditor().textProperty().addListener((ov, pv, nv) -> {
			if (heightBox.getEditor().textProperty().isEmpty().get())	return;
			try {
				heightBox.increment(0);	// forces the spinner to commit its value
			} catch (NumberFormatException e) {
				heightBox.getEditor().textProperty().set(pv);
				heightBox.increment(0);
			}
			if (realEdit) {
				realEdit = false;
				
				if (maintainRatio.isSelected()) {
					int prefWidth = (int)Math.ceil(heightBox.getValue()*defaultRatio);
					widthBox.getValueFactory().setValue(prefWidth);
				}
				realEdit = true;
			}
		});
		
		ObservableList<String> items = FXCollections.observableArrayList(
				"None","Low","High");
		this.smoothBox = new ComboBox<String>(items);
		this.smoothBox.setValue("Low");
		this.smoothBox.setMaxWidth(Double.MAX_VALUE);
		
		this.gui = new VBox();
		this.gui.setSpacing(20);
		
		pane.contentTextProperty().addListener((arg0) -> {	// set it to refresh the gui when... the content texts?
			this.updateGUI();
		});
		this.setTitle("Save options");	// set the words on it
		pane.setHeaderText("Please configure the final image");
		pane.getButtonTypes().addAll(new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });	// add buttons
		this.updateGUI();
		
		this.setResultConverter((btn) -> { return null; }); //this needn't return anything
		
		realEdit = true;
	}
	
	
	private void updateGUI() {
		this.gui.getChildren().clear();
		this.gui.getChildren().add(this.maintainRatio);
		
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setMaxWidth(Double.MAX_VALUE);
		grid.setAlignment(Pos.CENTER_LEFT);
		grid.getChildren().clear();
		grid.add(new Label("Width:"), 0, 0);
		grid.add(this.widthBox, 1, 0);
		grid.add(new Label("Height:"), 0, 1);
		grid.add(this.heightBox, 1, 1);
		grid.add(new Label("Smoothing:"), 0, 2);
		grid.add(this.smoothBox, 1, 2);
		this.gui.getChildren().add(grid);
		
		this.getDialogPane().setContent(this.gui);
	}
	
	
	public int[] getDims() {
		return new int[] { widthBox.getValue(), heightBox.getValue() };
	}
	
	
	public int getSmoothing() {
		if (smoothBox.getValue().equals("None"))
			return 1;
		else if (smoothBox.getValue().equals("Low"))
			return 2;
		else if (smoothBox.getValue().equals("High"))
			return 3;
		else
			return 0;
	}

}
