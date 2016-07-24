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

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import triiuark.android.app.trimote.Log;

@UsedViaReflection
public class UdpAction extends NetAction
{

	private class DatagramPacketSender
			extends AsyncTask<DatagramPacket, Integer, Object>
	{
		@Override
		protected Object doInBackground(final DatagramPacket... params)
		{
			if (sSocket == null) {
				try {
					sSocket = new DatagramSocket();
				} catch (final SocketException e) {
					Log.e("socket is null" + mPath, e);
					return null;
				}
			}

			try {
				sSocket.send(params[0]);
				Log.d("send" + mPath);
			} catch (final IOException e) {
				Log.e("send failed " + mPath, e);
			}

			return null;
		}

	}

	private static DatagramSocket sSocket;

	DatagramPacket mPacket;

	void createDatagram()
	{
		final InetAddress address = getAddress(mAddress);
		if (address == null) {
			mPacket = null;
			return;
		}
		final String type = "bytes";
		byte[]       data;
		if (type.equals("bytes")) {
			final String[] b = mValue.split(":");
			data = new byte[b.length];
			for (int ii = 0; ii < b.length; ++ii) {
				data[ii] = (byte) Integer.parseInt(b[ii], 16);
			}
		} else {
			data = mValue.getBytes();
		}

		mPacket = new DatagramPacket(data, data.length, address, mPort);
	}

	@Override
	public boolean invoke()
	{
		boolean result = false;

		if (mPacket == null) {
			createDatagram();
			if (mPacket == null) {
				Log.e("packet is null" + mPath);
				return false;
			}
		}

		if (super.invoke()) {
			AsyncTask task = new DatagramPacketSender().execute(mPacket);
			if (task.isCancelled()) {
				Log.e("task canceled" + mPath);
			} else {
				result = true;
			}
		}
		return result;
	}
}
