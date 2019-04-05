/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.images.transformers.impl;

import com.adobe.acs.commons.images.ImageTransformer;
import com.day.image.Layer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ACS AEM Commons - Image Transformer - Adjust Brightness and Contrast
 */
@Component
@Property(name = ImageTransformer.PROP_TYPE,
          value = AdjustImageTransformerImpl.TYPE)
@Service
public class AdjustImageTransformerImpl implements ImageTransformer {
    private static final Logger log = LoggerFactory.getLogger(AdjustImageTransformerImpl.class);

    static final String TYPE = "adjust";

    private static final String KEY_BRIGHTNESS = "brightness";
    private static final String KEY_BRIGHTNESS_ALIAS = "b";

    private static final String KEY_CONTRAST = "contrast";
    private static final String KEY_CONTRAST_ALIAS = "c";

    @Override
    public final Layer transform(final Layer layer, final ValueMap properties) {
        if (properties == null || properties.isEmpty()) {
            log.warn("Transform [ {} ] requires parameters.", TYPE);
            return layer;
        }

        log.debug("Transforming with [ {} ]", TYPE);

        int brightness = properties.get(KEY_BRIGHTNESS, properties.get(KEY_BRIGHTNESS_ALIAS, 0));
        float contrast = properties.get(KEY_CONTRAST, properties.get(KEY_CONTRAST_ALIAS, 1.0)).floatValue();

        layer.adjust(brightness, contrast);

        return layer;
    }
}
