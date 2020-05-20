/*
 * Copyright (c) 2008-2019 Haulmont.
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

package spec.haulmont.cuba.web.components.composite.components.stepper;

import com.haulmont.cuba.gui.components.TextField;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import io.jmix.core.commons.events.Subscription;
import io.jmix.ui.components.Button;
import io.jmix.ui.components.CssLayout;
import io.jmix.ui.components.Field;
import io.jmix.ui.components.ValidationException;
import io.jmix.ui.components.data.ValueSource;
import io.jmix.ui.components.impl.CompositeComponent;
import io.jmix.ui.components.impl.CompositeDescriptor;
import io.jmix.ui.components.impl.CompositeWithCaption;
import io.jmix.ui.components.impl.CompositeWithContextHelp;
import io.jmix.ui.components.impl.CompositeWithHtmlCaption;
import io.jmix.ui.components.impl.CompositeWithHtmlDescription;
import io.jmix.ui.components.impl.CompositeWithIcon;
import io.jmix.ui.widgets.JmixTextField;

import java.util.Collection;
import java.util.function.Consumer;

@CompositeDescriptor("stepper-field.xml")
public class TestStepperField extends CompositeComponent<CssLayout> implements Field<Integer>,
        CompositeWithCaption, CompositeWithHtmlCaption, CompositeWithHtmlDescription,
        CompositeWithIcon, CompositeWithContextHelp {

    public static final String NAME = "testStepperField";

    /* Nested Components */
    private TextField<Integer> valueField;
    private Button upBtn;
    private Button downBtn;

    @Override
    protected void setComposition(CssLayout composition) {
        super.setComposition(composition);

        valueField = getInnerComponent("stepper_valueField");
        JmixTextField jmixTextField = valueField.unwrap(JmixTextField.class);
        jmixTextField.addShortcutListener(createAdjustmentShortcut(ShortcutAction.KeyCode.ARROW_UP, 1));
        jmixTextField.addShortcutListener(createAdjustmentShortcut(ShortcutAction.KeyCode.ARROW_DOWN, -1));

        upBtn = getInnerComponent("stepper_upBtn");
        downBtn = getInnerComponent("stepper_downBtn");

        upBtn.addClickListener(clickEvent -> updateValue(1));
        downBtn.addClickListener(clickEvent -> updateValue(-1));
    }

    private ShortcutListener createAdjustmentShortcut(int keyCode, int adjustment) {
        return new ShortcutListener(null, keyCode, (int[]) null) {
            @Override
            public void handleAction(Object sender, Object target) {
                updateValue(adjustment);
            }
        };
    }

    private void updateValue(int adjustment) {
        Integer currentValue = getValue();
        setValue(currentValue != null ? currentValue + adjustment : adjustment);
    }

    @Override
    public boolean isRequired() {
        return valueField.isRequired();
    }

    @Override
    public void setRequired(boolean required) {
        valueField.setRequired(required);
        getComposition().setRequiredIndicatorVisible(required);
    }

    @Override
    public String getRequiredMessage() {
        return valueField.getRequiredMessage();
    }

    @Override
    public void setRequiredMessage(String msg) {
        valueField.setRequiredMessage(msg);
    }

    @Override
    public void addValidator(Consumer<? super Integer> validator) {
        valueField.addValidator(validator);
    }

    @Override
    public void removeValidator(Consumer<Integer> validator) {
        valueField.removeValidator(validator);
    }

    @Override
    public Collection<Consumer<Integer>> getValidators() {
        return valueField.getValidators();
    }

    @Override
    public boolean isEditable() {
        return valueField.isEditable();
    }

    @Override
    public void setEditable(boolean editable) {
        valueField.setEditable(editable);
        upBtn.setEnabled(editable);
        downBtn.setEnabled(editable);
    }

    @Override
    public Integer getValue() {
        return valueField.getValue();
    }

    @Override
    public void setValue(Integer value) {
        valueField.setValue(value);
    }

    @Override
    public Subscription addValueChangeListener(Consumer<ValueChangeEvent<Integer>> listener) {
        return valueField.addValueChangeListener(listener);
    }

    @Override
    public boolean isValid() {
        return valueField.isValid();
    }

    @Override
    public void validate() throws ValidationException {
        valueField.validate();
    }

    @Override
    public void setValueSource(ValueSource<Integer> valueSource) {
        valueField.setValueSource(valueSource);
        getComposition().setRequiredIndicatorVisible(valueField.isRequired());
    }

    @Override
    public ValueSource<Integer> getValueSource() {
        return valueField.getValueSource();
    }

    @Override
    public boolean isHtmlSanitizerEnabled() {
        return getComposition().isHtmlSanitizerEnabled();
    }

    @Override
    public void setHtmlSanitizerEnabled(boolean htmlSanitizerEnabled) {
        getComposition().setHtmlSanitizerEnabled(htmlSanitizerEnabled);
    }
}
