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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Map;

@UsedViaReflection
public final class LircAction extends UdpAction
{
	private byte[] mData;

	@Override
	public void setArgs(final Context context, String path, final Map<String, String> args)
			throws ActionConfigurationException
	{
		super.setArgs(context, path, args);

		int code;

		try {
			int radix = 10;
			String value = mValue;

			if (value.startsWith("0x")) {
				value = value.substring(2);
				radix = 16;
			}

			code = Integer.parseInt(value, radix);
		} catch (final NumberFormatException e) {
			throw new ActionConfigurationException(
					"You have an error: value has to be an integer: " + mValue);
		}

		mData = createCode(code);
	}

	@Override
	protected void createDatagram()
	{
		final InetAddress address = getAddress(mAddress);
		if (address == null) {
			mPacket = null;
			return;
		}
		mPacket = new DatagramPacket(mData, mData.length, address, mPort);
	}

	private static byte[] createCode(final int code)
	{
		final byte[] res = new byte[64];

		for (int ii = 0; ii < 61; ii += 4) {
			res[ii] = (byte) 0x01;
			res[ii + 1] = (byte) 0x00;
			res[ii + 2] = (byte) 0x01;
			res[ii + 3] = (byte) 0x80;
		}

		for (int ii = 0x0001, jj = 60; ii <= 0xffff; ii <<= 0x01, jj -= 4) {
			if ((code & ii) > 0) {
				res[jj] = 0x02;
			}
		}

		res[62] = (byte) 0x02;
		res[63] = (byte) 0x80;

		return res;
	}

//	public String getDebugString()
//	{
//		String debug = super.getDebugString();
//		return debug;
//	}
}
