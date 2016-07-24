package yass;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import net.sourceforge.sokobanyasc.joriswit.yass.R;

public class YASSActivity extends Activity {

    private static boolean mNativeLibLoaded = false;

    private Board mBoard;

    private String mSolution;

    private boolean mPlayback;
    private boolean mPlaybackPaused;
    private Board mPlaybackBoard;
    private int mPlaybackSolutionPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_yass);

        final BoardView boardView = (BoardView)findViewById(R.id.view);

        if(savedInstanceState != null) {
            mBoard = savedInstanceState.getParcelable("board");
            mSolution = savedInstanceState.getString("solution");
        } else {
            Intent intent = getIntent();
            if (intent.getAction().equals("nl.joriswit.sokosolver.SOLVE")) {
                String board = intent.getStringExtra("LEVEL");
                if (board != null) {
                    mBoard = Board.fromXSB(board);
                } else {
                    mBoard = Board.createEmpty();
                }
            } else if (intent.getAction().equals("nl.joriswit.sokosolver.OPTIMIZE")) {
                String board = intent.getStringExtra("LEVEL");
                if (board != null) {
                    mBoard = Board.fromXSB(board);
                } else {
                    mBoard = Board.createEmpty();
                }
                // Note: SOLUTION parameter may also be a non-solution game
                mSolution = intent.getStringExtra("SOLUTION");
            } else {
                mBoard = Board.createEmpty();
            }
        }
        boardView.setBoard(mBoard);

        final ImageButton wallButton = (ImageButton)findViewById(R.id.wall_button);
        final ImageButton boxButton = (ImageButton)findViewById(R.id.box_button);
        final ImageButton playerButton = (ImageButton)findViewById(R.id.player_button);
        final ImageButton goalButton = (ImageButton)findViewById(R.id.goal_button);
        final ImageButton eraseButton = (ImageButton)findViewById(R.id.erase_button);

        wallButton.setPressed(true);

        wallButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                wallButton.setPressed(true);
                boxButton.setPressed(false);
                playerButton.setPressed(false);
                goalButton.setPressed(false);
                eraseButton.setPressed(false);
                boardView.setActiveTool('#');
                return true;
            }
        });
        boxButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                wallButton.setPressed(false);
                boxButton.setPressed(true);
                playerButton.setPressed(false);
                goalButton.setPressed(false);
                eraseButton.setPressed(false);
                boardView.setActiveTool('$');
                return true;
            }
        });
        playerButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                wallButton.setPressed(false);
                boxButton.setPressed(false);
                playerButton.setPressed(true);
                goalButton.setPressed(false);
                eraseButton.setPressed(false);
                boardView.setActiveTool('@');
                return true;
            }
        });
        goalButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                wallButton.setPressed(false);
                boxButton.setPressed(false);
                playerButton.setPressed(false);
                goalButton.setPressed(true);
                eraseButton.setPressed(false);
                boardView.setActiveTool('.');
                return true;
            }
        });
        eraseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                wallButton.setPressed(false);
                boxButton.setPressed(false);
                playerButton.setPressed(false);
                goalButton.setPressed(false);
                eraseButton.setPressed(true);
                boardView.setActiveTool(' ');
                return true;
            }
        });

        final ImageButton editButton = (ImageButton)findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(YASSActivity.this);
                builder
                        .setMessage(R.string.back_to_edit_mode_confirm)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == DialogInterface.BUTTON_POSITIVE) {
                                    mSolution = null;
                                    mPlayback = false;
                                    boardView.setBoard(mBoard);
                                    boardView.invalidate();
                                    updateToolbar();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
        });
        final ImageButton playPauseButton = (ImageButton)findViewById(R.id.playpause_button);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPlayback) {
                    mPlayback = false;
                    mPlaybackPaused = true;
                } else {
                    if (!mPlaybackPaused) {
                        mPlaybackSolutionPosition = 0;
                        mPlaybackBoard = (Board) mBoard.clone();
                        boardView.setBoard(mPlaybackBoard);
                    }
                    mPlayback = true;
                    mPlaybackPaused = false;
                    playbackSolution();
                }
                updateToolbar();
            }
        });
        final ImageButton stopButton = (ImageButton)findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayback = false;
                mPlaybackPaused = false;
                mPlaybackSolutionPosition = 0;
                mPlaybackBoard = (Board)mBoard.clone();
                boardView.setBoard(mPlaybackBoard);
                updateToolbar();
            }
        });

        // Load default settings on new install
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Check for settings expiration
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long lastSettingsDate = prefs.getLong("last_settings_date", 0);
        if (lastSettingsDate != 0) {
            // Settings expire 24 hours after last viewing the settings screen
            if (System.currentTimeMillis() > lastSettingsDate + (1000 * 60 * 60 * 24)) {
                int optimizerSearchMethodOrder = Integer.parseInt(prefs.getString("optimizer_search_method_order", "0"));
                int optimizerVicinitySearchBox1 = prefs.getInt("optimizer_vicinity_search_box1", 20);
                int optimizerVicinitySearchBox2 = prefs.getInt("optimizer_vicinity_search_box2", 10);

                if (optimizerSearchMethodOrder != 0
                    || optimizerVicinitySearchBox1 != 20
                    || optimizerVicinitySearchBox2 != 10) {
                    // Custom settings have expired, reset to defaults
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    edit.putString("optimizer_search_method_order", "0");
                    edit.putInt("optimizer_vicinity_search_box1", 20);
                    edit.putInt("optimizer_vicinity_search_box2", 10);
                    edit.apply();

                    Toast.makeText(this, R.string.settings_reset_to_defaults, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("board", mBoard);
        outState.putString("solution", mSolution);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToolbar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPlayback) {
            mPlayback = false;
            mPlaybackPaused = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.yass, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_paste_puzzle).setEnabled(mSolution == null && getClipboardText() != null);
        menu.findItem(R.id.action_copy_solution).setEnabled(mSolution != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_solve:
                startSolver(false);
                return true;
            case R.id.action_optimize:
                if(mSolution == null) {
                    final EditText lurdTextView = new EditText(this);
                    new AlertDialog.Builder(this)
                        .setTitle(R.string.paste_solution_title_text)
                        .setMessage(R.string.paste_solution_text)
                        .setView(lurdTextView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String lurd = lurdTextView.getText().toString();
                                Board tryPositionBoard = (Board)mBoard.clone();
                                if (tryPositionBoard.move(lurd)) {
                                    mSolution = lurd;
                                    updateToolbar();
                                    startSolver(true);
                                } else {
                                    new AlertDialog.Builder(YASSActivity.this)
                                            .setTitle(R.string.paste_solution_invalid_title_text)
                                            .setMessage(R.string.paste_solution_invalid_text)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                } else {
                    startSolver(true);
                }
                return true;
            case R.id.action_paste_puzzle:
                String xsb = getClipboardText();
                if(xsb != null) {
                    mBoard = Board.fromXSB(getClipboardText());
                    BoardView bv = (BoardView) YASSActivity.this.findViewById(R.id.view);
                    bv.setBoard(mBoard);
                    bv.invalidate();
                }
                return true;
            case R.id.action_copy_solution:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.clipboard_description), mSolution);
                clipboard.setPrimaryClip(clip);
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private String getClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData data = clipboard.getPrimaryClip();
            if (data != null)
                return String.valueOf(data.getItemAt(0).coerceToText(this));
        }
        return null;
    }

    private ProgressDialog mDialog;
    private long mAvailMem;
    private void startSolver(boolean optimizer) {

        if(mNativeLibLoaded) {
            ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            mAvailMem = memInfo.availMem;

            BoardView boardView = (BoardView) findViewById(R.id.view);
            boardView.setEnableEdit(false);
            mDialog = new ProgressDialog(this);
            if (!optimizer) {
                mDialog.setMessage(getString(R.string.solving_initial_status_text));
            } else {
                mDialog.setMessage(getString(R.string.optimizing_initial_status_text));
            }
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setCancelable(true);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mSolverTask.stopSolver();
                }
            });
            mDialog.show();

            mSolverTask = new SolverTask();
            mSolverTask.execute(mBoard, optimizer);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setMessage(R.string.native_library_load_error)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private SolverTask mSolverTask;

    final class SolverTask extends AsyncTask<Object, String, String> {

        void stopSolver() {
            //this.cancel(true);
            terminate();
        }

        @Override
        protected String doInBackground(Object... params) {

            int transpositionTableSize = (int)(mAvailMem / 1024 / 1024 / 2);

            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(YASSActivity.this);
            int solverSearchTime = Integer.parseInt(defaultSharedPreferences.getString("solver_search_time", "600"));
            int optimizerSearchTime = Integer.parseInt(defaultSharedPreferences.getString("optimizer_search_time", "3600"));
            int optimizerOptimization = Integer.parseInt(defaultSharedPreferences.getString("optimizer_optimization", "1"));
            int optimizerSearchMethodOrder = Integer.parseInt(defaultSharedPreferences.getString("optimizer_search_method_order", "0"));
            int optimizerVicinitySearchBox1 = defaultSharedPreferences.getInt("optimizer_vicinity_search_box1", 20);
            int optimizerVicinitySearchBox2 = defaultSharedPreferences.getInt("optimizer_vicinity_search_box2", 10);


            Board board = (Board)params[0];
            boolean startOptimizer = (Boolean)params[1];

            if(!startOptimizer) {
                return solve(
                        board.getWidth(), board.size(),
                        board.toXSBWithoutNewline(),
                        transpositionTableSize,
                        solverSearchTime
                );
            } else {
                return optimize(
                        board.getWidth(), board.size(),
                        board.toXSBWithoutNewline(),
                        mSolution,
                        transpositionTableSize,
                        optimizerSearchTime,
                        optimizerSearchMethodOrder,
                        optimizerVicinitySearchBox1,
                        optimizerVicinitySearchBox2,
                        optimizerOptimization
                );
            }
        }
        @Override
        protected void onProgressUpdate(String... progress) {
            String statusText = progress[0];
            mDialog.setMessage(statusText);
        }
        @Override
        protected void onPostExecute(String result) {

            if(result != null && result.length() > 0) {
                mSolution = result;
                invalidateOptionsMenu();

                mPlayback = true;
                mPlaybackBoard = (Board)mBoard.clone();
                mPlaybackSolutionPosition = 0;

                playbackSolution();

                updateToolbar();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("SOLUTION", result);
                setResult(RESULT_OK, resultIntent);
            }
            mDialog.dismiss();
        }

        native String optimize(int width, int height, String board,
                                      String game,
                                      int solverTranspositionTableSize,
                                      int searchTime,
                                      int optimizerSearchMethodOrder,
                                      int vicinityBox1,
                                      int vicinityBox2,
                                      int optimization);
        native String solve(int width, int height, String board,
                                   int solverTranspositionTableSize,
                                   int solverSearchTime);
        native void terminate();

        @SuppressWarnings("UnusedDeclaration") // Called using JNI
        public final void onProgress(String statusText) {
            this.publishProgress(statusText);
        }
    }

    private void updateToolbar() {
        View editControls = findViewById(R.id.edit_controls);
        editControls.setVisibility(mSolution == null ? View.VISIBLE : View.GONE);

        View playControls = findViewById(R.id.play_controls);
        playControls.setVisibility(mSolution == null ? View.GONE : View.VISIBLE);

        if(mSolution != null) {
            TextView currentSolutionText = (TextView) findViewById(R.id.current_solution);
            int pushes = 0;
            for(int i = 0; i < mSolution.length(); i++) { if(Character.isUpperCase(mSolution.charAt(i))) pushes++; }
            currentSolutionText.setText(String.format(getString(R.string.current_solution_text), mSolution.length(), pushes));
        }

        View stopButton = findViewById(R.id.stop_button);
        stopButton.setEnabled(mPlayback || mPlaybackSolutionPosition > 0);
        ImageButton playPauseButton = (ImageButton)findViewById(R.id.playpause_button);
        playPauseButton.setImageResource(mPlayback ? R.drawable.ic_av_pause : R.drawable.ic_av_play_arrow);
        BoardView boardView = (BoardView)findViewById(R.id.view);
        boardView.setEnableEdit(mSolution == null);
    }

    private void playbackSolution() {
        mPlaybackHandler.postDelayed(mPlaybackRunnable, 100);
    }

    private final Handler mPlaybackHandler = new Handler();

    private final Runnable mPlaybackRunnable = new Runnable() {
        public void run() {

            if(mPlayback && mPlaybackSolutionPosition < mSolution.length()) {
                mPlaybackBoard.move(mSolution.charAt(mPlaybackSolutionPosition));
                mPlaybackSolutionPosition++;

                BoardView bv = (BoardView) YASSActivity.this.findViewById(R.id.view);
                bv.setBoard(mPlaybackBoard);
                bv.invalidate();

                if (mPlaybackSolutionPosition < mSolution.length()) {
                    mPlaybackHandler.postDelayed(mPlaybackRunnable, 100);
                } else {
                    mPlayback = false;
                    updateToolbar();
                }
            } else {
                mPlayback = false;
                updateToolbar();
            }
        }
    };

    static {
    	try {
    		System.loadLibrary("yassjni");
            mNativeLibLoaded = true;
    	} catch(UnsatisfiedLinkError ule) {
            mNativeLibLoaded = false;
        }
    }
}
