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
import android.widget.TextView;

import static android.util.Log.ASSERT;
import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static android.util.Log.println;

public class Log
{
	private static final String LOG_PREFIX = "log.trimote";
	private static Context sContext;
	private static AlertDialog sAlertDialog;

	public static void setContext(Context context)
	{
		if (sContext == null) {
			sContext = context;
		}
	}

//	public static void v(String message)
//	{
//		log(VERBOSE, message, null, false);
//	}
//
//	public static void v(String message, Exception e)
//	{
//		log(VERBOSE, message, null, false);
//	}
//
//	public static void v(Exception e)
//	{
//		log(VERBOSE, null, e, false);
//	}

	public static void d(String message)
	{
		if(BuildConfig.IS_DEBUG) {
			log(DEBUG, message, null, false);
		}
	}

//	public static void d(String message, Exception e)
//	{
//		log(DEBUG, message, e, false);
//	}
//
//	public static void d(Exception e)
//	{
//		log(DEBUG, null, e, false);
//	}

	public static void i(String message)
	{
		//android.util.Log.i(LOG_PREFIX, message);
		if(BuildConfig.IS_DEBUG) {
			log(INFO, message, null, false);
		}
	}

	public static void w(String message)
	{
		if (BuildConfig.IS_DEBUG) {
			log(WARN, message, null, false);
		}
	}

	public static void w(Exception e)
	{
		if (BuildConfig.IS_DEBUG) {
			log(WARN, null, e, false);
		}
	}

	public static void e(String message)
	{
		log(ERROR, message, null, false);
	}

	public static void e(String message, Exception e)
	{
		log(ERROR, message, e, false);
	}

	public static void e(Exception e)
	{
		log(ERROR, null, e, false);
	}

	public static void se(String message)
	{
		log(ERROR, message, null, true);
	}

//	public static void se(Exception e)
//	{
//		log(ERROR, null, e, true);
//	}

	public static void se(String message, Exception e)
	{
		log(ERROR, message, e, true);
	}

	private static String getTraceString(int index)
	{
		String position = "";

		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		if (stackTrace.length > index) {
			position = "; " + stackTrace[index].getMethodName()
					+ " (" + stackTrace[index].getFileName() + ":"
					+ stackTrace[index].getLineNumber() + ")";
		}
		return position;
	}

	public static void log(int level, String message, Exception e, boolean showMessage)
	{
		String msg = "";
		if (message == null && e != null) {
			msg = e.getClass().getSimpleName() + ": " + e.getMessage();
		} else if (message != null && e != null) {
			msg = message + " (" + e.getMessage() + ")";
		} else if (message != null) {
			msg = message;
		}

		if (sContext != null && (showMessage || ((((MainActivity)sContext).showDebugInfo()
				&& (level == ERROR || level == ASSERT))))) {
			String m = message != null ? message : e != null ? e.getMessage() : msg;
			if (sAlertDialog == null) {
				sAlertDialog = new AlertDialog.Builder(sContext)
						.setTitle(android.R.string.dialog_alert_title)
						.setMessage(m)
						.setPositiveButton(android.R.string.ok, null)
						.setIcon(android.R.drawable.ic_dialog_alert).show();
				TextView textView = (TextView) sAlertDialog.findViewById(android.R.id.message);
				textView.setTextSize(10);
			} else {
				sAlertDialog.setMessage(m);
				sAlertDialog.show();
			}
		}

		if (BuildConfig.IS_DEBUG) {
			msg += getTraceString(5);

			println(level, LOG_PREFIX, msg);

			if (e != null) {
				e.printStackTrace();
			}
		}
	}
}
