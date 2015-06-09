<%@ tag description="instructorHome - Course table" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ tag import="teammates.common.util.Const" %>
<%@ attribute name="sessionRows" type="java.util.Collection" required="true" %>
<table class="table-responsive table table-striped table-bordered">
    <thead>
        <tr>
            <th id="button_sortname" onclick="toggleSort(this,1);"
                class="button-sort-none">
                Session Name<span class="icon-sort unsorted"></span></th>
            <th>Status</th>
            <th>
                <span title="<%= Const.Tooltips.FEEDBACK_SESSION_RESPONSE_RATE %>" 
                      data-toggle="tooltip" data-placement="top">Response Rate</span>
            </th>
            <th class="no-print">Action(s)</th>
        </tr>
    </thead>
    <c:forEach items="${sessionRows}" var="sessionRow" varStatus="i">
        <tr id="session${i.index}">
            <td>
                ${sessionRow.name}
            </td>
            <td>
                <span title="${sessionRow.tooltip}" data-toggle="tooltip" data-placement="top">
                    ${sessionRow.status}
                </span>
            </td>
            <td class="session-response-for-test${sessionRow.recent}">
                <a oncontextmenu="return false;" href="${sessionRow.href}">Show</a>
            </td>
            <td class="no-print">
                ${sessionRow.actions}
            </td>
        </tr>
    </c:forEach>
</table>