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
    var $html = Alfresco.util.encodeHTML;

    PRODYNA.module = PRODYNA.module || {};

    PRODYNA.module.NodeTrendDialog = function(htmlId)
    {
        PRODYNA.module.NodeTrendDialog.superclass.constructor.call(this, htmlId, []);

        this.baseCharts = [
        {
            labelSuffix : "total-score",
            fieldName : "totalScore",
            color : "#ff0000"
        },
        {
            labelSuffix : "view-score",
            fieldName : "viewScore",
            color : "#20ff20"
        },
        {
            labelSuffix : "edit-score",
            fieldName : "editScore",
            color : "#0000ff"
        },
        {
            labelSuffix : "tag-score",
            fieldName : "tagScore",
            color : "#40c0ff"
        },
        {
            labelSuffix : "rating-score",
            fieldName : "ratingScore",
            color : "#ffa040"
        },
        {
            labelSuffix : "comment-score",
            fieldName : "commentScore",
            color : "#ffd080"
        } ];

        this.setOptions(
        {
            templateUrl : Alfresco.constants.URL_SERVICECONTEXT + "modules/content-trends/trend-dialog",
            doBeforeDialogShow :
            {
                fn : this.adaptDialogBeforeShow,
                scope : this
            },
            doBeforeAjaxRequest :
            {
                fn : this.dummyForClose,
                scope : this
            },
            fileName : null,
            nodeRef : null,
            destroyOnHide : true,
            width : "60em"
        });

        return this;
    };

    YAHOO.extend(PRODYNA.module.NodeTrendDialog, Alfresco.module.SimpleDialog,
    {
        baseCharts : null,

        addGraph : function(title, valueField, color)
        {
            var graph = new AmCharts.AmGraph();
            graph.valueField = valueField;
            graph.bullet = "round";
            graph.hideBulletsCount = 10;
            graph.markerType = "circle";
            graph.title = title;
            graph.lineColor = color;
            graph.type = "line";

            this.widgets.chart.addGraph(graph);
        },

        setupChart : function()
        {
            this.widgets.chart = new AmCharts.AmSerialChart();
            this.widgets.chart.categoryField = "date";
            this.widgets.chart.pathToImages = Alfresco.constants.URL_RESCONTEXT + "amcharts/images/";

            var categoryAxis = this.widgets.chart.categoryAxis;
            categoryAxis.parseDates = true;
            categoryAxis.minPeriod = "mm";
            categoryAxis.dashLength = 1;
            categoryAxis.gridAlpha = 0.15;
            categoryAxis.axisColor = "#DADADA";

            var chartCursor = new AmCharts.ChartCursor();
            chartCursor.cursorPosition = "mouse";
            this.widgets.chart.addChartCursor(chartCursor);

            var legend = new AmCharts.AmLegend();
            this.widgets.chart.addLegend(legend);

            this.widgets.chart.dataProvider = [];
        },

        adaptDialogBeforeShow : function(form, dialog, obj)
        {
            // Dialog title
            var fileSpan = '<span class="light">' + $html(this.options.fileName) + '</span>';

            Alfresco.util.populateHTML([ dialog.id + "-title", this.msg("node-trend.title", fileSpan) ]);

            Dom.addClass(this.widgets.cancelButton, "hidden");

            this.setupChart();

            var node = Alfresco.util.NodeRef(this.options.nodeRef);
            Alfresco.util.Ajax.request(
            {
                method : "GET",
                url : YAHOO.lang.substitute("{prefix}slingshot/content-trends/history/{storeType}/{storeId}/{id}",
                {
                    prefix : Alfresco.constants.PROXY_URI,
                    storeType : node.storeType,
                    storeId : node.storeId,
                    id : node.id
                }),
                successCallback :
                {
                    fn : this.onDataLoaded,
                    obj : this,
                    scope : this
                },
                // TODO: failure message
                scope : this
            });
        },

        onDataLoaded : function(response)
        {
            var scores = response.json.historicScores, chartDataSet = [], i, ii;

            for (i = 0, ii = scores.length; i < ii; i++)
            {
                var valueSet = {}, property;
                for (property in scores[i])
                {
                    valueSet[property] = scores[i][property];
                }

                // parse the date
                valueSet.date = Alfresco.util.fromISO8601(valueSet.date);

                chartDataSet.push(valueSet);
            }

            for (i = 0; i < this.baseCharts.length; i++)
            {
                var msg = this.msg("node-trend." + this.baseCharts[i].labelSuffix);
                this.addGraph(msg, this.baseCharts[i].fieldName, this.baseCharts[i].color);
            }

            this.widgets.chart.dataProvider = chartDataSet;

            this.widgets.chart.write(this.id + "-chart");
        },

        dummyForClose : function()
        {
            // NO-OP just to avoid form submission
            return false;
        }
    });
})();