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
package com.vaadin.hummingbird.uitest.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.router.NavigationEvent;
import com.vaadin.hummingbird.router.NavigationHandler;
import com.vaadin.hummingbird.router.Resolver;
import com.vaadin.hummingbird.router.RouterUI;
import com.vaadin.hummingbird.router.ViewRenderer;
import com.vaadin.server.VaadinServlet;

@WebServlet(asyncSupported = true, urlPatterns = { "/view/*" })
@VaadinServletConfiguration(ui = RouterUI.class, productionMode = false)
public class ViewTestServlet extends VaadinServlet {

    private static ViewClassLocator viewLocator;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        viewLocator = new ViewClassLocator(getService().getClassLoader());

        getService().getRouter().reconfigure(configuration -> {
            configuration.setResolver(new Resolver() {
                @Override
                public NavigationHandler resolve(
                        NavigationEvent navigationEvent) {
                    try {
                        return new ViewRenderer(
                                viewLocator.findViewClass(navigationEvent
                                        .getLocation().getFirstSegment()),
                                ViewTestLayout.class);
                    } catch (ClassNotFoundException e) {
                        return new ViewRenderer(ErrorView.class,
                                ViewTestLayout.class);
                    }
                }
            });
        });
    }

    static ViewClassLocator getViewLocator() {
        return viewLocator;
    }

}