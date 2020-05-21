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
package io.jmix.ui.exception;

import com.vaadin.server.ErrorEvent;
import io.jmix.core.BeanLocator;
import io.jmix.core.common.util.ReflectionHelper;
import io.jmix.core.impl.BeanLocatorAware;
import io.jmix.ui.App;
import io.jmix.ui.AppUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.OrderComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class that holds the collection of exception handlers and delegates unhandled exception processing to them. Handlers
 * form the chain of responsibility.
 *
 * <p>A set of exception handlers is configured by defining <code>ExceptionHandlersConfiguration</code> beans
 * in spring.xml. If a project needs specific handlers, it should define a bean of such type with its own
 * <strong>id</strong>, e.g. <code>refapp_ExceptionHandlersConfiguration</code></p>
 *
 * <p>An instance of this class is bound to {@link App}.</p>
 */
public class ExceptionHandlers {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlers.class);

    protected App app;
    protected BeanLocator beanLocator;

    protected List<ExceptionHandler> handlers = new ArrayList<>();
    protected List<UiExceptionHandler> genericHandlers = new ArrayList<>();

    protected ExceptionHandler defaultHandler;

    public ExceptionHandlers(App app, BeanLocator beanLocator) {
        this.app = app;
        this.beanLocator = beanLocator;
        this.defaultHandler = new DefaultExceptionHandler();
    }

    /**
     * @return default exception handler which is used when none of registered handlers have handled an exception
     */
    public ExceptionHandler getDefaultHandler() {
        return defaultHandler;
    }

    /**
     * Set the default handler instead of initialized in constructor.
     *
     * @param defaultHandler default handler instance
     */
    public void setDefaultHandler(ExceptionHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    /**
     * Adds new Web-level handler if it is not yet registered.
     *
     * @param handler handler instance
     */
    public void addHandler(ExceptionHandler handler) {
        if (!handlers.contains(handler)) {
            handlers.add(handler);
        }
    }

    /**
     * Adds new GUI-level handler if it is not yet registered.
     *
     * @param handler handler instance
     */
    public void addHandler(UiExceptionHandler handler) {
        if (!genericHandlers.contains(handler)) {
            genericHandlers.add(handler);
        }
    }

    /**
     * Delegates exception handling to registered handlers.
     *
     * @param event error event generated by Vaadin
     */
    public void handle(ErrorEvent event) {
        for (ExceptionHandler handler : handlers) {
            if (handler.handle(event, app)) {
                return;
            }
        }

        AppUI ui = AppUI.getCurrent();
        if (ui != null) {
            for (UiExceptionHandler handler : genericHandlers) {
                if (handler.handle(event.getThrowable(), ui)) {
                    return;
                }
            }
        }
        defaultHandler.handle(event, app);
    }

    /**
     * Create all Web handlers defined by <code>ExceptionHandlersConfiguration</code> beans in spring.xml and
     * GUI handlers defined as Spring-beans.
     */
    public void createByConfiguration() {
        removeAll();

        // Web handlers
        Map<String, ExceptionHandlersConfiguration> map = beanLocator.getAll(ExceptionHandlersConfiguration.class);

        // Project-level handlers must run before platform-level
        List<ExceptionHandlersConfiguration> configurations = new ArrayList<>(map.values());
        Collections.reverse(configurations);

        for (ExceptionHandlersConfiguration conf : configurations) {
            for (Class aClass : conf.getHandlerClasses()) {
                try {
                    ExceptionHandler handler = ReflectionHelper.<ExceptionHandler>newInstance(aClass);

                    if (handler instanceof BeanLocatorAware) {
                        ((BeanLocatorAware) handler).setBeanLocator(beanLocator);
                    }

                    addHandler(handler);
                } catch (NoSuchMethodException e) {
                    log.error("Unable to instantiate {}", aClass, e);
                }
            }
        }

        // GUI handlers
        Map<String, UiExceptionHandler> handlerMap = beanLocator.getAll(UiExceptionHandler.class);

        List<UiExceptionHandler> handlers = new ArrayList<>(handlerMap.values());
        handlers.sort(new OrderComparator());

        for (UiExceptionHandler handler : handlers) {
            addHandler(handler);
        }
    }

    /**
     * Remove all handlers.
     */
    public void removeAll() {
        handlers.clear();
        genericHandlers.clear();
    }
}