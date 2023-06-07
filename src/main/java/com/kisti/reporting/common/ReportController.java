package com.kisti.reporting.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;

@Slf4j
@RestController
@RequestMapping("/reporting/common/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 리포트 가져오기
     *
     * @param report 조회할 리포트 정보
     *
     * return 조회된 리포트 파일 (PDF or Image)
     */
    @PostMapping("")
    public void makeReport(@RequestBody Report report, HttpServletRequest req, HttpServletResponse res) throws Exception {
        System.out.println("Successfully sent message: ");
        File reportFile = null;
        try {
            reportFile = reportService.makeReportPDF(report);

            try {
                fileToResponse(reportFile, res);
            } catch (Exception e) {
                APIResult rtn = APIResult.builder()
                        .success(false)
                        .errors(APIError.builder().message("파일을 찾을수 없습니다. (reportPath)").build())
                        .build();

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(rtn);

                res.setContentType("application/json; charset=UTF-8");
                PrintWriter printwriter = res.getWriter();
                printwriter.println(json);
                printwriter.flush();
                printwriter.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 임시로 저장해둔 리포트 파일을 삭제 한다
            if (reportFile != null && reportFile.exists()) {
                reportFile.deleteOnExit();
            }
        }
    }

    /**
     * 파일 다운로드 처리 (Response)
     * @param file 다운로드 처리할 파일
     * @param res 웹응답
     * @throws Exception 예외처리
     */
    public void fileToResponse(File file, HttpServletResponse res) throws Exception {
        int fileSize = 0;
        if (file != null && file.exists()) {
            fileSize = (int) file.length();
        }

        if (fileSize > 0) {
            String mimeType = "application/octet-stream";
            try {
                mimeType = new Tika().detect(file);
                //System.out.println(">>>>> Mime: " + mimeType);
            } catch (Exception e) {
                e.printStackTrace();
            }

            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));

            //res.setBufferSize(fileSize);
            res.setContentType(mimeType);
            res.setContentLength(fileSize);

            FileCopyUtils.copy(in, res.getOutputStream());
            in.close();
            res.getOutputStream().flush();
            res.getOutputStream().close();
        } else {
            //throw new Exception("해당 경로의 파일을 찾을 수 없습니다. (" + file + ")");
            throw new Exception("해당 경로의 파일을 찾을 수 없습니다.");
        }
    }
}
