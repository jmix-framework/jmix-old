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

package io.jmix.ui.component.impl;

import com.vaadin.shared.Registration;
import io.jmix.core.*;
import io.jmix.core.common.event.Subscription;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.ui.component.*;
import io.jmix.ui.executor.BackgroundTask;
import io.jmix.ui.executor.BackgroundTaskHandler;
import io.jmix.ui.executor.BackgroundWorker;
import io.jmix.ui.executor.TaskLifeCycle;
import io.jmix.ui.icon.IconResolver;
import io.jmix.ui.icon.JmixIcon;
import io.jmix.ui.model.*;
import io.jmix.ui.model.impl.WeakCollectionChangeListener;
import io.jmix.ui.screen.Screen;
import io.jmix.ui.screen.UiControllerUtils;
import io.jmix.ui.widget.JmixPagination;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.jmix.core.common.util.Preconditions.checkNotNullArgument;

public class WebPagination extends WebAbstractComponent<JmixPagination> implements Pagination {

    protected static final String PAGINATION_STYLENAME = "c-pagination";
    protected static final String PAGINATION_COUNT_NUMBER_STYLENAME = "c-pagination-count-number";

    private static final Logger log = LoggerFactory.getLogger(WebPagination.class);

    protected Messages messages;
    protected DataManager dataManager;
    protected BackgroundWorker backgroundWorker;

    protected WebPagination.Adapter adapter;
    protected BaseCollectionLoader loader;

    protected boolean refreshing;
    protected Pagination.State state;
    protected Pagination.State lastState;
    protected int start;
    protected int size;
    protected boolean samePage;

    protected boolean autoLoad;
    protected BackgroundTaskHandler<Integer> rowsCountTaskHandler;

    protected Registration onLinkClickRegistration;
    protected Registration onPrevClickRegistration;
    protected Registration onNextClickRegistration;
    protected Registration onFirstClickRegistration;
    protected Registration onLastClickRegistration;
    protected Function<DataLoadContext, Long> totalCountDelegate;

    public WebPagination() {
        component = new JmixPagination();
        component.setStyleName(PAGINATION_STYLENAME);

        //hide all buttons. They will become visible after data is loaded
        component.getCountButton().setVisible(false);
        component.getPrevButton().setVisible(false);
        component.getNextButton().setVisible(false);
        component.getFirstButton().setVisible(false);
        component.getLastButton().setVisible(false);
    }

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    @Autowired
    public void setIconResolver(IconResolver iconResolver) {
        component.getFirstButton().setIcon(iconResolver.getIconResource(JmixIcon.ANGLE_DOUBLE_LEFT.source()));
        component.getPrevButton().setIcon(iconResolver.getIconResource(JmixIcon.ANGLE_LEFT.source()));
        component.getNextButton().setIcon(iconResolver.getIconResource(JmixIcon.ANGLE_RIGHT.source()));
        component.getLastButton().setIcon(iconResolver.getIconResource(JmixIcon.ANGLE_DOUBLE_RIGHT.source()));
    }

    @Autowired
    public void setBackgroundWorker(BackgroundWorker backgroundWorker) {
        this.backgroundWorker = backgroundWorker;
    }

    @Autowired
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public String getStyleName() {
        return StringUtils.normalizeSpace(super.getStyleName().replace(PAGINATION_STYLENAME, ""));
    }

    @Override
    public void setLoaderTarget(BaseCollectionLoader loader) {
        checkNotNullArgument(loader);

        this.loader = loader;

        if (adapter != null) {
            adapter.unbind();
        }

        adapter = createAdapter();

        initButtonListeners();
    }

    @Nullable
    @Override
    public BaseCollectionLoader getLoaderTarget() {
        return loader;
    }

    @Override
    public boolean getAutoLoad() {
        return autoLoad;
    }

