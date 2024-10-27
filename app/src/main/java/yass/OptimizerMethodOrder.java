package yass;

import android.content.Context;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Helper class to manipulate the order and state of the various optimizer methods
 */
class OptimizerMethodOrder extends ArrayList<OptimizerMethodOrder.OptimizerMethod> {

    static class OptimizerMethod {
        private char mMethodCode;
        private CharSequence mMethodText;
        private boolean mEnabled;

        @NonNull
        CharSequence getMethodText() {
            return this.mMethodText;
        }

        boolean getEnabled() {
            return this.mEnabled;
        }

        void setEnabled(boolean value) {
            this.mEnabled = value;
        }
    }

    private Context mContext;

    /**
     * Initializes the list with all 4 optimizer methods.
     *
     * @param context The Android application context
     */
    OptimizerMethodOrder(Context context) {
        this.mContext = context;
        CharSequence[] entryValues = context.getResources().getStringArray(R.array.preferences_optimizer_search_method_entry_key_array);
        CharSequence[] entryTexts = context.getResources().getStringArray(R.array.preferences_optimizer_search_method_entry_text_array);
        for (int index = 0; index < entryValues.length; index++){
            OptimizerMethod method = new OptimizerMethod();
            method.mMethodCode = entryValues[index].charAt(0);
            method.mMethodText = entryTexts[index];
            this.add(method);
        }
    }

    /**
     * Sorts the list based on the passed value. The four optimizer methods are identified by a character.
     * An uppercase character indicates the method is enabled, lowercase is disabled
     *
     * @param value The 4 character string containing all methods
     */
    void setValue(CharSequence value) {
        if (value.length() != this.size()) {
            throw new AssertionError();
        }
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            for (int index2 = index; index2 < value.length(); index2++) {
                OptimizerMethod method = this.get(index2);
                if (method.mMethodCode == Character.toUpperCase(c)) {
                    method.mEnabled = Character.isUpperCase(c);
                    if (index != index2) {
                        Collections.swap(this, index, index2);
                    }
                    break;
                }
            }
        }
    }

    @NonNull
    String getValue() {
        StringBuilder builder = new StringBuilder();
        for (OptimizerMethod method : this) {
            if (method.mEnabled) {
                builder.append(method.mMethodCode);
            } else {
                builder.append(Character.toLowerCase(method.mMethodCode));
            }
        }
        return builder.toString();
    }

    /**
     * Get an user-friendly description of the current method order
     */
    @NonNull
    String getSummary(String defaultValue) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (OptimizerMethod method : this) {
            if (method.mEnabled) {
                if (first) {
                    builder.append(method.mMethodText);
                    first = false;
                } else {
                    builder.append(", ");
                    builder.append(method.mMethodText.toString().toLowerCase());
                }
            }
        }
        if (compareSetting(getValue(), defaultValue)) {
            builder.append(mContext.getString(R.string.preferences_optimizer_search_method_default_order_postfix_text));
        }
        return builder.toString();
    }

    static boolean compareSetting(String setting1, String setting2) {
        // Remove all lowercase characters before checking equality; their
        // order does not matter since they are disabled.
        setting1 = setting1.replaceAll("[a-z]", "");
        setting2 = setting2.replaceAll("[a-z]", "");
        return setting1.equals(setting2);
    }

    void moveUp(int index) {
        Collections.swap(this, index - 1, index);
    }
    void moveDown(int index) {
        Collections.swap(this, index, index + 1);
    }

    private boolean isEnabled(char code){
        for (OptimizerMethod method : this) {
            if (method.mMethodCode == code) {
                return method.mEnabled;
            }
        }
        throw new AssertionError();
    }

    boolean isPermutationsEnabled() {
        return isEnabled('P');
    }

    boolean isRearrangementEnabled() {
        return isEnabled('R');
    }

    boolean isVicinitySearchEnabled() {
        return isEnabled('V');
    }

    boolean isGlobalSearchEnabled() {
        return isEnabled('G');
    }

    /**
     * Returns the index as used in the Optimizer.MethodOrder array in YASS.pas
     * @param code The optimizer method character code
     * @return The index of this method
     */
    private int getOrder(char code) {
        for (int index = 0; index < this.size(); index++) {
            OptimizerMethod method = this.get(index);
            if (method.mMethodCode == code) {
                // Index found! return +1 because the first is reserved for the fallback strategy (which is disabled)
                return index + 1;
            }
        }
        throw new AssertionError();
    }

    int getPermutationsOrder() {
        return getOrder('P');
    }

    int getRearrangementOrder() {
        return getOrder('R');
    }

    int getVicinitySearchOrder() {
        return getOrder('V');
    }

    int getGlobalSearchOrder() {
        return getOrder('G');
    }
}
