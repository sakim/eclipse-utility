package kr.pragmatic.compare.internal.ui;

import org.eclipse.osgi.util.NLS;

public class CompareUIMessages extends NLS {
	private static final String BUNDLE_NAME = "kr.pragmatic.compare.internal.ui.messages";//$NON-NLS-1$
	
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, CompareUIMessages.class);
	}
	
	public static String OpenComparablesAction_dialogTitle;
	public static String OpenComparablesAction_dialogMessage;
}
