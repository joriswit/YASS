package yass;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ListView;

import net.sourceforge.sokobanyasc.joriswit.yass.R;

public class OptimizerMethodPreference extends DialogPreference {

    private Context mContext;
    private OptimizerMethodOrder mOrder;
    private String mDefaultValue;
    private String mValue;

    public OptimizerMethodPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        mOrder = new OptimizerMethodOrder(mContext);
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        if (restoreValue) {
            this.mValue = this.getPersistedString("PRVg");
        } else if (defaultValue instanceof String) {
            this.mValue = (String)defaultValue;
        } else {
            this.mValue = "PRVg";
        }
        this.mOrder.setValue(this.mValue);
        refreshSummaryText();
    }

    private void refreshSummaryText() {
        this.mOrder.setValue(this.mValue);
        this.setSummary(this.mOrder.getSummary(mDefaultValue));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        mDefaultValue = a.getString(index);
        return mDefaultValue;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        this.mOrder.setValue(this.mValue);

        ListView listView = view.findViewById(R.id.list);
        OptimizerMethodListPreferenceAdapter customListPreferenceAdapter = new OptimizerMethodListPreferenceAdapter(mContext);
        listView.setAdapter(customListPreferenceAdapter);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mValue = mOrder.getValue();
            persistString(mValue);
            refreshSummaryText();
        }
    }
    
    private class OptimizerMethodListPreferenceAdapter extends ArrayAdapter<OptimizerMethodOrder.OptimizerMethod>
    {
    	OptimizerMethodListPreferenceAdapter(Context context)
        {
            super(context, R.layout.preference_optimizer_method_item, android.R.id.text1, mOrder);
        }

        public int getCount()
        {
            return mOrder.size();
        }

        @NonNull
        public OptimizerMethodOrder.OptimizerMethod getItem(int position)
        {
            return mOrder.get(position);
        }

        public long getItemId(int position)
        {
            return position;
        }

        @NonNull
        public View getView(final int position, View convertView, @NonNull ViewGroup parent)
        {
        	View v = convertView;
            if (v == null) {
            	LayoutInflater vi = (LayoutInflater) OptimizerMethodPreference.this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.preference_optimizer_method_item, parent, false);
            }
            
            final CheckedTextView tv = v.findViewById(android.R.id.text1);

            final OptimizerMethodOrder.OptimizerMethod method = getItem(position);

            tv.setText(method.getMethodText());
            tv.setChecked(method.getEnabled());
            tv.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          boolean checked = tv.isChecked();
                                          method.setEnabled(!checked);
                                          tv.setChecked(!checked);
                                      }
                                  });

            ImageButton up = v.findViewById(R.id.up);

            up.setEnabled(position > 0);
            up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOrder.moveUp(position);
                    notifyDataSetChanged();
                }
            });

            ImageButton down = v.findViewById(R.id.down);

            down.setEnabled(position < 3);
            down.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOrder.moveDown(position);
                    notifyDataSetChanged();
                }
            });

            return v;
        }
    }
}
