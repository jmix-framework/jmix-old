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

package io.jmix.auditui.screen;

import io.jmix.audit.EntityLog;
import io.jmix.audit.entity.EntityLogAttr;
import io.jmix.audit.entity.EntityLogItem;
import io.jmix.audit.entity.LoggedAttribute;
import io.jmix.audit.entity.LoggedEntity;
import io.jmix.core.*;
import io.jmix.core.Entity;
import io.jmix.core.entity.HasUuid;
import io.jmix.core.entity.User;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import io.jmix.ui.*;
import io.jmix.ui.actions.Action;
import io.jmix.ui.actions.DialogAction;
import io.jmix.ui.components.*;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionLoader;
import io.jmix.ui.model.DataContext;
import io.jmix.ui.screen.LookupComponent;
import io.jmix.ui.screen.*;
import org.apache.commons.lang3.time.DateUtils;

import javax.inject.Inject;
import java.util.*;

@UiController("entityLog.browse")
@UiDescriptor("entity-log-browser.xml")
@LookupComponent("entityLogTable")
public class EntityLogBrowser extends StandardLookup<EntityLogItem> {

    protected static final String SELECT_ALL_CHECK_BOX = "selectAllCheckBox";

    @Inject
    protected Messages messages;
    @Inject
    protected MessageBundle messageBundle;
    @Inject
    protected WindowConfig windowConfig;
    @Inject
    protected Metadata metadata;
    @Inject
    protected TimeSource timeSource;
    @Inject
    protected ReferenceToEntitySupport referenceToEntitySupport;
    @Inject
    protected ExtendedEntities extendedEntities;
    @Inject
    protected EntityLog entityLog;
    @Inject
    protected UiComponents uiComponents;
    @Inject
    protected MetadataTools metadataTools;
    @Inject
    protected Dialogs dialogs;
    @Inject
    protected Notifications notifications;
    @Inject
    protected ScreenBuilders screenBuilders;

    @Inject
    protected CollectionContainer<LoggedEntity> loggedEntityDc;
    @Inject
    protected CollectionLoader<LoggedEntity> loggedEntityDl;
    @Inject
    protected CollectionLoader<EntityLogItem> entityLogDl;
    @Inject
    protected CollectionLoader<User> usersDl;
    @Inject
    protected CollectionContainer<User> usersDc;
    @Inject
    protected CollectionContainer<LoggedAttribute> loggedAttrDc;
    @Inject
    protected CollectionLoader<LoggedAttribute> loggedAttrDl;
    @Inject
    protected LookupField changeTypeField;
    @Inject
    protected LookupField<String> entityNameField;
    @Inject
    protected LookupField<String> userField;
    @Inject
    protected LookupField<String> filterEntityNameField;
    @Inject
    protected DataContext dataContext;
    @Inject
    protected PickerField<Entity> instancePicker;
    @Inject
    protected Table<EntityLogItem> entityLogTable;
    @Inject
    protected GroupTable<LoggedEntity> loggedEntityTable;
    @Inject
    protected Table<EntityLogAttr> entityLogAttrTable;
    @Inject
    protected CheckBox manualCheckBox;
    @Inject
    protected CheckBox autoCheckBox;
    @Inject
    protected VBoxLayout actionsPaneLayout;
    @Inject
    protected ScrollBoxLayout attributesBoxScroll;
    @Inject
    protected DateField tillDateField;
    @Inject
    protected DateField fromDateField;
    @Inject
    protected Button cancelBtn;
    @Inject
    protected CheckBox selectAllCheckBox;

    protected TreeMap<String, String> entityMetaClassesMap;
    protected TreeMap<String, String> usersMap;

    protected List<String> systemAttrsList;

    // allow or not selectAllCheckBox to change values of other checkboxes
    protected boolean canSelectAllCheckboxGenerateEvents = true;

