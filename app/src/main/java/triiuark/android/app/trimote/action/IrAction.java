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
import android.hardware.ConsumerIrManager;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import triiuark.android.app.trimote.Log;

@UsedViaReflection
public class IrAction extends Action
{
	private static final int TYPE_RC5    = 0;
	private static final int TYPE_RC6    = 1;
	private static final int TYPE_SHARP  = 2;
	private static final int TYPE_NEC    = 3;
	private static final int TYPE_SONY20 = 4;

	private static boolean           sInitialized = false;
	private static ConsumerIrManager sIrManager   = null;
	private static Object            sService     = null;
	private static Method            sWrite       = null;

	private Code mCode = null;

	/**
	 * @param type    Code type @see IrAction.TYPE_...
	 * @param address the address part
	 * @param code    the code part
	 * @return a String containing a comma separated list of integers, where the
	 * first is the frequency followed by pulse - space pairs
	 */
	private static String calcCode(
			final int type,
			final int address,
			final int code)
	{
		String res = "";
		String tmp = "";
		int    sum = 0;
		char   curr;
		char   last;

		switch (type) {
			case TYPE_RC5:
				res = "36000";
				tmp = "h";                                       // S1, but start with a half bit
				tmp += (0x01 & (code >> 6)) > 0 ? " hl" : " lh"; // S2 or !C7
				tmp += " hl";                                    // T

				for (int ii = 4; ii >= 0; --ii) {
					if ((0x01 & (address >> ii)) > 0) {
						tmp += " lh";
					} else {
						tmp += " hl";
					}
				}
				for (int ii = 5; ii >= 0; --ii) {
					if ((0x01 & (code >> ii)) > 0) {
						tmp += " lh";
					} else {
						tmp += " hl";
					}
				}

				tmp += " l"; // end with a half bit

//				Log.i("RC-5 (" + address + ", " + code + "): " + tmp + mPath);


				tmp = tmp.replaceAll("[^h,l]", "");
				last = tmp.charAt(0);

				for (int ii = 0; ii < tmp.length(); ++ii) {
					if ((curr = tmp.charAt(ii)) == last) {
						++sum;
					} else {
						res += "," + (sum * 32);
						sum = 1;
					}

					last = curr;
				}
				res += "," + (sum * 32);

				Log.i("RC-5 (" + address + ", " + code + "): " + res);

				break;
			case TYPE_RC6:
				res = "36000";
				///    LEADER___ S_ M2 M1 M0 T___
				tmp = "hhhhhh ll hl lh lh lh llhh";
				for (int ii = 7; ii >= 0; --ii) {
					if ((0x01 & (address >> ii)) > 0) {
						tmp += " hl";
					} else {
						tmp += " lh";
					}
				}
				for (int ii = 7; ii >= 0; --ii) {
					if ((0x01 & (code >> ii)) > 0) {
						tmp += " hl";
					} else {
						tmp += " lh";
					}
				}
				tmp += " llllll"; /// signal free time

				Log.i("RC-6 (" + address + ", " + code + "): " + tmp);


				tmp = tmp.replaceAll("[^h,l]", "");
				last = tmp.charAt(0);

				for (int ii = 0; ii < tmp.length(); ++ii) {
					if ((curr = tmp.charAt(ii)) == last) {
						++sum;
					} else {
						res += "," + (sum * 16);
						sum = 1;
					}

					last = curr;
				}
				res += "," + (sum * 16);

				Log.i("RC-6 (" + address + ", " + code + "): " + res);

				break;
			case TYPE_SHARP:
				res = "38028";
				for (int ii = 0; ii < 5; ++ii) {
					if ((0x01 & (address >> ii)) > 0) {
						res += ",12,63";
						tmp += ",12,63";
					} else {
						res += ",12,26";
						tmp += ",12,26";
					}
				}
				for (int ii = 0; ii < 8; ++ii) {
					if ((0x01 & (code >> ii)) > 0) {
						res += ",12,63";
						tmp += ",12,26";
					} else {
						res += ",12,26";
						tmp += ",12,63";
					}
				}
				res += ",12,26,12,26"; /// C14 + C15 are always 0!?

				/// 40ms pause + address + inverted code + inverted C14, C15
				res += ",12,1520" + tmp + ",12,63,12,63";

				/// append end
				res += ",12,26";

				Log.i("SHARP (" + address + ", " + code + "): " + res);


				break;
			case TYPE_NEC:
				///    FREQ- BURST PAUSE
				res = "38028,340,170";
				for (int ii = 0; ii < 8; ++ii) {
					if ((0x01 & (address >> ii)) > 0) {
						res += ",21,63";
						tmp += ",21,21";
					} else {
						res += ",21,21";
						tmp += ",21,63";
					}
				}
				res += tmp;
				tmp = "";
				for (int ii = 0; ii < 8; ++ii) {
					if ((0x01 & (code >> ii)) > 0) {
						res += ",21,63";
						tmp += ",21,21";
					} else {
						res += ",21,21";
						tmp += ",21,63";
					}
				}
				res += tmp + ",21,21"; /// orig has to be about 1554

				Log.i("NEC (" + address + ", " + code + "): " + res);

				break;
			case TYPE_SONY20:
				res += "40244,96,24";
				for (int ii = 0; ii < 7; ++ii) {
					if ((0x01 & (code >> ii)) > 0) {
						res += ",48,24";
					} else {
						res += ",24,24";
					}
				}
				for (int ii = 0; ii < 13; ++ii) {
					if ((0x01 & (address >> ii)) > 0) {
						res += ",48,24";
					} else {
						res += ",24,24";
					}
				}
				Log.i("SONY20 (" + address + ", " + code + "): " + res);
				break;
		}

		return res;
	}

