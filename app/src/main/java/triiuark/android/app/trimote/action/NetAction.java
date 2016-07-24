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
import android.os.AsyncTask;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import triiuark.android.app.trimote.Log;

public abstract class NetAction extends Action
{
	private static Map<String, InetAddress> sAddresses;

	String mValue;
	String mAddress;
	int    mPort;

	private static class AddressResolver extends AsyncTask<String, Integer, InetAddress>
	{
		@Override
		protected InetAddress doInBackground(final String... args)
		{
			InetAddress res = null;
			try {
				res = InetAddress.getByName(args[0]);
				if (sAddresses.put(args[0], res) == null) {
					/// args[0] was not in sAddresses
					Log.d("added address: " + res.getCanonicalHostName() + "/" + sAddresses.size());
				}
			} catch (final UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return res;
		}
	}

	@Override
	public void setArgs(final Context context, String path, final Map<String, String> args)
			throws ActionConfigurationException
	{
		super.setArgs(context, path, args);

		mPort = -1;
		mValue = args.get("value");
		mAddress = args.get("address");

		if (mValue != null && mAddress != null && !mValue.isEmpty() && !mAddress.isEmpty()) {
			final Pattern p = Pattern.compile("^(.*):([0-9]{1,5})$");
			final Matcher m = p.matcher(mAddress);
			if (m.find()) {
				mAddress = m.group(1);
				/*  NOTE empty address means localhost
				 *       this can someone use to do stuff on his/her android device
				 */

				mPort = Integer.parseInt(m.group(2));
				if (mPort > 0xffff) {
					/// NOTE mPort < 0 can't happen - regexp
					throw new ActionConfigurationException(
							"Config error: invalid port: " + mPort);
				}

				/// TODO check also mAddress with a regexp or so
				addAddress(mAddress);
			} else {
				throw new ActionConfigurationException(
						"Config error: address and/or port not properly set: " +
								mAddress);
			}
		} else {
			throw new ActionConfigurationException("Config error: address and/or value not set");
		}
	}

	private void addAddress(final String address)
	{
		if (sAddresses == null) {
			sAddresses = new HashMap<String, InetAddress>();
		}

		if (sAddresses.containsKey(address)) {
			return;
		}

		new AddressResolver().execute(address);
	}

	InetAddress getAddress(final String address)
	{
		if (!sAddresses.containsKey(address)) {
			/// runs in another thread so I can't return the address
			/// at this moment
			addAddress(address);

			return null;
		}

		return sAddresses.get(address);
	}
}
