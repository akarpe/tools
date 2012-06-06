/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 *  All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *
 * @author bfitzpat
 ******************************************************************************/
package org.switchyard.tools.ui.editor.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.soa.sca.sca1_1.model.sca.Interface;
import org.eclipse.soa.sca.sca1_1.model.sca.JavaInterface;
import org.eclipse.soa.sca.sca1_1.model.sca.Service;
import org.eclipse.soa.sca.sca1_1.model.sca.WSDLPortType;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.util.WSDLResourceImpl;
import org.switchyard.tools.ui.editor.impl.SwitchyardSCAEditor;

/**
 * @author bfitzpat
 *
 */
public final class InterfaceOpsUtil {

    private  InterfaceOpsUtil() {
        // empty constructor
    }

    /**
     * @param intfc WSDL interface
     * @return String array of operation names
     */
    private static String[] getOperationsForWSDLInterface(WSDLPortType intfc) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("");
        IFile modelFile = SwitchyardSCAEditor.getActiveEditor().getModelFile();
        IProject project = modelFile.getProject();
        String wsdlToFind = intfc.getInterface();
        String wsdlPathStr = null;
        int indexOfHash = wsdlToFind.indexOf('#');
        if (indexOfHash > -1) {
            wsdlPathStr = wsdlToFind.substring(0, indexOfHash);
        } else {
            wsdlPathStr = wsdlToFind;
        }
        String portBreak = "#wsdl.porttype(";
        int portStart = wsdlToFind.indexOf(portBreak) + portBreak.length();
        int portEnd = wsdlToFind.lastIndexOf(')');
        String portTypeStr = wsdlToFind.substring(portStart, portEnd);
        IPath wsdlPath = modelFile.getParent().getParent().getProjectRelativePath();
        wsdlPath = wsdlPath.removeLastSegments(1);
        wsdlPath = wsdlPath.append(wsdlPathStr);
        if (project.exists(wsdlPath)) {
            final IResource wsdlFile = project.findMember(wsdlPath);
            final Definition[] holder = new Definition[1];
            BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                public void run() {
                    try {
                        ResourceSet resourceSet = new ResourceSetImpl();
                        WSDLResourceImpl resource = (WSDLResourceImpl) resourceSet.getResource(
                                URI.createPlatformResourceURI(wsdlFile.getFullPath().toString(), true), true);
                        holder[0] = resource.getDefinition();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            Definition definition = holder[0];
            @SuppressWarnings("unchecked")
            Map<?, PortType> portTypes = definition.getPortTypes();
            Collection<PortType> ports = portTypes.values();
            for (PortType portType : ports) {
                if (portType.getQName().getLocalPart().equals(portTypeStr)) {
                    @SuppressWarnings("unchecked")
                    List<Operation> ops = portType.getOperations();
                    for (Operation operation : ops) {
                        String opName = operation.getName();
                        list.add(opName);
                    }
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * @param intfc Java interface
     * @return String array of operation names
     */
    private static String[] getOperationsForJavaInterface(JavaInterface intfc) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("");
        IFile modelFile = SwitchyardSCAEditor.getActiveEditor().getModelFile();
        IProject project = modelFile.getProject();
        String classToFind = intfc.getInterface();
        IJavaProject javaProject = JavaCore.create(project);
        try {
            IType findType = javaProject.findType(classToFind);
            if (findType != null) {
                IMethod[] methods = findType.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    IMethod method = methods[i];
                    list.add(method.getElementName());
                }
            }
        } catch (JavaModelException e1) {
            e1.fillInStackTrace();
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * @param svc Service to check interface for
     * @return String array of operation names
     */
    public static String[] gatherOperations(Service svc) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("");
        Interface intfc = svc.getInterface();
        if (intfc == null) {
            if (svc.getPromote() != null) {
                intfc = svc.getPromote().getInterface();
            }
        }
        if (intfc != null && intfc instanceof JavaInterface) {
            JavaInterface javaIntfc = (JavaInterface) intfc;
            String[] ops = getOperationsForJavaInterface(javaIntfc);
            return ops;
        } else if (intfc != null && intfc instanceof WSDLPortType) {
            WSDLPortType wsdlIntfc = (WSDLPortType) intfc;
            String[] ops = getOperationsForWSDLInterface(wsdlIntfc);
            return ops;
        }
        
        return list.toArray(new String[list.size()]);
    }
}
