/*
 * Copyright 2020 Haulmont.
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

package com.haulmont.cuba.gui.xml.layout.loaders;

import com.google.common.base.Strings;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.xml.data.DatasourceLoaderHelper;
import io.jmix.ui.xml.layout.loaders.DataGridLoader;

@SuppressWarnings("rawtypes")
public class CubaDataGridLoader extends DataGridLoader {

    @Override
    protected DataGrid createComponentInternal() {
        return factory.create(DataGrid.NAME);
    }

    @Override
    protected DataGridDataHolder createDataGridDataHolder() {
        return new CubaDataGridDataHolder();
    }

    @Override
    protected boolean initDataContainer(DataGridDataHolder holder) {
        String datasourceId = element.attributeValue("datasource");
        if (Strings.isNullOrEmpty(datasourceId)) {
            return false;
        }

        CollectionDatasource datasource = DatasourceLoaderHelper.loadListComponentDatasource(
                datasourceId, context, (ComponentLoaderContext) getComponentContext()
        );

        ((CubaDataGridDataHolder) holder).setDatasource(datasource);
        holder.setMetaClass(datasource.getMetaClass());
        holder.setFetchPlan(datasource.getView());

        return true;
    }

    @Override
    protected boolean setupDataContainer(DataGridDataHolder holder) {
        CollectionDatasource datasource = ((CubaDataGridDataHolder) holder).getDatasource();
        if (datasource == null) {
            return false;
        }

        // todo dynamic attributes
        // addDynamicAttributes(resultComponent, metaClass, datasource, null, availableColumns);

        ((DataGrid) resultComponent).setDatasource(datasource);
        return true;
    }

    protected static class CubaDataGridDataHolder extends DataGridDataHolder {

        protected CollectionDatasource datasource;

        public CollectionDatasource getDatasource() {
            return datasource;
        }

        public void setDatasource(CollectionDatasource datasource) {
            this.datasource = datasource;
        }
    }
}