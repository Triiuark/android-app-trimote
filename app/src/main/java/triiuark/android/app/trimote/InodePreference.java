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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class InodePreference extends DialogPreference
{
	private static final int TYPE_FILE = 0x01;
	private static final  int TYPE_DIR  = TYPE_FILE << 0x01;

	private static final class InodeComparator implements Comparator<File>
	{
		@Override
		public final int compare(final File a, final File b)
		{
			final boolean aIsDir = a.isDirectory();
			final boolean bIsDir = b.isDirectory();
			if (aIsDir == bIsDir) {
				return a.getName().compareTo(b.getName());
			} else if (aIsDir) {
				return -1;
			}
			return 1;
		}
	}

	private final class InodeAdapter extends ArrayAdapter<String>
	{
		private String mPath;

		public InodeAdapter(final Context context, @SuppressWarnings("SameParameterValue") final int resourceId)
		{
			super(context, resourceId);

			mPath = mInode;
			if (mPath == null || mPath.isEmpty()) {
				mPath = Environment
						.getExternalStorageDirectory()
						.getAbsolutePath();
			}

			File inode = new File(mPath);
			if (!inode.isDirectory()
				|| !inode.canRead()
				|| !inode.canExecute()) {
				inode = inode.getParentFile();
				if (inode == null) {
					mPath = "/";
				} else {
					try {
						mPath = inode.getCanonicalPath();
					} catch (final IOException e) {
						mPath = inode.getAbsolutePath();
					}
				}
			}

			readDir("");
		}

		private File readDir(final String name)
		{
			final File inode = new File(mPath + name);
			if (!inode.exists()) {
				return null;
			}

			if (!inode.isDirectory()
				|| !inode.canRead()
				|| !inode.canExecute()) {
				return inode;
			}

			try {
				mPath = inode.getCanonicalPath();
			} catch (final IOException e) {
				mPath = inode.getAbsolutePath();
			}

			clear();

			if (!mPath.equals("/")) {
				if (!mPath.isEmpty()) {
					add("..");
				}
				mPath += "/";
			}

			final File[] inodes = inode.listFiles();
			if (inodes != null) {
				Arrays.sort(inodes, LC_COLLATE_C_ORDER);
				for (final File current : inodes) {
					if (current.isDirectory()) {
						if (!current.canExecute() || !current.canRead()) {
							continue;
						}
						add(current.getName() + "/");
					} else if (current.canRead() && (mAttrType & TYPE_FILE) == TYPE_FILE
							   && (!mAttrNeedsWrite || current.canWrite())) {
						add(current.getName());
					}
				}
			}

			return inode;
		}

		public String getInode(final int position)
		{
			String res = null;

			final String current = getItem(position);
			if (current != null) {
				final File inode = readDir(current);
				if (inode != null
					&& (!mAttrNeedsWrite || inode.canWrite())) {
					if (inode.isDirectory()) {
						if ((mAttrType & TYPE_DIR) == TYPE_DIR) {
							res = mPath;
						}
					} else if ((mAttrType & TYPE_FILE) == TYPE_FILE) {
						res = mPath + current;
					}
				}
			}

			return res;
		}
	}

	private static final InodeComparator LC_COLLATE_C_ORDER =
			new InodeComparator();

	private final Context  mContext;
	private       TextView mCurrent;
	private       String   mInode;
	private       int      mAttrType;
	private       boolean  mAttrNeedsWrite;

	public InodePreference(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);

		mContext = context;
		mAttrNeedsWrite = false;
		mAttrType = TYPE_FILE | TYPE_DIR;

		final TypedArray a =
				context.obtainStyledAttributes(
						attrs,
						R.styleable.InodePreference);

		int       index;
		final int count = a.getIndexCount();
		for (int ii = 0; ii < count; ++ii) {
			index = a.getIndex(ii);
			switch (index) {
				case R.styleable.InodePreference_needsWrite:
					mAttrNeedsWrite = a.getBoolean(index, false);
					break;
				case R.styleable.InodePreference_type:
					mAttrType = a.getInt(index, 0);
					break;
			}
		}

		a.recycle();
	}

	private void setInode(final String inode)
	{
		final boolean wasBlocking = shouldDisableDependents();

		mInode = inode;

		persistString(mInode);

		final boolean isBlocking = shouldDisableDependents();
		if (isBlocking != wasBlocking) {
			notifyDependencyChange(isBlocking);
		}
	}

	@Override
	public CharSequence getSummary()
	{
		CharSequence sum;

		if (mInode == null || mInode.isEmpty()) {
			sum = super.getSummary();
		} else {
			sum = mInode;
		}

		return sum;
	}

	@Override
	protected void onSetInitialValue(
			final boolean restoreValue,
			final Object defaultValue)
	{
		final String s;

		if (restoreValue) {
			s = getPersistedString(mInode);
		} else {
			s = (String) defaultValue;
		}

		setInode(s);
	}

	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index)
	{
		return a.getString(index);
	}

	@Override
	public View onCreateDialogView()
	{
		setDialogLayoutResource(R.layout.inode_preference);

		final View view = super.onCreateDialogView();

		mCurrent = (TextView) view.findViewById(R.id.inode_preference_selected);
		mCurrent.setText(mInode);

		final ListView listView =
				(ListView) view.findViewById(R.id.inode_preference_list);

		listView.setAdapter(
				new InodeAdapter(
						mContext,
						R.layout.inode_preference_list_item));

		listView.setOnItemClickListener(
				new AdapterView.OnItemClickListener()
				{
					@Override
					public void onItemClick(
							final AdapterView<?> parent,
							final View view,
							final int position,
							final long id)
					{
						mInode = ((InodeAdapter) parent.getAdapter())
								.getInode(position);
						if (mInode != null) {
							mCurrent.setText(mInode);
						}
					}
				});

		return view;
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult)
	{
		if (positiveResult) {
			setInode(mCurrent.getText().toString());
			notifyChanged();
		}
	}
}
