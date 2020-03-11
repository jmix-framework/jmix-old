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

import com.haulmont.cuba.gui.components.TreeTable;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.xml.data.DatasourceLoaderHelper;
import io.jmix.ui.xml.layout.loaders.TreeTableLoader;
import org.dom4j.Element;

@SuppressWarnings("rawtypes")
public class CubaTreeTableLoader extends TreeTableLoader {

    @Override
    public void createComponent() {
        resultComponent = factory.create(TreeTable.NAME);
        loadId(resultComponent, element);
        createButtonsPanel(resultComponent, element);
    }

    @Override
    protected TableDataHolder createTableDataHolder() {
        return new CubaTreeTableDataHolder();
    }

    @Override
    protected boolean initDataContainer(TableDataHolder holder) {
        Element rowsElement = element.element("rows");
        if (rowsElement == null) {
            return false;
        }

        CollectionDatasource datasource = DatasourceLoaderHelper.loadTableDatasource(
                element, rowsElement, context, (ComponentLoaderContext) getComponentContext()
        );

        ((CubaTreeTableDataHolder) holder).setDatasource(datasource);
        holder.setMetaClass(datasource.getMetaClass());
        holder.setFetchPlan(datasource.getView());
        return true;
    }

    @Override
    protected boolean setupDataContainer(TableDataHolder holder) {
        CollectionDatasource datasource = ((CubaTreeTableDataHolder) holder).getDatasource();
        if (datasource == null) {
            return false;
        }
        // todo dynamic attributes
        // addDynamicAttributes(resultComponent, metaClass, datasource, null, availableColumns);
        ((TreeTable) resultComponent).setDatasource(datasource);
        return true;
    }

    protected static class CubaTreeTableDataHolder extends TableDataHolder {

        protected CollectionDatasource datasource;

        public CollectionDatasource getDatasource() {
            return datasource;
        }

        public void setDatasource(CollectionDatasource datasource) {
            this.datasource = datasource;
        }
    }
}