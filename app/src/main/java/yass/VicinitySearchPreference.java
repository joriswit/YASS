package yass;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import net.sourceforge.sokobanyasc.joriswit.yass.R;

public class VicinitySearchPreference extends DialogPreference {

    private int mDefaultValue;
    private int mValue;
    private String[] mValues;

    public VicinitySearchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mValues = new String[46];
        int index = 0;

        for (int i = 1; i < 10; i += 1) {
            mValues[index] = Integer.toString(i);
            index++;
        }
        for (int i = 10; i < 100; i += 5) {
            mValues[index] = Integer.toString(i);
            index++;
        }
        for (int i = 100; i <= 1000; i += 50) {
            mValues[index] = Integer.toString(i);
            index++;
        }
    }
    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        if(restoreValue) {
            this.mValue = this.getPersistedInt(-1);
        } else if(defaultValue instanceof Integer) {
            this.mValue = (Integer)defaultValue;
        }
        refreshSummaryText();
    }

    private void refreshSummaryText() {
        if(this.mValue == -1) {
            this.setSummary(R.string.preferences_optimizer_vicinity_search_summary_disabled_text);
        } else {
            this.setSummary(
                    getContext()
                        .getResources()
                        .getQuantityString(R.plurals.preferences_optimizer_vicinity_search_summary_enabled_text, this.mValue, this.mValue)
                );
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        mDefaultValue = a.getInteger(index, 0);
        return mDefaultValue;
    }

    private NumberPicker mPicker;
    private Switch mSwitch1;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mPicker = (NumberPicker)view.findViewById(R.id.numberPicker);
        mSwitch1 = (Switch)view.findViewById(R.id.switch1);
        TextView defaultValueTextView = (TextView)view.findViewById(R.id.default_value);
        mPicker.setMinValue(0);
        mPicker.setMaxValue(mValues.length - 1);
        mPicker.setDisplayedValues(mValues);
        mPicker.setWrapSelectorWheel(false);
        if(this.mValue == -1) {
            mPicker.setValue(findIndex(mDefaultValue));
            mSwitch1.setChecked(false);
        } else {
            mPicker.setValue(findIndex(this.mValue));
            mSwitch1.setChecked(true);
        }
        if (mDefaultValue != -1) {
            defaultValueTextView.setText(
                    String.format(getContext().getString(R.string.preferences_optimizer_vicinity_search_default_text), mDefaultValue)
            );
        } else {
            defaultValueTextView.setText(R.string.preferences_optimizer_vicinity_search_default_disabled_text);
        }
    }

    private int findIndex(int value) {
        for (int i = mValues.length - 1; i >= 0; i--) {
            if (Integer.parseInt(mValues[i]) <= value) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            if(mSwitch1.isChecked()) {
                mValue = Integer.parseInt(mValues[mPicker.getValue()]);
            } else {
                mValue = -1;
            }
            this.persistInt(mValue);
            refreshSummaryText();
        }
    }
}