    @Override
    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
    }

    @Nullable
    @Override
    public Function<DataLoadContext, Long> getTotalCountDelegate() {
        return totalCountDelegate;
    }

    @Override
    public void setTotalCountDelegate(Function<DataLoadContext, Long> countDelegate) {
        this.totalCountDelegate = countDelegate;
    }

    @Override
    public Subscription addBeforeRefreshListener(Consumer<BeforeRefreshEvent> listener) {
        return getEventHub().subscribe(BeforeRefreshEvent.class, listener);
    }

    @Override
    public void setButtonsAlignment(ButtonsAlignment position) {
        component.setButtonsAlignment(position);
    }

    @Override
    public ButtonsAlignment getButtonsAlignment() {
        return component.getButtonsAlignment();
    }

    protected void initButtonListeners() {
        unregisterListeners();
        onLinkClickRegistration = component.getCountButton().addClickListener(event -> onLinkClick());
        onPrevClickRegistration = component.getPrevButton().addClickListener(event -> onPrevClick());
        onNextClickRegistration = component.getNextButton().addClickListener(event -> onNextClick());
        onFirstClickRegistration = component.getFirstButton().addClickListener(event -> onFirstClick());
        onLastClickRegistration = component.getLastButton().addClickListener(event -> onLastClick());
    }

    protected void unregisterListeners() {
        if (onLinkClickRegistration != null)
            onLinkClickRegistration.remove();

        if (onPrevClickRegistration != null)
            onPrevClickRegistration.remove();

        if (onNextClickRegistration != null)
            onNextClickRegistration.remove();

        if (onFirstClickRegistration != null)
            onFirstClickRegistration.remove();

        if (onLastClickRegistration != null)
            onLastClickRegistration.remove();
    }

    protected Adapter createAdapter() {
        return new LoaderAdapter(loader);
    }

    protected void onLinkClick() {
        showRowsCountValue(adapter.getCount());
    }

    protected void onPrevClick() {
        int firstResult = adapter.getFirstResult();
        int newStart = adapter.getFirstResult() - adapter.getMaxResults();
        adapter.setFirstResult(newStart < 0 ? 0 : newStart);
        if (refreshData()) {
            // todo rp table scroll to first item
            /*if (target instanceof WebAbstractTable) {
                resetCurrentDataPage((Table) target);
            }*/
        } else {
            adapter.setFirstResult(firstResult);
        }
    }

    protected void onNextClick() {
        int firstResult = adapter.getFirstResult();
        adapter.setFirstResult(adapter.getFirstResult() + adapter.getMaxResults());
        if (refreshData()) {
            if (state == Pagination.State.LAST && size == 0) {
                adapter.setFirstResult(firstResult);
                int maxResults = adapter.getMaxResults();
                adapter.setMaxResults(maxResults + 1);
                refreshData();
                adapter.setMaxResults(maxResults);
            }
            // todo rp table scroll to first item
            /*if (target instanceof WebAbstractTable) {
                resetCurrentDataPage((Table) target);
            }*/
        } else {
            adapter.setFirstResult(firstResult);
        }
    }

    protected void onFirstClick() {
        int firstResult = adapter.getFirstResult();
        adapter.setFirstResult(0);
        if (refreshData()) {
            // todo rp table scroll to first item
            /*if (target instanceof WebAbstractTable) {
                resetCurrentDataPage((Table) target);
            }*/
        } else {
            adapter.setFirstResult(firstResult);
        }
    }

    protected void onLastClick() {
        int count = adapter.getCount();
        int itemsToDisplay = count % adapter.getMaxResults();
        if (itemsToDisplay == 0) itemsToDisplay = adapter.getMaxResults();

        int firstResult = adapter.getFirstResult();
        adapter.setFirstResult(count - itemsToDisplay);
        if (refreshData()) {
            // todo rp table scroll to first item
            /*if (target instanceof WebAbstractTable) {
                resetCurrentDataPage((Table) target);
            }*/
        } else {
            adapter.setFirstResult(firstResult);
        }
    }

    protected boolean refreshData() {
        if (hasSubscriptions(Pagination.BeforeRefreshEvent.class)) {
            Pagination.BeforeRefreshEvent event = new Pagination.BeforeRefreshEvent(this);

            publish(Pagination.BeforeRefreshEvent.class, event);

            if (event.isRefreshPrevented()) {
                return false;
            }
        }

        refreshing = true;
        try {
            adapter.refresh();
        } finally {
            refreshing = false;
        }

        return true;
    }

    protected void onCollectionChanged() {
        if (adapter == null) {
            return;
        }

        String msgKey;
        size = adapter.size();
        start = 0;

        boolean refreshSizeButton = false;
        if (samePage) {
            state = lastState == null ? Pagination.State.FIRST_COMPLETE : lastState;
            start = adapter.getFirstResult();
            samePage = false;
            refreshSizeButton = Pagination.State.LAST.equals(state);
        } else if ((size == 0 || size < adapter.getMaxResults()) && adapter.getFirstResult() == 0) {
            state = Pagination.State.FIRST_COMPLETE;
            lastState = state;
        } else if (size == adapter.getMaxResults() && adapter.getFirstResult() == 0) {
            state = Pagination.State.FIRST_INCOMPLETE;
            lastState = state;
        } else if (size == adapter.getMaxResults() && adapter.getFirstResult() > 0) {
            state = Pagination.State.MIDDLE;
            start = adapter.getFirstResult();
            lastState = state;
        } else if (size < adapter.getMaxResults() && adapter.getFirstResult() > 0) {
            state = Pagination.State.LAST;
            start = adapter.getFirstResult();
            lastState = state;
        } else {
            state = Pagination.State.FIRST_COMPLETE;
            lastState = state;
        }

        String countValue;
        switch (state) {
            case FIRST_COMPLETE:
                component.getCountButton().setVisible(false);
                component.getPrevButton().setVisible(false);
                component.getNextButton().setVisible(false);
                component.getFirstButton().setVisible(false);
                component.getLastButton().setVisible(false);
                if (size == 1) {
                    msgKey = "table.rowsCount.msg2Singular1";
                } else if (size % 100 > 10 && size % 100 < 20) {
                    msgKey = "table.rowsCount.msg2Plural1";
                } else {
                    switch (size % 10) {
                        case 1:
                            msgKey = "table.rowsCount.msg2Singular";
                            break;
                        case 2:
                        case 3:
                        case 4:
                            msgKey = "table.rowsCount.msg2Plural2";
                            break;
                        default:
                            msgKey = "table.rowsCount.msg2Plural1";
                    }
                }
                countValue = String.valueOf(size);
                break;
            case FIRST_INCOMPLETE:
                component.getCountButton().setVisible(true);
                component.getPrevButton().setVisible(false);
                component.getNextButton().setVisible(true);
                component.getFirstButton().setVisible(false);
                component.getLastButton().setVisible(true);
                msgKey = "table.rowsCount.msg1";
                countValue = countValue(start, size);
                break;
            case MIDDLE:
                component.getCountButton().setVisible(true);
                component.getPrevButton().setVisible(true);
                component.getNextButton().setVisible(true);
                component.getFirstButton().setVisible(true);
                component.getLastButton().setVisible(true);
                msgKey = "table.rowsCount.msg1";
                countValue = countValue(start, size);
                break;
            case LAST:
                component.getCountButton().setVisible(false);
                component.getPrevButton().setVisible(true);
                component.getNextButton().setVisible(false);
                component.getFirstButton().setVisible(true);
                component.getLastButton().setVisible(false);
                msgKey = "table.rowsCount.msg2Plural2";
                countValue = countValue(start, size);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        component.getLabel().setValue(messages.formatMessage("", msgKey, countValue));

        // update visible total count
        if (component.getCountButton().isVisible() && !refreshing || refreshSizeButton) {
            if (autoLoad) {
                loadRowsCount();
            } else {
                component.getCountButton().setCaption(messages.getMessage("table.rowsCount.msg3"));
                component.getCountButton().removeStyleName(PAGINATION_COUNT_NUMBER_STYLENAME);
                component.getCountButton().setEnabled(true);
            }
        }
    }

    protected String countValue(int start, int size) {
        if (size == 0) {
            return String.valueOf(size);
        } else {
            return (start + 1) + "-" + (start + size);
        }
    }

    protected void loadRowsCount() {
        if (rowsCountTaskHandler != null
                && rowsCountTaskHandler.isAlive()) {
            log.debug("Cancel previous rows count task");
            rowsCountTaskHandler.cancel();
            rowsCountTaskHandler = null;
        }
        rowsCountTaskHandler = backgroundWorker.handle(getLoadCountTask());
        rowsCountTaskHandler.execute();
    }

    protected BackgroundTask<Long, Integer> getLoadCountTask() {
        if (getFrame() == null) {
            throw new IllegalStateException("Pagination component is not attached to the Frame");
        }

        Screen screen = UiControllerUtils.getScreen(getFrame().getFrameOwner());
        return new BackgroundTask<Long, Integer>(30, screen) {

            @Override
            public Integer run(TaskLifeCycle<Long> taskLifeCycle) {
                return adapter.getCount();
            }

            @Override
            public void done(Integer result) {
                showRowsCountValue(result);
            }

            @Override
            public void canceled() {
                log.debug("Loading rows count for screen '{}' is canceled", screen);
            }

            @Override
            public boolean handleTimeoutException() {
                log.warn("Time out while loading rows count for screen '{}'", screen);
                return true;
            }
        };
    }

    protected void showRowsCountValue(int count) {
        component.getCountButton().setCaption(String.valueOf(count)); // todo rework with datatype
        component.getCountButton().addStyleName(PAGINATION_COUNT_NUMBER_STYLENAME);
        component.getCountButton().setEnabled(false);
    }

    public interface Adapter {
        void unbind();

        int getFirstResult();

        int getMaxResults();

        void setFirstResult(int startPosition);

        void setMaxResults(int maxResults);

        int getCount();

        int size();

        void refresh();
    }

    @SuppressWarnings("rawtypes")
    protected class LoaderAdapter implements WebPagination.Adapter {

        protected CollectionContainer container;

        protected Consumer<CollectionContainer.CollectionChangeEvent> containerCollectionChangeListener;
        protected WeakCollectionChangeListener weakContainerCollectionChangeListener;

        protected BaseCollectionLoader loader;

        @SuppressWarnings("unchecked")
        public LoaderAdapter(BaseCollectionLoader loader) {
            this.loader = loader;
            this.container = loader.getContainer();

            containerCollectionChangeListener = e -> {
                samePage = CollectionChangeType.REFRESH != e.getChangeType();
                onCollectionChanged();
            };

            weakContainerCollectionChangeListener = new WeakCollectionChangeListener(
                    container, containerCollectionChangeListener);

            onCollectionChanged();
        }

        @Override
        public void unbind() {
            weakContainerCollectionChangeListener.removeItself();
        }

        @Override
        public int getFirstResult() {
            return loader.getFirstResult();
        }

        @Override
        public int getMaxResults() {
            return loader != null ? loader.getMaxResults() : Integer.MAX_VALUE;
        }

        @Override
        public void setFirstResult(int startPosition) {
            if (loader != null)
                loader.setFirstResult(startPosition);
        }

        @Override
        public void setMaxResults(int maxResults) {
            if (loader != null)
                loader.setMaxResults(maxResults);
        }

        @SuppressWarnings("unchecked")
        @Override
        public int getCount() {
            if (loader == null) {
                return container.getItems().size();
            }

            if (loader instanceof CollectionLoader) {
                LoadContext context = ((CollectionLoader) loader).createLoadContext();
                if (totalCountDelegate == null) {
                    return (int) dataManager.getCount(context);
                } else {
                    return Math.toIntExact(totalCountDelegate.apply(context));
                }
            } else if (loader instanceof KeyValueCollectionLoader) {
                ValueLoadContext context = ((KeyValueCollectionLoader) loader).createLoadContext();
                if (totalCountDelegate == null) {
                    QueryTransformer transformer = QueryTransformerFactory.createTransformer(context.getQuery().getQueryString());
                    // TODO it doesn't work for query containing scalars in select
                    transformer.replaceWithCount();
                    context.getQuery().setQueryString(transformer.getResult());
                    context.setProperties(Collections.singletonList("cnt"));
                    List<KeyValueEntity> list = dataManager.loadValues(context);
                    Number count = list.get(0).getValue("cnt");
                    return count == null ? 0 : count.intValue();
                } else {
                    return Math.toIntExact(totalCountDelegate.apply(context));
                }
            } else {
                log.warn("Unsupported loader type: {}", loader.getClass().getName());
                return 0;
            }
        }

        @Override
        public int size() {
            return container.getItems().size();
        }

        @Override
        public void refresh() {
            if (loader != null)
                loader.load();
        }
    }
}