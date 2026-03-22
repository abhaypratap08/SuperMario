package view.screens;

import control.ButtonAction;

public class MainMenu {
    private int lineNumber;

    public MainMenu() {
        lineNumber = 0;
    }

    public void changeSelection(ButtonAction input) {
        if (input == ButtonAction.SELECTION_DOWN) {
            if (lineNumber == 3) lineNumber = 0;
            else lineNumber++;
        } else {
            if (lineNumber == 0) lineNumber = 3;
            else lineNumber--;
        }
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
