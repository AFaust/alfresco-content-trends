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
    /**
     * YUI Library aliases
     */
    var Dom = YAHOO.util.Dom, Event = YAHOO.util.Event;

    /**
     * Alfresco Slingshot aliases
     */
    var $html = Alfresco.util.encodeHTML, $hasEventInterest = Alfresco.util.hasEventInterest, $links = Alfresco.util.activateLinks, $combine = Alfresco.util.combinePaths, $userProfile = Alfresco.util.userProfileLink, $siteURL = Alfresco.util.siteURL, $date = function(
            date, format)
    {
        return Alfresco.util.formatDate(Alfresco.util.fromISO8601(date), format);
    }, $relTime = Alfresco.util.relativeTime, $isValueSet = Alfresco.util.isValueSet;

    var PREF_CONTENT_TRENDS_PREFIX = "com.prodyna.share.content-trends.", PREF_SORT_FIELD_SUFFIX = ".sortField";

    PRODYNA.dashlet = PRODYNA.dashlet || {};

    PRODYNA.renderer = PRODYNA.renderer || {};

    /**
     * Custom view renderer extending from the standard Alfresco one - this renderer respects the restrictions of this dashlet
     */
    PRODYNA.renderer.ContentTrendsListViewRenderer = function(name)
    {
        PRODYNA.renderer.ContentTrendsListViewRenderer.superclass.constructor.call(this, name);

        return this;
    };

            YAHOO
                    .extend(
                            PRODYNA.renderer.ContentTrendsListViewRenderer,
                            Alfresco.DocumentListViewRenderer,
                            {

                                /**
                                 * Description/detail custom datacell formatter
                                 * 
                                 * @method renderCellDescription
                                 * @param scope
                                 *            {object} The DocumentList object
                                 * @param elCell
                                 *            {object}
                                 * @param oRecord
                                 *            {object}
                                 * @param oColumn
                                 *            {object}
                                 * @param oData
                                 *            {object|string}
                                 */
                                renderCellDescription : function(scope, elCell, oRecord, oColumn, oData)
                                {
                                    var desc = "", i, j, record = oRecord.getData(), jsNode = record.jsNode, properties = jsNode.properties, isContainer = jsNode.isContainer, isLink = jsNode.isLink, title = "", titleHTML = "", version = "", canComment = jsNode.permissions.user.CreateChildren;

                                    if (jsNode.isLink)
                                    {
                                        // Link handling
                                        oRecord.setData("displayName", scope.msg("details.link-to", record.location.file));
                                    }
                                    else if (properties.title && properties.title !== record.displayName && scope.options.useTitle)
                                    {
                                        // Use title property if it's available. Supressed for links.
                                        titleHTML = '<span class="title">(' + $html(properties.title) + ')</span>';
                                    }

                                    // Version display
                                    if ($isValueSet(record.version) && !jsNode.isContainer && !jsNode.isLink)
                                    {
                                        version = '<span class="document-version">' + $html(record.version) + '</span>';
                                    }

                                    /**
                                     * Render using metadata template
                                     */
                                    record._filenameId = Alfresco.util.generateDomId();

                                    var metadataTemplate = record.metadataTemplate;
                                    if (metadataTemplate)
                                    {
                                        /* Banner */
                                        if (YAHOO.lang.isArray(metadataTemplate.banners))
                                        {
                                            var fnRenderBanner = function(p_key, p_value, p_meta)
                                            {
                                                var label = (p_meta !== null ? scope.msg(p_meta) + ': ' : ''), value = "";

                                                // render value from properties or custom renderer
                                                if (scope.renderers.hasOwnProperty(p_key) && typeof scope.renderers[p_key] === "function")
                                                {
                                                    value = scope.renderers[p_key].call(scope, record, label);
                                                }
                                                else
                                                {
                                                    if (jsNode.hasProperty(p_key))
                                                    {
                                                        value = '<span class="item">' + label + $html(jsNode.properties[p_key]) + '</span>';
                                                    }
                                                }

                                                return value;
                                            };

                                            var html, banner;
                                            for (i = 0, j = metadataTemplate.banners.length; i < j; i++)
                                            {
                                                banner = metadataTemplate.banners[i];
                                                if (!$isValueSet(banner.view) || banner.view == this.metadataBannerViewName)
                                                {
                                                    html = YAHOO.lang.substitute(banner.template, scope.renderers, fnRenderBanner);
                                                    if ($isValueSet(html))
                                                    {
                                                        desc += '<div class="info-banner">' + html + '</div>';
                                                    }
                                                }
                                            }
                                        }

                                        /* Title */
                                        if (YAHOO.lang.isString(metadataTemplate.title))
                                        {
                                            var fnRenderTitle = function(p_key, p_value, p_meta)
                                            {
                                                var label = (p_meta !== null ? '<em>' + scope.msg(p_meta) + '</em>: ' : ''), value = "";

                                                // render value from properties or custom renderer
                                                if (scope.renderers.hasOwnProperty(p_key) && typeof scope.renderers[p_key] === "function")
                                                {
                                                    value = scope.renderers[p_key].call(scope, record, label);
                                                }
                                                else
                                                {
                                                    if (jsNode.hasProperty(p_key))
                                                    {
                                                        value = '<div class="filename">'
                                                                + Alfresco.DocumentList.generateFileFolderLinkMarkup(scope, record);
                                                        value += label + $html(jsNode.properties[p_key]) + '</a></span></div>';
                                                    }
                                                }

                                                return value;
                                            };

                                            desc += YAHOO.lang.substitute(metadataTemplate.title, scope.renderers, fnRenderTitle);
                                        }
                                        else
                                        {
                                            desc += '<h3 class="filename"><span id="' + record._filenameId + '">'
                                                    + Alfresco.DocumentList.generateFileFolderLinkMarkup(scope, record);
                                            desc += $html(record.displayName) + '</a></span>' + titleHTML + version + '</h3>';
                                        }

                                        if (YAHOO.lang.isArray(metadataTemplate.lines))
                                        {
                                            var fnRenderTemplate = function(p_key, p_value, p_meta)
                                            {
                                                var label = (p_meta !== null ? '<em>' + scope.msg(p_meta) + '</em>: ' : ''), value = "";

                                                // render value from properties or custom renderer
                                                if (scope.renderers.hasOwnProperty(p_key) && typeof scope.renderers[p_key] === "function")
                                                {
                                                    value = scope.renderers[p_key].call(scope, record, label);
                                                }
                                                else
                                                {
                                                    if (jsNode.hasProperty(p_key))
                                                    {
                                                        value = '<span class="item">' + label + $html(jsNode.properties[p_key]) + '</span>';
                                                    }
                                                }

                                                return value;
                                            };

                                            var html, line;
                                            for (i = 0, j = metadataTemplate.lines.length; i < j; i++)
                                            {
                                                line = metadataTemplate.lines[i];
                                                if (!$isValueSet(line.view) || line.view == this.metadataLineViewName)
                                                {
                                                    html = YAHOO.lang.substitute(line.template, scope.renderers, fnRenderTemplate);
                                                    if ($isValueSet(html))
                                                    {
                                                        desc += '<div class="detail">' + html + '</div>';
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    elCell.innerHTML = desc;
                                }
                            }),

            PRODYNA.dashlet.ContentTrends = function(htmlId)
            {
                PRODYNA.dashlet.ContentTrends.superclass.constructor.call(this, "PRODYNA.dashlet.ContentTrends", htmlId, [ "container",
                        "datasource", "datatable", "paginator" ]);

                /*
                 * Initialise prototype properties
                 */
                this.currentPage = 1;
                this.totalRecords = 0;
                this.totalRecordsUpper = null;
                this.showingMoreActions = false;
                this.actions = {};
                this.afterDocListUpdate = [];
                this.doclistMetadata = {};
                this.previewTooltips = [];
                this.renderers = {};
                this.viewRenderers = {};
                this.dataSourceUrl = $combine(Alfresco.constants.URL_SERVICECONTEXT,
                        "components/dashlets/content-trends/data/content-trends");

                // these are really just dummy properties in order to support standard viewRenderer of doclist
                this.dragAndDropAllowed = false;
                this.dragAndDropEnabled = false;
                this.currentFilter = "dummy";

                /**
                 * Decoupled event listeners
                 */

                // dashlet specific event handlers
                YAHOO.Bubbling.on("contentTrendsRefresh", this.onContentTrendsRefresh, this);

                // general event handlers
                YAHOO.Bubbling.on("registerRenderer", this.onRegisterRenderer, this);
                YAHOO.Bubbling.on("registerViewRenderer", this.onRegisterViewRenderer, this);
                YAHOO.Bubbling.on("registerAction", this.onRegisterAction, this);

                return this;
            };

    /**
     * Extend from Alfresco.component.Base
     */
    YAHOO.extend(PRODYNA.dashlet.ContentTrends, Alfresco.component.Base);

    /**
     * Augment prototype with Actions module
     */
    YAHOO.lang.augmentProto(PRODYNA.dashlet.ContentTrends, Alfresco.doclib.Actions);

    /**
     * Augment prototype with main class implementation, ensuring overwrite is enabled
     */
    YAHOO.lang
            .augmentObject(
                    PRODYNA.dashlet.ContentTrends.prototype,
                    {
                        /**
                         * Object container for initialization options
                         * 
                         * @property options
                         * @type object
                         */
                        options :
                        {

                            /**
                             * Indicates which viewRenderer to use when displaying the content set.
                             * 
                             * @property viewRendererName
                             * @type string
                             * @default "simple-trends"
                             */
                            viewRendererName : "simple-trends",

                            /**
                             * An array containing the order of the viewRenderer keys
                             * 
                             * @property viewRendererNames
                             * @type array
                             * @default ["simple-trends"]
                             */
                            viewRendererNames : [ "simple-trends" ],

                            /**
                             * Flag indicating whether pagination is available or not.
                             * 
                             * @property usePagination
                             * @type boolean
                             * @default false
                             */
                            usePagination : false,

                            /**
                             * Current siteId. Not used in user dashlet mode.
                             * 
                             * @property siteId
                             * @type string
                             */
                            siteId : "",

                            /**
                             * ContainerId representing root container. Not used in user dashlet mode.
                             * 
                             * @property containerId
                             * @type string
                             * @default "documentLibrary"
                             */
                            containerId : "documentLibrary",

                            /**
                             * The score type to use if none has been explicitly selected
                             * 
                             * @property defaultScore
                             * @type string
                             * @default "totalScore
                             */
                            defaultScore : "totalScore",

                            /**
                             * Initial page to show on load (otherwise taken from URL hash).
                             * 
                             * @property initialPage
                             * @type int
                             */
                            initialPage : 1,

                            /**
                             * Number of items per page
                             * 
                             * @property pageSize
                             * @type int
                             */
                            pageSize : 50,

                            /**
                             * Delay time value for "More Actions" popup, in milliseconds
                             * 
                             * @property actionsPopupTimeout
                             * @type int
                             * @default 500
                             */
                            actionsPopupTimeout : 700,

                            /**
                             * Holds IDs to register preview tooltips with.
                             * 
                             * @property previewTooltips
                             * @type array
                             */
                            previewTooltips : null,

                            /**
                             * Valid inline edit mimetypes Currently allowed are plain text, HTML and XML only
                             * 
                             * @property inlineEditMimetypes
                             * @type object
                             */
                            inlineEditMimetypes :
                            {
                                "text/plain" : true,
                                "text/html" : true,
                                "text/xml" : true
                            },

                            /**
                             * Flag to indicate the list may be updated as a results of a REST API call
                             * 
                             * @property listUpdated
                             * @type boolean
                             */
                            listUpdated : false,

                            /**
                             * Whether the cm:title property is in use or not
                             * 
                             * @property useTitle
                             * @type boolean
                             */
                            useTitle : true,

                            /**
                             * Where to insert the "More..." actions split
                             * 
                             * @property actionsSplitAt
                             * @type number
                             * @default 3
                             */
                            actionsSplitAt : 3,

                            sortField : null
                        },

                        /**
                         * Current page being browsed.
                         * 
                         * @property currentPage
                         * @type int
                         * @default 1
                         */
                        currentPage : null,

                        /**
                         * Total number of records (documents + folders) in the currentPath.
                         * 
                         * @property totalRecords
                         * @type int
                         * @default 0
                         */
                        totalRecords : null,

                        /**
                         * Current actions menu being shown
                         * 
                         * @property currentActionsMenu
                         * @type object
                         * @default null
                         */
                        currentActionsMenu : null,

                        /**
                         * Whether "More Actions" pop-up is currently visible.
                         * 
                         * @property showingMoreActions
                         * @type boolean
                         * @default false
                         */
                        showingMoreActions : null,

                        /**
                         * Deferred actions menu element when showing "More Actions" pop-up.
                         * 
                         * @property deferredActionsMenu
                         * @type object
                         * @default null
                         */
                        deferredActionsMenu : null,

                        /**
                         * Deferred function calls for after a document list update
                         * 
                         * @property afterDocListUpdate
                         * @type array
                         */
                        afterDocListUpdate : null,

                        /**
                         * Metadata returned by doclist data webscript
                         * 
                         * @property doclistMetadata
                         * @type object
                         * @default null
                         */
                        doclistMetadata : null,

                        /**
                         * Registered metadata renderers. Register new renderers via registerRenderer() or "registerRenderer" bubbling event
                         * 
                         * @property renderers
                         * @type object
                         */
                        renderers : null,

                        /**
                         * Registered view renderers. Register new renderers via registerviewRenderer() or "registerViewRenderer" bubbling
                         * event
                         * 
                         * @property viewRenderers
                         * @type array
                         */
                        viewRenderers : null,

                        /**
                         * Indicates whether or not we allow the HTML5 drag and drop capability
                         * 
                         * @property dragAndDropAllowed
                         * @type boolean
                         */
                        dragAndDropAllowed : null,

                        /**
                         * Indicates whether or not the browser supports the HTML5 drag and drop capability
                         * 
                         * @property dragAndDropEnabled
                         * @type boolean
                         */
                        dragAndDropEnabled : null,

                        /**
                         * Current filter to filter document list.
                         * 
                         * @property currentFilter
                         * @type object
                         */
                        currentFilter : null,

                        /**
                         * Fired by YUI when parent element is available for scripting. Initial History Manager event registration
                         * 
                         * @method onReady
                         */
                        onReady : function()
                        {
                            // Reference to self used by inline functions
                            var me = this;

                            this.widgets.sortField = Alfresco.util.createYUIButton(this, "sortField-button", this.onSortField,
                            {
                                type : "menu",
                                menu : "sortField-menu",
                                lazyloadmenu : false
                            });

                            if (this.widgets.sortField !== null)
                            {
                                // Set the initial menu label
                                var menuItems = this.widgets.sortField.getMenu().getItems(), index;

                                for (index in menuItems)
                                {
                                    if (menuItems.hasOwnProperty(index))
                                    {
                                        if (menuItems[index].value === this.options.sortField)
                                        {
                                            this.widgets.sortField.set("label", menuItems[index].cfg.getProperty("text"));
                                            break;
                                        }
                                    }
                                }
                            }

                            // If the viewRenderer in the user preference is no longer available, use detailed
                            // Also determine the index value of the selected viewRenderer from viewRendererNames
                            var isViewRendererAvailable = false;
                            var selectedViewRendererIndex = 1;
                            var i, ii;
                            for (i = 0, ii = this.options.viewRendererNames.length; i < ii; i++)
                            {
                                if (this.options.viewRendererNames[i] === this.options.viewRendererName)
                                {
                                    isViewRendererAvailable = true;
                                    selectedViewRendererIndex = i;
                                    break;
                                }
                            }
                            if (!isViewRendererAvailable)
                            {
                                this.options.viewRendererName = this.options.viewRendererNames[0];
                            }

                            this._setupMetadataRenderers();

                            this._setupViewRenderers();

                            this._setupDataSource();

                            this._setupDataTable();
                            
                            // Services
                            this.services.preferences = new Alfresco.service.Preferences();

                            // Hook action events
                            var fnActionHandler = function(layer, args)
                            {
                                var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
                                if (owner !== null)
                                {
                                    // TODO: check containment in dashlet via id and DOM check
                                    if (typeof me[owner.title] === "function")
                                    {
                                        args[1].stop = true;
                                        var elIdentifier = args[1].target;
                                        if (typeof me.viewRenderers[me.options.viewRendererName] === "object")
                                        {
                                            elIdentifier = me.viewRenderers[me.options.viewRendererName]
                                                    .getDataTableRecordIdFromRowElement(me, args[1].target);
                                        }
                                        var record = me.widgets.dataTable.getRecord(elIdentifier).getData();
                                        me[owner.title].call(me, record, owner);
                                    }
                                }
                                return true;
                            };
                            YAHOO.Bubbling.addDefaultAction("action-link", fnActionHandler);
                            YAHOO.Bubbling.addDefaultAction("show-more", fnActionHandler);

                            // DocLib Actions module
                            this.modules.actions = new Alfresco.module.DoclibActions();

                            YAHOO.Bubbling.fire("postContentTrendsOnReady",
                            {
                                scope : this,
                                eventGroup : this.id
                            });

                            if (this.options.usePagination)
                            {
                                var handlePagination = function(state, me)
                                {
                                    me.widgets.paginator.setState(state);
                                    me.currentPage = state.page;

                                    YAHOO.Bubbling.fire("contentTrendsRefresh",
                                    {
                                        scope : me,
                                        eventGroup : me.id
                                    });
                                };

                                // YUI Paginator definition
                                this.widgets.paginator = new YAHOO.widget.Paginator(
                                {
                                    containers : [ this.id + "-paginator", this.id + "-paginatorBottom" ],
                                    rowsPerPage : this.options.pageSize,
                                    initialPage : this.currentPage,
                                    template : this.msg("pagination.template"),
                                    pageReportTemplate : this.msg("pagination.template.page-report"),
                                    previousPageLinkLabel : this.msg("pagination.previousPageLinkLabel"),
                                    nextPageLinkLabel : this.msg("pagination.nextPageLinkLabel")
                                });

                                this.widgets.paginator.subscribe("changeRequest", handlePagination, this);
                            }

                            YAHOO.Bubbling.fire("contentTrendsRefresh",
                            {
                                scope : this,
                                eventGroup : this.id
                            });

                            // Finally show the component body here to prevent UI artifacts on YUI button decoration
                            Dom.setStyle(this.id + "-body", "visibility", "visible");

                        },

                        /**
                         * DataTable Cell Renderers
                         */

                        /**
                         * Returns status custom datacell formatter
                         * 
                         * @method fnRenderCellStatus
                         */
                        fnRenderCellStatus : function()
                        {
                            var scope = this;

                            /**
                             * Status custom datacell formatter
                             * 
                             * @method renderCellStatus
                             * @param elCell
                             *            {object}
                             * @param oRecord
                             *            {object}
                             * @param oColumn
                             *            {object}
                             * @param oData
                             *            {object|string}
                             */
                            return function(elCell, oRecord, oColumn, oData)
                            {
                                if (typeof scope.viewRenderers[scope.options.viewRendererName] === "object")
                                {
                                    scope.viewRenderers[scope.options.viewRendererName].renderCellStatus(scope, elCell, oRecord, oColumn,
                                            oData);
                                }
                            };
                        },

                        /**
                         * Returns thumbnail custom datacell formatter
                         * 
                         * @method fnRenderCellThumbnail
                         */
                        fnRenderCellThumbnail : function()
                        {
                            var scope = this;

                            /**
                             * Thumbnail custom datacell formatter
                             * 
                             * @method renderCellThumbnail
                             * @param elCell
                             *            {object}
                             * @param oRecord
                             *            {object}
                             * @param oColumn
                             *            {object}
                             * @param oData
                             *            {object|string}
                             */
                            return function(elCell, oRecord, oColumn, oData)
                            {
                                if (typeof scope.viewRenderers[scope.options.viewRendererName] === "object")
                                {
                                    scope.viewRenderers[scope.options.viewRendererName].renderCellThumbnail(scope, elCell, oRecord,
                                            oColumn, oData);
                                }
                            };
                        },

                        /**
                         * Returns description/detail custom datacell formatter
                         * 
                         * @method fnRenderCellDescription
                         */
                        fnRenderCellDescription : function()
                        {
                            var scope = this;

                            /**
                             * Description/detail custom datacell formatter
                             * 
                             * @method renderCellDescription
                             * @param elCell
                             *            {object}
                             * @param oRecord
                             *            {object}
                             * @param oColumn
                             *            {object}
                             * @param oData
                             *            {object|string}
                             */
                            return function(elCell, oRecord, oColumn, oData)
                            {
                                if (typeof scope.viewRenderers[scope.options.viewRendererName] === "object")
                                {
                                    scope.viewRenderers[scope.options.viewRendererName].renderCellDescription(scope, elCell, oRecord,
                                            oColumn, oData);
                                }
                            };
                        },

                        /**
                         * Returns actions custom datacell formatter
                         * 
                         * @method fnRenderCellActions
                         */
                        fnRenderCellActions : function()
                        {
                            var scope = this;

                            /**
                             * Actions custom datacell formatter
                             * 
                             * @method renderCellActions
                             * @param elCell
                             *            {object}
                             * @param oRecord
                             *            {object}
                             * @param oColumn
                             *            {object}
                             * @param oData
                             *            {object|string}
                             */
                            return function(elCell, oRecord, oColumn, oData)
                            {
                                if (typeof scope.viewRenderers[scope.options.viewRendererName] === "object")
                                {
                                    scope.viewRenderers[scope.options.viewRendererName].renderCellActions(scope, elCell, oRecord, oColumn,
                                            oData);
                                }
                            };
                        },

                        /**
                         * Register a metadata renderer via Bubbling event
                         * 
                         * @method onRegisterRenderer
                         * @param layer
                         *            {object} Event fired (unused)
                         * @param args
                         *            {array} Event parameters (property name, rendering function)
                         */
                        onRegisterRenderer : function(layer, args)
                        {
                            var obj = args[1];
                            if (obj && $isValueSet(obj.propertyName) && $isValueSet(obj.renderer))
                            {
                                this.registerRenderer(obj.propertyName, obj.renderer);
                            }
                            else
                            {
                                Alfresco.logger.error("ContentTrends_onRegisterRenderer: Custom renderer registion invalid: " + obj);
                            }
                        },

                        /**
                         * Register a view renderer via Bubbling event
                         * 
                         * @method onRegisterViewRenderer
                         * @param layer
                         *            {object} Event fired (unused)
                         * @param args
                         *            {array} Event parameters (property name, rendering function)
                         */
                        onRegisterViewRenderer : function(layer, args)
                        {
                            var obj = args[1];
                            if (obj && $isValueSet(obj.renderer))
                            {
                                this.registerViewRenderer(obj.renderer);
                            }
                            else
                            {
                                Alfresco.logger.error("ContentTrends_onRegisterViewRenderer: Custom view renderer registion invalid: "
                                        + obj);
                            }
                        },

                        /**
                         * Register a metadata renderer
                         * 
                         * @method registerRenderer
                         * @param propertyName
                         *            {string} Property name to attach this renderer to
                         * @param renderer
                         *            {function} Rendering function
                         * @return {boolean} Success status of registration
                         */
                        registerRenderer : function(propertyName, renderer)
                        {
                            if ($isValueSet(propertyName) && $isValueSet(renderer))
                            {
                                this.renderers[propertyName] = renderer;
                                return true;
                            }
                            return false;
                        },

                        /**
                         * Register a view renderer and call its setupRenderer method
                         * 
                         * @method registerViewRenderer
                         * @param renderer
                         *            {object} Rendering object
                         * @return {boolean} Success status of registration
                         */
                        registerViewRenderer : function(renderer)
                        {
                            if ($isValueSet(renderer))
                            {
                                this.viewRenderers[renderer.name] = renderer;
                                this.viewRenderers[renderer.name].setupRenderer(this);
                                return true;
                            }
                            return false;
                        },

                        /**
                         * Configure standard metadata renderers
                         * 
                         * @method _setupMetadataRenderers
                         */
                        _setupMetadataRenderers : function()
                        {
                            /**
                             *
                             */
                            this.registerRenderer("i18nLabel", function(record, label)
                            {
                                // Just return the label, removing the trailing ": "
                                return label.replace(/:\s$/, "");
                            });

                            /**
                             * Locked / Working Copy banner
                             */
                            this.registerRenderer("lockBanner",
                                    function(record, label)
                                    {
                                        var properties = record.jsNode.properties, bannerUser = properties.lockOwner
                                                || properties.workingCopyOwner, bannerLink = Alfresco.DocumentList.generateUserLink(this,
                                                bannerUser), html = "";

                                        /* Google Docs Integration */
                                        if (record.workingCopy && $isValueSet(record.workingCopy.googleDocUrl))
                                        {
                                            if (bannerUser.userName === Alfresco.constants.USERNAME)
                                            {
                                                html = this.msg("details.banner.google-docs-owner", '<a href="'
                                                        + record.workingCopy.googleDocUrl + '" target="_blank">'
                                                        + this.msg("details.banner.google-docs.link") + '</a>');
                                            }
                                            else
                                            {
                                                html = this.msg("details.banner.google-docs-locked", bannerLink, '<a href="'
                                                        + record.workingCopy.googleDocUrl + '" target="_blank">'
                                                        + this.msg("details.banner.google-docs.link") + '</a>');
                                            }
                                        }
                                        /* Regular Working Copy handling */
                                        else
                                        {
                                            if (record.workingCopy && bannerUser.userName === Alfresco.constants.USERNAME)
                                            {
                                                html = this.msg("details.banner."
                                                        + (record.workingCopy.isWorkingCopy ? "editing" : "lock-owner"));
                                            }
                                            else
                                            {
                                                html = this.msg("details.banner.locked", bannerLink);
                                            }
                                        }
                                        return html;
                                    });

                            /**
                             * Date
                             */
                            this.registerRenderer("date", function(record, label)
                            {
                                var jsNode = record.jsNode, properties = jsNode.properties, html = "";

                                var dateI18N = "modified", dateProperty = properties.modified.iso8601;
                                if (record.workingCopy && record.workingCopy.isWorkingCopy)
                                {
                                    dateI18N = "editing-started";
                                }
                                else if (dateProperty === properties.created.iso8601)
                                {
                                    dateI18N = "created";
                                }

                                html = '<span class="item">'
                                        + label
                                        + this.msg("details." + dateI18N + "-by", $relTime(dateProperty), Alfresco.DocumentList
                                                .generateUserLink(this, properties.modifier)) + '</span>';

                                return html;
                            });

                            /**
                             * File size
                             */
                            this.registerRenderer("size", function(record, label)
                            {
                                var jsNode = record.jsNode, properties = jsNode.properties, html = "";

                                if (!jsNode.isContainer && !jsNode.isLink)
                                {
                                    html += '<span class="item">' + label + Alfresco.util.formatFileSize(jsNode.size) + '</span>';
                                }

                                return html;
                            });

                            /**
                             * Description
                             */
                            this
                                    .registerRenderer("description",
                                            function(record, label)
                                            {
                                                var jsNode = record.jsNode, properties = jsNode.properties, id = Alfresco.util
                                                        .generateDomId(), html = '<span id="' + id + '" class="faded">' + label
                                                        + this.msg("details.description.none") + '</span>';

                                                // Description non-blank?
                                                if (properties.description && properties.description !== "")
                                                {
                                                    html = '<span id="' + id + '" class="item">' + label
                                                            + $links($html(properties.description)) + '</span>';
                                                }

                                                return html;
                                            });
                        },

                        /**
                         * Configure standard view renderers
                         * 
                         * @method _setupViewRenderers
                         */
                        _setupViewRenderers : function()
                        {
                            var simpleViewRenderer = new PRODYNA.renderer.ContentTrendsListViewRenderer("simple-trends");
                            simpleViewRenderer.actionsColumnWidth = 80;
                            simpleViewRenderer.actionsSplitAtModifier = 0;
                            simpleViewRenderer.renderCellThumbnail = function(scope, elCell, oRecord, oColumn, oData)
                            {
                                var record = oRecord.getData(), node = record.jsNode, properties = node.properties, name = record.displayName, isContainer = node.isContainer, isLink = node.isLink, extn = name
                                        .substring(name.lastIndexOf(".")), imgId = node.nodeRef.nodeRef; // DD added

                                oColumn.width = 40;
                                Dom.setStyle(elCell, "width", oColumn.width + "px");
                                Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

                                if (isContainer)
                                {
                                    elCell.innerHTML = '<span class="folder-small">' + (isLink ? '<span class="link"></span>' : '')
                                            + Alfresco.DocumentList.generateFileFolderLinkMarkup(scope, record) + '<img id="' + imgId
                                            + '" src="' + Alfresco.constants.URL_RESCONTEXT
                                            + 'components/documentlibrary/images/folder-32.png" /></a>';
                                }
                                else
                                {
                                    var id = scope.id + '-preview-' + oRecord.getId();
                                    elCell.innerHTML = '<span id="' + id + '" class="icon32">'
                                            + (isLink ? '<span class="link"></span>' : '')
                                            + Alfresco.DocumentList.generateFileFolderLinkMarkup(scope, record) + '<img id="' + imgId
                                            + '" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/filetypes/'
                                            + Alfresco.util.getFileIcon(name) + '" alt="' + extn + '" title="' + $html(name)
                                            + '" /></a></span>';

                                    // Preview tooltip
                                    scope.previewTooltips.push(id);
                                }
                            };
                            simpleViewRenderer.setupRenderer = function(scope)
                            {
                                Dom.addClass(scope.id + this.buttonElementIdSuffix, this.buttonCssClass);
                                // Tooltip for thumbnail in Simple View
                                scope.widgets.previewTooltip = new YAHOO.widget.Tooltip(scope.id + "-previewTooltip",
                                {
                                    width : "108px"
                                });
                                scope.widgets.previewTooltip.contextTriggerEvent
                                        .subscribe(function(type, args)
                                        {
                                            var context = args[0], oRecord = scope.widgets.dataTable.getRecord(context.id), record = oRecord
                                                    .getData();

                                            this.cfg.setProperty("text", '<img src="' + Alfresco.DocumentList.generateThumbnailUrl(record)
                                                    + '" />');
                                        });
                            };

                            this.registerViewRenderer(simpleViewRenderer);

                            YAHOO.Bubbling.fire("postSetupViewRenderers",
                            {
                                scope : this,
                                eventGroup : this.id
                            });
                        },

                        /**
                         * DataSource set-up and event registration
                         * 
                         * @method _setupDataSource
                         * @protected
                         */
                        _setupDataSource : function()
                        {
                            var me = this;

                            // DataSource definition
                            this.widgets.dataSource = new YAHOO.util.DataSource(this.dataSourceUrl,
                            {
                                responseType : YAHOO.util.DataSource.TYPE_JSON,
                                responseSchema :
                                {
                                    resultsList : "items",
                                    metaFields :
                                    {
                                        paginationRecordOffset : "startIndex",
                                        totalRecords : "totalRecords",
                                        totalRecordsUpper : "totalRecordsUpper" // if null then totalRecords is accurate else totalRecords
                                    // is lower estimate (if
                                    // -1 upper estimate is unknown)
                                    }
                                }
                            });

                            // Intercept data returned from data webscript to extract custom metadata
                            this.widgets.dataSource.doBeforeCallback = function(oRequest, oFullResponse, oParsedResponse)
                            {
                                me.doclistMetadata = oFullResponse.metadata;

                                // Fire event with parent metadata
                                YAHOO.Bubbling.fire("doclistMetadata",
                                {
                                    metadata : me.doclistMetadata,
                                    eventGroup : me.id
                                });

                                // Check for parent node - won't be one for multi-parent queries (e.g. tags)
                                var permissions = null;
                                if (me.doclistMetadata.parent)
                                {
                                    permissions = me.doclistMetadata.parent.permissions;
                                    if (permissions && permissions.user)
                                    {
                                        // Container userAccess event
                                        YAHOO.Bubbling.fire("userAccess",
                                        {
                                            userAccess : permissions.user,
                                            eventGroup : me.id
                                        });
                                    }
                                }

                                if (typeof me.viewRenderers[me.options.viewRendererName] === "object")
                                {
                                    me.viewRenderers[me.options.viewRendererName].renderEmptyDataSourceHtml(me, permissions);
                                }

                                return oParsedResponse;
                            };
                        },

                        /**
                         * DataTable set-up and event registration
                         * 
                         * @method _setupDataTable
                         * @protected
                         */
                        _setupDataTable : function()
                        {
                            var me = this;

                            // DataTable column defintions
                            var columnDefinitions = [
                            {
                                key : "status",
                                label : "Status",
                                sortable : false,
                                formatter : this.fnRenderCellStatus(),
                                width : 16
                            },
                            {
                                key : "thumbnail",
                                label : "Preview",
                                sortable : false,
                                formatter : this.fnRenderCellThumbnail(),
                                width : 100
                            },
                            {
                                key : "fileName",
                                label : "Description",
                                sortable : false,
                                formatter : this.fnRenderCellDescription()
                            },
                            {
                                key : "actions",
                                label : "Actions",
                                sortable : false,
                                formatter : this.fnRenderCellActions(),
                                width : 200
                            } ];

                            // DataTable definition
                            this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + "-documents", columnDefinitions,
                                    this.widgets.dataSource,
                                    {
                                        renderLoopSize : this.options.usePagination ? 16 : Alfresco.util.RENDERLOOPSIZE,
                                        initialLoad : false,
                                        dynamicData : true,
                                        MSG_EMPTY : this.msg("message.loading")
                                    });

                            // Update totalRecords on the fly with value from server
                            this.widgets.dataTable.handleDataReturnPayload = function(oRequest, oResponse, oPayload)
                            {
                                me.totalRecords = oResponse.meta.totalRecords;
                                me.totalRecordsUpper = oResponse.meta.totalRecordsUpper;
                                return oResponse.meta;
                            };

                            // Custom error messages
                            this._setDefaultDataTableErrors(this.widgets.dataTable);

                            // Hook tableMsgShowEvent to clear out fixed-pixel width on <table> element (breaks resizer)
                            this.widgets.dataTable.subscribe("tableMsgShowEvent", function(oArgs)
                            {
                                // NOTE: Scope needs to be DataTable
                                this._elMsgTbody.parentNode.style.width = "";
                            });

                            // Override abstract function within DataTable to set custom error message
                            this.widgets.dataTable.doBeforeLoadData = function(sRequest, oResponse, oPayload)
                            {
                                if (oResponse.error)
                                {
                                    try
                                    {
                                        var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                                        me.widgets.dataTable.set("MSG_ERROR", response.message);
                                    }
                                    catch (e)
                                    {
                                        me._setDefaultDataTableErrors(me.widgets.dataTable);
                                    }
                                }
                                else if (oResponse.results.length === 0)
                                {
                                    // We don't get an renderEvent for an empty recordSet, but we'd like one anyway
                                    this.fireEvent("renderEvent",
                                    {
                                        type : "renderEvent"
                                    });
                                }
                                else
                                {
                                    // Add an Alfresco.util.Node instance to each result
                                    var i, ii;
                                    for (i = 0, ii = oResponse.results.length; i < ii; i++)
                                    {
                                        oResponse.results[i].jsNode = new Alfresco.util.Node(oResponse.results[i].node);
                                    }
                                }

                                // Must return true to have the "Loading..." message replaced by the error message
                                return true;
                            };

                            // Rendering complete event handler
                            this.widgets.dataTable
                                    .subscribe(
                                            "renderEvent",
                                            function()
                                            {
                                                Alfresco.logger.debug("DataTable renderEvent");

                                                // IE6 fix for long filename rendering issue
                                                if (0 < YAHOO.env.ua.ie && YAHOO.env.ua.ie < 7)
                                                {
                                                    var ie6fix = this.widgets.dataTable.getTableEl().parentNode;
                                                    // noinspection SillyAssignmentJS
                                                    ie6fix.className = ie6fix.className;
                                                }

                                                // Update the paginator if it's been created
                                                if (this.widgets.paginator)
                                                {
                                                    Alfresco.logger.debug("Setting paginator state: page=" + this.currentPage
                                                            + ", totalRecords=" + this.totalRecords);

                                                    this.widgets.paginator.setState(
                                                    {
                                                        page : this.currentPage,
                                                        totalRecords : this.totalRecords
                                                    });

                                                    if (this.totalRecordsUpper)
                                                    {
                                                        this.widgets.paginator.set("pageReportTemplate", this
                                                                .msg("pagination.template.page-report.more"));
                                                    }
                                                    else
                                                    {
                                                        this.widgets.paginator.set("pageReportTemplate", this
                                                                .msg("pagination.template.page-report"));
                                                    }

                                                    this.widgets.paginator.render();
                                                }

                                                if (this.listUpdated)
                                                {
                                                    // Scroll up (only) to the top of the documents
                                                    var yPos = Dom.getY(this.id + "-documents"), yScroll = YAHOO.env.ua.ie > 0 ? ((document.compatMode && document.compatMode !== "BackCompat") ? document.documentElement
                                                            : document.body).scrollTop
                                                            : window.scrollY;

                                                    if (yScroll > yPos)
                                                    {
                                                        window.scrollTo(0, yPos);
                                                    }
                                                }

                                                // Deferred functions specified?
                                                var i, j;
                                                for (i = 0, j = this.afterDocListUpdate.length; i < j; i++)
                                                {
                                                    this.afterDocListUpdate[i].call(this);
                                                }
                                                this.afterDocListUpdate = [];

                                                // Register preview tooltips
                                                this.widgets.previewTooltip.cfg.setProperty("context", this.previewTooltips);

                                                this.widgets.dataTable.set("renderLoopSize", this.options.usePagination ? 16
                                                        : Alfresco.util.RENDERLOOPSIZE);
                                            }, this, true);

                            // Enable row highlighting
                            this.widgets.dataTable.subscribe("rowMouseoverEvent", this.onEventHighlightRow, this, true);
                            this.widgets.dataTable.subscribe("rowMouseoutEvent", this.onEventUnhighlightRow, this, true);
                        },

                        /**
                         * Custom event handler to highlight row.
                         * 
                         * @method onEventHighlightRow
                         * @param oArgs.event
                         *            {HTMLEvent} Event object.
                         * @param oArgs.target
                         *            {HTMLElement} Target element.
                         */
                        onEventHighlightRow : function(oArgs)
                        {
                            if (typeof this.viewRenderers[this.options.viewRendererName] === "object")
                            {
                                this.viewRenderers[this.options.viewRendererName].onEventHighlightRow(this, oArgs);
                            }
                        },

                        /**
                         * Custom event handler to unhighlight row.
                         * 
                         * @method onEventUnhighlightRow
                         * @param oArgs.event
                         *            {HTMLEvent} Event object.
                         * @param oArgs.target
                         *            {HTMLElement} Target element.
                         */
                        onEventUnhighlightRow : function(oArgs)
                        {
                            if (typeof this.viewRenderers[this.options.viewRendererName] === "object")
                            {
                                this.viewRenderers[this.options.viewRendererName].onEventUnhighlightRow(this, oArgs);
                            }
                        },

                        onConfigContentTrendsClick : function(e)
                        {
                            Event.stopEvent(e);

                            var actionUrl = Alfresco.constants.URL_SERVICECONTEXT + "components/dashlets/content-trends/"
                                    + encodeURIComponent(this.options.componentId);

                            if (!this.configDialog)
                            {
                                this.configDialog = new Alfresco.module.SimpleDialog(this.id + "-configDialog").setOptions(
                                        {
                                            templateUrl : Alfresco.constants.URL_SERVICECONTEXT
                                                    + "modules/content-trends/config-dialog?componentId="
                                                    + encodeURIComponent(this.options.componentId),
                                            onSuccess :
                                            {
                                                fn : function(response)
                                                {
                                                    // successful save - we do not yet have an "update in-place", so reload the dashboard
                                                    window.location.reload();
                                                },
                                                scope : this
                                            },
                                            destroyOnHide : true,
                                            actionUrl : actionUrl
                                        }).show();
                            }
                        },

                        /**
                         * Sort Field select button click handler
                         * 
                         * @method onSortField
                         * @param type
                         *            {string} Event type, e.g. "click"
                         * @param args
                         *            {array} Arguments array, [0] = DomEvent, [1] = EventTarget
                         * @param obj
                         *            {object} Object passed back from subscribe method
                         */
                        onSortField : function(type, args, obj)
                        {
                            var domEvent = args[0], eventTarget = args[1];

                            if (eventTarget)
                            {
                                this.options.sortField = eventTarget.value;
                                this.widgets.sortField.set("label", eventTarget.cfg.getProperty("text"));

                                this.services.preferences.set(PREF_CONTENT_TRENDS_PREFIX + this.options.componentId
                                        + PREF_SORT_FIELD_SUFFIX, this.options.sortField);

                                YAHOO.Bubbling.fire("contentTrendsRefresh",
                                {
                                    scope : this,
                                    eventGroup : this.id
                                });
                            }

                            Event.preventDefault(domEvent);
                        },

                        onActionClick : function(layer, args)
                        {
                            // Check the event is directed towards this instance
                            if ($hasEventInterest(this.id, args))
                            {
                                var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
                                if (owner !== null)
                                {
                                    if (typeof this[owner.title] === "function")
                                    {
                                        args[1].stop = true;
                                        var elIdentifier = args[1].target;
                                        if (typeof this.viewRenderers[this.options.viewRendererName] === "object")
                                        {
                                            elIdentifier = this.viewRenderers[this.options.viewRendererName]
                                                    .getDataTableRecordIdFromRowElement(this, args[1].target);
                                        }
                                        var record = this.widgets.dataTable.getRecord(elIdentifier).getData();
                                        this[owner.title].call(this, record, owner);
                                    }
                                }
                            }
                        },

                        onContentTrendsRefresh : function(layer, args)
                        {
                            if ($hasEventInterest(this.id, args))
                            {
                                this._updateDocList.call(this);
                            }
                        },

                        /**
                         * PRIVATE FUNCTIONS
                         */

                        /**
                         * Resets the YUI DataTable errors to our custom messages NOTE: Scope could be YAHOO.widget.DataTable, so can't use
                         * "this"
                         * 
                         * @method _setDefaultDataTableErrors
                         * @param dataTable
                         *            {object} Instance of the DataTable
                         */
                        _setDefaultDataTableErrors : function(dataTable)
                        {
                            var msg = Alfresco.util.message;
                            dataTable.set("MSG_EMPTY", msg("message.empty", "PRODYNA.dashlet.ContentTrends"));
                            dataTable.set("MSG_ERROR", msg("message.error", "PRODYNA.dashlet.ContentTrends"));
                        },

                        /**
                         * Updates document list by calling data webscript with current site and path
                         * 
                         * @method _updateDocList
                         * @param p_obj.filter
                         *            {object} Optional filter to navigate with
                         * @param p_obj.page
                         *            {string} Optional page to navigate to (defaults to this.currentPage)
                         */
                        _updateDocList : function(p_obj)
                        {
                            p_obj = p_obj || {};
                            Alfresco.logger.debug("ContentTrends__updateDocList: ", p_obj.page);
                            var successPage = p_obj.page !== undefined ? p_obj.page : this.currentPage, me = this, params =
                            {
                                page : successPage
                            };

                            // Reset the custom error messages
                            this._setDefaultDataTableErrors(this.widgets.dataTable);

                            // Reset preview tooltips array
                            this.previewTooltips = [];

                            // More Actions menu no longer relevant
                            this.showingMoreActions = false;

                            var successHandler = function(sRequest, oResponse, oPayload)
                            {
                                Alfresco.logger.debug("currentPage was [" + this.currentPage + "] now [" + successPage + "]");
                                this.currentPage = successPage;

                                // Call destroy view on all viewRenderers then renderView on the selected view
                                var i, ii;
                                for (i = 0, ii = this.options.viewRendererNames.length; i < ii; i++)
                                {
                                    this.viewRenderers[this.options.viewRendererNames[i]].destroyView(this, sRequest, oResponse, oPayload);
                                }
                                this.viewRenderers[this.options.viewRendererName].renderView(this, sRequest, oResponse, oPayload);
                            };

                            var failureHandler = function(sRequest, oResponse)
                            {
                                // Clear out deferred functions
                                this.afterDocListUpdate = [];

                                if (oResponse.status === 401)
                                {
                                    // Our session has likely timed-out, so refresh to offer the login page
                                    window.location.reload(true);
                                }
                                else
                                {
                                    try
                                    {
                                        var fnAfterFailedUpdate = function(responseMsg)
                                        {
                                            return function()
                                            {
                                                this.widgets.paginator.setState(
                                                {
                                                    totalRecords : 0
                                                });
                                                this.widgets.paginator.render();
                                                this.widgets.dataTable.set("MSG_ERROR", responseMsg);
                                                this.widgets.dataTable.showTableMessage(responseMsg, YAHOO.widget.DataTable.CLASS_ERROR);
                                            };
                                        };

                                        this.afterDocListUpdate
                                                .push(fnAfterFailedUpdate(YAHOO.lang.JSON.parse(oResponse.responseText).message));
                                        this.widgets.dataTable.initializeTable();
                                        this.widgets.dataTable.render();
                                        this.listUpdated = false;
                                    }
                                    catch (e)
                                    {
                                        Alfresco.logger.error(e);
                                        this._setDefaultDataTableErrors(this.widgets.dataTable);
                                    }
                                }
                            };

                            // Update the DataSource
                            var requestParams = this._buildDocListParams(params);
                            Alfresco.logger.debug("DataSource requestParams: ", requestParams);
                            this.widgets.dataSource.sendRequest(requestParams,
                            {
                                success : successHandler,
                                failure : failureHandler,
                                scope : this
                            });
                        },

                        /**
                         * Build URI parameter string for doclist JSON data webscript
                         * 
                         * @method _buildDocListParams
                         * @param p_obj.page
                         *            {string} Page number
                         * @param p_obj.pageSize
                         *            {string} Number of items per page
                         * @param p_obj.path
                         *            {string} Path to query
                         * @param p_obj.type
                         *            {string} Filetype to filter: "all", "documents", "folders"
                         * @param p_obj.site
                         *            {string} Current site
                         * @param p_obj.container
                         *            {string} Current container
                         */
                        _buildDocListParams : function(p_obj)
                        {
                            // Essential defaults
                            var siteMode = $isValueSet(this.options.siteId), obj =
                            {
                                path : "",
                                type : this.options.defaultScore,
                                site : this.options.siteId,
                                container : this.options.containerId,
                                filter : this.currentFilter
                            };

                            // Pagination in use?
                            if (this.options.usePagination)
                            {
                                obj.page = this.widgets.paginator.getCurrentPage() || this.currentPage;
                                obj.pageSize = this.widgets.paginator.getRowsPerPage();
                            }

                            // Passed-in overrides
                            if (typeof p_obj === "object")
                            {
                                obj = YAHOO.lang.merge(obj, p_obj);
                            }

                            // Build the URI stem
                            var uriPart = siteMode ? "site/{site}/{container}" : "node/alfresco/company/home", params = YAHOO.lang
                                    .substitute("/{type}/" + uriPart,
                                    {
                                        type : encodeURIComponent(obj.type),
                                        site : encodeURIComponent(obj.site),
                                        container : encodeURIComponent(obj.container)
                                    });

                            // Filter parameters
                            params += "?filter=" + encodeURIComponent(obj.filter);

                            // Paging parameters
                            if (this.options.usePagination)
                            {
                                params += "&size=" + obj.pageSize + "&pos=" + obj.page;
                            }

                            // Sort parameters
                            if (this.options.sortField != null)
                            {
                                params += "&sortField=" + encodeURIComponent(this.options.sortField);
                            }

                            if (!siteMode)
                            {
                                // Repository mode (don't resolve Site-based folders)
                                params += "&libraryRoot=" + encodeURIComponent(this.options.rootNode.toString());
                            }

                            // View mode and No-cache
                            params += "&noCache=" + new Date().getTime();

                            return params;
                        },

                        // dummy needed for standard viewRenderer
                        _removeDragAndDrop : function()
                        {

                        },

                        _addDragAndDrop : function()
                        {

                        }
                    }, true);
})();