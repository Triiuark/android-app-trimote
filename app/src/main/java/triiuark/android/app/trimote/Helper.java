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

public final class Helper
{
	public static final String XMLNS_ANDROID =
			"http://schemas.android.com/apk/res/android";

	public static int dp2px(final Context context, final float dp)
	{
		final float scale = context.getResources().getDisplayMetrics().density;

		return (int) (dp * scale + 0.5f);
	}
}
