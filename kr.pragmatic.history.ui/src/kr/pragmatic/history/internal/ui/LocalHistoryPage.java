package kr.pragmatic.history.internal.ui;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kr.pragmatic.history.internal.util.ResourceUtil;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IOpenEventListener;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.core.history.LocalFileHistory;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.internal.ui.actions.OpenRevisionAction;
import org.eclipse.team.internal.ui.history.AbstractHistoryCategory;
import org.eclipse.team.internal.ui.history.DateHistoryCategory;
import org.eclipse.team.internal.ui.history.DialogHistoryPageSite;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.internal.ui.history.MessageHistoryCategory;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryCompareAdapter;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IProgressConstants;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;

/**
 * Page for multi-file local history.
 * 
 * @author sakim (ccoroom@gmail.com)
 *
 */
public class LocalHistoryPage extends HistoryPage {
	
	public static final int ON = 1;
	public static final int OFF = 2;
	public static final int ALWAYS = 4;
	
	private IFile[] files;
	
	// cached for efficiency
	private LocalFileHistory[] localFileHistories;
	
	private TreeViewer treeViewer;
	
	private boolean shutdown = false;
	
	//grouping on
	private boolean groupingOff = false;
	//toggle constants for filtering same resource
	private boolean filteringOn = false;
	
	//toggle constants for default click action
	private int compareMode = OFF;

	protected LocalFileHistoryTableProvider historyTableProvider;
	private RefreshFileHistory refreshFileHistoryJob;
	private Composite localComposite;
	// actions
	private Action recentFilterAction;
	private Action groupByDateMode;
	private Action collapseAll;
	private Action compareModeAction;
	private Action getContentsAction;
	private CompareRevisionAction compareAction;
	private OpenRevisionAction openAction;
	
	private ListenerList resourceListenerList = new ListenerList();
	
	private IFileRevision currentSelection;
	
