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
    var $html = Alfresco.util.encodeHTML, renderer = function(record, label, propertyName)
    {
        var urlContext = Alfresco.constants.URL_RESCONTEXT + "components/content-trends/icons/", iconStyle = 'style="background-image:url('
                + urlContext + '{icon}-16.png)"', valueMarkup = '<span class="item content-trends-score" title="{title}" {iconStyle}><span><span title="{valueTitle}">{value}</span> / <span title="{changeTitle}" class="{changeType}">{change} %</span></span></span>', value = record.jsNode.properties[propertyName], change = record.jsNode.properties[propertyName
                + "Change"], obj = null, changeVal = (change == null || change == "") ? "-" : change, changeType = (!(change == null || change == "") && change) < 0 ? "negative"
                : ((change == null || change == "" || change == 0) ? "neutral" : "positive");

        obj =
        {
            iconStyle : iconStyle,
            value : value,
            change : changeVal,
            changeType : changeType,
            icon : propertyName,
            title : $html(this.msg("label.content-trends." + propertyName)),
            valueTitle : $html(this.msg("label.content-trends.value")),
            changeTitle : $html(this.msg("label.content-trends.change"))
        };

        var html = YAHOO.lang.substitute(valueMarkup, obj);
        return html;

    }, propertyNames = [ "ct_totalScore", "ct_viewScore", "ct_editScore", "ct_tagScore", "ct_ratingScore", "ct_commentScore" ], i = 0;

    var register = function(property)
    {
        YAHOO.Bubbling.fire("registerRenderer",
        {
            propertyName : property,
            renderer : function(record, label)
            {
                return renderer.call(this, record, label, property);
            }
        });
    };

    for (; i < propertyNames.length; i++)
    {
        var property = propertyNames[i];
        register(property);
    }
})();