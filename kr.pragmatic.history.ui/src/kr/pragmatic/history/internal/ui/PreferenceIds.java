package kr.pragmatic.history.internal.ui;

import org.eclipse.team.internal.ui.TeamUIPlugin;

public final class PreferenceIds {
	private PreferenceIds() {}
	
	public static final String PREFIX = TeamUIPlugin.ID + "."; //$NON-NLS-1$
	
	/*
	 * Preferences for the Local History Page
	 */
	public static final String PREF_ONLYRECENT_MODE = PREFIX + "show_only_recent_mode"; //$NON-NLS-1$
	public static final String PREF_GROUPBYDATE_MODE = PREFIX + "group_bydate2_mode"; //$NON-NLS-1$
}
