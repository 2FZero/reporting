package com.kisti.reporting.common;

import com.kisti.reporting.common.util.security.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;

@Slf4j
@Service
@Transactional(rollbackFor = {Exception.class})
public class ReportService {
    @Value("${comm.report.rootpath}")
    String reportRootPath;

    @Value("${comm.temppath}")
    String tempPath;

    @Value("${spring.datasource.driver-class-name}")
    String datasourceDriverClassName;

    @Value("${spring.datasource.url}")
    String datasourceUrl;

    @Value("${spring.datasource.username}")
    String datasourceUsername;

    @Value("${spring.datasource.password}")
    String datasourcePassword;

    @Value("${comm.fileUpload.filepath}")
    String uploadRoot;

    /**
     * 리포트 초기화
     *
     * @param report 리포트 조회 조건
     *  - reportPath: 리포트 양식 경로
     *  - reportParam: 리포트 파라미터
     * @return 리포트 생성된 JasperPrint 객체
     * @throws Exception 예외처리
     */
    public JasperPrint initReport(Report report) throws Exception {
        JasperPrint rtn = null;
        Connection conn = null;
        try {
            String reportTempPath = tempPath + "/report";
            String reportFilePath = reportRootPath + "/report";

            HashMap<String, Object> parameterts = new HashMap<>();

            if (report.getReportParam() != null) {
                parameterts.putAll(report.getReportParam());
            }

            parameterts.put("file_path", uploadRoot);
            parameterts.put("report_path", reportFilePath);

            Class.forName(datasourceDriverClassName);
            conn = DriverManager.getConnection(datasourceUrl, datasourceUsername, datasourcePassword);

            String yyyyMMdd = (new SimpleDateFormat("yyyyMMdd").format(new Date()));

            // 이전 날짜의 임시파일을 삭제한다.
            if (new File(reportTempPath).exists()) {
                File reportTempPathFile = new File(reportTempPath);
                File[] fileList = reportTempPathFile.listFiles();

                if (fileList != null && fileList.length > 0) {
                    for (File file : fileList) {
                        String filename = file.getName();
                        try {
                            LocalDate filenameDate = LocalDate.parse(filename, DateTimeFormatter.BASIC_ISO_DATE);
                            LocalDate today = LocalDate.now();
                            if (today.isAfter(filenameDate)) {
                                deleteFolder(file.getPath());
                            }
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }
                }
            }

            // 임시 파일 경로가 없으면 생성한다.
            if (!new File(reportTempPath + "/" + yyyyMMdd).exists()) {
                try {
                    new File(reportTempPath + "/" + yyyyMMdd).mkdirs();
                } catch (Exception e) {
                    e.getStackTrace();
                }
            }

            String reportaFilePath = "/report/" + report.getReportPath();
            JasperReport jasperReport = JasperCompileManager.compileReport(reportRootPath + reportaFilePath);
            rtn = JasperFillManager.fillReport(jasperReport, parameterts, conn);
        } catch (JRException | SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return rtn;
    }

    /**
     * 리포트 파일 생성 (PDF)
     *
     * @param report 리포트 조회 조건
     *  - reportPath: 리포트 양식 경로
     *  - reportParam: 리포트 파라미터
     * @return 리포트 생성된 PDF 파일 객체
     * @throws Exception 예외처리
     */
    public File makeReportPDF(Report report) throws Exception {
        String reportTempPath = tempPath + "/report";
        String yyyyMMdd = (new SimpleDateFormat("yyyyMMdd").format(new Date()));
        String tmpFileName = EncryptUtil.encryptMD5((new SimpleDateFormat("yyyyMMddHHmmssSSSS")).format(new Date())) + ".pdf";
        String outfilename = reportTempPath + "/" + yyyyMMdd + "/" + tmpFileName;

        JasperPrint jasperPrint = initReport(report);

        JRPdfExporter ex = new JRPdfExporter();
        ex.setExporterInput(new SimpleExporterInput(jasperPrint));
        ex.setExporterOutput(new SimpleOutputStreamExporterOutput(outfilename));
        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setCompressed(true);
        ex.setConfiguration(configuration);
        ex.exportReport();

        return new File(outfilename);
    }

    /**
     * [기타]
     * 폴더를 삭제 한다 (하위 파일,폴더 전부 삭제)
     * @param path 삭제할 폴더 경로
     */
    public static void deleteFolder(String path) {
        File folder = new File(path);
        try {
            if (folder.exists()) {
                File[] folderList = folder.listFiles(); //파일리스트 얻어오기

                assert folderList != null;
                for (File file : folderList) {
                    if (file.isFile()) {
                        file.delete();
                    } else {
                        deleteFolder(file.getPath());
                    }
                    file.delete();
                }
                folder.delete(); //폴더 삭제
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
    }
}
