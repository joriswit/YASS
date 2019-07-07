package yass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import net.sourceforge.sokobanyasc.joriswit.yass.R;

public class BoardView extends View {

    private Board board;
    private char activeTool = '#';
    private boolean enableEdit = true;

    public void setBoard(Board board) {
        this.board = board;
        invalidate();
    }
    public void setActiveTool(char tool) {
        this.activeTool = tool;
    }
    public void setEnableEdit(boolean enableEdit) {
        this.enableEdit = enableEdit;
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if(isInEditMode()) {
            this.board = Board.createEmpty();
        }
    }

    private Drawable floorDrawable;
    private Drawable goalDrawable;
    private Drawable boxDrawable;
    private Drawable boxOnGoalDrawable;
    private Drawable[] playerDrawables;
    private final int[] playerResIds = new int[] {
            R.drawable.player_u,
            R.drawable.player_d,
            R.drawable.player_l,
            R.drawable.player_r
    };
    private Drawable[] playerOnGoalDrawables;
    private final int[] playerOnGoalResIds = new int[] {
            R.drawable.player_goal_u,
            R.drawable.player_goal_d,
            R.drawable.player_goal_l,
            R.drawable.player_goal_r
    };
    private Drawable[] wallDrawables;
    private final int[] wallResIds = new int[] {
            R.drawable.wall,
            R.drawable.wall_u,
            R.drawable.wall_d,
            R.drawable.wall_ud,
            R.drawable.wall_l,
            R.drawable.wall_ul,
            R.drawable.wall_dl,
            R.drawable.wall_udl,
            R.drawable.wall_r,
            R.drawable.wall_ur,
            R.drawable.wall_dr,
            R.drawable.wall_udr,
            R.drawable.wall_lr,
            R.drawable.wall_ulr,
            R.drawable.wall_dlr,
            R.drawable.wall_udlr
    };
    private Drawable wallTopDrawable;

    protected void onFinishInflate () {
        super.onFinishInflate();

        floorDrawable = ContextCompat.getDrawable(getContext(), R.drawable.floor);
        goalDrawable = ContextCompat.getDrawable(getContext(), R.drawable.goal);
        boxDrawable = ContextCompat.getDrawable(getContext(), R.drawable.box);
        boxOnGoalDrawable = ContextCompat.getDrawable(getContext(), R.drawable.box_goal);
        playerDrawables = new Drawable[4];
        for(int i = 0; i < 4; i++) {
            playerDrawables[i] = ContextCompat.getDrawable(getContext(), playerResIds[i]);
        }
        playerOnGoalDrawables = new Drawable[4];
        for(int i = 0; i < 4; i++) {
            playerOnGoalDrawables[i] = ContextCompat.getDrawable(getContext(), playerOnGoalResIds[i]);
        }
        wallDrawables = new Drawable[16];
        for(int i = 0; i < 16; i++) {
            wallDrawables[i] = ContextCompat.getDrawable(getContext(), wallResIds[i]);
        }
        wallTopDrawable = ContextCompat.getDrawable(getContext(), R.drawable.wall_top);

    }

    /**
     * Gets the index in the {@link #playerDrawables} and {@link #playerOnGoalDrawables} arrays
     */
    private int getDirectionIndex()
    {
        switch(board.getLastDirection()) {
            case 'u':
                return 0;
            case 'd':
                return 1;
            case 'l':
                return 2;
            case 'r':
                return 3;
            default:
                return 0;
        }
    }

