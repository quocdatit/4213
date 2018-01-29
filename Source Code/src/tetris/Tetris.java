package tetris;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

import javax.swing.JFrame;

public class Tetris extends JFrame {

    private static final long serialVersionUID = -4722429764792514382L;
    private static final long FRAME_TIME = 1000L / 50L;

    private static final int TYPE_COUNT = TileType.values().length;


    private BoardPanel board;
    private SidePanel side;

    private boolean isPaused;
    private boolean isNewGame;
    private boolean isGameOver;

    private int level;
    private int score;

    private Random random;

    private Clock logicTimer;

    private TileType currentType;

    private TileType nextType;

    private int currentCol;

    private int currentRow;

    private int currentRotation;

    private int dropCooldown;

    private float gameSpeed; // Tốc độ

    private Tetris() {
        // Thiết lập cửa sổ chính của game
        super("Tetris - Xếp hình"); // Tiêu đề
        setLayout(new BorderLayout()); // Layout
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Mặc định khi nhấn close thì thoát game
        setResizable(false); // Không cho thay đổi kích thước cửa sổ game

        this.board = new BoardPanel(this); // Tạo khung chính của trò chơi
        this.side = new SidePanel(this); // Tạo vùng chứa các thông tin

        // Thêm vào cửa sổ chính
        add(board, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);

        // Bắt sự kiện nhấn các nút điều khiển game
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch(e.getKeyCode()) {
                    
                    // Khi nhấn nút "Xuống" làm cho khối di chuyển xuống nhanh hơn
                    case KeyEvent.VK_DOWN:
                        if(!isPaused && dropCooldown == 0) {
                            logicTimer.setCyclesPerSecond(25.0f);
                        }
                        break;
                    
                    // Khi nhấn "TRÁI" thì khối di chuyển sang trái
                    case KeyEvent.VK_LEFT:
                        if(!isPaused && board.isValidAndEmpty(currentType, currentCol - 1, currentRow, currentRotation)) {
                            currentCol--;
                        }
                        break;
                    // Khi nhấn "PHẢI" thì khối di chuyển sang phải
                    case KeyEvent.VK_RIGHT:
                        if(!isPaused && board.isValidAndEmpty(currentType, currentCol + 1, currentRow, currentRotation)) {
                            currentCol++;
                        }
                        break;
                    // UP: xoay theo ngược kim đồng hồ
//                    case KeyEvent.VK_UP:
//                        if(!isPaused) {
//                            rotatePiece((currentRotation == 0) ? 3 : currentRotation - 1);
//                        }
//                        break;
                    // Khi nhấn "LÊN" khối xoay theo cùng chiều kim đồng hồ
                    case KeyEvent.VK_UP:
                        if(!isPaused) {
                            rotatePiece((currentRotation == 3) ? 0 : currentRotation + 1);
                        }
                        break;
                    // Nhấn "PHÍM CÁCH" cho game tạm dừng
                    case KeyEvent.VK_SPACE:
                        if(!isGameOver && !isNewGame) {
                            isPaused = !isPaused;
                            logicTimer.setPaused(isPaused);
                        }
                        break;
                    // "ENTER" reset game
                    case KeyEvent.VK_ENTER:
                        if(isGameOver || isNewGame) {
                            resetGame();
                        }
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch(e.getKeyCode()) {

                    case KeyEvent.VK_S:
                        logicTimer.setCyclesPerSecond(gameSpeed);
                        logicTimer.reset();
                    break;
                }
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Starts the game running. Initializes everything and enters the game loop.
     */
    private void startGame() {
        /*
         * Initialize our random number generator, logic timer, and new game variables.
         */
        this.random = new Random();
        this.isNewGame = true;
        this.gameSpeed = 1.0f;

        /*
         * Setup the timer to keep the game from running before the user presses enter
         * to start it.
         */
        this.logicTimer = new Clock(gameSpeed);
        logicTimer.setPaused(true);

        while(true) {
                //Get the time that the frame started.
            long start = System.nanoTime();

            //Update the logic timer.
            logicTimer.update();

            /*
             * If a cycle has elapsed on the timer, we can update the game and
             * move our current piece down.
             */
            if(logicTimer.hasElapsedCycle()) {
                updateGame();
            }

            //Decrement the drop cool down if necessary.
            if(dropCooldown > 0) {
                dropCooldown--;
            }

            //Display the window to the user.
            renderGame();

            long delta = (System.nanoTime() - start) / 1000000L;
            if(delta < FRAME_TIME) {
                try {
                    Thread.sleep(FRAME_TIME - delta);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateGame() {
        if(board.isValidAndEmpty(currentType, currentCol, currentRow + 1, currentRotation)) {
                currentRow++;
        } else {
            board.addPiece(currentType, currentCol, currentRow, currentRotation);
            int cleared = board.checkLines();
            if(cleared > 0) {
                    score += 50 << cleared;
            }
            gameSpeed += 0.035f;
            logicTimer.setCyclesPerSecond(gameSpeed);
            logicTimer.reset();
            dropCooldown = 25;
            level = (int)(gameSpeed * 1.70f);
            spawnPiece();
        }
    }

    private void renderGame() {
        board.repaint();
        side.repaint();
    }

    private void resetGame() {
        this.level = 1;
        this.score = 0;
        this.gameSpeed = 1.0f;
        this.nextType = TileType.values()[random.nextInt(TYPE_COUNT)];
        this.isNewGame = false;
        this.isGameOver = false;		
        board.clear();
        logicTimer.reset();
        logicTimer.setCyclesPerSecond(gameSpeed);
        spawnPiece();
    }

    private void spawnPiece() {

        this.currentType = nextType;
        this.currentCol = currentType.getSpawnColumn();
        this.currentRow = currentType.getSpawnRow();
        this.currentRotation = 0;
        this.nextType = TileType.values()[random.nextInt(TYPE_COUNT)];

        if(!board.isValidAndEmpty(currentType, currentCol, currentRow, currentRotation)) {
            this.isGameOver = true;
            logicTimer.setPaused(true);
        }		
    }
    
    // Xoay khối
    private void rotatePiece(int newRotation) {

        int newColumn = currentCol;
        int newRow = currentRow;

        int left = currentType.getLeftInset(newRotation);
        int right = currentType.getRightInset(newRotation);
        int top = currentType.getTopInset(newRotation);
        int bottom = currentType.getBottomInset(newRotation);

        if(currentCol < -left) {
                newColumn -= currentCol - left;
        } else if(currentCol + currentType.getDimension() - right >= BoardPanel.COL_COUNT) {
                newColumn -= (currentCol + currentType.getDimension() - right) - BoardPanel.COL_COUNT + 1;
        }

        if(currentRow < -top) {
                newRow -= currentRow - top;
        } else if(currentRow + currentType.getDimension() - bottom >= BoardPanel.ROW_COUNT) {
                newRow -= (currentRow + currentType.getDimension() - bottom) - BoardPanel.ROW_COUNT + 1;
        }

        if(board.isValidAndEmpty(currentType, newColumn, newRow, newRotation)) {
            currentRotation = newRotation;
            currentRow = newRow;
            currentCol = newColumn;
        }
    }

    // Kiểm tra tạm dừng hay không
    public boolean isPaused() {
        return isPaused;
    }

    // Kiểm tra kết thúc game chưa
    public boolean isGameOver() {
        return isGameOver;
    }

    // Kiểm tra phải game mới không
    public boolean isNewGame() {
        return isNewGame;
    }

    // Điểm
    public int getScore() {
        return score;
    }

    // Cấp độ
    public int getLevel() {
        return level;
    }

    // Khối hiện tại
    public TileType getPieceType() {
        return currentType;
    }

    // Khối tiếp theo
    public TileType getNextPieceType() {
        return nextType;
    }

    // Cột hiện tại
    public int getPieceCol() {
        return currentCol;
    }

    // Dòng hiện tại
    public int getPieceRow() {
        return currentRow;
    }

    /**
     * Gets the rotation of the current piece.
     * @return The rotation.
     */
    public int getPieceRotation() {
        return currentRotation;
    }
    
    // Khởi chạy chương trình
    public static void main(String[] args) {
        Tetris tetris = new Tetris();
        tetris.startGame();
    }

}
