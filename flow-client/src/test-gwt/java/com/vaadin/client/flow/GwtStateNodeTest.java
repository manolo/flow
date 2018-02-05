/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.client.flow;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;

public class GwtStateNodeTest extends ClientEngineTestBase {

    private StateTree tree;

    private static class TestData {
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        tree = new StateTree(new Registry());
    }

    public void testSetCookie_getCookie_sameInstance() {
        StateNode node = new StateNode(1, tree);
        TestData data = new TestData();
        node.setNodeData(data);
        assertSame(data, node.getNodeData(TestData.class));
    }

}