    protected void onDraw(Canvas canvas)
    {
        int width = board.getWidth();
        int height = board.size();
        int tileSize = Math.min(getWidth() / (width + 2), getHeight() / (height + 2));
        int originX = (getWidth() - tileSize * width) / 2;
        int originY = (getHeight() - tileSize * height) / 2;

        for(int y = -((originY / tileSize) + 1); y < height + ((getHeight() - (originY + tileSize * height)) / tileSize) + 1; y++)
        {
            for(int x = -((originX / tileSize) + 1); x < width + ((getWidth() - (originX + tileSize * width)) / tileSize) + 1; x++)
            {
                char tile = ' ';
                if(x >= 0 && y >= 0 && x < width && y < height) {
                    tile = board.getXY(x, y);
                }
                Drawable drawable;
                boolean drawWallTop = false;
                switch(tile)
                {
                    case '.':
                        drawable = goalDrawable;
                        break;
                    case '$':
                        drawable = boxDrawable;
                        break;
                    case '*':
                        drawable = boxOnGoalDrawable;
                        break;
                    case '@':
                        drawable = playerDrawables[getDirectionIndex()];
                        break;
                    case '+':
                        drawable = playerOnGoalDrawables[getDirectionIndex()];
                        break;
                    case '#':
                        drawWallTop = true;
                        int wallIndex = 0;
                        if(y > 0 && board.getXY(x, y - 1) == '#')wallIndex += 1;else drawWallTop = false;
                        if(y < height - 1 && board.getXY(x, y + 1) == '#')wallIndex += 2;
                        if(x > 0 && board.getXY(x - 1, y) == '#')wallIndex += 4;else drawWallTop = false;
                        if(x < width - 1 && board.getXY(x + 1, y) == '#')wallIndex += 8;
                        if(drawWallTop && board.getXY(x - 1, y - 1) != '#')drawWallTop = false;
                        drawable = wallDrawables[wallIndex];
                        break;
                    default:
                        drawable = floorDrawable;
                        break;
                }
                int left = originX + x * tileSize;
                int top = originY + y * tileSize;
                int right = originX + x * tileSize + tileSize;
                int bottom = originY + y * tileSize + tileSize;
                drawable.setBounds(left, top, right, bottom);
                drawable.draw(canvas);

                if (drawWallTop) {
                    wallTopDrawable.setBounds(left - tileSize/2, top - tileSize/2, right - tileSize/2, bottom - tileSize/2);
                    wallTopDrawable.draw(canvas);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN
                && enableEdit) {

            int width = board.getWidth();
            int height = board.size();
            int tileSize = Math.min(getWidth() / (width + 2), getHeight() / (height + 2));
            int originX = (getWidth() - tileSize * width) / 2;
            int originY = (getHeight() - tileSize * height) / 2;

            int x = (int)Math.floor((event.getX() - originX) / tileSize);
            int y = (int)Math.floor((event.getY() - originY) / tileSize);

            while (x < 0) {
                if(width >= Board.MAX_BOARD_WIDTH) return true;
                for (int i = 0; i < height; i++) {
                    board.set(i, " " + board.get(i));
                }
                x++;
                width++;
            }
            while (x >= width) {
                if(width >= Board.MAX_BOARD_WIDTH) return true;
                for (int i = 0; i < height; i++) {
                    board.set(i, board.get(i) + " ");
                }
                width++;
            }
            while (y < 0) {
                if(height >= Board.MAX_BOARD_WIDTH) return true;
                StringBuilder outputBuffer = new StringBuilder(width);
                for (int i = 0; i < width; i++) {
                    outputBuffer.append(" ");
                }
                board.add(0, outputBuffer.toString());
                y++;
                height++;
            }
            while (y >= height) {
                if(height >= Board.MAX_BOARD_WIDTH) return true;
                StringBuilder outputBuffer = new StringBuilder(width);
                for (int i = 0; i < width; i++) {
                    outputBuffer.append(" ");
                }
                board.add(outputBuffer.toString());
                height++;
            }

            char val = board.getXY(x, y);

            if (activeTool == '#') {
                val = '#';
            } else if (activeTool == '$') {
                if (val == '.' || val == '+' || val == '$') {
                    val = '*';
                } else {
                    val = '$';
                }
            } else if (activeTool == '@') {
                board.removePlayer();
                if (val == '.' || val == '*' || val == '@') {
                    val = '+';
                } else {
                    val = '@';
                }
            } else if (activeTool == '.') {
                if (val == '$') {
                    val = '*';
                } else if (val == '@') {
                    val = '+';
                } else if (val == '.') {
                    val = '*';
                } else {
                    val = '.';
                }
            } else if (activeTool == ' ') {
                val = ' ';
            }
            board.setXY(x, y, val);

            invalidate();

            return true;
        } else {
            return false;
        }
    }

}
