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

package io.jmix.auditui;

import io.jmix.audit.JmixAuditConfiguration;
import io.jmix.core.JmixCoreConfiguration;
import io.jmix.core.annotation.JmixModule;
import io.jmix.core.annotation.JmixProperty;
import io.jmix.ui.JmixUiConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@JmixModule(dependsOn = {JmixCoreConfiguration.class, JmixAuditConfiguration.class, JmixUiConfiguration.class},
        properties = @JmixProperty(name = "jmix.core.fetchPlansConfig", value = "io/jmix/auditui/fetch-plans.xml", append = true))
@ComponentScan
@EnableTransactionManagement
public class JmixAuditUiConfiguration {
}
