/*
 * This file is part of Trimote.
 *
 * Copyright (c) 2013-2016 René Bählkow <triiuark@projekt182.de>
 *
 * Trimote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Trimote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Trimote. If not, see <http://www.gnu.org/licenses/>.
 */

package triiuark.android.app.trimote;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import triiuark.android.app.trimote.Helper;

public final class SeekBarPreference
		extends DialogPreference
		implements OnSeekBarChangeListener, OnClickListener
{

	private final Context mContext;

	private boolean  mIsChecked;
	private int      mValue;
	private SeekBar  mSeekBar;
	private CheckBox mDefaultButton;
	private TextView mValueView;

	private boolean mAttrWithDefault;
	private int     mAttrMin;
	private int     mAttrMax;
	private int     mAttrDefault;
	private String  mAttrDefaultString;
	private String  mAttrPrefix;
	private String  mAttrSuffix;

	public SeekBarPreference(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);

		mAttrWithDefault = true;
		mAttrMin = 0;
		mAttrMax = 100;
		mAttrDefault = 0;
		mAttrDefaultString = "";
		mAttrPrefix = "";
		mAttrSuffix = "";

		mIsChecked = false;
		mValue = 0;
		mContext = context;
		mSeekBar = null;
		mDefaultButton = null;
		mValueView = null;


		final String s = attrs.getAttributeValue(
				Helper.XMLNS_ANDROID,
				"defaultValue");
		if (s != null) {
			try {
				mAttrDefault = Integer.parseInt(s);
			} catch (final NumberFormatException e) {
				mAttrDefault = 0;
			}
		}

		final TypedArray a = context.obtainStyledAttributes(
				attrs,
				R.styleable.SeekBarPreference);

		int       index;
		final int count = a.getIndexCount();
		for (int ii = 0; ii < count; ++ii) {
			index = a.getIndex(ii);
			switch (index) {
				case R.styleable.SeekBarPreference_withDefault:
					mAttrWithDefault = a.getBoolean(index, true);
					break;
				case R.styleable.SeekBarPreference_minValue:
					mAttrMin = a.getInt(index, 0);
					break;
				case R.styleable.SeekBarPreference_maxValue:
					mAttrMax = a.getInt(index, 100);
					break;
				case R.styleable.SeekBarPreference_defaultString:
					mAttrDefaultString = a.getString(index);
					break;
				case R.styleable.SeekBarPreference_prefix:
					mAttrPrefix = a.getString(index);
					break;
				case R.styleable.SeekBarPreference_suffix:
					mAttrSuffix = a.getString(index);
					break;
			}
		}

		if (mAttrDefaultString.isEmpty()) {
			mAttrDefaultString = "" + mAttrDefault;
			if (!mAttrSuffix.isEmpty()) {
				mAttrDefaultString += mAttrSuffix;
			}
		}

		a.recycle();
	}

	private void setValueLabel()
	{
		if (mValueView != null) {
			mValueView.setText(getSummary());
		}
	}

	@Override
	public CharSequence getSummary()
	{
		CharSequence summary;
		if (mValue == mAttrDefault) {
			summary = mAttrPrefix + mAttrDefaultString;
		} else {
			summary = mAttrPrefix + mValue + mAttrSuffix;
		}

		return summary;
	}

	@Override
	protected void onSetInitialValue(
			final boolean restoreValue,
			final Object defaultValue)
	{
		if (restoreValue) {
			mValue = getPersistedInt(mValue);
		} else {
			mValue = (Integer) defaultValue;
		}

		setValueLabel();
	}

	@Override
	protected View onCreateDialogView()
	{
		// TODO remove hardcoded margins
		final int vMargin = Helper.dp2px(mContext, 16);
		final int hMargin = Helper.dp2px(mContext, 5);

		final LinearLayout baseLayout = new LinearLayout(mContext);
		baseLayout.setOrientation(LinearLayout.VERTICAL);
		baseLayout.setPadding(vMargin, hMargin, vMargin, hMargin);

		mValueView = new TextView(mContext);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			mValueView.setTextAppearance(android.R.style.TextAppearance_Medium);
		} else {
			//noinspection deprecation
			mValueView.setTextAppearance(mContext,
					android.R.style.TextAppearance_Medium);
		}
		mValueView.setGravity(Gravity.END);
		mValueView.setText(getSummary());
		mValueView.setPadding(vMargin, hMargin, vMargin, hMargin);
		baseLayout.addView(mValueView);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setMax(mAttrMax - mAttrMin);
		mSeekBar.setProgress(mValue - mAttrMin);
		mSeekBar.setOnSeekBarChangeListener(this);
		baseLayout.addView(mSeekBar);

		if (mAttrWithDefault) {
			final LayoutParams p = new LayoutParams(
					LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT,
					1);
			final LinearLayout buttonLayout = new LinearLayout(mContext);
			buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
			buttonLayout.setOnClickListener(this);
			buttonLayout.setPadding(vMargin, hMargin, vMargin, hMargin);
			baseLayout.addView(buttonLayout);

			final TextView buttonLabel = new TextView(mContext);
			buttonLabel.setText(mAttrDefaultString);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				buttonLabel.setTextAppearance(android.R.style.TextAppearance_Medium);
			} else {
				//noinspection deprecation
				buttonLabel.setTextAppearance(mContext,
						android.R.style.TextAppearance_Medium);
			}
			buttonLayout.addView(buttonLabel);
			buttonLabel.setLayoutParams(p);

			mDefaultButton = new CheckBox(mContext);
			mDefaultButton.setClickable(false);
			mDefaultButton.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
			buttonLayout.addView(mDefaultButton);
		}

		return baseLayout;
	}

	@Override
	protected void onPrepareDialogBuilder(final AlertDialog.Builder builder)
	{
		if (mAttrWithDefault && mValue == mAttrDefault) {
			if (mAttrDefault < mAttrMin || mAttrDefault > mAttrMax) {
				mSeekBar.setProgress(0);
			}
			mIsChecked = false;
			onClick(null);
		}
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult)
	{
		if (positiveResult) {
			if (getPersistedInt(mAttrDefault) != mValue) {
				persistInt(mValue);
				notifyChanged();
			}
		}
	}

	@Override
	public void onClick(final View view)
	{
		mIsChecked = !mIsChecked;
		mDefaultButton.setChecked(mIsChecked);
		mSeekBar.setEnabled(!mIsChecked);
		if (mIsChecked) {
			mValue = mAttrDefault;
		} else {
			mValue = mSeekBar.getProgress() + mAttrMin;
		}
		setValueLabel();
	}

	@Override
	public void onProgressChanged(
			final SeekBar sb,
			final int value,
			final boolean fromUser)
	{
		if (fromUser) {
			mValue = value + mAttrMin;
			setValueLabel();
		}
	}

	@Override
	public void onStartTrackingTouch(final SeekBar seekBar)
	{
		/// nothing to do here
	}

	@Override
	public void onStopTrackingTouch(final SeekBar seekBar)
	{
		/// nothing to do here
	}
}
