/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ace.webui.vaadin.extension;

import java.util.stream.Stream;

import org.apache.ace.client.repository.object.Feature2DistributionAssociation;
import org.apache.ace.client.repository.object.FeatureObject;

/**
 * Provides a UI extension for the details window showing some details on all associations.
 */
public class FeatureToDistributionAssocExtensionFactory extends BaseAssociationExtensionFactory<FeatureObject, Feature2DistributionAssociation> {

    public FeatureToDistributionAssocExtensionFactory() {
        super("Distributions");
    }

    @Override
    protected Stream<Feature2DistributionAssociation> getAssocations(FeatureObject feature) {
        return feature.getDistributions().stream()
            .flatMap(d -> feature.getAssociationsWith(d).stream());
    }
}