package kr.pragmatic.history.internal.ui;

import kr.pragmatic.history.internal.util.ResourceUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.ui.part.Page;


public class LocalHistoryPageSource implements IHistoryPageSource {
	
	private static LocalHistoryPageSource instance;
	
	public static IFile[] getFiles(Object[] objects) {
		return ResourceUtil.getFiles(objects);
//		return ResourceUtil.getResources(objects);
	}

	public boolean canShowHistoryFor(Object object) {
		return getFiles((Object[]) object) != null;
	}

	public Page createPage(Object object) {
		LocalHistoryPage page = new LocalHistoryPage();
		return page;
	}
	
	public synchronized static IHistoryPageSource getInstance() {
		if (instance == null)
			instance = new LocalHistoryPageSource();
		return instance;
	}
}
