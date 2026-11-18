package com.example.smart_attendance_system.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.example.smart_attendance_system.data.AttendanceSession;
import com.example.smart_attendance_system.data.AttendanceRecord;

public class ExcelExportUtils {

    private static final String TAG = "ExcelExportUtils";

    static {
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
    }

    public static File exportAttendanceReport(Context context, AttendanceSession session, List<AttendanceRecord> records) {
        if (session == null || records == null) return null;

        // Create Workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Report");

        // Create Header Style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create Data Style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.LEFT);
        
        // Create Center Data Style
        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.cloneStyleFrom(dataStyle);
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        int rowNum = 0;

        // --- Session Info Section ---
        String subject = session.getSubject() != null ? session.getSubject() : "Unknown Subject";
        String date = session.getDate() != null ? session.getDate() : "Unknown Date";
        String branch = session.getBranch() != null ? session.getBranch() : "";
        String year = session.getYear() != null ? session.getYear() : "";
        String section = session.getSection() != null ? session.getSection() : "";

        String startTime = session.getStartTime() != null ? session.getStartTime() : "";
        String endTime = session.getEndTime() != null ? session.getEndTime() : "";
        String facultyName = session.getFacultyName() != null ? session.getFacultyName() : "Unknown";
        String facultyEmail = session.getFacultyEmail() != null ? session.getFacultyEmail() : "";

        Row infoRow = sheet.createRow(rowNum++);
        createCell(infoRow, 0, "Subject: " + subject, null);
        
        infoRow = sheet.createRow(rowNum++);
        createCell(infoRow, 0, "Date: " + date, null);
        
        infoRow = sheet.createRow(rowNum++);
        createCell(infoRow, 0, "Time: " + startTime + " - " + endTime, null);
        
        infoRow = sheet.createRow(rowNum++);
        createCell(infoRow, 0, "Class: " + branch + " " + year + " " + section, null);

        infoRow = sheet.createRow(rowNum++);
        createCell(infoRow, 0, "Faculty: " + facultyName + " (" + facultyEmail + ")", null);
        
        rowNum++; // Formatting space

        // --- Table Header ---
        String[] columns = {"Enrollment No", "Student Name", "Email", "Status", "Marked Time", "Override"};
        Row headerRow = sheet.createRow(rowNum++);
        
        for (int i = 0; i < columns.length; i++) {
            createCell(headerRow, i, columns[i], headerStyle);
        }

        // --- Data Rows ---
        for (AttendanceRecord record : records) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, record.getEnrollmentNo() != null ? record.getEnrollmentNo() : "", centerStyle);
            createCell(row, 1, record.getStudentName() != null ? record.getStudentName() : "", dataStyle);
            createCell(row, 2, record.getEmail() != null ? record.getEmail() : "", dataStyle);
            
            // Status with simple styling logic
            createCell(row, 3, record.getStatus() != null ? record.getStatus() : "", centerStyle);
            
            createCell(row, 4, record.getMarkedTime() != null ? record.getMarkedTime() : "", centerStyle);
            
            String overrideInfo = record.isManualOverride() ? "Yes (" + (record.getMarkedBy() != null ? record.getMarkedBy() : "") + ")" : "No";
            createCell(row, 5, overrideInfo, centerStyle);
        }

        // Set fixed column widths (width = characters * 256)
        // columns = {"Enrollment No", "Student Name", "Email", "Status", "Marked Time", "Override"};
        sheet.setColumnWidth(0, 15 * 256); // Enrollment
        sheet.setColumnWidth(1, 25 * 256); // Name
        sheet.setColumnWidth(2, 30 * 256); // Email
        sheet.setColumnWidth(3, 12 * 256); // Status
        sheet.setColumnWidth(4, 15 * 256); // Time
        sheet.setColumnWidth(5, 20 * 256); // Override

        // Write to file
        try {
            // Check state of external storage
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "External storage not mounted");
                return null;
            }

            // Use app-specific public directory: /Android/data/package/files/Documents/
            File docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir == null) {
                Log.e(TAG, "Documents directory is null");
                // Fallback to cache dir if documents is null (rare)
                docsDir = context.getCacheDir(); 
            }
            
            if (!docsDir.exists() && !docsDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + docsDir.getAbsolutePath());
                return null;
            }
            
            String safeSubject = subject.replaceAll("[^a-zA-Z0-9.-]", "_");
            String safeDate = date.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = "Attendance_" + safeSubject + "_" + safeDate + ".xlsx";
            
            File file = new File(docsDir, fileName);
            
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();
            
            return file;

        } catch (IOException e) {
            Log.e(TAG, "Error writing Excel file", e);
            return null;
        }
    }

    public static File exportMasterSessionList(Context context, List<AttendanceSession> sessionList) {
        if (sessionList == null || sessionList.isEmpty()) return null;

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Master Attendance Log");

        // Header Style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Data Style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        int rowNum = 0;

        // Title Row
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Master Session List Report");
        titleCell.setCellStyle(dataStyle); // Simple style for now
        
        rowNum++; // Spacer

        // Headers
        String[] columns = {"Date", "Time", "Subject", "Faculty", "Class", "Status"};
        Row headerRow = sheet.createRow(rowNum++);
        for (int i = 0; i < columns.length; i++) {
            createCell(headerRow, i, columns[i], headerStyle);
        }

        // Data
        for (AttendanceSession session : sessionList) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, session.getDate(), dataStyle);
            createCell(row, 1, session.getStartTime() + " - " + session.getEndTime(), dataStyle);
            createCell(row, 2, session.getSubject(), dataStyle);
            createCell(row, 3, session.getFacultyName() + " (" + session.getFacultyEmail() + ")", dataStyle);
            createCell(row, 4, session.getBranch() + " " + session.getYear() + " " + session.getSection(), dataStyle);
            createCell(row, 5, session.getStatus(), dataStyle);
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.setColumnWidth(i, 20 * 256); // Fixed width approximation
        }

        // Save
        try {
             File docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
             if (docsDir == null) docsDir = context.getCacheDir();
             
             String fileName = "Master_Attendance_Report_" + System.currentTimeMillis() + ".xlsx";
             File file = new File(docsDir, fileName);
             
             FileOutputStream fos = new FileOutputStream(file);
             workbook.write(fos);
             fos.close();
             workbook.close();
             
             return file;
        } catch (IOException e) {
             Log.e(TAG, "Error writing Master Export", e);
             return null;
        }
    }

    private static void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}
