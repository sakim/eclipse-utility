/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package kr.pragmatic.history.internal.ui;

import java.util.Date;


import org.eclipse.compare.IModificationDate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.AbstractHistoryCategory;
import org.eclipse.team.internal.ui.history.DateHistoryCategory;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

import com.ibm.icu.text.DateFormat;

public class LocalHistoryTableProvider {
	
	/* private */ static final int COL_NAME = 0;
	/* private */ static final int COL_DATE = 1;
	
	/* private */ TreeViewer viewer;
	
	private Image localRevImage = null;
	private DateFormat dateFormat;
	
	/**
	 * The Local history label provider.
	 */
	private class LocalHistoryLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider, IFontProvider {
		
		private Image dateImage = null;
		private Font currentRevisionFont = null;
		
		private IPropertyChangeListener themeListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				LocalHistoryTableProvider.this.viewer.refresh();
			}
		};
		
		public LocalHistoryLabelProvider(){
				PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener);
		}
		
		public void dispose() {
			if (dateImage != null){
				dateImage.dispose();
				dateImage = null;
			}
			
			if (localRevImage != null) {
				localRevImage.dispose();
				localRevImage = null;
			}
					
			if (themeListener != null){
				PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
			}
			
			if (currentRevisionFont != null) {
				currentRevisionFont.dispose();
			}
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == COL_NAME) {
				if (element instanceof DateHistoryCategory) {
					if (dateImage == null) {
						ImageDescriptor dateDesc = TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY);
						dateImage = dateDesc.createImage();
					}
					return dateImage;
				}
			}
			
			if (columnIndex == COL_DATE) {
				if (element instanceof DateHistoryCategory) {
					return null;
				}
				
				if (getModificationDate(element) != -1) {
					return getRevisionImage();
				}
			}
			
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			
			if (columnIndex == COL_NAME) {
				if (element instanceof AbstractHistoryCategory){
					return ((AbstractHistoryCategory) element).getName();
				}
				
				if (element instanceof LocalFileRevision) {
					return ((LocalFileRevision) element).getName();
				}
			}
			
			if (columnIndex == COL_DATE) {
				if (element instanceof AbstractHistoryCategory){
					return ""; //$NON-NLS-1$
				}
				
				long date = getModificationDate(element);
				
				if (date != -1) {
					Date dateFromLong = new Date(date);
					return getDateFormat().format(dateFromLong);
				}
			}
			
			return ""; //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		public Color getForeground(Object element) {
			if (element instanceof AbstractHistoryCategory){
				ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				return current.getColorRegistry().get("org.eclipse.team.cvs.ui.fontsandcolors.cvshistorypagecategories"); //$NON-NLS-1$
			}
			
			if (isDeletedEdition(element)) {
				return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			}

			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		public Color getBackground(Object element) {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
		 */
		public Font getFont(Object element) {
			if (element instanceof AbstractHistoryCategory) {
				return getCurrentRevisionFont();
			}
			if (isCurrentEdition(element)) {
				return getCurrentRevisionFont();
			}
			return null;
		}

		private Font getCurrentRevisionFont() {
			if (currentRevisionFont == null) {
				Font defaultFont = JFaceResources.getDefaultFont();
				FontData[] data = defaultFont.getFontData();
				for (int i = 0; i < data.length; i++) {
					data[i].setStyle(SWT.BOLD);
				}
				currentRevisionFont = new Font(viewer.getTree().getDisplay(), data);
			}
			return currentRevisionFont;
		}
	}

	/**
	 * The history sorter
	 */
	private class HistoryComparator extends ViewerComparator {
		private boolean reversed = false;
		private int columnNumber;
		
		// column headings:	"Name" "Revision"
		private int[][] SORT_ORDERS_BY_COLUMN = {
				{COL_NAME, COL_DATE}, /* name, date */
				{COL_DATE, COL_NAME} /* date, name*/
		};

		/**
		 * The constructor.
		 * @param columnNumber 
		 */
		public HistoryComparator(int columnNumber) {
			this.columnNumber = columnNumber;
		}

		/**
		 * Compares two log entries, sorting first by the main column of this sorter,
		 * then by subsequent columns, depending on the column sort order.
		 */
		public int compare(Viewer compareViewer, Object o1, Object o2) {
			
			IFileRevision e1 = adaptToFileRevision(o1);
			IFileRevision e2 = adaptToFileRevision(o2);

			int result = 0;
			
			if (e1 == null || e2 == null) {
				result = super.compare(compareViewer, o1, o2);
			} else {
				int[] columnSortOrder = SORT_ORDERS_BY_COLUMN[columnNumber];
				for (int i = 0; i < columnSortOrder.length; ++i) {
					result = compareColumnValue(columnSortOrder[i], e1, e2);
					if (result != 0)
						break;
				}
			}
			if (reversed)
				result = -result;
			return result;
		}
		
		int compareColumnValue(int columnNumber, IFileRevision e1, IFileRevision e2) {
			switch (columnNumber) {
				case COL_NAME:
					String name1 = e1.getName();
					String name2 = e2.getName();
					
					return name1.compareTo(name2);
				case COL_DATE:
					long date1 = e1.getTimestamp();
					long date2 = e2.getTimestamp();
					if (date1 == date2)
						return 0;
	
					return date1 > date2 ? -1 : 1;
				default:
					return 0;
			}
		}

		/**
		 * Returns the number of the column by which this is sorting.
		 * @return the column number
		 */
		public int getColumnNumber() {
			return columnNumber;
		}

		/**
		 * Returns true for descending, or false
		 * for ascending sorting order.
		 * @return returns true if reversed
		 */
		public boolean isReversed() {
			return reversed;
		}

		/**
		 * Sets the sorting order.
		 * @param newReversed 
		 */
		public void setReversed(boolean newReversed) {
			reversed = newReversed;
		}
	}

	/**
	 * Creates the columns for the history table.
	 */
	private void createColumns(Tree tree, TableLayout layout) {
		SelectionListener headerListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int column = viewer.getTree().indexOf((TreeColumn) e.widget);
				HistoryComparator oldSorter = (HistoryComparator) viewer.getComparator();
				TreeColumn treeColumn = ((TreeColumn)e.widget);
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
					oldSorter.setReversed(!oldSorter.isReversed());
					viewer.getTree().setSortColumn(treeColumn);
					viewer.getTree().setSortDirection(oldSorter.isReversed() ? SWT.DOWN : SWT.UP);
					viewer.refresh();
				} else {
					viewer.getTree().setSortColumn(treeColumn);
					viewer.getTree().setSortDirection(SWT.UP);
					viewer.setComparator(new HistoryComparator(column));
				}
			}
		};
		
		// file name
		TreeColumn nameCol = new TreeColumn(tree, SWT.NONE);
		nameCol.setResizable(true);
		nameCol.setText(HistoryMessages.GenericHistoryTableProvider_FileName);
		nameCol.addSelectionListener(headerListener);
		
		// creation date
		TreeColumn dateCol = new TreeColumn(tree, SWT.NONE);
		dateCol.setResizable(true);
		dateCol.setText(TeamUIMessages.GenericHistoryTableProvider_RevisionTime);
		dateCol.addSelectionListener(headerListener);
		
		layout.addColumnData(new ColumnWeightData(15, true));
		layout.addColumnData(new ColumnWeightData(10, true));
	}
	
	/**
	 * Create a TreeViewer that can be used to display a list of IFile instances.
	 * This method provides the labels and sorter but does not provide a content provider
	 * 
	 * @param parent
	 * @return TableViewer
	 */
	public TreeViewer createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		GridData data = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(data);

		TableLayout layout = new TableLayout();
		tree.setLayout(layout);

		this.viewer = new TreeViewer(tree);
		
		createColumns(tree, layout);

		viewer.setLabelProvider(new LocalHistoryLabelProvider());

		// By default, reverse sort by revision. 
		// If local filter is on sort by date
		HistoryComparator sorter = new HistoryComparator(COL_DATE);
		sorter.setReversed(false);
		viewer.setComparator(sorter);
		
		return viewer;
	}
	
	protected long getModificationDate(Object element) {
		IModificationDate md = (IModificationDate)Utils.getAdapter(element, IModificationDate.class);
		if (md != null)
			return md.getModificationDate();
		if (element instanceof IFileState) {
			IFileState fs = (IFileState) element;
			return fs.getModificationTime();
		}
		if (element instanceof IFile) {
			IFile f = (IFile) element;
			return f.getLocalTimeStamp();
		}
		return -1;
	}
	
	protected boolean isCurrentEdition(Object element) {
		if (element instanceof IFile) {
			return true;
		}
		if (element instanceof IFileState) {
			return false;
		}
		if (element instanceof LocalFileRevision) {
			LocalFileRevision revision = (LocalFileRevision) element;
			
			// only current revision has a "real" current file
			if (revision.getFile() != null)
				return true;
		}
		
		return false;
	}
	
	protected boolean isDeletedEdition(Object element) {
		if (element instanceof IFile) {
			IFile f = (IFile) element;
			return !f.exists();
		}
		return false;
	}

	public Image getRevisionImage() {
		if (localRevImage == null) {
			ImageDescriptor localRevDesc = TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_LOCALREVISION_TABLE);
			localRevImage = localRevDesc.createImage();
		}
		return localRevImage;
	}
	
	public synchronized DateFormat getDateFormat() {
		if (dateFormat == null)
			dateFormat = DateFormat.getInstance();
		return dateFormat;
	}
	
	protected IFileRevision adaptToFileRevision(Object element) {
		// Get the log entry for the provided object
		IFileRevision entry = null;
		if (element instanceof IFileRevision) {
			entry = (IFileRevision) element;
		} else if (element instanceof IAdaptable) {
			entry = (IFileRevision) ((IAdaptable) element).getAdapter(IFileRevision.class);
		} else if (element instanceof AbstractHistoryCategory){
			entry = ((AbstractHistoryCategory) element).getRevisions()[0];
		}
		return entry;
	}
}
