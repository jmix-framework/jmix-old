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

import com.haulmont.cuba.gui.components.RowsCount;
import com.haulmont.cuba.gui.components.TreeTable;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.xml.data.ComponentLoaderHelper;
import com.haulmont.cuba.gui.xml.data.DatasourceLoaderHelper;
import io.jmix.dynattrui.DynAttrEmbeddingStrategies;
import io.jmix.ui.component.Table;
import io.jmix.ui.component.formatter.Formatter;
import io.jmix.ui.xml.layout.loader.TreeTableLoader;
import org.dom4j.Element;

import java.util.List;

import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
public class CubaTreeTableLoader extends TreeTableLoader {

    @Override
    public void loadComponent() {
        super.loadComponent();

        ComponentLoaderHelper.loadSettingsEnabled((TreeTable) resultComponent, element);
        ComponentLoaderHelper.loadTableValidators(resultComponent, element, context, getClassManager());
    }

    @Override
    @Nullable
    protected Formatter<?> loadFormatter(Element element) {
        return ComponentLoaderHelper.loadFormatter(element, getClassManager(), getContext());
    }

    @Override
    protected CubaTreeTableDataHolder initTableDataHolder() {
        CubaTreeTableDataHolder holder = new CubaTreeTableDataHolder();
        Element rowsElement = element.element("rows");
        if (rowsElement == null) {
            return holder;
        }

        CollectionDatasource datasource = DatasourceLoaderHelper.loadTableDatasource(
                element, rowsElement, context, (ComponentLoaderContext) getComponentContext()
        );

        holder.setDatasource(datasource);
        holder.setMetaClass(datasource.getMetaClass());
        holder.setFetchPlan(datasource.getView());

        return holder;
    }

    @Override
    protected void setupDataContainer(TableDataHolder holder) {
        CollectionDatasource datasource = ((CubaTreeTableDataHolder) holder).getDatasource();
        if (datasource == null) {
            return;
        }
        ((TreeTable) resultComponent).setDatasource(datasource);

        DynAttrEmbeddingStrategies embeddingStrategies = beanLocator.get(DynAttrEmbeddingStrategies.class);
        embeddingStrategies.embedAttributes(resultComponent, getComponentContext().getFrame());
    }

    @Override
    protected void loadTableData() {
        // must be before datasource setting
        ComponentLoaderHelper.loadRowsCount((TreeTable) resultComponent, element, () -> factory.create(RowsCount.NAME));

        super.loadTableData();

        List<Table.Column> columns = resultComponent.getColumns();
        for (io.jmix.ui.component.Table.Column column : columns) {
            ComponentLoaderHelper.loadTableColumnValidators(resultComponent, column, context, getClassManager(), getMessages());
        }
    }

    protected static class CubaTreeTableDataHolder extends TableDataHolder {

        protected CollectionDatasource datasource;

        public CollectionDatasource getDatasource() {
            return datasource;
        }

        public void setDatasource(CollectionDatasource datasource) {
            this.datasource = datasource;
        }

        @Override
        public boolean isContainerLoaded() {
            return super.isContainerLoaded() || datasource != null;
        }
    }
}
