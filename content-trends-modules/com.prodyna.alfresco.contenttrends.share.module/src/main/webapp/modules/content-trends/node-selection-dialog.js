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

if (PRODYNA === undefined || !PRODYNA)
{
    var PRODYNA = {};
}

(function()
{
    var Dom = YAHOO.util.Dom, $html = Alfresco.util.encodeHTML;

    PRODYNA.module = PRODYNA.module || {};

    PRODYNA.module.NodeSelectionDialog = function(htmlId)
    {
        PRODYNA.module.NodeSelectionDialog.superclass.constructor.call(this, htmlId, []);

        this.setOptions(
        {
            templateUrl : Alfresco.constants.URL_SERVICECONTEXT + "modules/content-trends/node-selection-dialog",
            doBeforeDialogShow :
            {
                fn : this.adaptDialogBeforeShow,
                scope : this
            },
            doBeforeAjaxRequest :
            {
                fn : this.submitToCallback,
                scope : this
            },
            fileName : null,
            nodeRef : null,
            destroyOnHide : true,
            callback :
            {
                fn : null,
                obj : null,
                scope : this
            }
        });

        return this;
    };

    YAHOO.extend(PRODYNA.module.NodeSelectionDialog, Alfresco.module.SimpleDialog,
    {
        adaptDialogBeforeShow : function(form, dialog, obj)
        {
            // Dialog title
            var fileSpan = '<span class="light">' + $html(this.options.fileName) + '</span>';

            Alfresco.util.populateHTML([ dialog.id + "-title", this.msg("node-selection.title", fileSpan) ]);

            this.widgets.nodeSelection = new Alfresco.ObjectFinder(this.id + "-nodes-cntrl", this.id + "-nodes");
            this.widgets.nodeSelection.setOptions(
            {
                field : "nodes",
                compactMode : false,
                mandatory : true,
                selectActionLabel : Alfresco.util.message("button.select"),
                itemFamily : "node",
                itemType : "ct:scored",
                multipleSelectMode : true,
                displayMode : "items",
                currentItem : this.options.nodeRef,
                startLocation : "{parent}"
            });

            // need to call this explicitly as both default load events (page load and dialog template load) have already been passed and
            // object-finder dependencies may still be in the queue if they weren't already available
//            Alfresco.util.YUILoaderHelper.loadComponents(false);
        },

        submitToCallback : function()
        {

            var selectedNodesStr = Dom.get(this.id + "-nodes-cntrl-added").value, selectedNodes = selectedNodesStr.split(",");

            if (this.options.callback && (typeof this.options.callback.fn == "function"))
            {
                var scope = this.options.callback.scope || this, obj = this.options.callback.obj || {};
                this.options.callback.fn.call(scope, selectedNodes, obj);
            }

            return false;
        }
    });
})();