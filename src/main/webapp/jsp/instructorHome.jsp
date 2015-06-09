<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib tagdir="/WEB-INF/tags/instructor" prefix="ti" %>
<c:set var="jsIncludes">
    <script type="text/javascript" src="/js/instructor.js"></script>
    <script type="text/javascript" src="/js/instructorHome.js"></script>
    <script type="text/javascript" src="/js/ajaxResponseRate.js"></script>
    <script type="text/javascript" src="/js/instructorFeedbackAjaxRemindModal.js"></script>
</c:set>
<ti:instructorPage pageTitle="TEAMMATES - Instructor" bodyTitle="Instructor Home" jsIncludes="${jsIncludes}">
    <div class="row">
        <div class="col-md-2 pull-right">
            <a class="btn btn-primary btn-md" href="${data.instructorCourseLink}" 
                id="addNewCourse">Add New Course</a>
        </div>
        <div class="col-md-6 pull-right">
            <ti:homeSearch />
        </div>
    </div>
    <br />
    <t:statusMessage />
    <ti:remindModal />
    <c:if test="${data.account.instructor}">
        <c:if test="${data.unarchivedCoursesCount > 1}">
            <div class="row">
                <ti:homeSort />
            </div>
        </c:if>
        <br />
        <c:forEach items="${data.courseTables}" var="courseTable" varStatus="i">
            <ti:homeCoursePanel courseTable="${courseTable}" index="${i.index}">
                <c:if test="${not empty courseTable.rows}">
                    <ti:homeCourseTable sessionRows="${courseTable.rows}" />
                </c:if>
            </ti:homeCoursePanel>
        </c:forEach>
        <ti:copyModal />
    </c:if>
</ti:instructorPage>