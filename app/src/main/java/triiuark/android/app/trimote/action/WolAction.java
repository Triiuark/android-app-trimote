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
import java.util.Arrays;
import java.util.Map;

@UsedViaReflection
public class WolAction extends UdpAction
{
	private byte[] mEtherAddress;

	@Override
	public void setArgs(final Context context, String path, final Map<String, String> args)
			throws ActionConfigurationException
	{
		if (!args.containsKey("address")) {
			args.put("address", "255.255.255.255:1");
		}

		super.setArgs(context, path, args);

		if (mValue != null && !mValue.isEmpty()) {
			try {
				final String[] bytes = mValue.split(":");
				if (bytes.length != 6) {
					throw new ActionConfigurationException(
							"length of mac address is not 6 bytes");
				}
				mEtherAddress = new byte[6];
				for (int ii = 0; ii < mEtherAddress.length; ++ii) {
					mEtherAddress[ii] = (byte) Integer.parseInt(bytes[ii], 16);
				}
			} catch (final NumberFormatException e) {
				throw new ActionConfigurationException(
						"address value is not a number");
			}
		} else {
			throw new ActionConfigurationException(
					"address is not a valid mac address");
		}
	}

	@Override
	protected void createDatagram()
	{
		final InetAddress address = getAddress(mAddress);

		if (address == null) {
			return;
		}

		final byte[] wakeup = new byte[6 + 16 * mEtherAddress.length];


		Arrays.fill(wakeup, 0, 6, (byte) 0xff);

		for (int ii = 6; ii < wakeup.length; ii += mEtherAddress.length) {
			System.arraycopy(mEtherAddress, 0, wakeup, ii,
					mEtherAddress.length);
		}

		mPacket = new DatagramPacket(wakeup, wakeup.length, address, mPort);
	}

}
