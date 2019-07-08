package yass;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Board extends ArrayList<String> implements Cloneable, Parcelable {

    public static final int MAX_BOARD_WIDTH = 50;

    private char mLastDirection = 'u';

    private Board() {}
    private Board(List<String> list) {
        super(list);
    }
    public static Board fromXSB(String level) {
        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(level.split("\\n")));

        // Remove CR character (might be left over after splitting CRLF line ending on LF)
        for (int row = 0; row < lines.size(); row++) {
            lines.set(row, lines.get(row).replace("\r", ""));
        }
        // Remove leading non-board lines
        while (lines.size() > 0 && !isABoardLine(lines.get(0))) {
            lines.remove(0);
        }
        // Remove trailing non-board lines
        while (lines.size() > 0 && !isABoardLine(lines.get(lines.size() - 1))) {
            lines.remove(lines.size() - 1);
        }
        // Find maximum width
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, line.length());
        }
        // Make all lines the same length
        for (int row = 0; row < lines.size(); row++) {
            while(lines.get(row).length() < width) {
                lines.set(row, lines.get(row) + " ");
            }
        }
        if (lines.size() > 0 && width > 0) {
            return new Board(lines);
        } else {
            return createEmpty();
        }
    }
    public static Board createEmpty() {
        Board newBoard = new Board();
        newBoard.add("      ");
        newBoard.add("      ");
        newBoard.add("      ");
        newBoard.add("      ");
        newBoard.add("      ");
        newBoard.add("      ");
        return newBoard;
    }
    public int getWidth() {
        return get(0).length();
    }
    public char getXY(int x, int y) {
        return get(y).charAt(x);
    }
    public void setXY(int x, int y, char tile) {
        set(y, get(y).substring(0, x) + tile + get(y).substring(x + 1));
    }
    private boolean inBounds(int x, int y) {
        return (y >= 0 && x >= 0 && y < size() && x < getWidth());
    }

    public char getLastDirection()
    {
        return mLastDirection;
    }

    public String toXSBWithoutNewline() {
        return toXSB().replace("\n", "");
    }

    public String toXSB() {
        StringBuilder outputBuffer = new StringBuilder();
        for (String line : this) {
            outputBuffer.append(line);
            outputBuffer.append("\n");
        }
        return outputBuffer.toString();
    }

    /**
     * Do a player move, returns true if a valid move is done
     * @param direction the character indicating the direction of the move
     */
    public boolean move(char direction) {

        int playerX = -1, playerY = -1;
        for (int y = 0; y < size(); y++) {
            String line = get(y);
            for (int x = 0; x < line.length(); x++) {
                if (line.charAt(x) == '@' || line.charAt(x) == '+') {
                    playerX = x;
                    playerY = y;
                }
            }
        }
        if (playerX == -1 || playerY == -1) {
            return false;
        }
        int nextPosX = playerX, nextPosY = playerY, secondPosX = playerX, secondPosY = playerY;
        switch (Character.toLowerCase(direction)) {
            case 'u':
                nextPosY = playerY - 1;
                secondPosY = playerY - 2;
                break;
            case 'd':
                nextPosY = playerY + 1;
                secondPosY = playerY + 2;
                break;
            case 'l':
                nextPosX = playerX - 1;
                secondPosX = playerX - 2;
                break;
            case 'r':
                nextPosX = playerX + 1;
                secondPosX = playerX + 2;
                break;
            default:
                return false;
        }

        if (!inBounds(nextPosX, nextPosY)) {
            return false;
        }
        if (getXY(nextPosX, nextPosY) == '$' || getXY(nextPosX, nextPosY) == '*') {
            // Push
            if (!inBounds(secondPosX, secondPosY)) {
                return false;
            }
            if (getXY(secondPosX, secondPosY) == ' ' || getXY(secondPosX, secondPosY) == '-' || getXY(secondPosX, secondPosY) == '_') {
                setXY(secondPosX, secondPosY, '$');
            } else if (getXY(secondPosX, secondPosY) == '.') {
                setXY(secondPosX, secondPosY, '*');
            } else {
                return false;
            }
        }

        if (getXY(nextPosX, nextPosY) == ' ' || getXY(nextPosX, nextPosY) == '-' || getXY(nextPosX, nextPosY) == '_' || getXY(nextPosX, nextPosY) == '$') {
            setXY(nextPosX, nextPosY, '@');
        } else if (getXY(nextPosX, nextPosY) == '.' || getXY(nextPosX, nextPosY) == '*') {
            setXY(nextPosX, nextPosY, '+');
        } else {
            return false;
        }

        if (getXY(playerX, playerY) == '+') {
            setXY(playerX, playerY, '.');
        } else {
            setXY(playerX, playerY, ' ');
        }

        mLastDirection = Character.toLowerCase(direction);

        return true;
    }

    /**
     * Perform multiple player moves
     * @param lurd the string containing the player moves
     * @return true if all moves are valid
     */
    public boolean move(String lurd) {
        for(char c : lurd.toCharArray()) {
            if (!move(c)) {
                return false;
            }
        }
        return true;
    }

    public void removePlayer() {
        for (int y = 0; y < size(); y++) {
            String line = get(y);
            for (int x = 0; x < line.length(); x++) {
                if (line.charAt(x) == '@') {
                    setXY(x, y, ' ');
                }
                if (line.charAt(x) == '+') {
                    setXY(x, y, '.');
                }
            }
        }
    }

    private static boolean isABoardLine(String textLine) {
        // skip trailing spaces and other floor characters, e.g., '_'
        // first and last character in a line belonging to a board must be a wall or a box-on-goal
        return textLine.matches("^[\\-_ ]*[#*]([# _\\-.$*@+]*[#*])?[_\\- ]*$");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(this);
    }

    public static final Parcelable.Creator<Board> CREATOR =
            new Parcelable.Creator<Board>() {
                public Board createFromParcel(Parcel in) {
                    Board b = new Board();
                    in.readStringList(b);
                    return b;
                }

                public Board[] newArray(int size) {
                    return new Board[size];
                }
            };

}
