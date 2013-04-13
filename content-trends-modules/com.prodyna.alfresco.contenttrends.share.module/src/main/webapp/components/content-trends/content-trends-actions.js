/*
 * Copyright 2012 PRODYNA AG
 * 
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/eclipse-1.0.php or http://www.nabucco.org/License.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

(function()
{
    YAHOO.Bubbling.fire("registerAction",
    {
        actionName : "onActionNodeScoreHistory",
        fn : function(record)
        {
            var fileName = record.displayName, nodeRef = record.nodeRef, dialog = new PRODYNA.module.NodeTrendDialog(this.id
                    + "-content-trends-" + Alfresco.util.generateDomId());

            dialog.setOptions(
            {
                fileName : fileName,
                nodeRef : nodeRef
            }).show();
        }
    });

    YAHOO.Bubbling.fire("registerAction",
    {
        actionName : "onActionNodeScoreHistoryComparison",
        fn : function(record)
        {
            var fileName = record.displayName, nodeRef = record.nodeRef, dialog = new PRODYNA.module.NodeSelectionDialog(this.id
                    + "-node-selection-" + Alfresco.util.generateDomId());

            dialog.setOptions(
                    {
                        nodeRef : nodeRef,
                        fileName : fileName,
                        callback :
                        {
                            scope : this,
                            obj : nodeRef,
                            fn : function(selectedNodes, baseNode)
                            {
                                var compareDialog = new PRODYNA.module.CompareTrendDialog(this.id + "-trend-comparison-"
                                        + Alfresco.util.generateDomId()), nodeRefs = [];

                                var i = 0, ii = selectedNodes.length, baseNodeSelected = false;
                                for (; i < ii; i++)
                                {
                                    baseNodeSelected = baseNodeSelected || selectedNodes[i] == baseNode;
                                    nodeRefs.push(selectedNodes[i]);
                                }

                                if (!baseNodeSelected)
                                {
                                    nodeRefs.push(baseNode);
                                }

                                if (nodeRefs.length == 1)
                                {
                                    // TODO: show error message
                                }
                                else
                                {

                                    compareDialog.setOptions(
                                    {
                                        nodeRefs : nodeRefs
                                    }).show();
                                }
                            }
                        }
                    }).show();
        }
    });
})();