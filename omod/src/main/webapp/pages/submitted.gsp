<!--

    This Source Code Form is subject to the terms of the Mozilla Public License,
    v. 2.0. If a copy of the MPL was not distributed with this file, You can
    obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
    the terms of the Healthcare Disclaimer located at http://openmrs.org/license.

    Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
    graphic logo is a trademark of OpenMRS Inc.

-->

<%
    ui.decorateWith("appui", "standardEmrPage", [ title: ui.message("casereport.label") ])

    ui.includeJavascript("uicommons", "angular.min.js")
    ui.includeJavascript("uicommons", "angular-resource.min.js")
    ui.includeJavascript("uicommons", "angular-ui/angular-ui-router.min.js")
    ui.includeJavascript("uicommons", "angular-app.js")
    ui.includeJavascript("uicommons", "ngDialog/ngDialog.js")
    ui.includeJavascript("uicommons", "angular-common.js")
    ui.includeJavascript("uicommons", "angular-common-error.js")
    ui.includeJavascript("uicommons", "angular-sanitize.min.js")
    ui.includeJavascript("uicommons", "filters/display.js")
    ui.includeJavascript("uicommons", "filters/serverDate.js")
    ui.includeJavascript("uicommons", "services/personService.js")
    ui.includeJavascript("casereport", "lib/bootstrap/ui-bootstrap-tpls-2.2.0.min.js")
    ui.includeJavascript("casereport", "lib/filesaver.js")
    ui.includeJavascript("casereport", "caseReportService.js")
    ui.includeJavascript("casereport", "casereport-filters.js")
    ui.includeJavascript("casereport", "submitted.js")

    ui.includeCss("uicommons", "ngDialog/ngDialog.min.css")
    ui.includeCss("casereport", "lib/bootstrap/bootstrap.min.css")
    ui.includeCss("casereport", "casereport.css")
%>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: "/" + OPENMRS_CONTEXT_PATH + "/index.htm" },
        { label: "${ ui.message('casereport.label')}" , link: '${ui.pageLink("casereport", "caseReports")}'},
        {label: "${ ui.message("casereport.submittedCaseReports.label")}" }
    ];
</script>

<div id="casereports-submitted">
    <ui-view/>
</div>

<script type="text/javascript">
    angular.bootstrap("#casereports-submitted", [ "casereports.submitted" ])
</script>