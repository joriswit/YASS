package yass;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

import net.sourceforge.sokobanyasc.joriswit.yass.R;

import java.lang.ref.WeakReference;

public class ProgressDialogFragment extends DialogFragment {

    interface SolverStatusUpdate {
        void onSolverDone(String solution);
    }

    private WeakReference<ProgressDialogFragment.SolverStatusUpdate> mStatusUpdate;
    private ProgressDialogFragment.SolverTask mSolverTask;
    public Board mBoard;
    public String mSolution;
    public boolean mOptimizer;
    public long mAvailMem;
    public int mSolverSearchTime;
    public int mOptimizerSearchTime;
    public int mOptimizerOptimization;
    public OptimizerMethodOrder mOptimizerSearchMethodOrder;
    public int mOptimizerVicinitySearchBox1;
    public int mOptimizerVicinitySearchBox2;
    public int mOptimizerVicinitySearchBox3;
    private String mMessage;

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mStatusUpdate = new WeakReference<SolverStatusUpdate>((ProgressDialogFragment.SolverStatusUpdate)activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Create and execute the background task.
        mSolverTask = new ProgressDialogFragment.SolverTask();
        mSolverTask.execute();
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        if (mMessage == null) {
            if (!mOptimizer) {
                mMessage = getString(R.string.solving_initial_status_text);
            } else {
                mMessage = getString(R.string.optimizing_initial_status_text);
            }
        }
        progressDialog.setMessage(mMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return progressDialog;
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        mSolverTask.stopSolver();
    }

    private final class SolverTask extends AsyncTask<Object, String, String> {

        void stopSolver() {
            terminate();
        }

        @Override
        protected String doInBackground(Object... params) {

            int transpositionTableSize = (int)(mAvailMem / 1024 / 1024 / 2);

            if (!mOptimizer) {
                return solve(
                        mBoard.getWidth(), mBoard.size(),
                        mBoard.toXSBWithoutNewline(),
                        transpositionTableSize,
                        mSolverSearchTime
                );
            } else {
                return optimize(
                        mBoard.getWidth(), mBoard.size(),
                        mBoard.toXSBWithoutNewline(),
                        mSolution,
                        transpositionTableSize,
                        mOptimizerSearchTime,
                        mOptimizerSearchMethodOrder.isPermutationsEnabled(),
                        mOptimizerSearchMethodOrder.isRearrangementEnabled(),
                        mOptimizerSearchMethodOrder.isGlobalSearchEnabled(),
                        mOptimizerSearchMethodOrder.isVicinitySearchEnabled(),
                        mOptimizerSearchMethodOrder.getPermutationsOrder(),
                        mOptimizerSearchMethodOrder.getRearrangementOrder(),
                        mOptimizerSearchMethodOrder.getGlobalSearchOrder(),
                        mOptimizerSearchMethodOrder.getVicinitySearchOrder(),
                        mOptimizerVicinitySearchBox1,
                        mOptimizerVicinitySearchBox2,
                        mOptimizerVicinitySearchBox3,
                        mOptimizerOptimization
                );
            }
        }
        @Override
        protected void onProgressUpdate(String... progress) {
            mMessage = progress[0];
            ProgressDialog progressDialog = ((ProgressDialog) ProgressDialogFragment.this.getDialog());
            if (progressDialog != null) {
                progressDialog.setMessage(mMessage);
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (mStatusUpdate != null) {
                ProgressDialogFragment.SolverStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    statusUpdate.onSolverDone(result);
                }
            }
        }
        native String optimize(int width, int height, String board,
                               String game,
                               int solverTranspositionTableSize,
                               int searchTime,
                               boolean optimizerMethodPermutationsEnabled,
                               boolean optimizerMethodRearrangementEnabled,
                               boolean optimizerMethodGlobalSearchEnabled,
                               boolean optimizerMethodVicinitySearchEnabled,
                               int optimizerMethodPermutationsOrder,
                               int optimizerMethodRearrangementOrder,
                               int optimizerMethodGlobalSearchOrder,
                               int optimizerMethodVicinitySearchOrder,
                               int vicinityBox1,
                               int vicinityBox2,
                               int vicinityBox3,
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

}