	@Override
	public void setArgs(final Context context, String path, final Map<String, String> args)
			throws ActionConfigurationException
	{
		if (!sInitialized) {
			sInitialized = true;
			/// init service
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				sIrManager =
						(ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
				if (!sIrManager.hasIrEmitter()) {
					sIrManager = null;
				}
			}

			if (sIrManager == null) {
				//noinspection ResourceType
				sService = context.getSystemService("irda");
				if (sService != null) {
					final Class<?> c = sService.getClass();
					final Class<?> p[] = {String.class};

					try {
						sWrite = c.getMethod("write_irsend", p);
					} catch (final NoSuchMethodException e) {
						Log.se("No ir write method found" + mPath, e);
					}
				}
			}
		}

//		if (sIrManager == null && sWrite == null) {
//			throw new ActionConfigurationException("No infrared support");
//		}

		if (args == null) {
			throw new ActionConfigurationException("Missing attributes on IR action");
		}

		super.setArgs(context, path, args);

		mCode = null;

		final int type = getType(args.get("type"));

		int address;
		try {
			String s = args.get("address");
			if (s == null) {
				throw new ActionConfigurationException("IR Action: missing address");
			}
			address = Integer.parseInt(s);
		} catch (final NumberFormatException e) {
			throw new ActionConfigurationException("IR Action: address is not an integer");
		}

		int    radix = 10;
		String value = args.get("value");
		if (value == null) {
			throw new ActionConfigurationException("IR Action: missing value");
		}
		if (value.startsWith("0x")) {
			value = value.substring(2);
			radix = 16;
		}
		try {
			mCode = new Code(calcCode(type, address, Integer.parseInt(value, radix)));
		} catch (final NumberFormatException e) {
			throw new ActionConfigurationException("IR Action: value ist not an integer");
		}
	}

	private int getType(String type) throws ActionConfigurationException
	{
		if (type == null) {
			throw new ActionConfigurationException("IR Action: missing type");
		}

		int result;
		if (type.equalsIgnoreCase("rc5")) {
			result = TYPE_RC5;
		} else if (type.equalsIgnoreCase("rc6")) {
			result = TYPE_RC6;
		} else if (type.equalsIgnoreCase("sharp")) {
			result = TYPE_SHARP;
		} else if (type.equalsIgnoreCase("nec")) {
			result = TYPE_NEC;
		} else if (type.equalsIgnoreCase("sony20")) {
			result = TYPE_SONY20;
		} else {
			throw new ActionConfigurationException(
					"IR Action: unknown type: " + type);
		}
		return result;
	}

	@Override
	public boolean invoke()
	{
		boolean result = false;

		Log.i("Args: " + mDebug + mPath);

		if (super.invoke()) {
			if (mCode != null) {
				long now = System.currentTimeMillis();

				if (sIrManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					try {
						sIrManager.transmit(mCode.getFrequency(), mCode.getPattern());
						result = true;
					} catch (Exception e) {
						Log.se(e.getMessage() + mPath, e);
					}
				} else if (sWrite != null) {
					try {
						sWrite.invoke(sService, mCode.getString());
						result = true;
					} catch (IllegalAccessException e) {
						Log.se(e.getMessage() + mPath, e);
					} catch (InvocationTargetException e) {
						Log.se(e.getMessage() + mPath, e);
					}
				} else {
					Log.se("No ir service found" + mPath);
				}

				Log.i("Code: " + mCode.getString() + mPath);
				Log.i("      Length: " + (mCode.getLength() / 1000.0f) + "ms" + mPath);
				Log.i("      Time:   " + (System.currentTimeMillis() - now) + "ms" + mPath);
			} else {
				Log.w("Code: null" + mPath);
			}
		}

		return result;
	}

	private class Code
	{
		private String mCode      = null;
		private int[]  mPattern   = null;
		private int    mFrequency = Integer.MIN_VALUE;
		private int    mLength    = Integer.MIN_VALUE;

		public Code(String code)
		{
			mCode = code;
		}

		public String getString()
		{
			return mCode;
		}

		public int getFrequency()
		{
			if (mFrequency == Integer.MIN_VALUE) {
				convertCode();
			}

			return mFrequency;
		}

		public int[] getPattern()
		{
			if (mPattern == null) {
				convertCode();
			}

			return mPattern;
		}

		public int getLength()
		{
			if (mLength == Integer.MIN_VALUE) {
				convertCode();
			}

			return mLength;
		}

		private void convertCode()
		{
			String[] values = mCode.split(",");
			mFrequency = Integer.parseInt(values[0]);

			float period = (1000000.0f / mFrequency); // in µs
			mLength = 0;
			mPattern = new int[values.length - 1];
			for (int ii = 1; ii < mPattern.length + 1; ++ii) {
				mPattern[ii - 1] = Math.round(period * Integer.parseInt(values[ii]));
				mLength += mPattern[ii - 1];
			}
		}
	}
}
