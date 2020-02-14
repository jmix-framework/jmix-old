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

package io.jmix.ui.components.data.options;

import io.jmix.core.AppBeans;
import io.jmix.core.Metadata;
import io.jmix.core.commons.events.Subscription;
import io.jmix.core.commons.events.sys.VoidSubscription;
import io.jmix.core.entity.Entity;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.ui.components.data.Options;
import io.jmix.ui.components.data.meta.EntityOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Options based on a simple collection that contains entities.
 *
 * @param <E> entity type
 */
public class ListEntityOptions<E extends Entity> extends ListOptions<E> implements Options<E>, EntityOptions<E> {

    private static final Logger log = LoggerFactory.getLogger(ListEntityOptions.class);

    protected E selectedItem = null;

    public ListEntityOptions(List<E> options) {
        super(options);
    }

    @Override
    public List<E> getItemsCollection() {
        return (List<E>) super.getItemsCollection();
    }

    @Override
    public void setSelectedItem(E item) {
        this.selectedItem = item;
    }

    public E getSelectedItem() {
        return selectedItem;
    }

    @Override
    public boolean containsItem(E item) {
        return getItemsCollection().contains(item);
    }

    @Override
    public void updateItem(E item) {
        // do nothing
        log.debug("The 'updateItem' method is ignored, because underlying collection may be unmodifiable");
    }

    @Override
    public void refresh() {
        // do nothing
        log.debug("The 'refresh' method is ignored because the underlying collection contains static data");
    }

    @Override
    public Subscription addValueChangeListener(Consumer<ValueChangeEvent<E>> listener) {
        return VoidSubscription.INSTANCE;
    }

    @Override
    public MetaClass getEntityMetaClass() {
        Metadata metadata = AppBeans.get(Metadata.NAME);
        MetaClass metaClass = null;
        if (selectedItem != null) {
            metaClass = metadata.getClass(selectedItem);
        } else {
            List<E> itemsCollection = getItemsCollection();
            if (!itemsCollection.isEmpty()) {
                metaClass = metadata.getClass(itemsCollection.get(0));
            }
        }
        return metaClass;
    }
}