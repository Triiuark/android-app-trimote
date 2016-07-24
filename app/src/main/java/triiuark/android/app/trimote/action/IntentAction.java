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

package triiuark.android.app.trimote.action;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.lang.reflect.Field;
import java.util.Map;

import triiuark.android.app.trimote.Log;

@UsedViaReflection
public class IntentAction extends Action
{
	private Context mContext;
	private String  mIntent;
	private String  mData;
	private String  mType;
	private String  mPackage;

	@Override
	public void setArgs(final Context context, String path, final Map<String, String> args)
			throws ActionConfigurationException
	{
		super.setArgs(context, path, args);

		mContext = context;
		mIntent = args.get("value");
		mData = args.get("address");
		mType = args.get("type");
		mPackage = args.get("package");

		final Object mExtraInt = args.get("extraInt");

		Log.d(mExtraInt.toString());
	}

	@Override
	public boolean invoke()
	{
		boolean result = false;

		if (super.invoke()) {
			if (mIntent == null || mIntent.isEmpty()) {
				mIntent = "DEFAULT";
			}
			try {
				final Field f = Intent.class.getField("ACTION_" + mIntent.toUpperCase());
				final Intent intent = new Intent((String) f.get(null));

				if (mData != null && !mData.isEmpty()) {
					final Uri uri = Uri.parse(mData);
					if (mType != null && !mType.isEmpty()) {
						intent.setDataAndType(uri, mType);
					} else {
						intent.setData(uri);
					}
				}

				if (mPackage != null && !mPackage.isEmpty()) {
					intent.setPackage(mPackage);
				}

				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				intent.putExtra("position", 0);
				mContext.startActivity(intent);
				result = true;
			} catch (final ActivityNotFoundException e) {
				Log.se(e.getMessage() + mPath, e);
			} catch (final NoSuchFieldException e) {
				Log.se(e.getMessage() + mPath, e);
			} catch (final IllegalAccessException e) {
				Log.se(e.getMessage() + mPath, e);
			} catch (final IllegalArgumentException e) {
				Log.se(e.getMessage() + mPath, e);
			}
		}

		return result;
	}
}
