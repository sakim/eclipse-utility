package kr.pragmatic.history.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IContributorResourceAdapter;

/**
 * @author sakim
 *
 */
public class ResourceUtil {
	
	public static IFile[] getFiles(Object[] elements) {
		Set/*<IFile>*/ fileSet = new HashSet/*<IFile>*/();
		
		for (int i = 0; i < elements.length; i++) {
			IFile[] files = getFiles(elements[i]);
			fileSet.addAll(Arrays.asList(files));
		}
		
		return (IFile[]) fileSet.toArray(new IFile[fileSet.size()]);
	}
	
	public static IFile[] getFiles(Object o) {
		List/*<IFile>*/ fileList = new ArrayList/*<IFile>*/();
		
		if (o instanceof IProject) {
			try {
				IResource[] resources = ((IProject) o).members();
				return getFiles(resources);
			} catch (CoreException e) {
				// ignore
			}
		}
		
		IFolder folder = null;
		
		folder = getFolder(o);
		
		if (folder != null) {
			try {
				IResource[] resources = folder.members();
				
				for (int i = 0; i < resources.length; i++) {
					// recursive
					IFile[] files = getFiles(resources[i]);
					fileList.addAll(Arrays.asList(files));
				}
			} catch (CoreException e) {
				// ignore
			}
		} else {
			IFile file = getFile(o);
			
			if (file != null && !isHidden(file))
				fileList.add(file);
		}
		
		return (IFile[]) fileList.toArray(new IFile[fileList.size()]);
	}
	
	private static boolean isHidden(IFile file) {
		return file.getName().startsWith(".")
			|| (file.getFileExtension() != null && file.getFileExtension().equalsIgnoreCase("class"));
	}

	private static IFolder getFolder(Object o) {
		IFolder folder = null;
		
		if (o instanceof IFolder) {
			folder = (IFolder) o;
		} else if (o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) o;
			folder = (IFolder)adaptable.getAdapter(IFolder.class);
		}
		return folder;
	}
	
	public static IFile getFile(Object o) {
		IFile file = null;
		if (o instanceof IFile) {
			file = (IFile) o;
		} else if (o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) o;
			file = (IFile)adaptable.getAdapter(IFile.class);
		}
		
		return file;
	}
	
	/**
	 * Returns the list of resources contained in the given elements.
	 * @param elements
	 * @return the list of resources contained in the given elements.
	 */
	public static IResource[] getResources(Object[] elements) {
		List resources = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			IResource resource = getResource(elements[i]);
			
			if (resource != null)
				resources.add(resource);
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	public static IResource getResource(Object o) {
		IResource resource = null;
		if (o instanceof IResource) {
			resource = (IResource) o;
		} else if (o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) o;
			resource = (IResource)adaptable.getAdapter(IResource.class);
			if (resource == null) {
				IContributorResourceAdapter adapter = (IContributorResourceAdapter)adaptable.getAdapter(IContributorResourceAdapter.class);
				if (adapter != null)
					resource = adapter.getAdaptedResource(adaptable);
			}
		}
		return resource;
	}
	
	public static IFile getWorkspaceFile(IPath path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		if (root == null)
			return null;
		
		return root.getFile(path);
	}
	
}
