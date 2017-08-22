import javafx.animation.*;
import javafx.scene.transform.Rotate;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Othello extends Application {
    static final String BOARD_GAME_NAME = "Othello";
    static final int BOARD_SIZE = 10;
    static final int BOX_SIZE = 40;
    static final int FLIP_DURATION = 500;

    static int blackOwnerPoints = 0;
    static int whiteOwnerPoints = 0;

    static final OwnerType startingOwner = OwnerType.BLACK;

    static OwnerType currentTurn = startingOwner;

    static FlowPane root;
    static OthelloPane othelloPane;
    static TitleLabel ownerTurnLabel;

    @Override
    public void start(Stage stage) {
        root = new FlowPane(Orientation.VERTICAL);
        root.setAlignment(Pos.CENTER);
        ownerTurnLabel = new TitleLabel(0,true);
        othelloPane = new OthelloPane(BOARD_SIZE, BOX_SIZE, FLIP_DURATION);

        root.getChildren().addAll(ownerTurnLabel, othelloPane);

        updateOwnerTurnTitle();
        setupClickListeners();
        presentStage(stage);
    }

    public void setupClickListeners() { // {{{
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                final Owner currentOwner = othelloPane.getOwner(row, column);

                final int f_row = row;
                final int f_column = column;

                othelloPane.getBox(row, column).setOnMouseClicked(event -> {
                    if (othelloPane.hasGameEnded()) {
                        String winner = "";
                        boolean haveTied = false;

                        if (whiteOwnerPoints < blackOwnerPoints) {
                            winner = OwnerType.BLACK.toString();
                        } else if (whiteOwnerPoints > blackOwnerPoints) {
                            winner = OwnerType.WHITE.toString();
                        } else {
                            haveTied = true;
                        }

                        ownerTurnLabel.setText(haveTied ? "Tie" : winner + " Wins");
                        return;
                    } else if (currentOwner.getType() == OwnerType.NONE && othelloPane.isValidPosition(f_row, f_column, currentTurn)) {
                        currentOwner.setType(currentTurn);
                        othelloPane.updateBoardForFlips(f_row, f_column);
                        nextTurn();
                        updateOwnerTurnTitle();
                        //othelloPane.highlightPositions(f_row, f_column, currentTurn);
                    }

                });
            }
        }


    } //}}}
    public void nextTurn() { //{{{
        if (currentTurn == OwnerType.BLACK) {
            currentTurn = OwnerType.WHITE;
        } else {
            currentTurn = OwnerType.BLACK;
        }
    } //}}}
    public void updateOwnerTurnTitle() { // {{{
        ownerTurnLabel.setText(currentTurn + " Player's Turn");
    } //}}}
    public void presentStage(Stage stage) { // {{{
        stage.setTitle(BOARD_GAME_NAME);
        Scene scene = new Scene(root, 500, 500);
        stage.setScene(scene);
        stage.show();
    } // }}}
    public static void main(String[] args) { // {{{
        launch(args);
    } //}}}
}
class OthelloPane extends GridPane { //{{{
    private String backgroundInHex = "#654321";
    private int boardSize;
    private Duration flipDuration;
    private int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};

    private Owner[][] owners;
    private Pane[][] boxes;

    public OthelloPane(int boardSize, int boxSize, double flipDuration) {
        super();

        owners = new Owner[boardSize][boardSize];
        boxes = new Pane[boardSize][boardSize];

        this.boardSize = boardSize;
        this.flipDuration = Duration.millis(flipDuration);

        // setup grid constaints
        for (int i = 0; i < boardSize; i++)
            getRowConstraints().add( new RowConstraints(boxSize));
        for (int i = 0; i < boardSize; i++)
            getColumnConstraints().add( new ColumnConstraints(boxSize));

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Pane box = new Pane();
                Owner owner = new Owner(boxSize, OwnerType.NONE);
                box.setStyle("-fx-background-color: " + backgroundInHex + ";");
                box.getChildren().add(owner);
                setOwner(i, j, owner);
                setBox(i, j, box);
                add(box, j, i);
            }
        }

        int middle = boardSize/2;

        Owner topLeft = getOwner(middle - 1, middle - 1);
        Owner topRight = getOwner(middle - 1, middle);
        Owner bottomLeft = getOwner(middle, middle - 1);
        Owner bottomRight = getOwner(middle, middle);

        topLeft.setType(OwnerType.BLACK);
        bottomRight.setType(OwnerType.BLACK);
        topRight.setType(OwnerType.WHITE);
        bottomLeft.setType(OwnerType.WHITE);

        setGridLinesVisible(true);
        setAlignment(Pos.CENTER);
    }

    public boolean hasGameEnded() {
        OwnerType previousNonEmptyOwnerType = null;
        boolean containsBlankBox = false;
        boolean containsMoreThanOneTypeOfOwner = false;

        for (Owner[] row: owners) {
            for (Owner owner: row) {
                if (owner.getType() != previousNonEmptyOwnerType)
                    containsMoreThanOneTypeOfOwner = true;

                if (owner.getType() == OwnerType.NONE)
                    containsBlankBox = true;
                else
                    previousNonEmptyOwnerType = owner.getType();
            }
        }

        return !containsBlankBox || !containsMoreThanOneTypeOfOwner;

    }

    // flip related
    public void updateBoardForFlips(int originalRow, int originalColumn) {
        OwnerType[][] ownerTypes = new OwnerType[boardSize][boardSize];

        // Setup OwnerType board representation of owners.
        for (int i = 0; i < boardSize; i++)
            for (int j = 0; j < boardSize; j++)
                ownerTypes[i][j] = getOwner(i, j).getType();

        for (int[] directionGroup: directions) {
            int rowDirection = directionGroup[0];
            int columnDirection = directionGroup[1];
            if (isFlipableDirection(originalRow, originalColumn, rowDirection, columnDirection, null)) {
                flipInDirection(ownerTypes, originalRow, originalColumn, rowDirection, columnDirection);
            }
        }

    }

    public boolean isValidPosition(int row, int column, OwnerType type) {
        boolean valid = false;
        for (int[] directionGroup: directions) {
            int rowDirection = directionGroup[0];
            int columnDirection = directionGroup[1];
            valid = valid || isFlipableDirection(row, column, rowDirection, columnDirection, type);
            if (valid) break;
        }
        return valid;
    }

    public void flipInDirection(OwnerType[][] ownerTypes, int originalRow, int originalColumn, int rowDirection, int columnDirection) {
        // Contract: Will not modify the contents of 'ownerTypes', because
        // all flips will act upon the 'owners' array.
        OwnerType originalOwnerType = ownerTypes[originalRow][originalColumn];

        int row = originalRow + rowDirection;
        int column = originalColumn + columnDirection;

        while (row < boardSize && row >= 0 && column < boardSize && column >= 0) {
            OwnerType ownerType = ownerTypes[row][column];

            if (ownerType == OwnerType.NONE || ownerType == originalOwnerType) {
                break;
            }

            Owner owner = getOwner(row, column);

            RotateTransition firstRotator = new RotateTransition(flipDuration, owner);
            firstRotator.setAxis(Rotate.Y_AXIS);
            firstRotator.setFromAngle(0);
            firstRotator.setToAngle(90);
            firstRotator.setInterpolator(Interpolator.LINEAR);
            firstRotator.setOnFinished(e -> owner.setType(originalOwnerType));

            RotateTransition secondRotator = new RotateTransition(flipDuration, owner);
            secondRotator.setAxis(Rotate.Y_AXIS);
            secondRotator.setFromAngle(90);
            secondRotator.setToAngle(180);
            secondRotator.setInterpolator(Interpolator.LINEAR);

            new SequentialTransition(firstRotator, secondRotator).play();

            row += rowDirection;
            column += columnDirection;
        }
    }

    public boolean isFlipableDirection(int originalRow, int originalColumn, int rowDirection, int columnDirection, OwnerType optionalOwnerType) {
        OwnerType originalOwnerType = (optionalOwnerType == null) ? getOwner(originalRow, originalColumn).getType() : optionalOwnerType;

        int row = originalRow + rowDirection;
        int column = originalColumn + columnDirection;

        int count = 0;
        while (row < boardSize && row >= 0 && column < boardSize && column >= 0) {
            Owner owner = getOwner(row, column);
            OwnerType ownerType = owner.getType();

            if (ownerType == OwnerType.NONE) {
                return false;
            } else if (ownerType == originalOwnerType) {
                return count > 0;
            }

            row += rowDirection;
            column += columnDirection;
            count++;
        }

        return false;
    }

    // getters
    public Pane getBox(int row, int column) {
        return boxes[row][column];
    }
    public Owner getOwner(int row, int column) {
        return owners[row][column];
    }

    // setters
    private void setBox(int row, int column, Pane box) {
        boxes[row][column] = box;
    }
    private void setOwner(int row, int column, Owner owner) {
        owners[row][column] = owner;
    }
} //}}}
class Owner extends Circle { //{{{
    private OwnerType type = OwnerType.NONE;
    private Owner() {} // Force all objects to use my provided constructor.
    public Owner(int size, OwnerType type) {
        super();

        int center = size / 2;
        setType(type);
        setCenterX(center);
        setCenterY(center);
        setRadius(center-5);
    }
    public OwnerType getType() { return type; }
    public void setType(OwnerType type) {
        this.type = type;
        setFill(type.getColor());
    }
} //}}}
class TitleLabel extends Label { // {{{
    final int padding = 30;
    private TitleLabel() {}
    public TitleLabel(int fontSize, boolean bold) {
        setFont(new Font(30));
        setMaxWidth(Double.MAX_VALUE);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30,0,30,0));

        if (bold) setStyle("-fx-font-weight: bold");
    }
} //}}}
enum OwnerType { // {{{
    NONE,
    WHITE,
    BLACK;

    public Color getColor() {
        switch (this) {
            case WHITE: return Color.WHITE;
            case BLACK: return Color.BLACK;
            default: return null;
        }
    }

    public String toString() {
        switch (this) {
            case WHITE: return "White";
            case BLACK: return "Black";
            default: return "None";
        }
    }
}; //}}}
