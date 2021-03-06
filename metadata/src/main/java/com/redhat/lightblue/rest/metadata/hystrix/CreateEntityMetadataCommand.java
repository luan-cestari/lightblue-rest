/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.metadata.hystrix;

import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.rest.metadata.RestMetadataConstants;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class create the metadata if it doesn't exist, and updates it if entity schema exists, but version doesn't
 *
 * @author nmalik
 * @author bserdar
 */
public class CreateEntityMetadataCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetEntityRolesCommand.class);
    private final String entity;
    private final String version;
    private final String data;

    public CreateEntityMetadataCommand(String clientKey, String entity, String version, String data) {
        this(clientKey, null, entity, version, data);
    }

    public CreateEntityMetadataCommand(String clientKey, Metadata metadata, String entity, String version, String data) {
        super(CreateEntityMetadataCommand.class, clientKey, metadata);
        this.entity = entity;
        this.version = version;
        this.data = data;
    }

    @Override
    protected String run() {
        LOGGER.debug("run: entity={}, version={}", entity, version);
        Error.reset();
        Error.push(getClass().getSimpleName());
        Error.push(entity);
        Error.push(version);
        try {
            EntityMetadata emd = getJsonTranslator().parse(EntityMetadata.class,JsonUtils.json(data));
            if (!emd.getName().equals(entity)) {
                throw Error.get(RestMetadataConstants.ERR_NO_NAME_MATCH, entity);
            }
            if (!emd.getVersion().getValue().equals(version)) {
                throw Error.get(RestMetadataConstants.ERR_NO_VERSION_MATCH, version);
            }

            Metadata md = getMetadata();
            LOGGER.debug("Metadata instance:{}", md);
            // See if entity already exists
            if(md.getEntityInfo(entity)!=null) {
                LOGGER.debug("Entity exists: {}, creating version {}",entity, version);
                md.createNewSchema(emd);
                md.updateEntityInfo(emd.getEntityInfo());
            } else {
                LOGGER.debug("Creating new metadata:{} {}",entity,version);
                md.createNewMetadata(emd);
            }
            emd = md.getEntityMetadata(entity, version);
            return getJSONParser().convert(emd).toString();
        } catch (Error e) {
            return e.toString();
        } catch (Exception e) {
            LOGGER.error("Failure: {}", e);
            return Error.get(RestMetadataConstants.ERR_REST_ERROR, e.toString()).toString();
        }
    }
}
