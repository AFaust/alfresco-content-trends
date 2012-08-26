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
    var $html = Alfresco.util.encodeHTML, Event = YAHOO.util.Event, Dom = YAHOO.util.Dom;

    PRODYNA.module = PRODYNA.module || {};

    PRODYNA.module.CompareTrendDialog = function(htmlId)
    {
        PRODYNA.module.CompareTrendDialog.superclass.constructor.call(this, htmlId, []);

        this.graphs = [];
        this.trendData = [];
        this.currentScoreType = "totalScore";
        this.chartReady = false;

        this.setOptions(
        {
            templateUrl : Alfresco.constants.URL_SERVICECONTEXT + "modules/content-trends/compare-dialog",
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
            nodeRefs : [],
            destroyOnHide : true,
            width : "60em"
        });

        return this;
    };

    YAHOO
            .extend(
                    PRODYNA.module.CompareTrendDialog,
                    Alfresco.module.SimpleDialog,
                    {
                        graphs : null,

                        trendData : null,

                        currentScoreType : null,
                        
                        chartReady : null,

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

                            return graph;
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
                            Alfresco.util.populateHTML([ dialog.id + "-title", this.msg("compare-trend.title") ]);

                            Dom.addClass(this.widgets.cancelButton, "hidden");

                            this.setupChart();

                            var i = 0, jj = this.options.nodeRefs.length;
                            for (; i < jj; i++)
                            {
                                var node = Alfresco.util.NodeRef(this.options.nodeRefs[i]);
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
                                        scope : this
                                    },
                                    elementIndex : i,
                                    // TODO: failure message
                                    scope : this
                                });
                            }
                            
                            var me = this;
                            Event
                                    .addListener(
                                            this.id + "-scoreType",
                                            "change",
                                            function()
                                            {
                                                var scoreTypeSelect = Dom.get(this), scoreType = scoreTypeSelect.options[scoreTypeSelect.selectedIndex].value;
                                                me.currentScoreType = scoreType;
                                                me._updateChart.call(me);
                                            });
                        },

                        onDataLoaded : function(response)
                        {
                            var scores = response.json.historicScores, chartDataSet = [], i, ii, elementIndex = response.config.elementIndex;

                            for (i = 0, ii = scores.length; i < ii; i++)
                            {
                                var valueSet = {}, property;
                                for (property in scores[i])
                                {
                                    valueSet[property] = scores[i][property];
                                }

                                // parse the date
                                valueSet.date = Alfresco.util.fromISO8601(valueSet.date);
                                valueSet.date.setMilliseconds(0);
                                valueSet.date.setSeconds(0);

                                chartDataSet.push(valueSet);
                            }

                            this.trendData.push(
                            {
                                nodeRef : response.json.nodeRef,
                                fileName : response.json.name,
                                valueSet : chartDataSet
                            });

                            if (this.trendData.length == this.options.nodeRefs.length)
                            {
                                this._mergeData();
                                this._finalizeChart();
                            }
                        },

                        _finalizeChart : function()
                        {
                            /*
                             * The color of a graph is taken from a color circle. The color circle code is an adapted version of one of the
                             * examples from http://krazydad.com/tutorials/makecolors.php - A positive sine offset and lower baseColorValue
                             * is used to achieve darker colors in average as otherwise some graphs would be difficult to make out against
                             * the dialog background.
                             */
                            
                            var i = 0, ii = this.trendData.length, frequency = 2 * Math.PI / ii, sineOffset = 0.5, baseColorValue = 128 - sineOffset * 128, colorValueVariance = 127;
                            for (; i < ii; i++)
                            {
                                var r = (Math.sin(frequency * i) + sineOffset) * colorValueVariance + baseColorValue,
                                    g = (Math.sin(frequency * i + 2 * Math.PI / 3) + sineOffset) * colorValueVariance + baseColorValue,
                                    b = (Math.sin(frequency * i + 4 * Math.PI / 3) + sineOffset) * colorValueVariance + baseColorValue,
                                    rStr = Math.round(r).toString(16),
                                    gStr = Math.round(g).toString(16),
                                    bStr = Math.round(b).toString(16),
                                    rStr = rStr.length == 1 ? ("0" + rStr) : rStr,
                                    gStr = gStr.length == 1 ? ("0" + gStr) : gStr,
                                    bStr = bStr.length == 1 ? ("0" + bStr) : bStr,
                                    msg = this.trendData[i].fileName,
                                    scoreType = this.currentScoreType + "-" + this.trendData[i].nodeRef,
                                    color = "#" + rStr + gStr + bStr;
                                    
                                this.graphs.push(this.addGraph(msg, scoreType, color));
                            }

                            this.widgets.chart.write(this.id + "-chart");
                            this.chartReady = true;
                        },
                        
                        _updateChart : function(){
                            if (this.chartReady)
                            {
                                // update the graphs to display a different score
                                var i = 0, ii = this.trendData.length;
                                for (; i < ii; i++)
                                {
                                    this.graphs[i].valueField = this.currentScoreType + "-" + this.trendData[i].nodeRef;
                                    
                                    // we need to remove and add, otherwise validateNow won't have an effect
                                    // TODO: Why is that?
                                    this.widgets.chart.removeGraph(this.graphs[i]);
                                    this.widgets.chart.addGraph(this.graphs[i]);
                                }
                                
                                // TODO: This is rather slow in tests. Why is that? Should we use a different charting engine?
                                this.widgets.chart.validateNow();
                            }
                        },

                        _mergeData : function()
                        {
                            var fullDataSet = [], allDataProcessed = false, nodeIndices = {}, nodeDataIndices = {}, i = 0, ii = 0;

                            for (ii = this.trendData.length; i < ii; i++)
                            {
                                // initialize indices
                                nodeDataIndices[this.trendData[i].nodeRef] = 0;
                                nodeIndices[this.trendData[i].nodeRef] = i;
                            }

                            while (!allDataProcessed)
                            {
                                var nextDate = null, nextDataNodes = [];
                                for (i = 0, ii = this.trendData.length; i < ii; i++)
                                {

                                    var index = nodeDataIndices[this.trendData[i].nodeRef]
                                    if (index < this.trendData[i].valueSet.length)
                                    {
                                        var date = this.trendData[i].valueSet[index].date;

                                        if (nextDate == null || nextDate > date)
                                        {
                                            nextDate = date;
                                            nextDataNodes = [ this.trendData[i].nodeRef ];
                                        }
                                        else if (nextDate.utc == date.utc) // direct comparison does not work
                                        {
                                            // same date, so append to next data
                                            nextDataNodes.push(this.trendData[i].nodeRef);
                                        }
                                    }
                                }

                                allDataProcessed = nextDate == null;

                                if (!allDataProcessed)
                                {
                                    var j = 0, jj = nextDataNodes.length, entry = {};
                                    entry.date = date;

                                    for (; j < jj; j++)
                                    {
                                        var nodeRef = nextDataNodes[j], index = nodeDataIndices[nodeRef], nodeEntry = this.trendData[nodeIndices[nodeRef]].valueSet[index], property = null;
                                        nodeDataIndices[nodeRef] = index + 1;

                                        for (property in nodeEntry)
                                        {
                                            if (property != "date")
                                            {
                                                entry[property + "-" + nodeRef] = nodeEntry[property];
                                            }
                                        }
                                    }

                                    fullDataSet.push(entry);
                                }
                            }

                            this.widgets.chart.dataProvider = fullDataSet;
                        },

                        dummyForClose : function()
                        {
                            // NO-OP just to avoid form submission
                            return false;
                        }
                    });
})();