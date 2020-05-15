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

package io.jmix.ui.settings.component.binder;

import io.jmix.ui.components.Component;
import io.jmix.ui.components.GroupBoxLayout;
import io.jmix.ui.components.impl.WebGroupBox;
import io.jmix.ui.settings.component.ComponentSettings;
import io.jmix.ui.settings.component.GroupBoxSettings;
import io.jmix.ui.settings.component.SettingsWrapper;

@org.springframework.stereotype.Component(GroupBoxSettingsBinder.NAME)
public class GroupBoxSettingsBinder implements ComponentSettingsBinder<GroupBoxLayout, GroupBoxSettings> {

    public static final String NAME = "jmix_GroupBoxSettingsBinder";

    @Override
    public Class<? extends Component> getComponentClass() {
        return WebGroupBox.class;
    }

    @Override
    public Class<? extends ComponentSettings> getSettingsClass() {
        return GroupBoxSettings.class;
    }

    @Override
    public void applySettings(GroupBoxLayout groupBox, SettingsWrapper wrapper) {
        GroupBoxSettings settings = wrapper.getSettings();

        if (settings.getExpanded() != null) {
            groupBox.setExpanded(settings.getExpanded());
        }
    }

    @Override
    public boolean saveSettings(GroupBoxLayout groupBox, SettingsWrapper wrapper) {
        GroupBoxSettings settings = wrapper.getSettings();

        if (settings.getExpanded() == null
                || settings.getExpanded() != groupBox.isExpanded()) {
            settings.setExpanded(groupBox.isExpanded());

            return true;
        }

        return false;
    }

    @Override
    public GroupBoxSettings getSettings(GroupBoxLayout groupBox) {
        GroupBoxSettings settings = createSettings();
        settings.setId(groupBox.getId());
        settings.setExpanded(groupBox.isExpanded());

        return settings;
    }

    protected GroupBoxSettings createSettings() {
        return new GroupBoxSettings();
    }
}