    @Subscribe
    protected void onInit(InitEvent event) {
        entityLogTable.setTextSelectionEnabled(true);
        entityLogAttrTable.setTextSelectionEnabled(true);

        loggedEntityDl.load();

        systemAttrsList = Arrays.asList("createTs", "createdBy", "updateTs", "updatedBy", "deleteTs", "deletedBy", "version", "id");
        Map<String, Object> changeTypeMap = new LinkedHashMap<>();
        changeTypeMap.put(messages.getMessage("createField"), "C");
        changeTypeMap.put(messages.getMessage("modifyField"), "M");
        changeTypeMap.put(messages.getMessage("deleteField"), "D");
        changeTypeMap.put(messages.getMessage("restoreField"), "R");

        entityMetaClassesMap = getEntityMetaClasses();
        usersMap = getUsersMap();
        entityNameField.setOptionsMap(entityMetaClassesMap);
        changeTypeField.setOptionsMap(changeTypeMap);
        userField.setOptionsMap(usersMap);
        filterEntityNameField.setOptionsMap(entityMetaClassesMap);

        disableControls();
        setDateFieldTime();

        instancePicker.setEnabled(false);

        entityNameField.addValueChangeListener(e -> {
            if (entityNameField.isEditable())
                fillAttributes(e.getValue(), null, true);
        });

        loggedEntityDc.addItemChangeListener(e -> {
            if (e.getItem() != null) {
                loggedAttrDl.setParameter("entityId", e.getItem().getId());
                loggedAttrDl.load();
                fillAttributes(e.getItem().getName(), e.getItem(), false);
                checkAllCheckboxes();
            } else {
                setSelectAllCheckBox(false);
                clearAttributes();
            }
        });

        filterEntityNameField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                instancePicker.setEnabled(true);
                MetaClass metaClass = metadata.getSession().getClass(e.getValue());
                instancePicker.setMetaClass(metaClass);
            } else {
                instancePicker.setEnabled(false);
            }
            instancePicker.setValue(null);
        });
        selectAllCheckBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                enableAllCheckBoxes(e.getValue());
            }
        });

        entityLogTable.addGeneratedColumn("entityId", entity -> {
            if (entity.getObjectEntityId() != null) {
                return new Table.PlainTextCell(entity.getObjectEntityId().toString());
            }
            return null;
        }, Table.PlainTextCell.class);
    }

    @Subscribe("instancePicker.lookup")
    public void onInstancePickerLookup(Action.ActionPerformedEvent event) {
        final MetaClass metaClass = instancePicker.getMetaClass();
        if (instancePicker.isEditable()) {
            String currentWindowAlias;

            if (metaClass == null) {
                throw new IllegalStateException("Please specify metaclass or property for PickerField");
            }
            currentWindowAlias = windowConfig.getLookupScreenId(metaClass);

            Screen lookup;
            //if (windowConfig.hasWindow(currentWindowAlias)) {
            lookup = screenBuilders.lookup(metaClass.getJavaClass(), this)
                    .withScreenId(currentWindowAlias)
                    .withSelectHandler(items -> {
                        if (!items.isEmpty()) {
                            Object item = items.iterator().next();
                            instancePicker.setValue((Entity) item);
                        }
                    })
                    .build();
            //} else {
            // TODO entity browser ?
            //}
            lookup.addAfterCloseListener(afterCloseEvent -> instancePicker.focus());
            lookup.show();
        }
    }

    public TreeMap<String, String> getEntityMetaClasses() {
        TreeMap<String, String> options = new TreeMap<>();

        for (MetaClass metaClass : metadataTools.getAllPersistentMetaClasses()) {
            if (extendedEntities.getExtendedClass(metaClass) == null) {
                MetaClass originalMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                String originalName = originalMetaClass.getName();
                Class javaClass = originalMetaClass.getJavaClass();
                if (metadataTools.hasCompositePrimaryKey(metaClass) && !HasUuid.class.isAssignableFrom(javaClass)) {
                    continue;
                }
                String caption = messages.getMessage(javaClass, javaClass.getSimpleName()) + " (" + originalName + ")";
                options.put(caption, originalName);
            }
        }
        return options;
    }

    public TreeMap<String, String> getUsersMap() {
        TreeMap<String, String> options = new TreeMap<>();
        usersDl.load();
        for (User user : usersDc.getItems()) {
            options.put(metadataTools.getInstanceName(user), user.getLogin());
        }
        return options;
    }

    protected void enableControls() {
        loggedEntityTable.setEnabled(false);
        entityNameField.setEditable(false);
        autoCheckBox.setEditable(true);
        manualCheckBox.setEditable(true);
        for (Component c : attributesBoxScroll.getComponents())
            ((CheckBox) c).setEditable(true);
        actionsPaneLayout.setVisible(true);
    }

    protected void disableControls() {
        entityNameField.setEditable(false);
        loggedEntityTable.setEnabled(true);
        autoCheckBox.setEditable(false);
        manualCheckBox.setEditable(false);
        for (Component c : attributesBoxScroll.getComponents())
            ((CheckBox) c).setEditable(false);
        actionsPaneLayout.setVisible(false);
    }

    protected void fillAttributes(String metaClassName, LoggedEntity item, boolean editable) {
        clearAttributes();
        setSelectAllCheckBox(false);

        if (metaClassName != null) {
            MetaClass metaClass = extendedEntities.getEffectiveMetaClass(
                    metadata.getClass(metaClassName));
            List<MetaProperty> metaProperties = new ArrayList<>(metaClass.getProperties());
            selectAllCheckBox.setEditable(editable);
            Set<LoggedAttribute> enabledAttr = null;
            if (item != null)
                enabledAttr = item.getAttributes();
            for (MetaProperty property : metaProperties) {
                if (allowLogProperty(property)) {
                    if (metadataTools.isEmbedded(property)) {
                        MetaClass embeddedMetaClass = property.getRange().asClass();
                        for (MetaProperty embeddedProperty : embeddedMetaClass.getProperties()) {
                            if (allowLogProperty(embeddedProperty)) {
                                addAttribute(enabledAttr,
                                        String.format("%s.%s", property.getName(), embeddedProperty.getName()), editable);
                            }
                        }
                    } else {
                        addAttribute(enabledAttr, property.getName(), editable);
                    }
                }
            }

            //todo DynamicAttributes
//            Collection<CategoryAttribute> attributes = dynamicAttributes.getAttributesForMetaClass(metaClass);
//            if (attributes != null) {
//                for (CategoryAttribute categoryAttribute : attributes) {
//                    MetaPropertyPath propertyPath = DynamicAttributesUtils.getMetaPropertyPath(metaClass, categoryAttribute);
//                    MetaProperty property = propertyPath.getMetaProperty();
//                    if (allowLogProperty(property, categoryAttribute)) {
//                        addAttribute(enabledAttr, property.getName(), editable);
//                    }
//                }
//            }
        }
    }

    protected void addAttribute(Set<LoggedAttribute> enabledAttributes, String name, boolean editable) {
        CheckBox checkBox = uiComponents.create(CheckBox.class);
        if (enabledAttributes != null && isEntityHaveAttribute(name, enabledAttributes)) {
            checkBox.setValue(true);
        }
        checkBox.setId(name);
        checkBox.setCaption(name);
        checkBox.setEditable(editable);
        checkBox.addValueChangeListener(e -> checkAllCheckboxes());

        attributesBoxScroll.add(checkBox);
    }

    protected void enableAllCheckBoxes(boolean b) {
        if (canSelectAllCheckboxGenerateEvents) {
            for (Component box : attributesBoxScroll.getComponents())
                ((CheckBox) box).setValue(b);
        }
    }

    protected void checkAllCheckboxes() {
        CheckBox selectAllCheckBox = (CheckBox) attributesBoxScroll.getOwnComponent(SELECT_ALL_CHECK_BOX);
        if (selectAllCheckBox != null) {
            for (Component c : attributesBoxScroll.getComponents()) {
                if (!c.equals(selectAllCheckBox)) {
                    CheckBox checkBox = (CheckBox) c;
                    if (!checkBox.getValue()) {
                        setSelectAllCheckBox(false);
                        return;
                    }
                }
            }
            if (attributesBoxScroll.getComponents().size() != 1)
                setSelectAllCheckBox(true);
        }
    }

    public void setSelectAllCheckBox(boolean value) {
        canSelectAllCheckboxGenerateEvents = false;
        boolean isEditable = selectAllCheckBox.isEditable();
        try {
            selectAllCheckBox.setEditable(true);
            selectAllCheckBox.setValue(value);
        } finally {
            canSelectAllCheckboxGenerateEvents = true;
            selectAllCheckBox.setEditable(isEditable);
        }
    }

    public void setDateFieldTime() {
        Date date = timeSource.currentTimestamp();
        fromDateField.setValue(DateUtils.addDays(date, -1));
        tillDateField.setValue(DateUtils.addDays(date, 1));
    }

    public void clearAttributes() {
        for (Component c : attributesBoxScroll.getComponents())
            if (!SELECT_ALL_CHECK_BOX.equals(c.getId()))
                attributesBoxScroll.remove(c);
    }

    public boolean isEntityHaveAttribute(String propertyName, Set<LoggedAttribute> enabledAttr) {
        if (enabledAttr != null && !systemAttrsList.contains(propertyName)) {
            for (LoggedAttribute logAttr : enabledAttr)
                if (logAttr.getName().equals(propertyName))
                    return true;
        }
        return false;
    }

    public LoggedAttribute getLoggedAttribute(String name, Set<LoggedAttribute> enabledAttr) {
        for (LoggedAttribute atr : enabledAttr)
            if (atr.getName().equals(name))
                return atr;
        return null;
    }

    @Subscribe("loggedEntityTable.create")
    public void onLoggedEntityTableCreate(Action.ActionPerformedEvent event) {
        LoggedEntity entity = metadata.create(LoggedEntity.class);
        entity.setAuto(false);
        entity.setManual(false);
        setSelectAllCheckBox(false);
        loggedEntityDc.getMutableItems().add(entity);
        loggedEntityTable.setEditable(true);
        loggedEntityTable.setSelected(entity);

        enableControls();

        entityNameField.setEditable(true);
        entityNameField.focus();
    }

    @Subscribe("loggedEntityTable.edit")
    public void onLoggedEntityTableEdit(Action.ActionPerformedEvent event) {
        enableControls();

        loggedEntityTable.setEnabled(false);
        cancelBtn.focus();
    }

    @Subscribe("searchBtn")
    public void onSearchBtnClick(Button.ClickEvent event) {
        Entity entity = instancePicker.getValue();
        if (entity != null) {
            Object entityId = referenceToEntitySupport.getReferenceId(entity);
            if (entityId instanceof UUID) {
                entityLogDl.setParameter("entityId", entityId);
            } else if (entityId instanceof String) {
                entityLogDl.setParameter("stringEntityId", entityId);
            } else if (entityId instanceof Integer) {
                entityLogDl.setParameter("intEntityId", entityId);
            } else if (entityId instanceof Long) {
                entityLogDl.setParameter("longEntityId", entityId);
            }
        } else {
            entityLogDl.removeParameter("entityId");
            entityLogDl.removeParameter("stringEntityId");
            entityLogDl.removeParameter("intEntityId");
            entityLogDl.removeParameter("longEntityId");
        }
        if (userField.getValue() != null) {
            entityLogDl.setParameter("user", userField.getValue());
        } else {
            entityLogDl.removeParameter("user");
        }
        if (changeTypeField.getValue() != null) {
            entityLogDl.setParameter("changeType", changeTypeField.getValue());
        } else {
            entityLogDl.removeParameter("changeType");
        }
        if (filterEntityNameField.getValue() != null) {
            entityLogDl.setParameter("entityName", filterEntityNameField.getValue());
        } else {
            entityLogDl.removeParameter("entityName");
        }
        if (fromDateField.getValue() != null) {
            entityLogDl.setParameter("fromDate", fromDateField.getValue());
        } else {
            entityLogDl.removeParameter("fromDate");
        }
        if (tillDateField.getValue() != null) {
            entityLogDl.setParameter("tillDate", tillDateField.getValue());
        } else {
            entityLogDl.removeParameter("tillDate");
        }
        entityLogDl.load();
    }

    @Subscribe("clearEntityLogTableBtn")
    public void onClearEntityLogTableBtnClick(Button.ClickEvent event) {
        userField.setValue(null);
        filterEntityNameField.setValue(null);
        changeTypeField.setValue(null);
        instancePicker.setValue(null);
        fromDateField.setValue(null);
        tillDateField.setValue(null);
    }

    @Subscribe("reloadBtn")
    public void onReloadBtnClick(Button.ClickEvent event) {
        entityLog.invalidateCache();
        notifications.create()
                .withCaption(messages.getMessage("changesApplied"))
                .withType(Notifications.NotificationType.HUMANIZED)
                .show();
    }

    protected boolean allowLogProperty(MetaProperty metaProperty /*, CategoryAttribute categoryAttribute*/) {
        if (systemAttrsList.contains(metaProperty.getName())) {
            return false;
        }
        Range range = metaProperty.getRange();
        if (range.isClass() && metadataTools.hasCompositePrimaryKey(range.asClass()) &&
                !HasUuid.class.isAssignableFrom(range.asClass().getJavaClass())) {
            return false;
        }
        if (range.isClass() && range.getCardinality().isMany()) {
            return false;
        }
        //todo DynamicAttributes
//        if (categoryAttribute != null &&
//                BooleanUtils.isTrue(categoryAttribute.getIsCollection())) {
//            return false;
//        }
        return true;
    }

    @Subscribe("saveBtn")
    protected void onSaveBtnClick(Button.ClickEvent event) {
        LoggedEntity selectedEntity = loggedEntityTable.getSelected().iterator().next();
        selectedEntity = dataContext.merge(selectedEntity);
        Set<LoggedAttribute> enabledAttributes = selectedEntity.getAttributes();
        for (Component c : attributesBoxScroll.getComponents()) {
            CheckBox currentCheckBox = (CheckBox) c;
            if (SELECT_ALL_CHECK_BOX.equals(currentCheckBox.getId()))
                continue;
            Boolean currentCheckBoxValue = currentCheckBox.getValue();
            if (currentCheckBoxValue && !isEntityHaveAttribute(currentCheckBox.getId(), enabledAttributes)) {
                //add attribute if checked and not exist in table
                LoggedAttribute newLoggedAttribute = metadata.create(LoggedAttribute.class);
                newLoggedAttribute.setName(currentCheckBox.getId());
                newLoggedAttribute.setEntity(selectedEntity);
                dataContext.merge(newLoggedAttribute);
            }
            if (!currentCheckBoxValue && isEntityHaveAttribute(currentCheckBox.getId(), enabledAttributes)) {
                //remove attribute if unchecked and exist in table
                LoggedAttribute removeAtr = getLoggedAttribute(currentCheckBox.getId(), enabledAttributes);
                if (removeAtr != null)
                    dataContext.remove(removeAtr);
            }
        }
        dataContext.commit();

        loggedEntityDl.load();
        disableControls();
        loggedEntityTable.setEnabled(true);
        loggedEntityTable.focus();

        entityLog.invalidateCache();
    }

    @Subscribe("removeBtn")
    protected void onRemoveBtnClick(Button.ClickEvent event) {
        Set<LoggedEntity> selectedItems = loggedEntityTable.getSelected();
        if (!selectedItems.isEmpty()) {
            dialogs.createOptionDialog()
                    .withCaption(messages.getMessage("dialogs.Confirmation"))
                    .withMessage(messages.getMessage("dialogs.Confirmation.Remove"))
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(e -> {
                                for (LoggedEntity item : selectedItems) {
                                    if (item.getAttributes() != null) {
                                        Set<LoggedAttribute> attributes = new HashSet<>(item.getAttributes());
                                        for (LoggedAttribute attribute : attributes) {
                                            dataContext.remove(attribute);
                                        }
                                        dataContext.commit();
                                    }
                                    dataContext.remove(item);
                                    dataContext.commit();
                                }
                                loggedEntityDc.getMutableItems().removeAll(selectedItems);
                                entityLog.invalidateCache();
                            }),
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .show();
        }
    }

    @Subscribe("cancelBtn")
    protected void onCancelBtnClick(Button.ClickEvent event) {
        loggedEntityDl.load();
        disableControls();
        loggedEntityTable.setEnabled(true);
        loggedEntityTable.focus();
    }
}
