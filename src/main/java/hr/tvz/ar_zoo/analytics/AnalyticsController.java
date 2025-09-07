package hr.tvz.ar_zoo.analytics;

import hr.tvz.ar_zoo.analytics.dto.TrackDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsEventRepository repo;

    public AnalyticsController(AnalyticsEventRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/analytics/track")
    public ResponseEntity<Void> track(@RequestBody TrackDTO dto) {
        if (dto == null || dto.type() == null || dto.clientId() == null) return ResponseEntity.badRequest().build();
        AnalyticsEvent ev = new AnalyticsEvent();
        ev.setType(dto.type());
        ev.setPage(dto.page());
        ev.setModelId(dto.modelId());
        ev.setModelName(dto.modelName());
        ev.setClientId(dto.clientId());
        ev.setTimestamp(Instant.now());
        repo.save(ev);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/analytics/export")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws IOException {

        LocalDate dateFrom = (from != null) ? from : LocalDate.now().minusDays(30);
        LocalDate dateTo   = (to   != null) ? to   : LocalDate.now();
        Instant start = dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end   = dateTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<AnalyticsEvent> events = repo.findByTimestampBetween(start, end);

        ZoneId Z = ZoneId.systemDefault();
        Map<LocalDate, List<AnalyticsEvent>> byDay = events.stream().collect(
                Collectors.groupingBy(e -> LocalDateTime.ofInstant(e.getTimestamp(), Z).toLocalDate(),
                        TreeMap::new, Collectors.toList())
        );

        Map<String, Long> animalClicksTotal = events.stream()
                .filter(e -> "ANIMAL_CLICK".equalsIgnoreCase(e.getType()))
                .collect(Collectors.groupingBy(e -> Optional.ofNullable(e.getModelName()).orElse("(nepoznato)"),
                        Collectors.counting()));

        Map<LocalDate, Map<String, Long>> animalClicksByDay = events.stream()
                .filter(e -> "ANIMAL_CLICK".equalsIgnoreCase(e.getType()))
                .collect(Collectors.groupingBy(
                        e -> LocalDateTime.ofInstant(e.getTimestamp(), Z).toLocalDate(),
                        TreeMap::new,
                        Collectors.groupingBy(e -> Optional.ofNullable(e.getModelName()).orElse("(nepoznato)"),
                                Collectors.counting()
                        )
                ));

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle header = wb.createCellStyle();
            Font bold = wb.createFont(); bold.setBold(true); header.setFont(bold);

            Sheet s1 = wb.createSheet("Sažetak po danu");
            int r = 0;
            Row h = s1.createRow(r++);
            String[] cols = {"Datum", "Unikatni korisnici", "Otvaranja aplikacije", "Pokrenut kviz", "Klikovi na životinje"};
            for (int i=0;i<cols.length;i++){ Cell c=h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header); }

            for (var entry : byDay.entrySet()) {
                LocalDate d = entry.getKey();
                List<AnalyticsEvent> list = entry.getValue();
                long uniqueUsers = list.stream().map(AnalyticsEvent::getClientId).filter(Objects::nonNull).distinct().count();
                long appOpens = list.stream().filter(e -> "APP_OPEN".equalsIgnoreCase(e.getType())).count();
                long quizStart = list.stream().filter(e -> "QUIZ_START".equalsIgnoreCase(e.getType())).count();
                long clicks = list.stream().filter(e -> "ANIMAL_CLICK".equalsIgnoreCase(e.getType())).count();

                Row row = s1.createRow(r++);
                row.createCell(0).setCellValue(d.toString());
                row.createCell(1).setCellValue(uniqueUsers);
                row.createCell(2).setCellValue(appOpens);
                row.createCell(3).setCellValue(quizStart);
                row.createCell(4).setCellValue(clicks);
            }
            for (int i=0;i<cols.length;i++) s1.autoSizeColumn(i);

            Sheet s2 = wb.createSheet("Top životinje (ukupno)");
            Row h2 = s2.createRow(0);
            String[] cols2 = {"Životinja", "Klikovi"};
            for (int i=0;i<cols2.length;i++){ Cell c=h2.createCell(i); c.setCellValue(cols2[i]); c.setCellStyle(header); }

            AtomicInteger r2 = new AtomicInteger(1);
            animalClicksTotal.entrySet().stream()
                    .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                    .forEach(e -> {
                        Row row = s2.createRow(r2.getAndIncrement());
                        row.createCell(0).setCellValue(e.getKey());
                        row.createCell(1).setCellValue(e.getValue());
                    });
            s2.autoSizeColumn(0); s2.autoSizeColumn(1);

            Sheet s3 = wb.createSheet("Životinje po danu");
            Row h3 = s3.createRow(0);
            String[] cols3 = {"Datum", "Životinja", "Klikovi"};
            for (int i=0;i<cols3.length;i++){ Cell c=h3.createCell(i); c.setCellValue(cols3[i]); c.setCellStyle(header); }

            int r3 = 1;
            for (var dayEntry : animalClicksByDay.entrySet()) {
                LocalDate d = dayEntry.getKey();
                Map<String, Long> map = dayEntry.getValue();
                for (var e : map.entrySet().stream()
                        .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                        .toList()) {
                    Row row = s3.createRow(r3++);
                    row.createCell(0).setCellValue(d.toString());
                    row.createCell(1).setCellValue(e.getKey());
                    row.createCell(2).setCellValue(e.getValue());
                }
            }
            for (int i=0;i<cols3.length;i++) s3.autoSizeColumn(i);

            Sheet s4 = wb.createSheet("Sirovi događaji");
            Row h4 = s4.createRow(0);
            String[] cols4 = {"Timestamp", "Tip", "Stranica", "ModelName", "ModelId", "ClientId"};
            for (int i=0;i<cols4.length;i++){ Cell c=h4.createCell(i); c.setCellValue(cols4[i]); c.setCellStyle(header); }

            AtomicInteger r4 = new AtomicInteger(1);
            events.stream()
                    .sorted(Comparator.comparing(AnalyticsEvent::getTimestamp))
                    .forEach(e -> {
                        Row row = s4.createRow(r4.getAndIncrement());
                        row.createCell(0).setCellValue(e.getTimestamp().toString());
                        row.createCell(1).setCellValue(Optional.ofNullable(e.getType()).orElse(""));
                        row.createCell(2).setCellValue(Optional.ofNullable(e.getPage()).orElse(""));
                        row.createCell(3).setCellValue(Optional.ofNullable(e.getModelName()).orElse(""));
                        row.createCell(4).setCellValue(Optional.ofNullable(e.getModelId()).orElse(""));
                        row.createCell(5).setCellValue(Optional.ofNullable(e.getClientId()).orElse(""));
                    });
            for (int i=0;i<cols4.length;i++) s4.autoSizeColumn(i);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=ar-zoo-analytics.xlsx");
            wb.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

}
