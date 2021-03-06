/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.tenant.templates;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;

import com.sitewhere.common.MarshalUtils;
import com.sitewhere.microservice.multitenant.MicroserviceTenantEngine;
import com.sitewhere.microservice.zookeeper.ZkUtils;
import com.sitewhere.rest.model.tenant.TenantTemplate;
import com.sitewhere.server.lifecycle.LifecycleComponent;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.microservice.multitenant.ITenantTemplate;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;
import com.sitewhere.spi.server.lifecycle.LifecycleComponentType;
import com.sitewhere.tenant.spi.microservice.ITenantManagementMicroservice;
import com.sitewhere.tenant.spi.templates.ITenantTemplateManager;

/**
 * Manages templates that can be used to create new tenants.
 * 
 * @author Derek
 */
public class TenantTemplateManager extends LifecycleComponent implements ITenantTemplateManager {

    /** Folder that contains default content shared by all tenants */
    private static final String DEFAULT_TENANT_CONTENT_FOLDER = "default";

    /** Map of templates by template id */
    private Map<String, ITenantTemplate> templatesById = new HashMap<String, ITenantTemplate>();

    public TenantTemplateManager() {
	super(LifecycleComponentType.TenantTemplateManager);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.server.lifecycle.LifecycleComponent#start(com.sitewhere.spi
     * .server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void start(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	Map<String, ITenantTemplate> updated = new HashMap<String, ITenantTemplate>();

	// Loop through tenant folders and pull templates.
	File root = ((ITenantManagementMicroservice<?>) getMicroservice()).getTenantTemplatesRoot();
	File[] folders = root.listFiles(File::isDirectory);
	for (File folder : folders) {
	    File tfile = new File(folder, MicroserviceTenantEngine.TENANT_TEMPLATE_PATH);
	    if (tfile.exists()) {
		InputStream input;
		try {
		    input = new FileInputStream(tfile);
		    byte[] content = IOUtils.toByteArray(input);
		    TenantTemplate template = MarshalUtils.unmarshalJson(content, TenantTemplate.class);
		    updated.put(template.getId(), template);
		} catch (IOException e) {
		    getLogger().warn("Unable to unmarshal template.", e);
		}
	    }
	}
	synchronized (templatesById) {
	    templatesById.clear();
	    templatesById.putAll(updated);
	}

	getLogger().info("Tenant template manager found the following templates:");
	for (ITenantTemplate template : getTenantTemplates()) {
	    getLogger().info("[" + template.getId() + "] " + template.getName());
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.server.tenant.ITenantTemplateManager#getTenantTemplates ()
     */
    @Override
    public List<ITenantTemplate> getTenantTemplates() throws SiteWhereException {
	List<ITenantTemplate> list = new ArrayList<>();
	list.addAll(getTemplatesById().values());

	// Sort by template name.
	list.sort(new Comparator<ITenantTemplate>() {

	    @Override
	    public int compare(ITenantTemplate o1, ITenantTemplate o2) {
		return o1.getName().compareTo(o2.getName());
	    }
	});
	return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.tenant.spi.templates.ITenantTemplateManager#
     * copyTemplateContentsToZk(java.lang.String,
     * org.apache.curator.framework.CuratorFramework, java.lang.String)
     */
    @Override
    public void copyTemplateContentsToZk(String templateId, CuratorFramework curator, String tenantPath)
	    throws SiteWhereException {
	ITenantTemplate template = getTemplatesById().get(templateId);
	if (template == null) {
	    throw new SiteWhereException("Tenant template not found: " + templateId);
	}

	File root = ((ITenantManagementMicroservice<?>) getMicroservice()).getTenantTemplatesRoot();

	// Copy default content shared by all tenants.
	File defaultFolder = new File(root, DEFAULT_TENANT_CONTENT_FOLDER);
	if (!defaultFolder.exists()) {
	    throw new SiteWhereException("Default folder not found at '" + defaultFolder.getAbsolutePath() + "'.");
	}
	ZkUtils.copyFolderRecursivelytoZk(curator, tenantPath, defaultFolder, defaultFolder);

	// Copy template contents on top of default.
	File templateFolder = new File(root, templateId);
	if (!templateFolder.exists()) {
	    throw new SiteWhereException(
		    "Tenant template folder not found at '" + templateFolder.getAbsolutePath() + "'.");
	}
	ZkUtils.copyFolderRecursivelytoZk(curator, tenantPath, templateFolder, templateFolder);
    }

    protected Map<String, ITenantTemplate> getTemplatesById() {
	return templatesById;
    }

    protected void setTemplatesById(Map<String, ITenantTemplate> templatesById) {
	this.templatesById = templatesById;
    }
}