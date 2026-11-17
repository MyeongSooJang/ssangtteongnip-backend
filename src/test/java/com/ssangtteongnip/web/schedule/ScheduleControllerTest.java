package com.ssangtteongnip.web.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.ScheduleService;
import com.ssangtteongnip.application.service.request.ScheduleRequest;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.schedule.Schedule;
import com.ssangtteongnip.domain.schedule.ScheduleType;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
@Import(SecurityConfig.class)
class ScheduleControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    @Test
    void POST_schedules_일정을_등록한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        JobApplication app = JobApplication.create(1L, company, "백엔드 개발자", LocalDate.now());
        Schedule schedule = Schedule.create(app, "1차 면접", LocalDateTime.now(), ScheduleType.INTERVIEW, "경기도 성남시 판교역로 166", "");
        given(scheduleService.register(eq(1L), any())).willReturn(schedule);

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "applicationId", 1,
                                "title", "1차 면접",
                                "scheduledAt", LocalDateTime.now().toString(),
                                "type", "INTERVIEW",
                                "address", "경기도 성남시 판교역로 166"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("1차 면접"))
                .andExpect(jsonPath("$.data.address").value("경기도 성남시 판교역로 166"));
    }

    private Schedule schedule(String address) {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        JobApplication app = JobApplication.create(1L, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
        return Schedule.create(app, "1차 면접", LocalDateTime.of(2026, 11, 10, 14, 0), ScheduleType.INTERVIEW, address, "");
    }

    @Test
    void 기간을_지정하지_않으면_전체_일정을_조회한다() throws Exception {
        given(scheduleService.findAll(1L)).willReturn(List.of(schedule("판교")));

        mockMvc.perform(get("/api/schedules")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("1차 면접"));
    }

    @Test
    void 기간을_지정하면_해당_기간의_일정만_조회한다() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 30, 23, 59);
        given(scheduleService.findByPeriod(1L, from, to)).willReturn(List.of(schedule("판교")));

        mockMvc.perform(get("/api/schedules")
                        .header("Authorization", BEARER)
                        .param("from", "2026-11-01T00:00:00")
                        .param("to", "2026-11-30T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].address").value("판교"));
    }

    @Test
    void 기간의_한쪽만_지정하면_전체_일정을_조회한다() throws Exception {
        given(scheduleService.findAll(1L)).willReturn(List.of(schedule("판교")));

        mockMvc.perform(get("/api/schedules")
                        .header("Authorization", BEARER)
                        .param("from", "2026-11-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("1차 면접"));
    }

    @Test
    void 수정할_주소가_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/schedules/1/address")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("address", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void POST_schedules_주소가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "applicationId", 1,
                                "title", "1차 면접",
                                "scheduledAt", LocalDateTime.now().toString(),
                                "type", "INTERVIEW"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PATCH_schedules_address_주소를_수정한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        JobApplication app = JobApplication.create(1L, company, "백엔드 개발자", LocalDate.now());
        Schedule schedule = Schedule.create(app, "1차 면접", LocalDateTime.now(), ScheduleType.INTERVIEW, "서울시 강남구", "");
        given(scheduleService.updateAddress(eq(1L), eq(1L), any())).willReturn(schedule);

        mockMvc.perform(patch("/api/schedules/1/address")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("address", "서울시 강남구"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.address").value("서울시 강남구"));
    }

    @Test
    void POST_schedules_제목_주소_메모를_구분해_서비스에_전달한다() throws Exception {
        given(scheduleService.register(eq(1L), any())).willReturn(schedule("경기도 성남시 판교역로 166"));

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "applicationId", 1,
                                "title", "1차 면접",
                                "scheduledAt", "2026-11-10T14:00:00",
                                "type", "INTERVIEW",
                                "address", "경기도 성남시 판교역로 166",
                                "note", "정장 착용"))))
                .andExpect(status().isCreated());

        verify(scheduleService).register(1L, new ScheduleRequest(
                1L, "1차 면접", LocalDateTime.of(2026, 11, 10, 14, 0), ScheduleType.INTERVIEW,
                "경기도 성남시 판교역로 166", "정장 착용"));
    }
}
