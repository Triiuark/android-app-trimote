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

import android.content.Context;

import java.util.Map;

import triiuark.android.app.trimote.Log;

public abstract class Action
{
	private int mWait;

	protected String mDebug;
	protected String mPath;

	public void setArgs(final Context context, String path, final Map<String, String> args)
			throws ActionConfigurationException
	{
		mDebug = "";
		mPath = path;
		mWait = 0;

		if (args != null) {
			for (Map.Entry<String, String> arg : args.entrySet()) {
				mDebug += arg.getKey() + ": " + arg.getValue() + ", ";
			}
			if (mDebug.length() > 2) {
				mDebug = mDebug.substring(0, mDebug.length() - 2);
			}

			final String wait = args.get("wait");
			if (wait != null && !wait.isEmpty()) {
				try {
					mWait = Integer.parseInt(wait);
					if (mWait < 0) {
						throw new ActionConfigurationException(
								"wait value has to be > -1");
					}
				} catch (final NumberFormatException e) {
					throw new ActionConfigurationException(
							"wait value is not a number");
				}
			}
		}
	}

	public boolean invoke()
	{
		if (mWait > 0) {
			try {
				Thread.sleep(mWait);
			} catch (final InterruptedException e) {
				Log.w(e);
				return false;
			}
		}

		return true;
	}

	public String getDebugString()
	{
		return mDebug;
	}
}
