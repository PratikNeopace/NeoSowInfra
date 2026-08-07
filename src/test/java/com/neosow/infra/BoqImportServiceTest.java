package com.neosow.infra;

import com.neosow.infra.dto.boq.ImportJobResponseDTO;
import com.neosow.infra.mapper.BoqMapper;
import com.neosow.infra.model.BoqItem;
import com.neosow.infra.model.ImportJob;
import com.neosow.infra.model.ImportJobStatus;
import com.neosow.infra.repository.BoqItemRepository;
import com.neosow.infra.repository.ImportErrorRepository;
import com.neosow.infra.repository.ImportJobRepository;
import com.neosow.infra.service.impl.BoqImportServiceImpl;
import com.neosow.infra.security.UserDetailsImpl;

import com.neosow.infra.repository.UserRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class BoqImportServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private ImportErrorRepository importErrorRepository;

    @Mock
    private BoqItemRepository boqItemRepository;

    @Mock
    private BoqMapper boqMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BoqImportServiceImpl boqImportService;

    private UUID adminUserId;
    private SecurityContext originalSecurityContext;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        originalSecurityContext = SecurityContextHolder.getContext();
        
        UserDetailsImpl userDetails = new UserDetailsImpl(
                adminUserId, "admin@neosow.com", "password", true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        
        SecurityContext mockContext = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(mockContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.setContext(originalSecurityContext);
    }

    @Test
    void testGenerateTemplate() {
        ByteArrayInputStream stream = boqImportService.generateTemplate();
        assertNotNull(stream);
        assertTrue(stream.available() > 0);
    }

    @Test
    void testImportValidBoqExcel() throws IOException {
        // Create an in-memory valid BOQ workbook matching the prototype structure
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PARTITON");
            
            // Row 2: MAIN HEADING
            Row row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue("MAIN HEADING");
            row2.createCell(1).setCellValue("PARTITION WORK");
            
            // Row 3: Headers
            Row row3 = sheet.createRow(2);
            String[] headers = {
                    "S.NO.", "SUB-HEADING", "WIDTH", "HEIGHT", "DEPTH", 
                    "QTY", "UNIT", "NO. OF UNIT", "TOTAL QTY.", "RATE", "AMOUNT", "REF. IMAGE"
            };
            for (int i = 0; i < headers.length; i++) {
                row3.createCell(i).setCellValue(headers[i]);
            }
            
            // Row 5: Group starter
            Row row5 = sheet.createRow(4);
            row5.createCell(0).setCellValue(1);
            row5.createCell(1).setCellValue("GYPSUM PARTITION");

            // Row 6: Description label row (should be skipped)
            Row row6Label = sheet.createRow(5);
            row6Label.createCell(1).setCellValue("Description-");
            
            // Row 7: Details Row with values (SQ.FT. unit)
            Row row7 = sheet.createRow(6);
            row7.createCell(1).setCellValue("Gypsum board work specifications");
            row7.createCell(2).setCellValue("5' 0\""); // 5.0
            row7.createCell(3).setCellValue("10' 0\""); // 10.0 -> Area 50.0
            row7.createCell(6).setCellValue("SQ.FT.");
            row7.createCell(7).setCellValue(2); // no of units -> Total Qty 100.00
            row7.createCell(9).setCellValue(150); // Rate 150 -> Amount 15000.00

            // Row 8: Legend row (should be skipped)
            Row row8Legend = sheet.createRow(7);
            row8Legend.createCell(1).setCellValue("BY DEACTIVATE VALUE");
            
            // Row 9: Summary stop condition
            Row row9 = sheet.createRow(8);
            row9.createCell(7).setCellValue("GRAND TOTAL");
            
            workbook.write(out);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "boq_specifications.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        ImportJob mockJob = ImportJob.builder()
                .id(UUID.randomUUID())
                .status(ImportJobStatus.PROCESSING)
                .build();

        Mockito.when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        Mockito.when(boqMapper.toDto(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            return ImportJobResponseDTO.builder()
                    .id(j.getId())
                    .status(j.getStatus().toString())
                    .totalRows(j.getTotalRows())
                    .successRows(j.getSuccessRows())
                    .failedRows(j.getFailedRows())
                    .build();
        });

        // Act
        ImportJobResponseDTO result = boqImportService.importBoq(file, null);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getSuccessRows());
        assertEquals(0, result.getFailedRows());

        Mockito.verify(boqItemRepository, Mockito.times(1)).save(any(BoqItem.class));
    }

    @Test
    void testImportRealExcelFile() throws IOException {
        java.io.File fileObj = new java.io.File("/Users/pratikghodke/Desktop/NeoSowInfra/BOQ SPECIFICATIONS 25.02.2026.xlsx");
        if (!fileObj.exists()) {
            return;
        }
        byte[] content = java.nio.file.Files.readAllBytes(fileObj.toPath());
        MockMultipartFile file = new MockMultipartFile(
                "file", "BOQ SPECIFICATIONS 25.02.2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        );

        Mockito.when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        Mockito.when(boqMapper.toDto(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            return ImportJobResponseDTO.builder()
                    .id(j.getId())
                    .status(j.getStatus().toString())
                    .totalRows(j.getTotalRows())
                    .successRows(j.getSuccessRows())
                    .failedRows(j.getFailedRows())
                    .build();
        });

        ImportJobResponseDTO result = boqImportService.importBoq(file, null);
        System.out.println("Real Import Result - Status: " + result.getStatus() + 
                           ", Total: " + result.getTotalRows() + 
                           ", Success: " + result.getSuccessRows() + 
                           ", Failed: " + result.getFailedRows());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(0, result.getFailedRows());

        // Now test the updated file
        java.io.File updatedFileObj = new java.io.File("/Users/pratikghodke/Downloads/UPDATED BOQ SPECIFICATIONS 17.06.2026.xlsx");
        byte[] updatedContent = java.nio.file.Files.readAllBytes(updatedFileObj.toPath());
        MockMultipartFile updatedFile = new MockMultipartFile(
                "file", "UPDATED BOQ SPECIFICATIONS 17.06.2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                updatedContent
        );

        ImportJobResponseDTO updatedResult = boqImportService.importBoq(updatedFile, null);
        System.out.println("Updated File Import Result - Status: " + updatedResult.getStatus() + 
                           ", Total: " + updatedResult.getTotalRows() + 
                           ", Success: " + updatedResult.getSuccessRows() + 
                           ", Failed: " + updatedResult.getFailedRows());
        assertEquals("COMPLETED", updatedResult.getStatus());
        assertEquals(0, updatedResult.getFailedRows());
    }

    @Test
    void testImportBoqValidationRules() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PARTITON");
            
            // Row 2: MAIN HEADING
            Row row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue("MAIN HEADING");
            row2.createCell(1).setCellValue("VALIDATION TESTS");
            
            // Row 3: Headers
            Row row3 = sheet.createRow(2);
            String[] headers = {
                    "S.NO.", "SUB-HEADING", "WIDTH", "HEIGHT", "DEPTH", 
                    "QTY", "UNIT", "NO. OF UNIT", "TOTAL QTY.", "RATE", "AMOUNT", "REF. IMAGE"
            };
            for (int i = 0; i < headers.length; i++) {
                row3.createCell(i).setCellValue(headers[i]);
            }
            
            // Sub-heading starter
            Row r5 = sheet.createRow(4);
            r5.createCell(0).setCellValue(1);
            r5.createCell(1).setCellValue("GYPSUM PARTITION");
            
            // Row 6: AREA UNIT (SQ.FT.) with dimensions missing but valid Qty (should succeed)
            Row r6 = sheet.createRow(5);
            r6.createCell(1).setCellValue("SQFT item with qty fallback");
            r6.createCell(5).setCellValue(75); // qty
            r6.createCell(6).setCellValue("SQ.FT.");
            r6.createCell(7).setCellValue(2); // no of units
            r6.createCell(9).setCellValue(100); // rate
            
            // Row 7: VOLUME UNIT (CU.FT.) with invalid text in dimension column but valid Qty (should succeed)
            Row r7 = sheet.createRow(6);
            r7.createCell(1).setCellValue("CUFT item with text dimension and qty fallback");
            r7.createCell(2).setCellValue("SQ.FT."); // invalid dimension text
            r7.createCell(5).setCellValue(30); // qty
            r7.createCell(6).setCellValue("CU.FT.");
            r7.createCell(7).setCellValue(1); // no of units
            r7.createCell(9).setCellValue(200); // rate
            
            // Row 8: COUNT UNIT (NOS.) with no dimensions (should succeed)
            Row r8 = sheet.createRow(7);
            r8.createCell(1).setCellValue("NOS item with no dimensions");
            r8.createCell(6).setCellValue("NOS.");
            r8.createCell(7).setCellValue(5); // no of units
            r8.createCell(9).setCellValue(50); // rate
            
            // Row 9: AREA UNIT (SQ.FT.) with both dimensions and qty missing (should fail validation)
            Row r9 = sheet.createRow(8);
            r9.createCell(1).setCellValue("SQFT item missing both dimensions and qty");
            r9.createCell(6).setCellValue("SQ.FT.");
            r9.createCell(7).setCellValue(2);
            r9.createCell(9).setCellValue(100);

            // Row 10: Summary stop condition
            Row r10 = sheet.createRow(9);
            r10.createCell(7).setCellValue("GRAND TOTAL");
            
            workbook.write(out);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "boq_validation_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        Mockito.when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        Mockito.when(boqMapper.toDto(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            return ImportJobResponseDTO.builder()
                    .id(j.getId())
                    .status(j.getStatus().toString())
                    .totalRows(j.getTotalRows())
                    .successRows(j.getSuccessRows())
                    .failedRows(j.getFailedRows())
                    .build();
        });

        ImportJobResponseDTO result = boqImportService.importBoq(file, null);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(4, result.getTotalRows());
        assertEquals(3, result.getSuccessRows());
        assertEquals(1, result.getFailedRows());
    }

    @Test
    void testImportMultiSheetExcel() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet 1: PARTITION
            Sheet sheet1 = workbook.createSheet("PARTITION");
            Row sh1Row2 = sheet1.createRow(1);
            sh1Row2.createCell(0).setCellValue("MAIN HEADING");
            sh1Row2.createCell(1).setCellValue("PARTITION WORK");
            
            Row sh1Row3 = sheet1.createRow(2);
            String[] headers = {
                    "S.NO.", "SUB-HEADING", "WIDTH", "HEIGHT", "DEPTH", 
                    "QTY", "UNIT", "NO. OF UNIT", "TOTAL QTY.", "RATE", "AMOUNT", "REF. IMAGE"
            };
            for (int i = 0; i < headers.length; i++) {
                sh1Row3.createCell(i).setCellValue(headers[i]);
            }
            
            Row r5 = sheet1.createRow(4);
            r5.createCell(0).setCellValue(1);
            r5.createCell(1).setCellValue("GYPSUM PARTITION");
            
            Row r6 = sheet1.createRow(5);
            r6.createCell(1).setCellValue("Description-");
            
            Row r7 = sheet1.createRow(6);
            r7.createCell(1).setCellValue("Gypsum board work specifications");
            r7.createCell(6).setCellValue("SQ.FT.");
            r7.createCell(5).setCellValue(50);
            r7.createCell(7).setCellValue(2);
            r7.createCell(9).setCellValue(150);

            // Sheet 2: CARPET
            Sheet sheet2 = workbook.createSheet("CARPET");
            Row sh2Row2 = sheet2.createRow(1);
            sh2Row2.createCell(0).setCellValue("MAIN HEADING");
            sh2Row2.createCell(1).setCellValue("CARPET WORK");
            
            Row sh2Row3 = sheet2.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                sh2Row3.createCell(i).setCellValue(headers[i]);
            }
            
            Row r2_5 = sheet2.createRow(4);
            r2_5.createCell(0).setCellValue(1);
            r2_5.createCell(1).setCellValue("WOODEN CARPET");
            
            Row r2_6 = sheet2.createRow(5);
            r2_6.createCell(1).setCellValue("Description-");
            
            Row r2_7 = sheet2.createRow(6);
            r2_7.createCell(1).setCellValue("Wooden carpet specifications");
            r2_7.createCell(6).setCellValue("SQ.FT.");
            r2_7.createCell(5).setCellValue(40);
            r2_7.createCell(7).setCellValue(1);
            r2_7.createCell(9).setCellValue(200);
            
            workbook.write(out);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "boq_multisheet_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        Mockito.when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        Mockito.when(boqMapper.toDto(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            return ImportJobResponseDTO.builder()
                    .id(j.getId())
                    .status(j.getStatus().toString())
                    .totalRows(j.getTotalRows())
                    .successRows(j.getSuccessRows())
                    .failedRows(j.getFailedRows())
                    .build();
        });

        ImportJobResponseDTO result = boqImportService.importBoq(file, null);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getSuccessRows());
        assertEquals(0, result.getFailedRows());
    }

    @Test
    void testPrintDownloadedFile() throws IOException {
        java.io.File fileObj = new java.io.File("/Users/pratikghodke/Downloads/BOQ SPECIFICATIONS (1).xlsx");
        if (!fileObj.exists()) {
            System.out.println("FILE DOES NOT EXIST");
            return;
        }
        try (Workbook workbook = WorkbookFactory.create(new java.io.FileInputStream(fileObj))) {
            Sheet sheet = workbook.getSheetAt(0);
            int last = sheet.getLastRowNum();
            for (int r = 0; r <= Math.min(last, 15); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    System.out.println("Row " + r + ": null");
                    continue;
                }
                StringBuilder sb = new StringBuilder("Row " + r + ": ");
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    sb.append("[").append(c).append("]=").append(cell == null ? "null" : cell.toString()).append(" | ");
                }
                System.out.println(sb.toString());
            }
        }
    }

    @Test
    void testImportBoq_SuperAdminRole_AutoApproves() throws IOException {
        // Arrange
        // Setup SecurityContext with both ROLE_ADMIN and ROLE_SUPER_ADMIN
        UserDetailsImpl userDetails = new UserDetailsImpl(
                adminUserId, "superadmin@neosow.com", "password", true,
                java.util.Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
                )
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext mockContext = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(mockContext);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("PARTITION");
        
        Row mainHeadingRow = sheet.createRow(1);
        Cell mhLabel = mainHeadingRow.createCell(0);
        mhLabel.setCellValue("MAIN HEADING");
        Cell mhValue = mainHeadingRow.createCell(1);
        mhValue.setCellValue("PARTITION WORK");
        
        Row headerRow = sheet.createRow(2);
        headerRow.createCell(0).setCellValue("S.NO.");
        headerRow.createCell(1).setCellValue("SUB-HEADING");
        headerRow.createCell(2).setCellValue("WIDTH");
        headerRow.createCell(3).setCellValue("HEIGHT");
        headerRow.createCell(4).setCellValue("DEPTH");
        headerRow.createCell(5).setCellValue("QTY");
        headerRow.createCell(6).setCellValue("UNIT");
        headerRow.createCell(7).setCellValue("NO. OF UNITS");
        headerRow.createCell(8).setCellValue("TOTAL QTY");
        headerRow.createCell(9).setCellValue("RATE");
        headerRow.createCell(10).setCellValue("AMOUNT");
        headerRow.createCell(11).setCellValue("REF. IMAGE");

        // Row A
        Row rowA = sheet.createRow(3);
        rowA.createCell(0).setCellValue("1.0");
        rowA.createCell(1).setCellValue("100MM GYPSUM PARTITION");

        // Row B
        Row rowB = sheet.createRow(4);
        rowB.createCell(1).setCellValue("Description-");

        // Row C
        Row rowC = sheet.createRow(5);
        rowC.createCell(1).setCellValue("Partition thickness is 100mm");
        rowC.createCell(2).setCellValue(10.0);
        rowC.createCell(3).setCellValue(10.0);
        rowC.createCell(4).setCellValue(0.0);
        rowC.createCell(5).setCellValue(""); // qty formula fallback
        rowC.createCell(6).setCellValue("SQ.FT.");
        rowC.createCell(7).setCellValue(1.0);
        rowC.createCell(8).setCellValue(100.0);
        rowC.createCell(9).setCellValue(150.0);
        rowC.createCell(10).setCellValue(15000.0);

        // Grand total row
        Row totalRow = sheet.createRow(6);
        totalRow.createCell(0).setCellValue("GRAND TOTAL");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] bytes = bos.toByteArray();
        workbook.close();

        MockMultipartFile file = new MockMultipartFile("file", "boq_superadmin_test.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        Mockito.when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        Mockito.when(boqMapper.toDto(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            return ImportJobResponseDTO.builder()
                    .id(j.getId())
                    .status(j.getStatus().toString())
                    .totalRows(j.getTotalRows())
                    .successRows(j.getSuccessRows())
                    .failedRows(j.getFailedRows())
                    .build();
        });

        // Act
        ImportJobResponseDTO result = boqImportService.importBoq(file, null);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getSuccessRows());

        // Capture saved BoqItem and check status
        org.mockito.ArgumentCaptor<BoqItem> captor = org.mockito.ArgumentCaptor.forClass(BoqItem.class);
        Mockito.verify(boqItemRepository, Mockito.times(1)).save(captor.capture());
        
        BoqItem savedItem = captor.getValue();
        assertEquals(com.neosow.infra.model.BoqItemStatus.APPROVED, savedItem.getStatus());
        assertEquals("ROLE_SUPER_ADMIN", savedItem.getUploadedRole());
    }

    @Test
    void testCreateManualBoqItem_SuperAdmin_AutoApproves() {
        // Arrange
        UserDetailsImpl userDetails = new UserDetailsImpl(
                adminUserId, "superadmin@neosow.com", "password", true,
                java.util.Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
                )
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext mockContext = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(mockContext);

        com.neosow.infra.dto.boq.BoqItemDTO inputDto = com.neosow.infra.dto.boq.BoqItemDTO.builder()
                .mainHeading("PAINTING WORK")
                .subHeading("Emulsion Painting")
                .description("Two coats of premium emulsion")
                .unit("SQ.FT.")
                .rate(java.math.BigDecimal.valueOf(45.50))
                .build();

        Mockito.when(boqItemRepository.save(any(BoqItem.class))).thenAnswer(inv -> {
            BoqItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        Mockito.when(boqMapper.toDto(any(BoqItem.class))).thenAnswer(inv -> {
            BoqItem item = inv.getArgument(0);
            return com.neosow.infra.dto.boq.BoqItemDTO.builder()
                    .id(item.getId())
                    .mainHeading(item.getMainHeading())
                    .subHeading(item.getSubHeading())
                    .description(item.getDescription())
                    .unit(item.getUnit())
                    .rate(item.getRate())
                    .status(item.getStatus().toString())
                    .build();
        });

        // Act
        com.neosow.infra.dto.boq.BoqItemDTO result = boqImportService.createManualBoqItem(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals("PAINTING WORK", result.getMainHeading());
        assertEquals("Emulsion Painting", result.getSubHeading());
        
        org.mockito.ArgumentCaptor<BoqItem> captor = org.mockito.ArgumentCaptor.forClass(BoqItem.class);
        Mockito.verify(boqItemRepository).save(captor.capture());
        BoqItem saved = captor.getValue();
        assertEquals(com.neosow.infra.model.BoqItemStatus.APPROVED, saved.getStatus());
        assertEquals("ROLE_SUPER_ADMIN", saved.getUploadedRole());
    }

    @Test
    void testCreateManualBoqItem_Admin_PendingApproval() {
        // Arrange
        // SecurityContext is already configured with ROLE_ADMIN in setUp()
        com.neosow.infra.dto.boq.BoqItemDTO inputDto = com.neosow.infra.dto.boq.BoqItemDTO.builder()
                .mainHeading("WOODEN WORK")
                .subHeading("Wooden Door Frame")
                .description("Veneer finish wooden frame")
                .unit("R.FT.")
                .rate(java.math.BigDecimal.valueOf(180.00))
                .build();

        Mockito.when(boqItemRepository.save(any(BoqItem.class))).thenAnswer(inv -> {
            BoqItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        Mockito.when(boqMapper.toDto(any(BoqItem.class))).thenAnswer(inv -> {
            BoqItem item = inv.getArgument(0);
            return com.neosow.infra.dto.boq.BoqItemDTO.builder()
                    .id(item.getId())
                    .mainHeading(item.getMainHeading())
                    .subHeading(item.getSubHeading())
                    .description(item.getDescription())
                    .unit(item.getUnit())
                    .rate(item.getRate())
                    .status(item.getStatus().toString())
                    .build();
        });

        // Act
        com.neosow.infra.dto.boq.BoqItemDTO result = boqImportService.createManualBoqItem(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.getStatus());
        
        org.mockito.ArgumentCaptor<BoqItem> captor = org.mockito.ArgumentCaptor.forClass(BoqItem.class);
        Mockito.verify(boqItemRepository).save(captor.capture());
        BoqItem saved = captor.getValue();
        assertEquals(com.neosow.infra.model.BoqItemStatus.PENDING_APPROVAL, saved.getStatus());
        assertEquals("ROLE_ADMIN", saved.getUploadedRole());
    }

    @Test
    void testImportBoq_WithQuotationType() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("PARTITION");
        Row mhRow = sheet.createRow(1);
        mhRow.createCell(0).setCellValue("MAIN HEADING");
        mhRow.createCell(1).setCellValue("PREMIUM PARTITION WORK");

        Row hRow = sheet.createRow(2);
        hRow.createCell(0).setCellValue("S.NO.");
        hRow.createCell(1).setCellValue("SUB-HEADING");
        hRow.createCell(2).setCellValue("WIDTH");
        hRow.createCell(3).setCellValue("HEIGHT");
        hRow.createCell(6).setCellValue("UNIT");
        hRow.createCell(7).setCellValue("NO. OF UNITS");
        hRow.createCell(9).setCellValue("RATE");

        Row r1 = sheet.createRow(4);
        r1.createCell(0).setCellValue("1");
        r1.createCell(1).setCellValue("LUXURY GLASS PARTITION");

        Row r2 = sheet.createRow(5);
        r2.createCell(1).setCellValue("Double glazed partition with acoustical seal");
        r2.createCell(2).setCellValue(10.0);
        r2.createCell(3).setCellValue(10.0);
        r2.createCell(6).setCellValue("SQ.FT.");
        r2.createCell(7).setCellValue(10.0);
        r2.createCell(9).setCellValue(500.0);

        Row grandTotalRow = sheet.createRow(6);
        grandTotalRow.createCell(0).setCellValue("GRAND TOTAL");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] bytes = bos.toByteArray();
        workbook.close();

        MockMultipartFile file = new MockMultipartFile("file", "boq_premium.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        Mockito.when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        Mockito.when(boqMapper.toDto(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob j = inv.getArgument(0);
            return ImportJobResponseDTO.builder()
                    .id(j.getId())
                    .status(j.getStatus().toString())
                    .quotationType(j.getQuotationType())
                    .build();
        });

        ImportJobResponseDTO result = boqImportService.importBoq(file, com.neosow.infra.model.QuotationType.PREMIUM_RANGE_QUOTE);

        assertNotNull(result);
        assertEquals(com.neosow.infra.model.QuotationType.PREMIUM_RANGE_QUOTE, result.getQuotationType());

        org.mockito.ArgumentCaptor<BoqItem> captor = org.mockito.ArgumentCaptor.forClass(BoqItem.class);
        Mockito.verify(boqItemRepository).save(captor.capture());
        BoqItem saved = captor.getValue();
        assertEquals(com.neosow.infra.model.QuotationType.PREMIUM_RANGE_QUOTE, saved.getQuotationType());
    }
}

