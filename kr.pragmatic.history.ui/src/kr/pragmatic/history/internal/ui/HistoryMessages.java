package kr.pragmatic.history.internal.ui;

import org.eclipse.osgi.util.NLS;

public class HistoryMessages extends NLS {
	
	private static final String BUNDLE_NAME = "kr.pragmatic.history.internal.ui.messages";//$NON-NLS-1$
	
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, HistoryMessages.class);
	}
	
	public static String ShowLocalHistory_0;
	public static String GenericHistoryTableProvider_FileName;
	
	public static String LocalHistoryPage_RecentFilterAction;
	public static String LocalHistoryPage_RecentFilterTip;
}
