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

    public VicinitySearchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        mPicker.setMinValue(1);
        mPicker.setMaxValue(100);
        mPicker.setWrapSelectorWheel(false);
        if(this.mValue == -1) {
            mPicker.setValue(mDefaultValue);
            mSwitch1.setChecked(false);
        } else {
            mPicker.setValue(this.mValue);
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

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            if(mSwitch1.isChecked()) {
                mValue = mPicker.getValue();
            } else {
                mValue = -1;
            }
            this.persistInt(mValue);
            refreshSummaryText();
        }
    }
}
