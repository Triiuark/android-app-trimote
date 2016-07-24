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

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
//import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import triiuark.android.app.trimote.action.Action;

import static android.util.Log.ERROR;

public class MainActivity extends Activity
{
	private static final int ID_INTENT_REQUEST_SETTINGS = 0xf001;
	private static final int ID_NOTIFICATION            = 0x0f01;

	private LinearLayout              mLayout;
	private LinearLayout              mTabLayout;
	private LinearLayout.LayoutParams mRowParams;
	private LinearLayout.LayoutParams mTabButtonParams;
	private boolean                   mRestartRequired;
	private boolean                   mReloadRequired;
	private SharedPreferences         mSettings;

	private int mCurrentPage;
	private int mCurrentRow;
	private int mCurrentButton;
	private int mCurrentButtonAction;

	@SuppressWarnings("FieldCanBeLocal")
	private OnSharedPreferenceChangeListener mSettingsChangeListener;

	private String  mPrefRemoteFile;
	private int     mPrefTheme;
	private int     mPrefButtonTextSize;
	private int     mPrefTabTextSize;
	private int     mPrefLabelTextSize;
	private int     mPrefButtonHeight;
	private int     mPrefTabHeight;
	private int     mPrefVerticalMargin;
	private int     mPrefHorizontalMargin;
	private int     mPrefBackground;
	private boolean mPrefDebug;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.setContext(this);

