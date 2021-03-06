/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.sqlserver;

import com.microsoft.azure.telemetry.TelemetryData;
import com.microsoft.azure.telemetry.TelemetryProxy;
import com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionAzureKeyVaultProvider;
import com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionKeyStoreProvider;
import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class KeyVaultProviderInitializer implements BeanPostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KeyVaultProviderInitializer.class);

    private KeyVaultProperties properties;
    private final TelemetryProxy telemetryProxy;

    public  KeyVaultProviderInitializer(KeyVaultProperties properties) {
        this.properties = properties;
        this.telemetryProxy = new TelemetryProxy(properties.isAllowTelemetry());

    }
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof DataSource) {
            try {

                LOG.info("initializing DataSource AlwaysEncryption Vault provider");
                trackCustomEvent();

                final SQLServerColumnEncryptionAzureKeyVaultProvider akvProvider =
                        new SQLServerColumnEncryptionAzureKeyVaultProvider(properties.getClientId(),
                                properties.getClientSecret());

                final Map<String, SQLServerColumnEncryptionKeyStoreProvider> keyStoreMap =
                        new HashMap<String, SQLServerColumnEncryptionKeyStoreProvider>();
                keyStoreMap.put(akvProvider.getName(), akvProvider);

                SQLServerConnection.registerColumnEncryptionKeyStoreProviders(keyStoreMap);

            } catch (SQLException ex) {
                LOG.error(ex.getMessage());
                throw new FatalBeanException(ex.getMessage());
            }
        }
        return bean;
    }
    private void trackCustomEvent() {
        final HashMap<String, String> customTelemetryProperties = new HashMap<>();

        final String[] packageNames = this.getClass().getPackage().getName().split("\\.");

        if (packageNames.length > 1) {
            customTelemetryProperties.put(TelemetryData.SERVICE_NAME, packageNames[packageNames.length - 1]);
        }
        telemetryProxy.trackEvent(ClassUtils.getUserClass(this.getClass()).getSimpleName(), customTelemetryProperties);
    }
}