	private final class LocalHistoryContentProvider implements ITreeContentProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IFileHistory) {
				// The entries of already been fetch so return them
				IFileHistory fileHistory = (IFileHistory) inputElement;
				return fileHistory.getFileRevisions();
			}
			if (inputElement instanceof IFileRevision[]) {
				return (IFileRevision[]) inputElement;
			}
			if (inputElement instanceof AbstractHistoryCategory[]){
				return (AbstractHistoryCategory[]) inputElement;
			}
			return new Object[0];
		}

		public void dispose() {
			// Nothing to do
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Nothing to do
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof AbstractHistoryCategory){
				return ((AbstractHistoryCategory) parentElement).getRevisions();
			}
			
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof AbstractHistoryCategory){
				return ((AbstractHistoryCategory) element).hasRevisions();
			}
			return false;
		}
	}

	private class LocalFileHistoryTableProvider extends LocalHistoryTableProvider {
		protected IFileRevision adaptToFileRevision(Object element) {
			// Get the log entry for the provided object
			IFileRevision entry = null;
			if (element instanceof IFileRevision) {
				entry = (IFileRevision) element;
			} else if (element instanceof IAdaptable) {
				entry = (IFileRevision) ((IAdaptable) element).getAdapter(IFileRevision.class);
			} else if (element instanceof AbstractHistoryCategory){
				IFileRevision[] revisions = ((AbstractHistoryCategory) element).getRevisions();
				if (revisions.length > 0)
					entry = revisions[0]; 
			}
			return entry;
		}

		protected long getModificationDate(Object element) {
			IFileRevision entry = adaptToFileRevision(element);
			if (entry != null)
				return entry.getTimestamp();
			return -1;
		}
		
		protected boolean isDeletedEdition(Object element) {
			IFileRevision entry = adaptToFileRevision(element);
			return (!entry.exists());
		}
	}
	
	private class RefreshFileHistory extends Job {
		public RefreshFileHistory() {
			super(TeamUIMessages.LocalHistoryPage_FetchLocalHistoryMessage);
		}
		public IStatus run(IProgressMonitor monitor)  {
			try {
				IStatus status = Status.OK_STATUS;

				if (shutdown)
					return status;
				
				List/*<IFileRevision>*/ allRevisions = new ArrayList/*<IFileRevision>*/();
				
				// Assign the instance variable to a local so it does not get cleared well we are refreshing
				for (int i = 0; i < localFileHistories.length; i++) {
					LocalFileHistory fileHistory = localFileHistories[i];
					if (fileHistory == null)
						continue;
					
					try {
						fileHistory.refresh(Policy.subMonitorFor(monitor, 50));
						IFileRevision[] revisions = fileHistory.getFileRevisions();
						
						allRevisions.addAll(Arrays.asList(revisions));
					} catch (CoreException ex) {
						status = new TeamStatus(ex.getStatus().getSeverity(), TeamUIPlugin.ID, ex.getStatus().getCode(), ex.getMessage(), ex, files[i]);
					}
				}
				
				IFileRevision[] all = (IFileRevision[]) allRevisions.toArray(new IFileRevision[allRevisions.size()]);
	
				update(all, Policy.subMonitorFor(monitor, 50));
	
				if (status != Status.OK_STATUS ) {
					this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
					this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
				}
				
				return status;
			} finally {
				monitor.done();
			}
		}
	}
	
	private class LocalFileHistoryFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (filteringOn) {
				if (element instanceof LocalFileRevision) {
					LocalFileRevision revision = (LocalFileRevision) element;
					
					// only current revision has a "real" current file
					if (revision.getFile() == null) {
						return false;
					}
				}
			}
			
			return true;
		}
	}
	
	private class HistoryResourceListener implements IResourceChangeListener {
		private IFile file;
		
		public HistoryResourceListener(IFile file) {
			this.file = file;
		}
		
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta root = event.getDelta();
		
			if (file == null)
				 return;
			
			IResourceDelta resourceDelta = root.findMember(file.getFullPath());
			if (resourceDelta != null){
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						refresh();
					}
				});
			}
		}
	}
	
	public LocalHistoryPage() {
		super();
	}
	
	public boolean inputSet() {
		removeAllResourceListener();
		
		IFile[] tempFiles = getFiles();
		this.files = tempFiles;
		if (tempFiles == null)
			return false;
		
		// Resource change listener
		for (int i = 0; i < files.length; i++) {
			HistoryResourceListener resourceListener = new HistoryResourceListener(files[i]);
			ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
			resourceListenerList.add(resourceListener);
		}
		
		//blank current input only after we're sure that we have a file
		//to fetch history for
		this.treeViewer.setInput(null);
		
		localFileHistories = new LocalFileHistory[files.length];
		for (int i = 0; i < files.length; i++)
			localFileHistories[i] = new LocalFileHistory(files[i], !getHistoryPageSite().isModal());
		
		if (refreshFileHistoryJob == null)
			refreshFileHistoryJob = new RefreshFileHistory();
		
		//always refresh the history if the input gets set
		refreshHistory(true);
		return true;
	}

	protected IFile[] getFiles() {
		return LocalHistoryPageSource.getFiles((Object[]) getInput());
	}

	private void refreshHistory(boolean refetch) {
		if (Policy.DEBUG_HISTORY) {
			String time = new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
			System.out.println(time + ": LocalHistoryPage#refreshHistory, refetch = " + refetch); //$NON-NLS-1$
		}
		
		if (refreshFileHistoryJob.getState() != Job.NONE){
			refreshFileHistoryJob.cancel();
		}
		IHistoryPageSite parentSite = getHistoryPageSite();
		Utils.schedule(refreshFileHistoryJob, getWorkbenchSite(parentSite));
	}

	private IWorkbenchPartSite getWorkbenchSite(IHistoryPageSite parentSite) {
		IWorkbenchPart part = parentSite.getPart();
		if (part != null)
			return part.getSite();
		return null;
	}
	
	public void createControl(Composite parent) {

		localComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		localComposite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		localComposite.setLayoutData(data);
		
		treeViewer = createTree(localComposite);
		
		contributeActions();
		
		IHistoryPageSite parentSite = getHistoryPageSite();
		if (parentSite != null && parentSite instanceof DialogHistoryPageSite && treeViewer != null)
			parentSite.setSelectionProvider(treeViewer);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(localComposite, IHelpContextIds.LOCAL_HISTORY_PAGE);
	}
	
	private void contributeActions() {
		final IPreferenceStore store = TeamUIPlugin.getPlugin().getPreferenceStore();
		
		// Show only the most recent revision
		recentFilterAction = new Action(HistoryMessages.LocalHistoryPage_RecentFilterAction, TeamUIPlugin.getImageDescriptor(HistoryUIImages.IMG_FILTER_HISTORY)) {
			public void run() {
				filteringOn = !filteringOn;
				store.setValue(PreferenceIds.PREF_ONLYRECENT_MODE, filteringOn);
				recentFilterAction.setChecked(filteringOn);
				treeViewer.refresh();
			}
		};
		filteringOn = store.getBoolean(PreferenceIds.PREF_ONLYRECENT_MODE);
		recentFilterAction.setToolTipText(HistoryMessages.LocalHistoryPage_RecentFilterTip);
		recentFilterAction.setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(HistoryUIImages.IMG_FILTER_HISTORY));
		recentFilterAction.setHoverImageDescriptor(TeamUIPlugin.getImageDescriptor(HistoryUIImages.IMG_FILTER_HISTORY));
		recentFilterAction.setChecked(filteringOn);
		
		//Group by Date
		groupByDateMode = new Action(TeamUIMessages.LocalHistoryPage_GroupRevisionsByDateAction, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY)){
			public void run() {
				groupingOff = !groupingOff;
				store.setValue(PreferenceIds.PREF_GROUPBYDATE_MODE, groupingOff);
				refreshHistory(false);
			}
		};
		groupingOff = store.getBoolean(PreferenceIds.PREF_GROUPBYDATE_MODE);
		groupByDateMode.setChecked(!groupingOff);
		groupByDateMode.setToolTipText(TeamUIMessages.LocalHistoryPage_GroupRevisionsByDateTip);
		groupByDateMode.setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY));
		groupByDateMode.setHoverImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY));
		
		//Collapse All
		collapseAll =  new Action(TeamUIMessages.LocalHistoryPage_CollapseAllAction, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COLLAPSE_ALL)) {
			public void run() {
				treeViewer.collapseAll();
			}
		};
		collapseAll.setToolTipText(TeamUIMessages.LocalHistoryPage_CollapseAllTip); 
		collapseAll.setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COLLAPSE_ALL));
		collapseAll.setHoverImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COLLAPSE_ALL));
		
		IHistoryPageSite historyPageSite = getHistoryPageSite();
		if (!historyPageSite.isModal()) {
			//Compare Mode Action
			if ((compareMode & ALWAYS) == 0) {
				compareModeAction = new Action(TeamUIMessages.LocalHistoryPage_CompareModeAction, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPARE_VIEW)) {
					public void run() {
						// switch the mode
						compareMode = compareMode == ON ? OFF : ON;
						compareModeAction.setChecked(compareMode == ON);
					}
				};
				compareModeAction.setToolTipText(TeamUIMessages.LocalHistoryPage_CompareModeTip); 
				compareModeAction.setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPARE_VIEW));
				compareModeAction.setHoverImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPARE_VIEW));
				compareModeAction.setChecked(compareMode == ON);
				getContentsAction = getContextMenuAction(TeamUIMessages.LocalHistoryPage_GetContents, true /* needs progress */, new IWorkspaceRunnable() { 
					public void run(IProgressMonitor monitor) throws CoreException {
						monitor.beginTask(null, 100);
						try {
							IStorage currentStorage = currentSelection.getStorage(new SubProgressMonitor(monitor, 50));
							InputStream in = currentStorage.getContents();
							IFile file = ResourceUtil.getWorkspaceFile(currentStorage.getFullPath());
							if(confirmOverwrite(file)) {
								(file).setContents(in, false, true, new SubProgressMonitor(monitor, 50));	
							}
						} catch (TeamException e) {
							throw new CoreException(e.getStatus());
						} finally {
							monitor.done();
						}
					}
				});
			}
		
			// Click Compare action
			compareAction = createCompareAction();
			compareAction.setEnabled(!treeViewer.getSelection().isEmpty());
			treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					compareAction.setCurrentFileRevision(getCurrentFileRevision());
					compareAction.selectionChanged((IStructuredSelection) treeViewer.getSelection());
				}
			});
			
			// Only add the open action if compare mode is not always on
			if (!((compareMode & (ALWAYS | ON)) == (ALWAYS | ON))) {
				openAction = new OpenRevisionAction(TeamUIMessages.LocalHistoryPage_OpenAction, this);
				openAction.setEnabled(!treeViewer.getSelection().isEmpty());
				treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
					public void widgetSelected(SelectionEvent e) {
						openAction.selectionChanged((IStructuredSelection) treeViewer.getSelection());
					}
				});
			}
			
			OpenStrategy handler = new OpenStrategy(treeViewer.getTree());
			handler.addOpenListener(new IOpenEventListener() {
				public void handleOpen(SelectionEvent e) {
					if (getSite() != null) {
						StructuredSelection tableStructuredSelection = (StructuredSelection) treeViewer.getSelection();
						if ((compareMode & ON) > 0) {
							compareAction.selectionChanged(new StructuredSelection(tableStructuredSelection.getFirstElement()));
							compareAction.run();
						} else {
							//Pass in the entire structured selection to allow for multiple editor openings
							StructuredSelection sel = tableStructuredSelection;
							if (openAction != null) {
								openAction.selectionChanged(sel);
								openAction.run();
							}
						}
					}
				}
			});
			
			//Contribute actions to popup menu
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(treeViewer.getTree());
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager menuMgr) {
					fillTableMenu(menuMgr);
				}
			});
			menuMgr.setRemoveAllWhenShown(true);
			treeViewer.getTree().setMenu(menu);
			
			//Don't add the object contribution menu items if this page is hosted in a dialog
			IWorkbenchPart part = historyPageSite.getPart();
			if (part != null) {
				IWorkbenchPartSite workbenchPartSite = part.getSite();
				workbenchPartSite.registerContextMenu(menuMgr, treeViewer);
			}
			IPageSite pageSite = historyPageSite.getWorkbenchPageSite();
			if (pageSite != null) {
				IActionBars actionBars = pageSite.getActionBars();
				// Contribute toggle text visible to the toolbar drop-down
				IMenuManager actionBarsMenu = actionBars.getMenuManager();
				if (actionBarsMenu != null){
					actionBarsMenu.removeAll();
				}
				actionBars.updateActionBars();
			}
		}
		
		//Create the local tool bar
		IToolBarManager tbm = historyPageSite.getToolBarManager();
		if (tbm != null) {
			String qualifier = getFileNameQualifier();
			//Add groups
			tbm.add(new Separator(qualifier+"grouping"));	//$NON-NLS-1$
			tbm.appendToGroup(qualifier+"grouping", groupByDateMode); //$NON-NLS-1$
			tbm.add(new Separator(qualifier+"collapse")); //$NON-NLS-1$
			tbm.appendToGroup(qualifier+"collapse", collapseAll); //$NON-NLS-1$
			if (compareModeAction != null)
				tbm.appendToGroup(qualifier+"collapse", compareModeAction);  //$NON-NLS-1$
			tbm.appendToGroup(qualifier+"collapse", recentFilterAction);  //$NON-NLS-1$
			tbm.update(false);
		}
	}
	
	private String getFileNameQualifier() {
		//Just append the current system time to generate a unique group name
		return Long.toString(System.currentTimeMillis());
	}
	
	protected CompareRevisionAction createCompareAction() {
		return new CompareRevisionAction(this);
	}
	
	protected void fillTableMenu(IMenuManager manager) {
		// file actions go first (view file)
		IHistoryPageSite parentSite = getHistoryPageSite();
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		
		if (files != null && !parentSite.isModal()){
			if (openAction != null)
				manager.add(openAction);
			if (compareAction != null)
				manager.add(compareAction);
			if (getContentsAction != null) {
				ISelection sel = treeViewer.getSelection();
				if (!sel.isEmpty()) {
					if (sel instanceof IStructuredSelection) {
						IStructuredSelection tempSelection = (IStructuredSelection) sel;
						if (tempSelection.size() == 1) {
							manager.add(new Separator("getContents")); //$NON-NLS-1$
							manager.add(getContentsAction);
						}
					}
				}
			}
		}
	}

	/**
	 * Creates the tree that displays the local file revisions
	 * 
	 * @param parent the parent composite to contain the group
	 * @return the group control
	 */
	protected TreeViewer createTree(Composite parent) {
		historyTableProvider = new LocalFileHistoryTableProvider();
		TreeViewer viewer = historyTableProvider.createTree(parent);
		viewer.setContentProvider(new LocalHistoryContentProvider());
		viewer.setFilters(new ViewerFilter[] {new LocalFileHistoryFilter()});
		return viewer;
	}

	public Control getControl() {
		return localComposite;
	}

	public void setFocus() {
		localComposite.setFocus();
	}

	public String getDescription() {
		return null;
	}

	public String getName() {
		Object[] selections = (Object[]) getInput();

		if (selections.length == 1 && selections[0] instanceof IProject) {
			IProject project = (IProject) selections[0];
			return "Project: " + project.getName();
		}
		
		if (getFiles() != null && getFiles().length > 0) {
			IFile[] files = getFiles();
			
			if (files.length == 1) {
				return files[0].getName();
			} else {
				return files.length + " files";
			}
		}
		return ""; //$NON-NLS-1$
	}

	public boolean isValidInput(Object object) {
		return (object instanceof IFile);
	}

	public void refresh() {
		refreshHistory(true);
	}

	public Object getAdapter(Class adapter) {
		if(adapter == IHistoryCompareAdapter.class) {
			return this;
		}
		return null;
	}

    public void dispose() {
    	shutdown = true;
    	
    	removeAllResourceListener();

		//Cancel any incoming 
		if (refreshFileHistoryJob != null) {
			if (refreshFileHistoryJob.getState() != Job.NONE) {
				refreshFileHistoryJob.cancel();
			}
		}
	}

	private void removeAllResourceListener() {
		Object[] listeners = resourceListenerList.getListeners();
    	for (int i = 0; i < listeners.length; i++) {
    		ResourcesPlugin.getWorkspace().removeResourceChangeListener((IResourceChangeListener) listeners[i]);
    	}
    	resourceListenerList.clear();
	}
    
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryCompareAdapter#prepareInput(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.CompareConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void prepareInput(ICompareInput input, CompareConfiguration configuration, IProgressMonitor monitor) {
		Object right = input.getRight();
		if (right != null) {
			String label = getLabel(right);
			if (label != null)
				configuration.setRightLabel(label);
			Image image = getImage(right);
			if (image != null)
				configuration.setRightImage(image);
		}
		Object left = input.getLeft();
		if (left != null) {
			String label = getLabel(left);
			if (label != null)
				configuration.setLeftLabel(label);
			Image image = getImage(left);
			if (image != null)
				configuration.setLeftImage(image);
		}
	}

	protected Image getImage(Object right) {
		if (right instanceof FileRevisionTypedElement || right instanceof LocalFileRevision || right instanceof IFileRevision) {
			return historyTableProvider.getRevisionImage();
		}
		if (right instanceof ITypedElement) {
			ITypedElement te = (ITypedElement) right;
			return te.getImage();
		}
		return null;
	}

	protected String getLabel(Object object) {
		if (object instanceof IFileRevision) {
			IFileRevision revision = (IFileRevision) object;
			long timestamp = revision.getTimestamp();
			if (timestamp > 0)
			return NLS.bind(TeamUIMessages.LocalHistoryPage_0, historyTableProvider.getDateFormat().format(new Date(timestamp)));
		}
		if (object instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement e = (FileRevisionTypedElement) object;
			return getLabel(e.getRevision());
		}
		if (object instanceof LocalResourceTypedElement) {
			return TeamUIMessages.LocalHistoryPage_1;
		}
		return null;
	}

	/**
	 * Method invoked from a background thread to update the viewer with the given revisions.
	 * @param revisions the revisions for the file
	 * @param monitor a progress monitor
	 */
	protected void update(final IFileRevision[] revisions, IProgressMonitor monitor) {
		// Group the revisions (if appropriate) before running in the UI thread
		final AbstractHistoryCategory[] categories = groupRevisions(revisions, monitor);
		// Update the tree in the UI thread
		Utils.asyncExec(new Runnable() {
			public void run() {
				if (Policy.DEBUG_HISTORY) {
					String time = new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
					System.out.println(time + ": LocalHistoryPage#update, the tree is being updated in the UI thread"); //$NON-NLS-1$
				}
				if (categories != null) {
					Object[] elementsToExpand = mapExpandedElements(categories, treeViewer.getExpandedElements());
					treeViewer.getTree().setRedraw(false);
					treeViewer.setInput(categories);
					//if user is switching modes and already has expanded elements
					//selected try to expand those, else expand all
					if (elementsToExpand.length > 0)
						treeViewer.setExpandedElements(elementsToExpand);
					else {
						treeViewer.expandAll();
						Object[] el = treeViewer.getExpandedElements();
						if (el != null && el.length > 0) {
							treeViewer.setSelection(new StructuredSelection(el[0]));
							treeViewer.getTree().deselectAll();
						}
					}
					treeViewer.getTree().setRedraw(true);
				} else {
					if (revisions.length > 0) {
						treeViewer.setInput(revisions);
					} else {
						treeViewer.setInput(new AbstractHistoryCategory[] {getErrorMessage()});
					}
				}
			}
		}, treeViewer);
	}

	private AbstractHistoryCategory[] groupRevisions(IFileRevision[] revisions, IProgressMonitor monitor) {
		if (!groupingOff)
			return sortRevisions(revisions, monitor);
		return null;
	}
	
	private Object[] mapExpandedElements(AbstractHistoryCategory[] categories, Object[] expandedElements) {
		//store the names of the currently expanded categories in a map
		HashMap elementMap = new HashMap();
		for (int i=0; i<expandedElements.length; i++){
			elementMap.put(((DateHistoryCategory)expandedElements[i]).getName(), null);
		}
		
		//Go through the new categories and keep track of the previously expanded ones
		ArrayList expandable = new ArrayList();
		for (int i = 0; i<categories.length; i++){
			//check to see if this category is currently expanded
			if (elementMap.containsKey(categories[i].getName())){
				expandable.add(categories[i]);
			}
		}
		return (Object[]) expandable.toArray(new Object[expandable.size()]);
	}

	private AbstractHistoryCategory[] sortRevisions(IFileRevision[] revisions, IProgressMonitor monitor) {
		
		try {
			monitor.beginTask(null, 100);
			//Create the 4 categories
			DateHistoryCategory[] tempCategories = new DateHistoryCategory[4];
			//Get a calendar instance initialized to the current time
			Calendar currentCal = Calendar.getInstance();
			tempCategories[0] = new DateHistoryCategory(TeamUIMessages.HistoryPage_Today, currentCal, null);
			//Get yesterday 
			Calendar yesterdayCal = Calendar.getInstance();
			yesterdayCal.roll(Calendar.DAY_OF_YEAR, -1);
			tempCategories[1] = new DateHistoryCategory(TeamUIMessages.HistoryPage_Yesterday, yesterdayCal, null);
			//Get this month
			Calendar monthCal = Calendar.getInstance();
			monthCal.set(Calendar.DAY_OF_MONTH, 1);
			tempCategories[2] = new DateHistoryCategory(TeamUIMessages.HistoryPage_ThisMonth, monthCal, yesterdayCal);
			//Everything before after week is previous
			tempCategories[3] = new DateHistoryCategory(TeamUIMessages.HistoryPage_Previous, null, monthCal);
		
			ArrayList finalCategories = new ArrayList();
			for (int i = 0; i<tempCategories.length; i++){
				tempCategories[i].collectFileRevisions(revisions, false);
				if (tempCategories[i].hasRevisions())
					finalCategories.add(tempCategories[i]);
			}
			
			if (finalCategories.size() == 0){
				//no revisions found for the current mode, so add a message category
				finalCategories.add(getErrorMessage());
			}
			
			return (AbstractHistoryCategory[])finalCategories.toArray(new AbstractHistoryCategory[finalCategories.size()]);
		} finally {
			monitor.done();
		}
	}
	
	private MessageHistoryCategory getErrorMessage(){
		MessageHistoryCategory messageCategory = new MessageHistoryCategory(getNoChangesMessage());
		return messageCategory;
	}

	protected String getNoChangesMessage() {
		return TeamUIMessages.LocalHistoryPage_NoRevisionsFound;
	}

	/**
	 * Get the most recent revision of selected element.
	 * 
	 * @return current revision of selected element. It can be null.
	 */
	public IFileRevision getCurrentFileRevision() {
		IStructuredSelection ssl = (IStructuredSelection) treeViewer.getSelection();
		Object tempRevision = ssl.getFirstElement();
		if (tempRevision instanceof IFileRevision) {
			IFileRevision revision = (IFileRevision) tempRevision;
			try {
				// retrieve current revision
				IPath path = revision.getStorage(null).getFullPath();
				return new LocalFileRevision(ResourceUtil.getWorkspaceFile(path));
			} catch (CoreException e) {
				// do nothing
				return null;
			}
		}
		 
		return null;
	}
	
	private Action getContextMenuAction(String title, final boolean needsProgressDialog, final IWorkspaceRunnable action) {
		return new Action(title) {
			public void run() {
				try {
					if (files == null) return;
					ISelection selection = treeViewer.getSelection();
					if (!(selection instanceof IStructuredSelection)) return;
					IStructuredSelection ss = (IStructuredSelection)selection;
					Object o = ss.getFirstElement();
					
					if (o instanceof AbstractHistoryCategory)
						return;
					
					currentSelection = (IFileRevision)o;
					if(needsProgressDialog) {
						PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								try {				
									action.run(monitor);
								} catch (CoreException e) {
									throw new InvocationTargetException(e);
								}
							}
						});
					} else {
						try {				
							action.run(null);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}							
				} catch (InvocationTargetException e) {
					IHistoryPageSite parentSite = getHistoryPageSite();
					Utils.handleError(parentSite.getShell(), e, null, null);
				} catch (InterruptedException e) {
					// Do nothing
				}
			}
			
			public boolean isEnabled() {
				ISelection selection = treeViewer.getSelection();
				if (!(selection instanceof IStructuredSelection)) return false;
				IStructuredSelection ss = (IStructuredSelection)selection;
				if(ss.size() != 1) return false;
				return true;
			}
		};
	}
	
	private boolean confirmOverwrite(IFile file) {
		if (file != null && file.exists()) {
			String title = TeamUIMessages.LocalHistoryPage_OverwriteTitle;
			String msg = TeamUIMessages.LocalHistoryPage_OverwriteMessage;
			IHistoryPageSite parentSite = getHistoryPageSite();
			final MessageDialog dialog = new MessageDialog(parentSite.getShell(), title, null, msg, MessageDialog.QUESTION, 
					new String[] {IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
			final int[] result = new int[1];
			parentSite.getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					result[0] = dialog.open();
				}
			});
			if (result[0] != 0) {
				// cancel
				return false;
			}
		}
		return true;
	}
	
	public void setClickAction(boolean compare) {
		compareMode = compare ? ON : OFF;
		if (compareModeAction != null)
			compareModeAction.setChecked(compareMode == ON);
	}
}