		getSettings();
		setMainLayout();
		setPages(parseXml());
	}

	private void getSettings()
	{
		/// this forces reading of android:defaultValue attribute
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		if (mSettings == null) {
			mSettings = PreferenceManager.getDefaultSharedPreferences(this);

			mSettingsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
			{
				@Override
				public void onSharedPreferenceChanged(
						final SharedPreferences prefs,
						final String key)
				{
					if (key.equals("theme")) {
						setPrefTheme();
					} else if (key.equals("lock")) {
						setPrefLock();
					} else if (key.equals("sleep")) {
						setPrefSleep();
					} else if (key.equals("notification")) {
						setPrefNotification();
					} else if (key.equals("brightness")) {
						setPrefBrightness();
					} else {
						mReloadRequired = true;
					}
					Log.d("Setting changed: " + key);
				}
			};
			mSettings.registerOnSharedPreferenceChangeListener(mSettingsChangeListener);
		}

		setPrefTheme();
		setPrefBackground();
		setPrefLock();
		setPrefSleep();
		setPrefNotification();
		setPrefBrightness();

		try {
			mPrefRemoteFile = mSettings.getString("file", "");
		} catch (final ClassCastException e) {
			mPrefRemoteFile = "";
		}
		try {
			mPrefDebug = mSettings.getBoolean("debug", true);
		} catch (final ClassCastException e) {
			mPrefDebug = false;
		}
		try {
			mPrefButtonTextSize = mSettings.getInt("button_text_size", 0);
		} catch (final ClassCastException e) {
			mPrefButtonTextSize = 0;
		}
		try {
			mPrefTabTextSize = mSettings.getInt("tab_text_size", 0);
		} catch (final ClassCastException e) {
			mPrefTabTextSize = 0;
		}
		try {
			mPrefLabelTextSize = mSettings.getInt("label_text_size", 0);
		} catch (final ClassCastException e) {
			mPrefLabelTextSize = 0;
		}
		try {
			mPrefButtonHeight = Helper.dp2px(this, mSettings.getInt("button_height", 0));
		} catch (final ClassCastException e) {
			mPrefButtonHeight = 0;
		}
		try {
			mPrefTabHeight = Helper.dp2px(this, mSettings.getInt("tab_height", 0));
		} catch (final ClassCastException e) {
			mPrefTabHeight = 0;
		}
		try {
			mPrefVerticalMargin = Helper.dp2px(this, mSettings.getInt("vertical_margin", 0));
		} catch (final ClassCastException e) {
			mPrefVerticalMargin = 0;
		}
		try {
			mPrefHorizontalMargin = Helper.dp2px(this, mSettings.getInt("horizontal_margin", 0));
		} catch (final ClassCastException e) {
			mPrefHorizontalMargin = 0;
		}
	}

	private void setMainLayout()
	{
		LinearLayout layout = new LinearLayout(this); /// root view
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT,
				1f);
		p.setMargins(0, 0, 0, 0);

		mTabButtonParams = new LinearLayout.LayoutParams(
				0,
				mPrefTabHeight > 0 ? mPrefTabHeight : LayoutParams.MATCH_PARENT,
				1f);
		mTabButtonParams.gravity = Gravity.CENTER_VERTICAL;

		mRowParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				0,
				1f);

		mLayout = new LinearLayout(this);
		mLayout.setLayoutParams(p);
		mLayout.setOrientation(LinearLayout.VERTICAL);
		mLayout.setGravity(Gravity.BOTTOM);

		layout.addView(mLayout);

		mTabLayout = new LinearLayout(this);
		mTabLayout.setOrientation(LinearLayout.HORIZONTAL);
		mTabLayout.setLayoutParams(mRowParams);

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		layout.addView(mTabLayout, lp);

		Button menuButton = new Button(this);
		menuButton.setText("≡");
		menuButton.setLayoutParams(mTabButtonParams);
		if (mPrefTabTextSize > 0) {
			menuButton.setTextSize(mPrefTabTextSize);
		}
		menuButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				//openOptionsMenu();
				showOptions();
			}
		});

		mTabLayout.addView(menuButton);

		setContentView(layout);
	}

	private void setPages(final ArrayList<LinearLayout> pages)
	{
		if (pages != null && pages.size() > 0) {
			LinearLayout page = pages.get(0);
			mLayout.addView(page);

			if (pages.size() > 1) {
				Button button;
				final ToggleButton tabButtons[] =
						new ToggleButton[pages.size()];
				for (int ii = 0; ii < pages.size(); ++ii) {
					tabButtons[ii] = new ToggleButton(this);
					button = tabButtons[ii];
					button.setLines(1);
					if (mPrefTabTextSize > 0) {
						button.setTextSize(mPrefTabTextSize);
					}
					button.setLayoutParams(mTabButtonParams);
					final int val = ii;
					if (pages.get(ii).getContentDescription() != null
							&& pages.get(ii).getContentDescription().length() > 0) {
						tabButtons[ii].setText(pages.get(ii).getContentDescription());
						tabButtons[ii].setTextOn(pages.get(ii).getContentDescription());
						tabButtons[ii].setTextOff(pages.get(ii).getContentDescription());
					} else {
						String text = String.format(Locale.getDefault(), "%d", ii + 1);
						tabButtons[ii].setText(text);
						tabButtons[ii].setTextOn(text);
						tabButtons[ii].setTextOff(text);
					}

					button.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(final View v)
						{
							Button button;
							for (final ToggleButton mButton : tabButtons) {
								button = mButton;
								if (button != v) {
									mButton.setChecked(false);
								} else {
									mButton.setChecked(true);
								}
							}
							mLayout.removeAllViews();
							mLayout.addView(pages.get(val));
						}
					});
					mTabLayout.addView(button);
				}
				tabButtons[0].setChecked(true);
			}
		}
	}

	private LinearLayout addPage(
			ArrayList<LinearLayout> pages,
			String label)
	{
		LinearLayout page = new LinearLayout(this);
		page.setOrientation(LinearLayout.VERTICAL);
		page.setContentDescription(label);

		pages.add(page);

		return page;
	}

	private LinearLayout addRow(LinearLayout page)
	{
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setLayoutParams(mRowParams);

		page.addView(row);

		return row;
	}

	private void addSeparator(LinearLayout page, String label)
	{
		LinearLayout separator = new LinearLayout(this);
		separator.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, Helper.dp2px(this, 1f), 1f);

		/// hack to get default text view foreground color
		int labelColor = new TextView(this).getCurrentTextColor();

		if (label.length() > 0) {
			final LinearLayout hr = new LinearLayout(this);
			p.setMargins(mPrefHorizontalMargin, 0, mPrefHorizontalMargin, mPrefVerticalMargin);
			hr.setBackgroundColor(labelColor);
			hr.setLayoutParams(p);

			final TextView l = new TextView(this);
			p = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT,
					1f);
			p.setMargins(mPrefHorizontalMargin, mPrefVerticalMargin, mPrefHorizontalMargin, 0);
			l.setText(label);
			if (mPrefLabelTextSize > 0) {
				l.setTextSize(mPrefLabelTextSize);
			}
			l.setLayoutParams(p);
			separator.addView(l);
			separator.addView(hr);
		} else {
			p.setMargins(mPrefHorizontalMargin, mPrefVerticalMargin, mPrefHorizontalMargin,
						 mPrefVerticalMargin);
			separator.setBackgroundColor(labelColor);
			separator.setLayoutParams(p);
		}
		page.addView(separator);
	}

	private Button addButton(LinearLayout row)
	{
		Button button = new Button(this);
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				0,
				LayoutParams.MATCH_PARENT,
				1f);
		p.gravity = Gravity.CENTER_VERTICAL;
		if (mPrefButtonHeight > 0) {
			p.height = mPrefButtonHeight;
		}
		button.setLayoutParams(p);
		button.setLines(1);

		if (mPrefButtonTextSize > 0) {
			button.setTextSize(mPrefButtonTextSize);
		}

		row.addView(button);

		return button;
	}

	private ArrayList<LinearLayout> parseXml()
	{
		ArrayList<LinearLayout> pages = null;
		try {
			InputStream inputStream;

			if (mPrefRemoteFile.length() == 0) {
				AssetManager assetManager = getAssets();
				inputStream = assetManager.open("trimote.xml");
			} else {
				inputStream = new URL("file://" + mPrefRemoteFile).openStream();
			}

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.parse(new InputSource(inputStream));

			pages = parseXmlPages(doc.getElementsByTagName("page"));

			if (pages.size() == 0) {
				Log.se("No pages found in xml");
			}
		} catch (final MalformedURLException e) {
			Log.e(e);
		} catch (final FileNotFoundException e) {
			String message = String.format(
					getString(R.string.error_file_not_found), mPrefRemoteFile);
			Log.se(message, e);
		} catch (final IOException e) {
			Log.e(e);
		} catch (final ParserConfigurationException e) {
			Log.e(e);
		} catch (final SAXException e) {
			Log.se("File not valid xml", e);
		} catch (final RuntimeException e) {
			Log.e(e);
		}

		return pages;
	}

	private ArrayList<LinearLayout> parseXmlPages(NodeList pages)
	{
		ArrayList<LinearLayout> pageList = new ArrayList<LinearLayout>();

		Node page;
		mCurrentPage = 0;
		for (int ii = 0; ii < pages.getLength(); ++ii) {
			page = pages.item(ii);
			if (page.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			} else if (!page.getNodeName().equals("page")) {
				Log.w("Unsupported element '" + page.getNodeName() + "'" + getCurrentPath());
				continue;
			}

			++mCurrentPage;
			mCurrentRow    = 0;
			parseXmlRows(addPage(pageList, getAttribute(page, "label")), page.getChildNodes());
		}

		return pageList;
	}

	private void parseXmlRows(LinearLayout page, NodeList rows)
	{
		Node row;
		mCurrentRow = 0;
		for (int ii = 0; ii < rows.getLength(); ++ii) {
			row = rows.item(ii);

			if (row.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (row.getNodeName().equals("separator")) {
				addSeparator(page, getAttribute(row, "label"));
				continue;
			} else if(!row.getNodeName().equals("row")) {
				Log.w("Unsupported Element '" + row.getNodeName() + "'" + getCurrentPath());
				continue;
			}

			++mCurrentRow;
			mCurrentButton = 0;
			parseXmlButtons(addRow(page), row.getChildNodes());
		}
	}

	private void parseXmlButtons(LinearLayout row, NodeList buttons)
	{
		Node button;
		mCurrentButton = 0;
		for (int ii = 0; ii < buttons.getLength(); ++ii) {
			button = buttons.item(ii);
			if (button.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			} else if (!button.getNodeName().equals("button")) {
				Log.w("Unsupported Element '" + button.getNodeName() + "'" + getCurrentPath());
				continue;
			}
			++mCurrentButton;
			mCurrentButtonAction = 0;
			parseXmlButtonChildren(addButton(row), button.getChildNodes());
		}
	}

	private void parseXmlButtonChildren(Button button, NodeList buttonChildren)
	{
		NodeList actions;
		Node     buttonChild;

		String buttonText;
		String buttonColorString;
		int    buttonColor;
		int    repeatTime = 0;
		float  buttonFontSize;

		final ArrayList<Action> actionList = new ArrayList<Action>();

		for (int ii = 0; ii < buttonChildren.getLength(); ++ii) {
			buttonChild = buttonChildren.item(ii);
			if (buttonChild.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			if (buttonChild.getNodeName().equals("actions")) {
				actions = buttonChild.getChildNodes();
				repeatTime = getIntAttribute(buttonChild, "repeat");
				actionList.addAll(parseXmlButtonActions(actions));
			} else if (buttonChild.getNodeName().equals("text")) {
				buttonText = buttonChild.getTextContent();
				if (buttonText != null) {
					button.setText(buttonText.trim());
				}

				buttonColorString = getAttribute(buttonChild, "color");
				if (buttonColorString.length() > 0) {
					buttonColor = Color.parseColor(getAttribute(buttonChild, "color"));
					button.setTextColor(buttonColor);
				}

				buttonFontSize = getFloatAttribute(buttonChild, "size");
				if (buttonFontSize > 0) {
					button.setTextSize(buttonFontSize);
				}
			}
		}
		if (actionList.size() > 0) {
			if (repeatTime > 0) {
				button.setOnTouchListener(new TouchListener(actionList, repeatTime));
			} else {
				button.setOnClickListener(new ClickListener(actionList));
			}
		} else {
			if (button.getText().length() == 0) {
				button.setVisibility(View.INVISIBLE);
			} else {
				button.setError("Action error");
			}
		}
	}

	private ArrayList<Action> parseXmlButtonActions(NodeList actions)
	{
		Node   actionNode;
		String name;
		String first;

		ArrayList<Action> actionList = new ArrayList<Action>();
		Action            action;

		Class<?>       c;
		Constructor<?> constructor;

		mCurrentButtonAction = 0;
		for (int ii = 0; ii < actions.getLength(); ++ii) {
			actionNode = actions.item(ii);
			if (actionNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			++mCurrentButtonAction;
			name = actionNode.getNodeName();
			first = "" + name.charAt(0);
			name = Action.class.getPackage().getName() + '.'
					+ first.toUpperCase() + name.substring(1) + "Action";
			try {
				c = Class.forName(name);
				constructor = c.getConstructor();
				action = (Action) constructor.newInstance();
				action.setArgs(this, getCurrentPath(), getAttributes(actionNode));
				actionList.add(action);
			} catch (final ClassNotFoundException e) {
				Log.se("There is no action handler '" + actionNode.getNodeName() + "'"
							   + getCurrentPath(), e);
			} catch (final NoSuchMethodException e) {
				Log.e(e);
			} catch (final InstantiationException e) {
				Log.e(e);
			} catch (final IllegalAccessException e) {
				Log.e(e);
			} catch (final InvocationTargetException e) {
				Log.se(e.getMessage() + getCurrentPath(), e);
			} catch (final ClassCastException e) {
				Log.se(actionNode.getNodeName() + " is not an action handler"
							   + getCurrentPath(), e);
			}
		}
		return actionList;
	}

	private String getCurrentPath()
	{
		String position = "";
		if (mCurrentPage > 0) {
			position += " at: page " + mCurrentPage;
			if (mCurrentRow > 0) {
				position += ">row " + mCurrentRow;
				if (mCurrentButton > 0) {
					position += ">button " + mCurrentButton;
					if (mCurrentButtonAction > 0) {
						position += ">action " + mCurrentButtonAction;
					}
				}
			}
		}

		return position;
	}

	private float getFloatAttribute(
			final Node node,
			@SuppressWarnings("SameParameterValue") final String name)
	{
		float res = 0f;

		if (node != null) {
			final NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				final Node attribute = attributes.getNamedItem(name);
				if (attribute != null) {
					try {
						res = Float.parseFloat(attribute.getNodeValue().trim());
					} catch (final NumberFormatException e) {
						// nothing to do here
					}
				}
			}
		}

		return res;
	}

	private int getIntAttribute(
			final Node node,
			@SuppressWarnings("SameParameterValue") final String name)
	{
		int res = 0;

		if (node != null) {
			final NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				final Node attribute = attributes.getNamedItem(name);
				if (attribute != null) {
					try {
						res =
								Integer.parseInt(attribute.getNodeValue().trim());
					} catch (final NumberFormatException e) {
						Log.log(ERROR, "Attribute " + name + " has to be an integer.", e, true);
					}
				}
			}
		}

		return res;
	}

	private String getAttribute(final Node node, final String name)
	{
		String res = "";

		if (node != null) {
			final NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				final Node attribute = attributes.getNamedItem(name);
				if (attribute != null) {
					res = attribute.getNodeValue().trim();
				}
			}
		}

		return res;
	}

	private Map<String, String> getAttributes(final Node node)
	{
		Map<String, String> res = null;

		if (node != null) {
			final NamedNodeMap attrs = node.getAttributes();
			if (attrs != null) {
				final int length = attrs.getLength();
				if (length > 0) {
					res = new HashMap<String, String>();
					Node attr;
					for (int ii = 0; ii < length; ++ii) {
						attr = attrs.item(ii);
						res.put(attr.getNodeName(), attr.getNodeValue());
					}
				}
			}
		}

		return res;
	}

	private void showOptions()
	{
		mRestartRequired = false;
		mReloadRequired = false;

		startActivityForResult(
				new Intent().setClass(MainActivity.this, SettingsActivity.class),
				ID_INTENT_REQUEST_SETTINGS);
	}

	private void setPrefTheme()
	{
		int theme;
		try {
			theme = Integer.parseInt(mSettings.getString("theme", "0"));
		} catch (final Exception e) {
			theme = android.R.style.Theme_DeviceDefault_NoActionBar;
		}
		switch (theme) {
			case 0:
				theme = android.R.style.Theme_NoTitleBar;
				break;
			case 1:
				theme = android.R.style.Theme_Black_NoTitleBar;
				break;
			case 2:
				theme = android.R.style.Theme_Light_NoTitleBar;
				break;
			case 3:
				theme = android.R.style.Theme_DeviceDefault_NoActionBar;
				break;
			case 4:
				theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar;
				break;
			case 5:
				theme = android.R.style.Theme_Holo_NoActionBar;
				break;
			case 6:
				theme = android.R.style.Theme_Holo_Light_NoActionBar;
				break;
			default:
				theme = android.R.style.Theme_DeviceDefault_NoActionBar;
		}

		if (theme != mPrefTheme) {
			mRestartRequired = true;
			mPrefTheme = theme;
			setTheme(mPrefTheme);
		}
	}

	private void setPrefBackground()
	{
		int bg;
		try {
			bg = Integer.parseInt(mSettings.getString("background", "0"));
		} catch (final NumberFormatException e) {
			bg = 0;
		}

		switch (bg) {
			case 1:
				getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				break;
			case 2:
				getWindow().setBackgroundDrawableResource(
						android.R.drawable.screen_background_dark_transparent);
				break;
			case 3:
				getWindow().setBackgroundDrawableResource(
						android.R.drawable.screen_background_light_transparent);
				break;
		}
		if (bg > 0) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		}
		if (bg != mPrefBackground) {
			mRestartRequired = true;
		}
		mPrefBackground = bg;
	}

	private void setPrefLock()
	{
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		//getWindow().setBackgroundDrawableResource(android.R.drawable.screen_background_dark_transparent);
		//getWindow().setBackgroundDrawable(new ColorDrawable(0));
		boolean lock;
		try {
			lock = mSettings.getBoolean("lock", false);
		} catch (final ClassCastException e) {
			lock = false;
		}
		if (lock) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		} else {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}
	}

	private void setPrefSleep()
	{
		boolean sleep;
		try {
			sleep = mSettings.getBoolean("sleep", true);
		} catch (final ClassCastException e) {
			sleep = true;
		}
		if (sleep) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	private void setPrefNotification()
	{
		boolean notification;
		try {
			notification = mSettings.getBoolean("notification", true);
		} catch (final ClassCastException e) {
			notification = true;
		}

		final NotificationManager notificationManager =
				(NotificationManager) getSystemService(
						Context.NOTIFICATION_SERVICE);
		if (notification) {
			final Notification.Builder notificationBuilder =
					new Notification.Builder(this)
							.setSmallIcon(R.drawable.notification_icon)
							.setContentTitle(this.getString(this.getApplicationInfo().labelRes))
							.setOngoing(true);
			final Intent intent = new Intent(this, MainActivity.class);

			final TaskStackBuilder taskStackBuilder =
					TaskStackBuilder.create(this);
			taskStackBuilder.addParentStack(MainActivity.class);
			taskStackBuilder.addNextIntent(intent);

			final PendingIntent pendingIntent =
					taskStackBuilder.getPendingIntent(
							0,
							PendingIntent.FLAG_UPDATE_CURRENT);
			notificationBuilder.setContentIntent(pendingIntent);

			notificationManager.notify(ID_NOTIFICATION,
									   notificationBuilder.build());
		} else {
			notificationManager.cancel(ID_NOTIFICATION);
		}
	}

	private void setPrefBrightness()
	{
		float brightness;
		try {
			brightness = mSettings.getInt("brightness", -1) / 100.0f;
		} catch (final ClassCastException e) {
			brightness = -1;
		}

		final WindowManager.LayoutParams p =
				getWindow().getAttributes();
		p.screenBrightness = brightness;
		getWindow().setAttributes(p);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		showOptions();
		return true;
	}

	@Override
	public void onActivityResult(
			final int requestCode,
			final int resultCode,
			final Intent data)
	{
		if (requestCode == ID_INTENT_REQUEST_SETTINGS) {
			if (mReloadRequired) {
				Log.d("reload required");
				onCreate(null);
			} else if (mRestartRequired) {
				Log.d("restart required");
				final Intent i = getIntent();
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
			} else {
				Log.d("nothing required");
			}
		}
	}

	public boolean showDebugInfo()
	{
		return mPrefDebug;
	}

	private class ClickListener implements View.OnClickListener
	{
		private final ArrayList<Action> mActions;

		public ClickListener(final ArrayList<Action> actions)
		{
			mActions = actions;
		}

		@Override
		public void onClick(final View v)
		{
			String  debug = "";
			boolean result;
			for (Action action : mActions) {
				result = action.invoke();
				if (!result) {
					((Button) v).setError("Action failed");
				} else {
					((Button) v).setError(null);
				}
				if (mPrefDebug) {
					debug += (result ? "1: " : "0: ")
							+ action.getClass().getSimpleName().replaceAll("Action$", "") + ": "
							+ action.getDebugString() + "\n";
				}
			}
			if (mPrefDebug) {
				AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
						.setMessage(debug)
						.setPositiveButton(android.R.string.ok, null)
						.show();
				TextView textView = (TextView) dialog.findViewById(android.R.id.message);
				textView.setTextSize(10);
			}
		}
	}

	private class TouchListener implements View.OnTouchListener
	{
		private final ArrayList<Action> mActions;
		private final int               mRepeatTime;
		private       int               mCounter;
		private       long              mLast;
		private       Handler           mHandler;
		private       String            mDebug;
		private       boolean           mResult;
		private final Runnable mAction = new Runnable()
		{
			@Override
			public void run()
			{
				if (++mCounter > 50) {
					return;
				}
				final long start = System.currentTimeMillis();

				Log.d("last: " + (start - mLast) + "ms" + " / repeatTime: "
								  + mRepeatTime);
				mLast = start;


				mDebug = "";
				for (Action action : mActions) {
					mResult = action.invoke();
					if (mPrefDebug) {
						mDebug +=
								(mResult ? "1: " : "0: ") + action.getClass().getSimpleName()
										.replaceAll("Action$", "") + ": "
										+ action.getDebugString() + "\n";
					}
				}

				final long sleep = Math.max(
						1,
						mRepeatTime + start - System.currentTimeMillis());

				mHandler.postDelayed(this, sleep);
			}
		};

		public TouchListener(
				final ArrayList<Action> actions,
				final int repeatTime)
		{
			mRepeatTime = repeatTime;
			mActions = actions;
			mLast = 0;
		}

		@Override
		public boolean onTouch(final View v, final MotionEvent event)
		{
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mCounter = 0;
					v.setPressed(true);
					if (mHandler == null) {
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 1);
					}
					return true;
				case MotionEvent.ACTION_UP:
					v.setPressed(false);
					if (mHandler != null) {
						mHandler.removeCallbacks(mAction);
						mHandler = null;
					}
					if (!mResult) {
						((Button) v).setError("Action failed");
					} else {
						((Button) v).setError(null);
					}
					if (mPrefDebug) {
						AlertDialog dialog =
								new AlertDialog.Builder(MainActivity.this)
										.setMessage(mDebug)
										.setPositiveButton("OK", null)
										.show();
						TextView textView =
								(TextView) dialog.findViewById(android.R.id.message);
						textView.setTextSize(10);
					}
					return true;
			}

			return false;
		}
	}
}
