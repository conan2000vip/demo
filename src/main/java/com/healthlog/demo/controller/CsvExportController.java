package com.healthlog.demo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.service.csv.CsvExportService;
import com.healthlog.demo.service.csv.CsvExportService.CsvTable;
import com.healthlog.demo.util.SecurityContextUtil;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/profile/{profileId}/export/csv")
@RequiredArgsConstructor
public class CsvExportController {

    private static final int PREVIEW_LIMIT = 30; // JS 側の slice(0, 30) と同じ
    private final CsvExportService csvExportService;
    private final SecurityContextUtil securityContextUtil;
    private final UserRepository userRepository;

    @GetMapping("/preview")
    public Map<String, Object> preview(@PathVariable Long profileId, @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, HttpSession session) {
        User user = requireUser(session);
        CsvTable table = csvExportService.build(profileId, user.getId(), type, startDate, endDate);
        return Map.of("columns", table.columns(), "rows", table.rows().stream().limit(PREVIEW_LIMIT).toList());
    }

    @GetMapping
    public ResponseEntity<byte[]> download(@PathVariable Long profileId, @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, HttpSession session) {
        User user = requireUser(session);
        CsvTable table = csvExportService.build(profileId, user.getId(), type, startDate, endDate);

        // JS の csvFileName() と同じ: {type}_report_{yyyyMMdd}_{yyyyMMdd}.csv
        String fileName = type + "_report_" + startDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "_"
                + endDate.format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvExportService.toCsvBytes(table));
    }

    // 他のコントローラーの getCurrentUser と同じ方式
    private User requireUser(HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.LOGIN_USER);
        if (user != null) {
            return user;
        }
        String email = securityContextUtil.getCurrentUserEmail();
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "ログインしてください");
        }
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        return user;
    }
}