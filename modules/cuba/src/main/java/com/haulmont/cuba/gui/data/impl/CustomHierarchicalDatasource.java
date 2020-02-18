/*
 * Copyright 2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.haulmont.cuba.gui.data.impl;

import io.jmix.core.entity.Entity;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import io.jmix.core.entity.EntityAccessor;

import java.util.Collection;
import java.util.Map;

/**
 * Base class for custom implementations of {@link HierarchicalDatasource}.
 * <ul>
 * <li>In a subclass, implement the {@link #getEntities(Map)} method and return a collection of entities from it.
 * <li>Register your subclass in the {@code datasourceClass} attribute of the datasource XML element.
 * </ul>
 */
@Deprecated
public abstract class CustomHierarchicalDatasource<T extends Entity<K>, K>
        extends HierarchicalDatasourceImpl<T, K> {

    /**
     * Callback method to be implemented in subclasses.
     * @param params    datasource parameters, as described in {@link CollectionDatasource#refresh(Map)}
     * @return          collection of entities to populate the datasource
     */
    protected abstract Collection<T> getEntities(Map<String, Object> params);

    @Override
    protected void loadData(Map<String, Object> params) {
        Collection<T> entities = getEntities(params);

        detachListener(data.values());
        data.clear();

        if (entities != null) {
            for (T entity : entities) {
                data.put(EntityAccessor.<K>getEntityId(entity), entity);
                attachListener(entity);
            }
        }
    }
}
