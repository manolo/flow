/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client.hummingbird.nodefeature;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsMap.ForEachCallback;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.ReactiveChangeListener;
import com.vaadin.client.hummingbird.reactive.ReactiveEventRouter;
import com.vaadin.client.hummingbird.reactive.ReactiveValue;

import elemental.events.EventRemover;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A state node feature that structures data as a map.
 * <p>
 * The feature works as a reactive value with regards to the set of available
 * properties. A {@link Computation} will get a dependency on this feature by
 * iterating the properties. Accessing a property by name does not create a
 * dependency. The <code>Computation</code> is invalidated when a property is
 * added (properties are never removed). It is not invalidated when the value of
 * a property changes since the property is a reactive values of its own.
 *
 * @since
 * @author Vaadin Ltd
 */
public class NodeMap extends NodeFeature implements ReactiveValue {
    private final JsMap<String, MapProperty> properties = JsCollections.map();

    private final ReactiveEventRouter<MapPropertyAddListener, MapPropertyAddEvent> eventRouter = new ReactiveEventRouter<MapPropertyAddListener, MapPropertyAddEvent>(
            this) {
        @Override
        protected MapPropertyAddListener wrap(ReactiveChangeListener listener) {
            return listener::onChange;
        }

        @Override
        protected void dispatchEvent(MapPropertyAddListener listener,
                MapPropertyAddEvent event) {
            listener.onPropertyAdd(event);
        }
    };

    /**
     * Creates a new map feature.
     *
     * @param id
     *            the id of the feature
     * @param node
     *            the node of the feature
     */
    public NodeMap(int id, StateNode node) {
        super(id, node);
    }

    /**
     * Gets the property with a given name, creating it if necessary.
     * <p>
     * A {@link MapPropertyAddEvent} is fired if a new property instance is
     * created.
     *
     * @param name
     *            the name of the property
     * @return the property instance
     */
    public MapProperty getProperty(String name) {
        MapProperty property = properties.get(name);
        if (property == null) {
            property = new MapProperty(name, this);
            properties.set(name, property);

            eventRouter.fireEvent(new MapPropertyAddEvent(this, property));
        }

        return property;
    }

    /**
     * Checks if the given property is present and has a value.
     *
     * @param name
     *            the name of the property to check
     * @return true if the property exists and has a value, false otherwise
     */
    public boolean hasPropertyValue(String name) {
        if (!properties.has(name)) {
            return false;
        }

        return properties.get(name).hasValue();
    }

    /**
     * Iterates all properties in this map.
     *
     * @param callback
     *            the callback to invoke for each property
     */
    public void forEachProperty(ForEachCallback<String, MapProperty> callback) {
        eventRouter.registerRead();
        properties.forEach(callback);
    }

    @Override
    public JsonValue getDebugJson() {
        JsonObject json = WidgetUtil.createJsonObjectWithoutPrototype();

        properties.forEach((p, n) -> {
            if (p.hasValue()) {
                json.put(n, getAsDebugJson(p.getValue()));
            }
        });

        if (json.keys().length == 0) {
            return null;
        }

        return json;
    }

    @Override
    public EventRemover addReactiveChangeListener(
            ReactiveChangeListener listener) {
        return eventRouter.addReactiveListener(listener);
    }

    /**
     * Adds a listener that is informed whenever a new property is added to this
     * map.
     *
     * @param listener
     *            the property add listener
     * @return an event remover that can be used for removing the added listener
     */
    public EventRemover addPropertyAddListener(
            MapPropertyAddListener listener) {
        return eventRouter.addListener(listener);
    }
}