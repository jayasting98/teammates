package teammates.ui.webapi;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.util.Const;
import teammates.ui.output.FeedbackSessionData;
import teammates.ui.output.ResponseVisibleSetting;
import teammates.ui.output.SessionVisibleSetting;
import teammates.ui.request.FeedbackSessionUpdateRequest;
import teammates.ui.request.InvalidHttpRequestBodyException;

/**
 * SUT: {@link UpdateFeedbackSessionAction}.
 */
public class UpdateFeedbackSessionActionTest extends BaseActionTest<UpdateFeedbackSessionAction> {

    @Override
    protected String getActionUri() {
        return Const.ResourceURIs.SESSION;
    }

    @Override
    protected String getRequestMethod() {
        return PUT;
    }

    @Override
    @Test
    protected void testExecute() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        ______TS("Not enough parameters");

        FeedbackSessionUpdateRequest updateRequest = getTypicalFeedbackSessionUpdateRequest();

        verifyHttpParameterFailure(updateRequest);
        verifyHttpParameterFailure(updateRequest, Const.ParamsNames.COURSE_ID, session.getCourseId());
        verifyHttpParameterFailure(updateRequest, Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName());

        ______TS("success: Typical case");

        String[] param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };

        UpdateFeedbackSessionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);

        FeedbackSessionData response = (FeedbackSessionData) r.getOutput();

        session = logic.getFeedbackSession(session.getFeedbackSessionName(), session.getCourseId());
        assertEquals(session.getCourseId(), response.getCourseId());
        assertEquals(session.getTimeZone(), response.getTimeZone());
        assertEquals(session.getFeedbackSessionName(), response.getFeedbackSessionName());

        assertEquals(session.getInstructions(), response.getInstructions());

        assertEquals(session.getStartTime().toEpochMilli(), response.getSubmissionStartTimestamp());
        assertEquals(session.getEndTime().toEpochMilli(), response.getSubmissionEndTimestamp());
        assertEquals(session.getGracePeriodMinutes(), response.getGracePeriod().longValue());

        assertEquals(SessionVisibleSetting.CUSTOM, response.getSessionVisibleSetting());
        assertEquals(session.getSessionVisibleFromTime().toEpochMilli(),
                response.getCustomSessionVisibleTimestamp().longValue());
        assertEquals(ResponseVisibleSetting.CUSTOM, response.getResponseVisibleSetting());
        assertEquals(session.getResultsVisibleFromTime().toEpochMilli(),
                response.getCustomResponseVisibleTimestamp().longValue());

        assertEquals(session.isClosingEmailEnabled(), response.getIsClosingEmailEnabled());
        assertEquals(session.isPublishedEmailEnabled(), response.getIsPublishedEmailEnabled());

        assertEquals(session.getCreatedTime().toEpochMilli(), response.getCreatedAtTimestamp());
        assertNull(session.getDeletedTime());

        assertEquals("instructions", response.getInstructions());
        assertEquals(1444003051000L, response.getSubmissionStartTimestamp());
        assertEquals(1546003051000L, response.getSubmissionEndTimestamp());
        assertEquals(5, response.getGracePeriod().longValue());

        assertEquals(SessionVisibleSetting.CUSTOM, response.getSessionVisibleSetting());
        assertEquals(1440003051000L, response.getCustomSessionVisibleTimestamp().longValue());

        assertEquals(ResponseVisibleSetting.CUSTOM, response.getResponseVisibleSetting());
        assertEquals(1547003051000L, response.getCustomResponseVisibleTimestamp().longValue());

        assertFalse(response.getIsClosingEmailEnabled());
        assertFalse(response.getIsPublishedEmailEnabled());

        assertNotNull(response.getCreatedAtTimestamp());
        assertNull(response.getDeletedAtTimestamp());

        Map<String, Long> expectedStudentDeadlines = convertDeadlinesToLong(session.getStudentDeadlines());
        assertEquals(expectedStudentDeadlines, response.getStudentDeadlines());
        Map<String, Long> expectedInstructorDeadlines = convertDeadlinesToLong(session.getInstructorDeadlines());
        assertEquals(expectedInstructorDeadlines, response.getInstructorDeadlines());
    }

    @Test
    public void testExecute_changeDeadlineForStudents_shouldChangeDeadlinesCorrectlyWhenAppropriate() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");

        Map<String, Long> expectedStudentDeadlines = convertDeadlinesToLong(session.getStudentDeadlines());
        Instant endTime = session.getEndTime();
        // These are arbitrary.
        long endTimePlus1Day = endTime.plus(Duration.ofDays(1)).toEpochMilli();
        long endTimePlus2Days = endTime.plus(Duration.ofDays(2)).toEpochMilli();

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };

        ______TS("create new deadline extension for student");

        FeedbackSessionUpdateRequest updateRequest = getTypicalFeedbackSessionUpdateRequest();
        Map<String, Long> newStudentDeadlines = convertDeadlinesToLong(updateRequest.getStudentDeadlines());
        newStudentDeadlines.put("student1InCourse1@gmail.tmt", endTimePlus1Day);
        updateRequest.setStudentDeadlines(newStudentDeadlines);

        UpdateFeedbackSessionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);
        FeedbackSessionData response = (FeedbackSessionData) r.getOutput();

        expectedStudentDeadlines.put("student1InCourse1@gmail.tmt", endTimePlus1Day);
        assertEquals(expectedStudentDeadlines, response.getStudentDeadlines());

        ______TS("update deadline extension for student");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newStudentDeadlines = convertDeadlinesToLong(updateRequest.getStudentDeadlines());
        newStudentDeadlines.put("student1InCourse1@gmail.tmt", endTimePlus2Days);
        updateRequest.setStudentDeadlines(newStudentDeadlines);

        a = getAction(updateRequest, param);
        r = getJsonResult(a);
        response = (FeedbackSessionData) r.getOutput();

        expectedStudentDeadlines.put("student1InCourse1@gmail.tmt", endTimePlus2Days);
        assertEquals(expectedStudentDeadlines, response.getStudentDeadlines());

        ______TS("delete deadline extension for student");

        // The typical update request does not contain the course 1 student 1's email.
        updateRequest = getTypicalFeedbackSessionUpdateRequest();

        a = getAction(updateRequest, param);
        r = getJsonResult(a);
        response = (FeedbackSessionData) r.getOutput();

        // The deadline for course 1 student 1 was deleted; the map no longer contains a deadline for them.
        expectedStudentDeadlines.remove("student1InCourse1@gmail.tmt");
        assertEquals(expectedStudentDeadlines, response.getStudentDeadlines());

        ______TS("C_UD deadline extensions for students simultaneously");

        assertNull(expectedStudentDeadlines.get("student1InCourse1@gmail.tmt"));
        assertNotEquals(endTimePlus2Days, expectedStudentDeadlines.get("student3InCourse1@gmail.tmt"));
        assertNotNull(expectedStudentDeadlines.get("student4InCourse1@gmail.tmt"));

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newStudentDeadlines = convertDeadlinesToLong(updateRequest.getStudentDeadlines());
        newStudentDeadlines.put("student1InCourse1@gmail.tmt", endTimePlus1Day);
        newStudentDeadlines.put("student3InCourse1@gmail.tmt", endTimePlus2Days);
        newStudentDeadlines.remove("student4InCourse1@gmail.tmt");
        updateRequest.setStudentDeadlines(newStudentDeadlines);

        a = getAction(updateRequest, param);
        r = getJsonResult(a);
        response = (FeedbackSessionData) r.getOutput();

        // Create deadline.
        expectedStudentDeadlines.put("student1InCourse1@gmail.tmt", endTimePlus1Day);
        // Update deadline.
        expectedStudentDeadlines.put("student3InCourse1@gmail.tmt", endTimePlus2Days);
        // Delete deadline.
        expectedStudentDeadlines.remove("student4InCourse1@gmail.tmt");
        assertEquals(expectedStudentDeadlines, response.getStudentDeadlines());

        ______TS("change deadline extension for non-existent student; should throw EntityNotFoundException");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newStudentDeadlines = convertDeadlinesToLong(updateRequest.getStudentDeadlines());
        newStudentDeadlines.put("nonExistentStudent@gmail.tmt", endTimePlus1Day);
        updateRequest.setStudentDeadlines(newStudentDeadlines);

        a = getAction(updateRequest, param);
        assertThrows(EntityNotFoundException.class, a::execute);

        ______TS("change deadline extension for student to the same time as the end time; "
                + "should throw InvalidHttpRequestBodyException");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        Instant newEndTime = updateRequest.getSubmissionEndTime();
        newStudentDeadlines = convertDeadlinesToLong(updateRequest.getStudentDeadlines());
        newStudentDeadlines.put("student1InCourse1@gmail.tmt", newEndTime.toEpochMilli());
        updateRequest.setStudentDeadlines(newStudentDeadlines);

        a = getAction(updateRequest, param);
        assertThrows(InvalidHttpRequestBodyException.class, a::execute);

        ______TS("change deadline extension for student to before end time; "
                + "should throw InvalidHttpRequestBodyException");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newEndTime = updateRequest.getSubmissionEndTime();
        newStudentDeadlines = convertDeadlinesToLong(updateRequest.getStudentDeadlines());
        newStudentDeadlines.put("student1InCourse1@gmail.tmt", newEndTime.plus(Duration.ofMillis(-1)).toEpochMilli());
        updateRequest.setStudentDeadlines(newStudentDeadlines);

        a = getAction(updateRequest, param);
        assertThrows(InvalidHttpRequestBodyException.class, a::execute);

        logoutUser();
    }

    @Test
    public void testExecute_changeDeadlineForInstructors_shouldChangeDeadlinesCorrectlyWhenAppropriate() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");

        Map<String, Long> expectedInstructorDeadlines = convertDeadlinesToLong(session.getInstructorDeadlines());
        Instant endTime = session.getEndTime();
        // These are arbitrary.
        long endTimePlus1Day = endTime.plus(Duration.ofDays(1)).toEpochMilli();
        long endTimePlus2Days = endTime.plus(Duration.ofDays(2)).toEpochMilli();

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };

        ______TS("create new deadline extension for instructor");

        FeedbackSessionUpdateRequest updateRequest = getTypicalFeedbackSessionUpdateRequest();
        Map<String, Long> newInstructorDeadlines = convertDeadlinesToLong(updateRequest.getInstructorDeadlines());
        newInstructorDeadlines.put("helper@course1.tmt", endTimePlus1Day);
        updateRequest.setInstructorDeadlines(newInstructorDeadlines);

        UpdateFeedbackSessionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);
        FeedbackSessionData response = (FeedbackSessionData) r.getOutput();

        expectedInstructorDeadlines.put("helper@course1.tmt", endTimePlus1Day);
        assertEquals(expectedInstructorDeadlines, response.getInstructorDeadlines());

        ______TS("update deadline extension for instructor");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newInstructorDeadlines = convertDeadlinesToLong(updateRequest.getInstructorDeadlines());
        newInstructorDeadlines.put("helper@course1.tmt", endTimePlus2Days);
        updateRequest.setInstructorDeadlines(newInstructorDeadlines);

        a = getAction(updateRequest, param);
        r = getJsonResult(a);
        response = (FeedbackSessionData) r.getOutput();

        expectedInstructorDeadlines.put("helper@course1.tmt", endTimePlus2Days);
        assertEquals(expectedInstructorDeadlines, response.getInstructorDeadlines());

        ______TS("delete deadline extension for instructor");

        // The typical update request does not contain the course 1 helper instructor's email.
        updateRequest = getTypicalFeedbackSessionUpdateRequest();

        a = getAction(updateRequest, param);
        r = getJsonResult(a);
        response = (FeedbackSessionData) r.getOutput();

        // The deadline for course 1 helper instructor was deleted; the map no longer contains a deadline for them.
        expectedInstructorDeadlines.remove("helper@course1.tmt");
        assertEquals(expectedInstructorDeadlines, response.getInstructorDeadlines());

        ______TS("C_UD deadline extensions for instructors simultaneously");

        assertNull(expectedInstructorDeadlines.get("helper@course1.tmt"));
        assertNotEquals(endTimePlus2Days, expectedInstructorDeadlines.get("instructor1@course1.tmt"));
        assertNotNull(expectedInstructorDeadlines.get("instructor2@course1.tmt"));

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newInstructorDeadlines = convertDeadlinesToLong(updateRequest.getInstructorDeadlines());
        newInstructorDeadlines.put("helper@course1.tmt", endTimePlus1Day);
        newInstructorDeadlines.put("instructor1@course1.tmt", endTimePlus2Days);
        newInstructorDeadlines.remove("instructor2@course1.tmt");
        updateRequest.setInstructorDeadlines(newInstructorDeadlines);

        a = getAction(updateRequest, param);
        r = getJsonResult(a);
        response = (FeedbackSessionData) r.getOutput();

        // Create deadline.
        expectedInstructorDeadlines.put("helper@course1.tmt", endTimePlus1Day);
        // Update deadline.
        expectedInstructorDeadlines.put("instructor1@course1.tmt", endTimePlus2Days);
        // Delete deadline.
        expectedInstructorDeadlines.remove("instructor2@course1.tmt");
        assertEquals(expectedInstructorDeadlines, response.getInstructorDeadlines());

        ______TS("change deadline extension for non-existent instructor; "
                + "should throw EntityNotFoundException");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newInstructorDeadlines = convertDeadlinesToLong(updateRequest.getInstructorDeadlines());
        newInstructorDeadlines.put("nonExistentInstructor@gmail.tmt", endTimePlus1Day);
        updateRequest.setInstructorDeadlines(newInstructorDeadlines);

        a = getAction(updateRequest, param);
        assertThrows(EntityNotFoundException.class, a::execute);

        ______TS("change deadline extension for instructor to the same time as the end time; "
                + "should throw InvalidHttpRequestBodyException");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        Instant newEndTime = updateRequest.getSubmissionEndTime();
        newInstructorDeadlines = convertDeadlinesToLong(updateRequest.getInstructorDeadlines());
        newInstructorDeadlines.put("helper@course1.tmt", newEndTime.toEpochMilli());
        updateRequest.setInstructorDeadlines(newInstructorDeadlines);

        a = getAction(updateRequest, param);
        assertThrows(InvalidHttpRequestBodyException.class, a::execute);

        ______TS("change deadline extension for instructor to before end time; "
                + "should throw InvalidHttpRequestBodyException");

        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        newEndTime = updateRequest.getSubmissionEndTime();
        newInstructorDeadlines = convertDeadlinesToLong(updateRequest.getInstructorDeadlines());
        newInstructorDeadlines.put("helper@course1.tmt", newEndTime.plus(Duration.ofMillis(-1)).toEpochMilli());
        updateRequest.setInstructorDeadlines(newInstructorDeadlines);

        a = getAction(updateRequest, param);
        assertThrows(InvalidHttpRequestBodyException.class, a::execute);

        logoutUser();
    }

    @Test
    public void testExecute_startTimeEarlierThanVisibleTime_shouldGiveInvalidParametersError() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };
        FeedbackSessionUpdateRequest updateRequest = getTypicalFeedbackSessionUpdateRequest();
        updateRequest.setCustomSessionVisibleTimestamp(
                updateRequest.getSubmissionStartTime().plusSeconds(10).toEpochMilli());

        InvalidHttpRequestBodyException ihrbe = verifyHttpRequestBodyFailure(updateRequest, param);
        assertEquals("The start time for this feedback session cannot be "
                + "earlier than the time when the session will be visible.", ihrbe.getMessage());
    }

    @Test
    public void testExecute_differentFeedbackSessionVisibleResponseVisibleSetting_shouldConvertToSpecialTime()
            throws Exception {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        CourseAttributes course = typicalBundle.courses.get("typicalCourse1");

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        ______TS("success: Custom time zone, At open show session, 'later' show results");

        logic.updateCourseCascade(
                CourseAttributes.updateOptionsBuilder(course.getId())
                        .withTimezone("Asia/Kathmandu")
                        .build());

        String[] param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };
        FeedbackSessionUpdateRequest updateRequest = getTypicalFeedbackSessionUpdateRequest();
        updateRequest.setSessionVisibleSetting(SessionVisibleSetting.AT_OPEN);
        updateRequest.setResponseVisibleSetting(ResponseVisibleSetting.LATER);

        UpdateFeedbackSessionAction a = getAction(updateRequest, param);
        getJsonResult(a);

        session = logic.getFeedbackSession(session.getFeedbackSessionName(), session.getCourseId());
        assertEquals(Const.TIME_REPRESENTS_FOLLOW_OPENING, session.getSessionVisibleFromTime());
        assertEquals(Const.TIME_REPRESENTS_LATER, session.getResultsVisibleFromTime());

        ______TS("success: At open session visible time, custom results visible time, UTC");

        logic.updateCourseCascade(
                CourseAttributes.updateOptionsBuilder(course.getId())
                        .withTimezone("UTC")
                        .build());

        param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };
        updateRequest = getTypicalFeedbackSessionUpdateRequest();
        updateRequest.setSessionVisibleSetting(SessionVisibleSetting.AT_OPEN);

        a = getAction(updateRequest, param);
        getJsonResult(a);

        session = logic.getFeedbackSession(session.getFeedbackSessionName(), session.getCourseId());
        assertEquals(Const.TIME_REPRESENTS_FOLLOW_OPENING, session.getSessionVisibleFromTime());
        assertEquals(1547003051000L, session.getResultsVisibleFromTime().toEpochMilli());
    }

    @Test
    public void testExecute_masqueradeModeWithManualReleaseResult_shouldEditSessionSuccessfully() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");

        loginAsAdmin();

        String[] param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };
        param = addUserIdToParams(instructor1ofCourse1.getGoogleId(), param);
        FeedbackSessionUpdateRequest updateRequest = getTypicalFeedbackSessionUpdateRequest();
        updateRequest.setResponseVisibleSetting(ResponseVisibleSetting.LATER);

        UpdateFeedbackSessionAction a = getAction(updateRequest, param);
        getJsonResult(a);
    }

    @Test
    public void testExecute_invalidRequestBody_shouldThrowException() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName(),
        };
        FeedbackSessionUpdateRequest updateRequest = getTypicalFeedbackSessionUpdateRequest();
        updateRequest.setInstructions(null);

        verifyHttpRequestBodyFailure(updateRequest, param);
    }

    private FeedbackSessionUpdateRequest getTypicalFeedbackSessionUpdateRequest() {
        FeedbackSessionUpdateRequest updateRequest = new FeedbackSessionUpdateRequest();
        updateRequest.setInstructions("instructions");

        updateRequest.setSubmissionStartTimestamp(1444003051000L);
        updateRequest.setSubmissionEndTimestamp(1546003051000L);
        updateRequest.setGracePeriod(5);

        updateRequest.setSessionVisibleSetting(SessionVisibleSetting.CUSTOM);
        updateRequest.setCustomSessionVisibleTimestamp(1440003051000L);

        updateRequest.setResponseVisibleSetting(ResponseVisibleSetting.CUSTOM);
        updateRequest.setCustomResponseVisibleTimestamp(1547003051000L);

        updateRequest.setClosingEmailEnabled(false);
        updateRequest.setPublishedEmailEnabled(false);

        FeedbackSessionAttributes session1InCourse1 = typicalBundle.feedbackSessions
                .get("session1InCourse1");
        Map<String, Long> studentDeadlines = convertDeadlinesToLong(session1InCourse1.getStudentDeadlines());
        updateRequest.setStudentDeadlines(studentDeadlines);
        Map<String, Long> instructorDeadlines = convertDeadlinesToLong(session1InCourse1.getInstructorDeadlines());
        updateRequest.setInstructorDeadlines(instructorDeadlines);

        return updateRequest;
    }

    private Map<String, Long> convertDeadlinesToLong(Map<String, Instant> deadlines) {
        return deadlines.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toEpochMilli()));
    }

    @Override
    @Test
    protected void testAccessControl() throws Exception {
        InstructorAttributes instructor1OfCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes fs = typicalBundle.feedbackSessions.get("session1InCourse1");

        ______TS("non-existent feedback session");

        String[] submissionParams = new String[] {
                Const.ParamsNames.COURSE_ID, fs.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, "abcSession",
        };

        loginAsInstructor(instructor1OfCourse1.getGoogleId());
        verifyEntityNotFoundAcl(submissionParams);

        ______TS("inaccessible without ModifySessionPrivilege");

        submissionParams = new String[] {
                Const.ParamsNames.COURSE_ID, fs.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, fs.getFeedbackSessionName(),
        };

        verifyInaccessibleWithoutModifySessionPrivilege(submissionParams);

        ______TS("only instructors of the same course with correct privilege can access");

        verifyOnlyInstructorsOfTheSameCourseWithCorrectCoursePrivilegeCanAccess(
                Const.InstructorPermissions.CAN_MODIFY_SESSION, submissionParams);
    }

}
