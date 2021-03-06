/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.grpc.client.tenant;

import com.sitewhere.grpc.client.ApiDemux;
import com.sitewhere.grpc.client.spi.client.ITenantManagementApiChannel;
import com.sitewhere.grpc.client.spi.client.ITenantManagementApiDemux;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.microservice.MicroserviceIdentifier;

/**
 * Demultiplexes tenant management requests across one or more API channels.
 * 
 * @author Derek
 *
 * @param <IAssetManagementApiChannel>
 */
public class TenantManagementApiDemux extends ApiDemux<ITenantManagementApiChannel<?>>
	implements ITenantManagementApiDemux {

    /*
     * @see com.sitewhere.grpc.client.spi.IApiDemux#getTargetIdentifier()
     */
    @Override
    public String getTargetIdentifier() {
	return MicroserviceIdentifier.TenantManagement.getPath();
    }

    /*
     * @see
     * com.sitewhere.grpc.model.spi.IApiDemux#createApiChannel(java.lang.String)
     */
    @Override
    public ITenantManagementApiChannel<?> createApiChannel(String host) throws SiteWhereException {
	return new CachedTenantManagementApiChannel(this, host, getMicroservice().getInstanceSettings().getGrpcPort());
    }
}