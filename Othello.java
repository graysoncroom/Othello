// imports {{{
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
import javafx.event.Event;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import java.lang.Thread;
//}}}
public class Othello extends Application { // {{{
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
        ownerTurnLabel = new TitleLabel(0, true);
        othelloPane = new OthelloPane(BOARD_SIZE, BOX_SIZE, FLIP_DURATION);

        root.getChildren().addAll(ownerTurnLabel, othelloPane);

        updateOwnerTurnTitle();
        setupClickListeners();
        //othelloPane.highlightValidPositions(currentTurn);
        presentStage(stage);

    }

    public void setupClickListeners() { // {{{
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                final Owner currentOwner = othelloPane.getOwner(row, column);

                final int f_row = row;
                final int f_column = column;

                othelloPane.getBox(row, column).setOnMouseClicked(event -> {
                    if (currentOwner.getType() == OwnerType.NONE && othelloPane.isValidPosition(f_row, f_column, currentTurn)) {

                        currentOwner.setType(currentTurn);
                        othelloPane.updateBoardForFlips(f_row, f_column);

                        blackOwnerPoints = 0;
                        whiteOwnerPoints = 0;

                        for (int i = 0; i < BOARD_SIZE; i++) {
                            for (int j = 0; j < BOARD_SIZE; j++) {
                                Owner t_owner = othelloPane.getOwner(i, j);
                                if (t_owner.getType() == OwnerType.BLACK) {
                                    blackOwnerPoints++;
                                } else if (t_owner.getType() == OwnerType.WHITE) {
                                    whiteOwnerPoints++;
                                }
                            }
                        }

                        nextTurn();

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
                        } else {
                            //othelloPane.highlightValidPositions(currentTurn);
                            updateOwnerTurnTitle();
                            if (currentTurn == OwnerType.WHITE) {

                                Timeline timeLine = new Timeline(new KeyFrame(Duration.millis(FLIP_DURATION), ev -> {
                                    int positionRow = 0;
                                    int positionCol = 0;
                                    int count = 0;
                                    for (int i = 0; i < BOARD_SIZE; i++) {
                                        for (int j = 0; j < BOARD_SIZE; j++) {
                                            if (othelloPane.isValidPosition(i, j, OwnerType.WHITE)) {
                                                int tempCount = othelloPane.numFlips(i, j);
                                                if (tempCount > count) {
                                                    count = tempCount;
                                                    positionRow = i;
                                                    positionCol = j;
                                                }
                                            }
                                        }
                                    }

                                    Robot.click(othelloPane.getBox(positionRow, positionCol));
                                }));
                                timeLine.play();
                            }
                        }

                    }
                });
            }
        }
    } //}}}
    public void nextTurn() { //{{{
        currentTurn = (currentTurn == OwnerType.BLACK) ? OwnerType.WHITE : OwnerType.BLACK;
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
} //}}}
class Robot { // {{{
    public static void click(Node node) {
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_CLICKED, node.getLayoutX()/2, node.getLayoutY()/2, node.getLayoutX()/2, node.getLayoutY()/2, MouseButton.PRIMARY, 1, true, true, true, true, true, true, true, true, true, true, null));
    }
} //}}}
class OthelloPane extends GridPane { //{{{
    private String backgroundInHex = "#654321";
    private int boardSize;
    private int boxSize;
    private Duration flipDuration;
    private int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};

    private Owner[][] owners;
    private Pane[][] boxes;

    public OthelloPane(int boardSize, int boxSize, double flipDuration) {
        super();

        owners = new Owner[boardSize][boardSize];
        boxes = new Pane[boardSize][boardSize];

        this.boardSize = boardSize;
        this.boxSize = boxSize;
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
        boolean containsBlankBox = false;

        for (Owner[] row: owners) {
            for (Owner owner: row) {
                if (owner.getType() == OwnerType.NONE)
                    containsBlankBox = true;
            }
        }

        return !containsBlankBox;
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
        Owner owner = getOwner(row, column);
        for (int[] directionGroup: directions) {

                int rowDirection = directionGroup[0];
                int columnDirection = directionGroup[1];
                if (owner.getType() == OwnerType.NONE && isFlipableDirection(row, column, rowDirection, columnDirection, type)) {
                    return true;
                }
        }

        return valid;
    }

    public int numFlips(int originalRow, int originalColumn) {
        OwnerType originalOwnerType = getOwner(originalRow, originalColumn).getType();

        int highCount = 0;

        for (int[] directionGroup: directions) {
            int rowDirection = directionGroup[0];
            int columnDirection = directionGroup[1];

            int count = 0;

            int row = originalRow + rowDirection;
            int column = originalColumn + columnDirection;

            while (row < boardSize && row >= 0 && column < boardSize && column >= 0) {
                OwnerType ownerType = getOwner(row, column).getType();

                if (ownerType == OwnerType.NONE || ownerType == originalOwnerType) {
                    break;
                }

                count++;

                row += rowDirection;
                column += columnDirection;
            }

            if (count > highCount) {
                highCount = count;
            }
        }

        return highCount;
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

    public void highlightValidPositions(OwnerType type) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Owner owner = getOwner(i, j);

                owner.setType(owner.getType());
                owner.setRadius(boxSize/2 -5);
                owner.setOpacity(1);

                for (int[] directionGroup: directions) {
                    int rowDirection = directionGroup[0];
                    int columnDirection = directionGroup[1];

                    if (owner.getType() == OwnerType.NONE && isFlipableDirection(i, j, rowDirection, columnDirection, type)) {
                        owner.setFill(Color.YELLOW);
                        owner.setRadius(boxSize/2 -10);
                        owner.setOpacity(0.2);
                        break;
                    }
                }
            }
        }
    }

    public boolean isFlipableDirection(int originalRow, int originalColumn, int rowDirection, int columnDirection, OwnerType optionalOwnerType) {
        // hacky way to do an optional parameter
        OwnerType originalOwnerType = (optionalOwnerType == null) ? getOwner(originalRow, originalColumn).getType() : optionalOwnerType;

        int row = originalRow + rowDirection;
        int column = originalColumn + columnDirection;

        int count = 0;
        while (row < boardSize && row >= 0 && column < boardSize && column >= 0) {
            Owner owner = getOwner(row, column);
            OwnerType ownerType = owner.getType();

            if (ownerType == OwnerType.NONE || owner.getFill() == Color.YELLOW) {
                break;
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
