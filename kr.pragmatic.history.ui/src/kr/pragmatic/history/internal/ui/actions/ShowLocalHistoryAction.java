package kr.pragmatic.history.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import kr.pragmatic.history.internal.ui.HistoryMessages;
import kr.pragmatic.history.internal.ui.LocalHistoryPage;
import kr.pragmatic.history.internal.ui.LocalHistoryPageSource;
import kr.pragmatic.history.internal.util.ResourceUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;



public class ShowLocalHistoryAction extends ActionDelegate implements IObjectActionDelegate {
	
	private IStructuredSelection selection;
	private IWorkbenchPart targetPart;

	public ShowLocalHistoryAction() {
	}
	
	public void run(IAction action) {
		if (!available()) {
			MessageDialog.openInformation(getShell(), getPromptTitle(), HistoryMessages.ShowLocalHistory_0);
			return;
		}
		
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					Runnable r = new Runnable() {
						public void run() {
							IHistoryView view = TeamUI.showHistoryFor(TeamUIPlugin.getActivePage(), selection.toArray(), LocalHistoryPageSource.getInstance());
							IHistoryPage page = view.getHistoryPage();
							if (page instanceof LocalHistoryPage){
								LocalHistoryPage historyPage = (LocalHistoryPage) page;
								historyPage.setClickAction(isCompare());
							}
						};
					};
					TeamUIPlugin.getStandardDisplay().asyncExec(r);
				}
			});
		} catch (InvocationTargetException exception) {
			ErrorDialog.openError(getShell(), null, null, new Status(IStatus.ERROR, TeamUIPlugin.ID, IStatus.ERROR, TeamUIMessages.ShowLocalHistory_1, exception.getTargetException()));
		} catch (InterruptedException exception) {
		}
	}
	
	protected boolean isCompare() {
		return false;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		}
	}
	
	/**
	 * Whether one or more history revision is available.
	 *  
	 * @return
	 */
	public boolean available() {
		// selected resources and their children
		IFile[] files = ResourceUtil.getFiles(getSelection().toArray());
		
		for (int i = 0; i < files.length; i++) {
			if (available(files[i]))
				return true;
		}
		
		return false;
	}
	
	public boolean available(IFile file) {
		try {
			return file.getHistory(null).length > 0 ? true : false;
		} catch (CoreException e) {
			return false;
		}
	}
	
	protected Shell getShell() {
		if (targetPart != null)
			return targetPart.getSite().getShell();
		return TeamUIPlugin.getActivePage().getActivePart().getSite().getShell();
	}
	
	public IStructuredSelection getSelection() {
		return selection;
	}
	
	protected String getPromptTitle() {
		return TeamUIMessages.ShowLocalHistory_2;
	}
}
