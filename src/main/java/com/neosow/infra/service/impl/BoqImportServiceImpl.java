package com.neosow.infra.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neosow.infra.dto.boq.ImportJobResponseDTO;
import com.neosow.infra.exception.BadRequestException;
import com.neosow.infra.exception.ResourceNotFoundException;
import com.neosow.infra.mapper.BoqMapper;
import com.neosow.infra.model.BoqItem;
import com.neosow.infra.model.BoqItemStatus;
import com.neosow.infra.model.QuotationType;
import com.neosow.infra.dto.boq.BoqItemDTO;
import com.neosow.infra.dto.boq.ApprovedBoqItemDTO;
import com.neosow.infra.model.ImportError;
import com.neosow.infra.model.ImportJob;
import com.neosow.infra.model.ImportJobStatus;
import com.neosow.infra.model.User;
import com.neosow.infra.repository.BoqItemRepository;
import com.neosow.infra.repository.ImportErrorRepository;
import com.neosow.infra.repository.ImportJobRepository;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.service.BoqImportService;
import com.neosow.infra.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoqImportServiceImpl implements BoqImportService {

    private final ImportJobRepository importJobRepository;
    private final ImportErrorRepository importErrorRepository;
    private final BoqItemRepository boqItemRepository;
    private final UserRepository userRepository;
    private final BoqMapper boqMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> ALLOWED_UNITS = Arrays.asList(
            "SQ.FT.", "R.FT.", "CU.FT.", "NOS./ JOB", "SQ.MTR.", "R.MTR.", "CU.MTR.", "KGS", "NUMBER", "JOB", "NOS.", "NO.", "NO"
    );

    @Override
    @Transactional
    public ImportJobResponseDTO importBoq(MultipartFile file, QuotationType quotationType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        UUID userId = userDetails.getId();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String userRole = isSuperAdmin ? "ROLE_SUPER_ADMIN" : (isAdmin ? "ROLE_ADMIN" : "ROLE_USER");

        if (!"ROLE_ADMIN".equals(userRole) && !"ROLE_SUPER_ADMIN".equals(userRole)) {
            throw new AccessDeniedException("Access denied: only ADMIN and SUPER_ADMIN roles are allowed to upload BOQ sheets");
        }

        UUID adminId = "ROLE_ADMIN".equals(userRole) ? userId : null;

        log.info("Starting BOQ import for file: {} by user: {}", file.getOriginalFilename(), userId);

        ImportJob job = ImportJob.builder()
                .uploadedBy(userId)
                .role(userRole)
                .fileName(file.getOriginalFilename())
                .status(ImportJobStatus.PROCESSING)
                .quotationType(quotationType)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        job = importJobRepository.save(job);

        int totalRows = 0;
        int successRows = 0;
        int failedRows = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("The uploaded workbook contains no sheets");
            }
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                log.info("Processing sheet {}: {}", i, sheet.getSheetName());
                
                String mainHeading = "";
                String currentSubHeading = "";
            
                // Resolve Main Heading Context dynamically from first 10 rows
                for (int r = 0; r < Math.min(sheet.getLastRowNum() + 1, 10); r++) {
                    Row rRow = sheet.getRow(r);
                    if (rRow != null) {
                        Cell cellA = rRow.getCell(0);
                        Cell cellB = rRow.getCell(1);
                        if (cellA != null && cellB != null && "MAIN HEADING".equalsIgnoreCase(getCellValueAsString(cellA).trim())) {
                            mainHeading = getCellValueAsString(cellB).trim();
                            break;
                        }
                    }
                }

                // Detect Header Row dynamically by searching for marker labels in the first 10 rows
                Row headerRow = null;
                for (int r = 0; r < Math.min(sheet.getLastRowNum() + 1, 10); r++) {
                    Row rRow = sheet.getRow(r);
                    if (rRow != null) {
                        boolean isHeader = false;
                        for (int c = 0; c < rRow.getLastCellNum(); c++) {
                            Cell cell = rRow.getCell(c);
                            if (cell != null) {
                                String cellVal = getCellValueAsString(cell).trim().toUpperCase();
                                if (cellVal.contains("SUB-HEADING") || cellVal.contains("DESCRIPTION") || cellVal.contains("UNIT") || cellVal.contains("S.NO")) {
                                    isHeader = true;
                                    break;
                                }
                            }
                        }
                        if (isHeader) {
                            headerRow = rRow;
                            break;
                        }
                    }
                }

                int colSNo = 0;
                int colSubHeading = 1;
                int colWidth = -1;
                int colHeight = -1;
                int colDepth = -1;
                int colQty = -1;
                int colUnit = -1;
                int colNoOfUnit = -1;
                int colTotalQty = -1;
                int colRate = -1;
                int colAmount = -1;
                int colRefImage = -1;

                if (headerRow != null) {
                    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                        Cell cell = headerRow.getCell(c);
                        if (cell != null) {
                            String header = getCellValueAsString(cell).trim().toUpperCase();
                            if (header.contains("S.NO") || header.contains("S.NO.")) {
                                colSNo = c;
                            } else if (header.contains("SUB-HEADING") || header.contains("SUB HEADING")) {
                                colSubHeading = c;
                            } else if (header.contains("WIDTH")) {
                                colWidth = c;
                            } else if (header.contains("HEIGHT")) {
                                colHeight = c;
                            } else if (header.contains("DEPTH")) {
                                colDepth = c;
                            } else if (header.contains("TOTAL QTY") || header.contains("TOTAL QUANTITY")) {
                                colTotalQty = c;
                            } else if (header.contains("NO. OF UNIT") || header.contains("NO OF UNIT")) {
                                colNoOfUnit = c;
                            } else if (header.contains("QTY") || header.equals("QUANTITY")) {
                                colQty = c;
                            } else if (header.equals("UNIT")) {
                                colUnit = c;
                            } else if (header.equals("RATE") || header.contains("RATE (")) {
                                colRate = c;
                            } else if (header.equals("AMOUNT") || header.contains("AMOUNT (")) {
                                colAmount = c;
                            } else if (header.contains("REF. IMAGE") || header.contains("REF IMAGE") || header.contains("IMAGE")) {
                                colRefImage = c;
                            }
                        }
                    }
                } else {
                    // Fallback to default 12-column mapping if no header row is detected
                    colWidth = 2;
                    colHeight = 3;
                    colDepth = 4;
                    colQty = 5;
                    colUnit = 6;
                    colNoOfUnit = 7;
                    colTotalQty = 8;
                    colRate = 9;
                    colAmount = 10;
                    colRefImage = 11;
                }

                int lastRow = sheet.getLastRowNum();
                int startRow = (headerRow != null) ? headerRow.getRowNum() + 1 : 3;
                for (int r = startRow; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                // Check for GRAND TOTAL/GRAND TOTLA flag to terminate parsing
                boolean isGrandTotal = false;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        String cellVal = getCellValueAsString(cell).trim().toUpperCase();
                        if (cellVal.contains("GRAND TOTAL") || cellVal.contains("GRAND TOTLA")) {
                            isGrandTotal = true;
                            break;
                        }
                    }
                }
                if (isGrandTotal) {
                    log.info("Reached summary GRAND TOTAL row at index {}. Stopping parse.", r + 1);
                    break;
                }

                if (isRowEmpty(row)) {
                    continue;
                }

                // Check for Legend rows to skip
                boolean isLegendRow = false;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        String cellVal = getCellValueAsString(cell).trim().toUpperCase();
                        if (cellVal.contains("BY DEACTIVATE VALUE") || 
                            cellVal.contains("AS PER SUB CATEGORY FIXED UNIT") || 
                            cellVal.contains("CUSTOMISE VALUE") || 
                            cellVal.contains("BY DEFAULT FORMULA")) {
                            isLegendRow = true;
                            break;
                        }
                    }
                }
                if (isLegendRow) {
                    log.debug("Row {}: Skipped legend row", r + 1);
                    continue;
                }

                String sNo = colSNo >= 0 ? getCellValueAsString(row.getCell(colSNo)).trim() : "";
                String firstColBVal = colSubHeading >= 0 ? getCellValueAsString(row.getCell(colSubHeading)).trim() : "";
                String unitVal = colUnit >= 0 ? getCellValueAsString(row.getCell(colUnit)).trim() : "";
                String widthStr = colWidth >= 0 ? getCellValueAsString(row.getCell(colWidth)).trim() : "";
                String heightStr = colHeight >= 0 ? getCellValueAsString(row.getCell(colHeight)).trim() : "";
                String depthStr = colDepth >= 0 ? getCellValueAsString(row.getCell(colDepth)).trim() : "";

                // Skip header rows if they are read as data rows
                if ("UNIT".equalsIgnoreCase(unitVal) || "S.NO.".equalsIgnoreCase(sNo) || "S.NO".equalsIgnoreCase(sNo)) {
                    continue;
                }

                // 2) Track current sub-heading from rows where S.NO exists
                if (!sNo.isEmpty()) {
                    currentSubHeading = firstColBVal;
                    log.debug("Row {}: Detected sub-heading: '{}'", r + 1, currentSubHeading);
                }

                // 4) Skip Description label rows (e.g. "Description-" in B)
                if ("Description-".equalsIgnoreCase(firstColBVal)) {
                    log.debug("Row {}: Skipped Description- label row", r + 1);
                    continue;
                }

                // 3) Detect data row using UNIT column presence (process ONLY if UNIT is not empty)
                boolean isData = !unitVal.isEmpty();

                // If it establishes a subheading and is not a data row, skip to next row
                if (!sNo.isEmpty() && !isData) {
                    continue;
                }

                if (isData) {
                    totalRows++;
                    log.debug("Row {}: Processing data row in sheet {}. Current sub-heading: '{}'. Total rows so far: {}", 
                              r + 1, sheet.getSheetName(), currentSubHeading, totalRows);
                    
                    // Parse values
                    String description = firstColBVal;
                    String rawQtyStr = colQty >= 0 ? getCellValueAsString(row.getCell(colQty)).trim() : "";
                    String noOfUnitStr = colNoOfUnit >= 0 ? getCellValueAsString(row.getCell(colNoOfUnit)).trim() : "";
                    String rateStr = colRate >= 0 ? getCellValueAsString(row.getCell(colRate)).trim() : "";
                    String refImage = colRefImage >= 0 ? getCellValueAsString(row.getCell(colRefImage)).trim() : "";

                    // Validation
                    List<String> errors = new ArrayList<>();
                    
                    // Unit Check
                    String normUnit = unitVal.toUpperCase().replace(".", "").replace(" ", "");
                    boolean isValidUnit = false;
                    if (unitVal.isEmpty()) {
                        errors.add("Unit is mandatory");
                    } else {
                        isValidUnit = ALLOWED_UNITS.stream()
                                .anyMatch(u -> u.toUpperCase().replace(".", "").replace(" ", "").equals(normUnit));
                        if (!isValidUnit) {
                            errors.add("Invalid Unit value: '" + unitVal + "'");
                        }
                    }

                    // Check numeric boundaries for inputs
                    double widthVal = 0.0;
                    double heightVal = 0.0;
                    double depthVal = 0.0;

                    if (!widthStr.isEmpty()) {
                        if (isValidDimensionInput(widthStr)) {
                            try {
                                widthVal = parseDimension(widthStr);
                                if (widthVal < 0) errors.add("Width cannot be negative");
                            } catch (Exception e) {
                                widthVal = 0.0;
                            }
                        } else {
                            widthVal = 0.0;
                        }
                    }
                    if (!heightStr.isEmpty()) {
                        if (isValidDimensionInput(heightStr)) {
                            try {
                                heightVal = parseDimension(heightStr);
                                if (heightVal < 0) errors.add("Height cannot be negative");
                            } catch (Exception e) {
                                heightVal = 0.0;
                            }
                        } else {
                            heightVal = 0.0;
                        }
                    }
                    if (!depthStr.isEmpty()) {
                        if (isValidDimensionInput(depthStr)) {
                            try {
                                depthVal = parseDimension(depthStr);
                                if (depthVal < 0) errors.add("Depth cannot be negative");
                            } catch (Exception e) {
                                depthVal = 0.0;
                            }
                        } else {
                            depthVal = 0.0;
                        }
                    }

                    double noOfUnits = 1.0;
                    if (!noOfUnitStr.isEmpty()) {
                        try {
                            noOfUnits = Double.parseDouble(noOfUnitStr);
                            if (noOfUnits <= 0) errors.add("No of Units must be greater than 0");
                        } catch (NumberFormatException e) {
                            errors.add("Invalid No of Units numeric format: '" + noOfUnitStr + "'");
                        }
                    }

                    double rateVal = 0.0;
                    if (!rateStr.isEmpty()) {
                        try {
                            rateVal = Double.parseDouble(rateStr);
                            if (rateVal < 0) errors.add("Rate cannot be negative");
                        } catch (NumberFormatException e) {
                            errors.add("Invalid Rate numeric format: '" + rateStr + "'");
                        }
                    }

                    double qtyVal = 0.0;
                    boolean hasQtyVal = false;
                    if (!rawQtyStr.isEmpty()) {
                        try {
                            qtyVal = Double.parseDouble(rawQtyStr);
                            hasQtyVal = true;
                            if (qtyVal < 0) {
                                errors.add("Qty cannot be negative");
                            }
                        } catch (NumberFormatException e) {
                            errors.add("Invalid manual Qty format: '" + rawQtyStr + "'");
                        }
                    }

                    // Base Qty retrieval/calculation and conditional validations
                    double baseQty = 0.0;
                    int nonZeroDims = (widthVal > 0 ? 1 : 0) + (heightVal > 0 ? 1 : 0) + (depthVal > 0 ? 1 : 0);

                    if (errors.isEmpty() && isValidUnit) {
                        if (normUnit.contains("SQFT") || normUnit.contains("SQMTR")) {
                            if (nonZeroDims >= 2) {
                                double[] dims = {widthVal, heightVal, depthVal};
                                Arrays.sort(dims);
                                baseQty = dims[1] * dims[2];
                            } else if (hasQtyVal) {
                                baseQty = qtyVal;
                            } else {
                                errors.add("Area units (SQ.FT. / SQ.MTR.) require at least two dimensions or a valid Qty value");
                            }
                        } else if (normUnit.contains("RFT") || normUnit.contains("RMTR")) {
                            if (nonZeroDims >= 1) {
                                baseQty = Math.max(widthVal, Math.max(heightVal, depthVal));
                            } else if (hasQtyVal) {
                                baseQty = qtyVal;
                            } else {
                                errors.add("Length units (R.FT. / R.MTR.) require at least one dimension or a valid Qty value");
                            }
                        } else if (normUnit.contains("CUFT") || normUnit.contains("CUMTR")) {
                            if (nonZeroDims >= 3) {
                                baseQty = widthVal * heightVal * depthVal;
                            } else if (hasQtyVal) {
                                baseQty = qtyVal;
                            } else {
                                errors.add("Volume units (CU.FT. / CU.MTR.) require all three dimensions or a valid Qty value");
                            }
                        } else if (normUnit.contains("NOS") || normUnit.contains("NUMBER") || normUnit.contains("JOB") || normUnit.equals("NO")) {
                            if (nonZeroDims > 0) {
                                baseQty = (widthVal > 0 ? widthVal : 1.0) * (heightVal > 0 ? heightVal : 1.0) * (depthVal > 0 ? depthVal : 1.0);
                            } else if (hasQtyVal) {
                                baseQty = qtyVal;
                            } else {
                                baseQty = 1.0;
                            }
                        } else {
                            if (hasQtyVal) {
                                baseQty = qtyVal;
                            } else {
                                baseQty = 1.0;
                            }
                        }
                    }

                    if (errors.isEmpty()) {
                        // Recalculate Totals
                        double totalQty;
                        // Special item/count unit scaling exception
                        if (normUnit.contains("NOS") || normUnit.contains("NUMBER") || normUnit.contains("JOB") || normUnit.equals("NO")) {
                            totalQty = noOfUnits;
                        } else {
                            totalQty = baseQty * noOfUnits;
                        }
                        double finalAmount = totalQty * rateVal;

                        String truncatedSub = truncateString(currentSubHeading, 255);
                        String truncatedMain = truncateString(mainHeading, 255);
                        String truncatedWidth = truncateString(widthStr, 50);
                        String truncatedHeight = truncateString(heightStr, 50);
                        String truncatedDepth = truncateString(depthStr, 50);
                        String truncatedUnit = truncateString(unitVal, 50);

                        Optional<BoqItem> existingOpt = boqItemRepository.findFirstBySubHeadingIgnoreCase(truncatedSub);

                        BoqItemStatus boqStatus;
                        UUID approvedByUser;
                        LocalDateTime approvedAtTime;

                        if ("ROLE_SUPER_ADMIN".equals(userRole)) {
                            boqStatus = BoqItemStatus.APPROVED;
                            approvedByUser = userId;
                            approvedAtTime = LocalDateTime.now();
                        } else {
                            // If uploaded by ADMIN, check if it already exists as APPROVED and is identical
                            boolean isIdentical = false;
                            if (existingOpt.isPresent()) {
                                BoqItem existing = existingOpt.get();
                                if (existing.getStatus() == BoqItemStatus.APPROVED) {
                                    boolean descEqual = Objects.equals(existing.getDescription(), description);
                                    boolean unitEqual = Objects.equals(existing.getUnit(), truncatedUnit);
                                    boolean rateEqual = false;
                                    if (existing.getRate() != null) {
                                        BigDecimal newRate = BigDecimal.valueOf(rateVal).setScale(2, RoundingMode.HALF_UP);
                                        rateEqual = newRate.compareTo(existing.getRate()) == 0;
                                    }
                                    isIdentical = descEqual && unitEqual && rateEqual;
                                }
                            }

                            if (isIdentical) {
                                boqStatus = BoqItemStatus.APPROVED;
                                approvedByUser = existingOpt.get().getApprovedBy();
                                approvedAtTime = existingOpt.get().getApprovedAt();
                            } else {
                                boqStatus = BoqItemStatus.PENDING_APPROVAL;
                                approvedByUser = null;
                                approvedAtTime = null;
                            }
                        }

                        BoqItem item;
                        if (existingOpt.isPresent()) {
                            item = existingOpt.get();
                            item.setJobId(job.getId());
                            item.setMainHeading(truncatedMain);
                            item.setDescription(description);
                            item.setWidth(truncatedWidth);
                            item.setHeight(truncatedHeight);
                            item.setDepth(truncatedDepth);
                            item.setQty(BigDecimal.valueOf(baseQty).setScale(2, RoundingMode.HALF_UP));
                            item.setUnit(truncatedUnit);
                            item.setNoOfUnit(BigDecimal.valueOf(noOfUnits).setScale(2, RoundingMode.HALF_UP));
                            item.setTotalQty(BigDecimal.valueOf(totalQty).setScale(2, RoundingMode.HALF_UP));
                            item.setRate(BigDecimal.valueOf(rateVal).setScale(2, RoundingMode.HALF_UP));
                            item.setAmount(BigDecimal.valueOf(finalAmount).setScale(2, RoundingMode.HALF_UP));
                            item.setStatus(boqStatus);
                            item.setUploadedBy(userId);
                            item.setUploadedRole(userRole);
                            item.setApprovedBy(approvedByUser);
                            item.setApprovedAt(approvedAtTime);
                            item.setNewValue(false);
                            if (quotationType != null) {
                                item.setQuotationType(quotationType);
                            }
                        } else {
                            item = BoqItem.builder()
                                    .jobId(job.getId())
                                    .mainHeading(truncatedMain)
                                    .subHeading(truncatedSub)
                                    .description(description)
                                    .width(truncatedWidth)
                                    .height(truncatedHeight)
                                    .depth(truncatedDepth)
                                    .qty(BigDecimal.valueOf(baseQty).setScale(2, RoundingMode.HALF_UP))
                                    .unit(truncatedUnit)
                                    .noOfUnit(BigDecimal.valueOf(noOfUnits).setScale(2, RoundingMode.HALF_UP))
                                    .totalQty(BigDecimal.valueOf(totalQty).setScale(2, RoundingMode.HALF_UP))
                                    .rate(BigDecimal.valueOf(rateVal).setScale(2, RoundingMode.HALF_UP))
                                    .amount(BigDecimal.valueOf(finalAmount).setScale(2, RoundingMode.HALF_UP))
                                    .createdBy(userId)
                                    .adminId(adminId)
                                    .status(boqStatus)
                                    .uploadedBy(userId)
                                    .uploadedRole(userRole)
                                    .approvedBy(approvedByUser)
                                    .approvedAt(approvedAtTime)
                                    .isNewValue(true)
                                    .quotationType(quotationType)
                                    .build();
                        }

                        boqItemRepository.save(item);
                        successRows++;
                    } else {
                        // Log validations failure row
                        failedRows++;
                        
                        Map<String, String> rawRow = new HashMap<>();
                        rawRow.put("rowNumber", String.valueOf(r + 1));
                        rawRow.put("mainHeading", mainHeading);
                        rawRow.put("subHeading", currentSubHeading);
                        rawRow.put("description", description);
                        rawRow.put("width", widthStr);
                        rawRow.put("height", heightStr);
                        rawRow.put("depth", depthStr);
                        rawRow.put("qty", rawQtyStr);
                        rawRow.put("unit", unitVal);
                        rawRow.put("noOfUnit", noOfUnitStr);
                        rawRow.put("rate", rateStr);
                        rawRow.put("refImage", refImage);

                        String jsonRaw = "";
                        try {
                            jsonRaw = objectMapper.writeValueAsString(rawRow);
                        } catch (Exception ex) {
                            jsonRaw = rawRow.toString();
                        }

                        ImportError err = ImportError.builder()
                                .jobId(job.getId())
                                .rowNumber(r + 1)
                                .errorMessage(String.join("; ", errors))
                                .rawData(jsonRaw)
                                .build();
                        
                        importErrorRepository.save(err);
                    }
                }
            }
            }
            
            job.setStatus(ImportJobStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Fatal error parsing BOQ spreadsheet file", e);
            job.setStatus(ImportJobStatus.FAILED);
            // Record generic file parsing error
            ImportError err = ImportError.builder()
                    .jobId(job.getId())
                    .rowNumber(0)
                    .errorMessage("Fatal spreadsheet read failure: " + e.getMessage())
                    .build();
            importErrorRepository.save(err);
        }

        job.setTotalRows(totalRows);
        job.setSuccessRows(successRows);
        job.setFailedRows(failedRows);
        job.setUpdatedAt(LocalDateTime.now());
        
        ImportJob completedJob = importJobRepository.save(job);
        log.info("BOQ import finished. Status: {}, Total Rows: {}, Success: {}, Failed: {}", 
                completedJob.getStatus(), totalRows, successRows, failedRows);

        return mapToResponseDto(completedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportJobResponseDTO> getImportJobs(int page, int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        UUID userId = userDetails.getId();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (isSuperAdmin) {
            return importJobRepository.findAll(pageable).map(this::mapToResponseDto);
        } else {
            return importJobRepository.findByUploadedBy(userId, pageable).map(this::mapToResponseDto);
        }
    }

    @Override
    public ByteArrayInputStream generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("PARTITON");
            
            // Columns width settings
            sheet.setColumnWidth(0, 2500);  // S.NO.
            sheet.setColumnWidth(1, 12000); // SUB-HEADING & Description
            sheet.setColumnWidth(2, 2500);  // WIDTH
            sheet.setColumnWidth(3, 2500);  // HEIGHT
            sheet.setColumnWidth(4, 2500);  // DEPTH
            sheet.setColumnWidth(5, 2500);  // QTY
            sheet.setColumnWidth(6, 3000);  // UNIT
            sheet.setColumnWidth(7, 3000);  // NO. OF UNIT
            sheet.setColumnWidth(8, 3000);  // TOTAL QTY.
            sheet.setColumnWidth(9, 3000);  // RATE
            sheet.setColumnWidth(10, 4000); // AMOUNT
            sheet.setColumnWidth(11, 4000); // REF. IMAGE

            // Colors/Styles configuration
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.BLUE.getIndex());
            titleStyle.setFont(titleFont);

            // Row 1: MAIN HEADING Label
            Row row1 = sheet.createRow(1);
            Cell cellA2 = row1.createCell(0);
            cellA2.setCellValue("MAIN HEADING");
            cellA2.setCellStyle(headerStyle);
            Cell cellB2 = row1.createCell(1);
            cellB2.setCellValue("PARTITION WORK");
            cellB2.setCellStyle(titleStyle);

            // Row 2: Headers
            Row row2 = sheet.createRow(2);
            String[] headers = {
                    "S.NO.", "SUB-HEADING", "WIDTH", "HEIGHT", "DEPTH", 
                    "QTY", "UNIT", "NO. OF UNIT", "TOTAL QTY.", "RATE", "AMOUNT", "REF. IMAGE"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row2.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill sample data rows
            // SNo 1 Group
            Row r5 = sheet.createRow(4);
            r5.createCell(0).setCellValue(1);
            r5.createCell(1).setCellValue("100MM THICK GYPSUM PARTITION WITH ALUMINUM FRAMEWORK");

            Row r6 = sheet.createRow(5);
            r6.createCell(1).setCellValue("Description-");

            Row r7 = sheet.createRow(6);
            r7.createCell(1).setCellValue("Providing and fixing standard aluminium framework with infill...");
            r7.createCell(2).setCellValue("5");
            r7.createCell(3).setCellValue("10");
            r7.createCell(5).setCellValue(50);
            r7.createCell(6).setCellValue("SQ.FT.");
            r7.createCell(7).setCellValue(2);
            r7.createCell(8).setCellValue(100);
            r7.createCell(9).setCellValue(300);
            r7.createCell(10).setCellValue(30000);

            // SNo 2 Group
            Row r8 = sheet.createRow(7);
            r8.createCell(0).setCellValue(2);
            r8.createCell(1).setCellValue("SS RAILING WITH GLASS PARTITION");

            Row r9 = sheet.createRow(8);
            r9.createCell(1).setCellValue("SINGLE GLAZED GLASS STAIRCASE BALUSTER WITH SS 304 RAILING...");
            r9.createCell(2).setCellValue("10");
            r9.createCell(5).setCellValue(10);
            r9.createCell(6).setCellValue("R.FT.");
            r9.createCell(7).setCellValue(2);
            r9.createCell(8).setCellValue(20);
            r9.createCell(9).setCellValue(1000);
            r9.createCell(10).setCellValue(20000);

            // Row 10: Instructions Title
            Row r11 = sheet.createRow(10);
            r11.createCell(1).setCellValue("EXCEL FORMATTING VALIDATION GUIDELINES");

            String[] guidelines = {
                    "1. A BOQ Item group is declared by specifying the serial number (e.g. 1) in S.NO. and the title in SUB-HEADING.",
                    "2. The details row below the title must have a valid value in the UNIT column to trigger parsing.",
                    "3. Dimension values (Width, Height, Depth) can be entered as decimal numbers (e.g. 12.5) or feet-inch strings (e.g. 12' 6\").",
                    "4. Allowed units: SQ.FT., R.FT., CU.FT., NOS./ JOB, SQ.MTR., R.MTR., CU.MTR., KGS, NUMBER, JOB.",
                    "5. Dimensions are optional for count units (NOS./ JOB). Leave dimensions blank and fill the QTY directly if not needed.",
                    "6. Keep the sheet name exactly as 'PARTITON' when uploading."
            };
            int instructionRow = 11;
            for (String line : guidelines) {
                Row row = sheet.createRow(instructionRow++);
                row.createCell(1).setCellValue(line);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel template stream", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateSummary(UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import Job not found with ID: " + jobId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!isSuperAdmin && !job.getUploadedBy().equals(userDetails.getId())) {
            throw new AccessDeniedException("Access denied: you do not have permission to view this import job summary");
        }

        List<BoqItem> successItems = boqItemRepository.findByJobId(jobId);
        List<ImportError> failedErrors = importErrorRepository.findByJobIdOrderByRowNumberAsc(jobId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Sheet 1: Success Rows
            Sheet successSheet = workbook.createSheet("SUCCESS ROWS");
            successSheet.setColumnWidth(0, 4000); // Main Heading
            successSheet.setColumnWidth(1, 4000); // Sub Heading
            successSheet.setColumnWidth(2, 12000); // Description
            successSheet.setColumnWidth(3, 2500); // Width
            successSheet.setColumnWidth(4, 2500); // Height
            successSheet.setColumnWidth(5, 2500); // Depth
            successSheet.setColumnWidth(6, 2500); // Qty
            successSheet.setColumnWidth(7, 2500); // Unit
            successSheet.setColumnWidth(8, 2500); // No of Unit
            successSheet.setColumnWidth(9, 3000); // Total Qty
            successSheet.setColumnWidth(10, 3000); // Rate
            successSheet.setColumnWidth(11, 4000); // Amount

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row sHeader = successSheet.createRow(0);
            String[] sCols = {
                    "Main Heading", "Sub Heading", "Description", "Width", "Height", "Depth", 
                    "Qty", "Unit", "No of Unit", "Total Qty", "Rate", "Amount"
            };
            for (int i = 0; i < sCols.length; i++) {
                Cell c = sHeader.createCell(i);
                c.setCellValue(sCols[i]);
                c.setCellStyle(headerStyle);
            }

            int sIdx = 1;
            for (BoqItem item : successItems) {
                Row r = successSheet.createRow(sIdx++);
                r.createCell(0).setCellValue(item.getMainHeading());
                r.createCell(1).setCellValue(item.getSubHeading());
                r.createCell(2).setCellValue(item.getDescription());
                r.createCell(3).setCellValue(item.getWidth());
                r.createCell(4).setCellValue(item.getHeight());
                r.createCell(5).setCellValue(item.getDepth());
                r.createCell(6).setCellValue(item.getQty().doubleValue());
                r.createCell(7).setCellValue(item.getUnit());
                r.createCell(8).setCellValue(item.getNoOfUnit().doubleValue());
                r.createCell(9).setCellValue(item.getTotalQty().doubleValue());
                r.createCell(10).setCellValue(item.getRate().doubleValue());
                r.createCell(11).setCellValue(item.getAmount().doubleValue());
            }

            // Sheet 2: Failed Rows
            Sheet failedSheet = workbook.createSheet("FAILED ROWS");
            failedSheet.setColumnWidth(0, 2500); // Row Number
            failedSheet.setColumnWidth(1, 8000); // Error Message
            failedSheet.setColumnWidth(2, 15000); // Raw Row JSON

            Row fHeader = failedSheet.createRow(0);
            String[] fCols = {"Row Number", "Validation Error Messages", "Raw Data Input"};
            for (int i = 0; i < fCols.length; i++) {
                Cell c = fHeader.createCell(i);
                c.setCellValue(fCols[i]);
                c.setCellStyle(headerStyle);
            }

            int fIdx = 1;
            for (ImportError err : failedErrors) {
                Row r = failedSheet.createRow(fIdx++);
                r.createCell(0).setCellValue(err.getRowNumber());
                r.createCell(1).setCellValue(err.getErrorMessage());
                r.createCell(2).setCellValue(err.getRawData());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel summary stream", e);
        }
    }

    // Stateful parsing dimension helper
    private double parseDimension(String val) {
        if (val == null || val.trim().isEmpty()) {
            return 0.0;
        }
        String s = val.trim();

        // Standard decimal validation
        if (s.matches("^[-+]?[0-9]*\\.?[0-9]+$")) {
            return Double.parseDouble(s);
        }

        // Feet-inch pattern parsing (e.g. 5' 6" or 5ft 6in)
        try {
            String[] parts = s.split("'|ft|FT");
            if (parts.length > 0) {
                double feet = Double.parseDouble(parts[0].trim());
                double inches = 0.0;
                if (parts.length > 1) {
                    String inchStr = parts[1].replaceAll("[\"inIN\\s]", "");
                    if (!inchStr.isEmpty()) {
                        inches = Double.parseDouble(inchStr);
                    }
                }
                return feet + (inches / 12.0);
            }
        } catch (Exception e) {
            log.warn("Failed to parse feet-inch string: '{}' inside BOQ parser", val);
        }

        try {
            return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private boolean isValidDimensionInput(String s) {
        if (s == null || s.trim().isEmpty()) {
            return true;
        }
        String val = s.trim();
        // Standard decimal validation
        if (val.matches("^[-+]?[0-9]*\\.?[0-9]+$")) {
            return true;
        }
        // Feet-inch check: must contain at least some digits, and either ' or ft or FT
        if (val.matches("^.*\\d+.*$") && (val.contains("'") || val.toLowerCase().contains("ft"))) {
            return true;
        }
        return false;
    }

    private String truncateString(String val, int length) {
        if (val == null) {
            return "";
        }
        return val.length() > length ? val.substring(0, length) : val;
    }

    // Helper checks if POI Row is completely empty
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // General purpose cell reader
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    CellValue cellValue = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluate(cell);
                    if (cellValue != null) {
                        switch (cellValue.getCellType()) {
                            case STRING:
                                return cellValue.getStringValue();
                            case NUMERIC:
                                double val = cellValue.getNumberValue();
                                if (val == (long) val) {
                                    return String.valueOf((long) val);
                                }
                                return String.valueOf(val);
                            case BOOLEAN:
                                return String.valueOf(cellValue.getBooleanValue());
                            default:
                                return "";
                        }
                    }
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        try {
                            return String.valueOf(cell.getNumericCellValue());
                        } catch (Exception ex2) {
                            return cell.getCellFormula();
                        }
                    }
                }
                return "";
            case BLANK:
            default:
                return "";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoqItemDTO> getPendingBoqItems(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return boqItemRepository.findByStatus(BoqItemStatus.PENDING_APPROVAL, pageable)
                .map(this::mapToBoqItemDto);
    }

    @Override
    @Transactional
    public BoqItemDTO approveBoqItem(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        
        BoqItem item = boqItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOQ Item not found with ID: " + id));

        item.setStatus(BoqItemStatus.APPROVED);
        item.setApprovedBy(userDetails.getId());
        item.setApprovedAt(LocalDateTime.now());

        BoqItem saved = boqItemRepository.save(item);
        return mapToBoqItemDto(saved);
    }

    @Override
    @Transactional
    public BoqItemDTO rejectBoqItem(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        
        BoqItem item = boqItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOQ Item not found with ID: " + id));

        item.setStatus(BoqItemStatus.REJECTED);
        item.setApprovedBy(userDetails.getId());
        item.setApprovedAt(LocalDateTime.now());

        BoqItem saved = boqItemRepository.save(item);
        return mapToBoqItemDto(saved);
    }

    @Override
    @Transactional
    public BoqItemDTO createManualBoqItem(BoqItemDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        UUID userId = userDetails.getId();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String userRole = isSuperAdmin ? "ROLE_SUPER_ADMIN" : (isAdmin ? "ROLE_ADMIN" : "ROLE_USER");

        if (!"ROLE_ADMIN".equals(userRole) && !"ROLE_SUPER_ADMIN".equals(userRole)) {
            throw new AccessDeniedException("Access denied: only ADMIN and SUPER_ADMIN roles are allowed to create BOQ items");
        }

        // Validate fields
        if (dto.getMainHeading() == null || dto.getMainHeading().trim().isEmpty()) {
            throw new BadRequestException("Main heading is mandatory");
        }
        if (dto.getSubHeading() == null || dto.getSubHeading().trim().isEmpty()) {
            throw new BadRequestException("Sub heading is mandatory");
        }
        if (dto.getUnit() == null || dto.getUnit().trim().isEmpty()) {
            throw new BadRequestException("Unit is mandatory");
        }
        if (dto.getRate() == null) {
            throw new BadRequestException("Rate is mandatory");
        }
        if (dto.getRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Rate cannot be negative");
        }

        // Validate unit against ALLOWED_UNITS
        String normUnit = dto.getUnit().toUpperCase().replace(".", "").replace(" ", "");
        boolean isValidUnit = ALLOWED_UNITS.stream()
                .anyMatch(u -> u.toUpperCase().replace(".", "").replace(" ", "").equals(normUnit));
        if (!isValidUnit) {
            throw new BadRequestException("Invalid Unit value: '" + dto.getUnit() + "'");
        }

        UUID adminId = "ROLE_ADMIN".equals(userRole) ? userId : null;
        String truncatedMain = truncateString(dto.getMainHeading().trim(), 255);
        String truncatedSub = truncateString(dto.getSubHeading().trim(), 255);
        String truncatedUnit = truncateString(dto.getUnit().trim(), 50);

        Optional<BoqItem> existingOpt = boqItemRepository.findFirstBySubHeadingIgnoreCase(truncatedSub);

        BoqItemStatus boqStatus;
        UUID approvedByUser;
        LocalDateTime approvedAtTime;

        if ("ROLE_SUPER_ADMIN".equals(userRole)) {
            boqStatus = BoqItemStatus.APPROVED;
            approvedByUser = userId;
            approvedAtTime = LocalDateTime.now();
        } else {
            // ROLE_ADMIN: if existing approved item is identical, keep approved, otherwise pending approval
            boolean isIdentical = false;
            if (existingOpt.isPresent()) {
                BoqItem existing = existingOpt.get();
                if (existing.getStatus() == BoqItemStatus.APPROVED) {
                    boolean descEqual = Objects.equals(existing.getDescription(), dto.getDescription());
                    boolean unitEqual = Objects.equals(existing.getUnit(), truncatedUnit);
                    BigDecimal newRate = dto.getRate().setScale(2, RoundingMode.HALF_UP);
                    boolean rateEqual = existing.getRate() != null && newRate.compareTo(existing.getRate()) == 0;
                    isIdentical = descEqual && unitEqual && rateEqual;
                }
            }

            if (isIdentical) {
                boqStatus = BoqItemStatus.APPROVED;
                approvedByUser = existingOpt.get().getApprovedBy();
                approvedAtTime = existingOpt.get().getApprovedAt();
            } else {
                boqStatus = BoqItemStatus.PENDING_APPROVAL;
                approvedByUser = null;
                approvedAtTime = null;
            }
        }

        // We set default values for dimensions/qty/noOfUnit/totalQty/amount
        BigDecimal defaultQty = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        BigDecimal defaultNoOfUnit = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        BigDecimal defaultTotalQty = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rateVal = dto.getRate().setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountVal = rateVal.multiply(defaultTotalQty).setScale(2, RoundingMode.HALF_UP);

        BoqItem item;
        if (existingOpt.isPresent()) {
            item = existingOpt.get();
            item.setMainHeading(truncatedMain);
            item.setDescription(dto.getDescription());
            item.setUnit(truncatedUnit);
            item.setRate(rateVal);
            item.setAmount(amountVal);
            item.setQty(defaultQty);
            item.setNoOfUnit(defaultNoOfUnit);
            item.setTotalQty(defaultTotalQty);
            item.setStatus(boqStatus);
            item.setUploadedBy(userId);
            item.setUploadedRole(userRole);
            item.setApprovedBy(approvedByUser);
            item.setApprovedAt(approvedAtTime);
            if (dto.getQuotationType() != null) {
                item.setQuotationType(dto.getQuotationType());
            }
        } else {
            item = BoqItem.builder()
                    .mainHeading(truncatedMain)
                    .subHeading(truncatedSub)
                    .description(dto.getDescription())
                    .qty(defaultQty)
                    .unit(truncatedUnit)
                    .noOfUnit(defaultNoOfUnit)
                    .totalQty(defaultTotalQty)
                    .rate(rateVal)
                    .amount(amountVal)
                    .createdBy(userId)
                    .adminId(adminId)
                    .status(boqStatus)
                    .uploadedBy(userId)
                    .uploadedRole(userRole)
                    .approvedBy(approvedByUser)
                    .approvedAt(approvedAtTime)
                    .isNewValue(true)
                    .quotationType(dto.getQuotationType())
                    .build();
        }

        BoqItem saved = boqItemRepository.save(item);
        return mapToBoqItemDto(saved);
    }

    private BoqItemDTO mapToBoqItemDto(BoqItem item) {
        if (item == null) {
            return null;
        }
        BoqItemDTO dto = boqMapper.toDto(item);
        dto.setUploadedBy(getUserEmail(item.getUploadedBy()));
        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ApprovedBoqItemDTO> getApprovedBoqItems(QuotationType quotationType) {
        if (quotationType != null) {
            return boqItemRepository.findApprovedBoqItemsByQuotationType(quotationType);
        }
        return boqItemRepository.findApprovedBoqItems();
    }

    private ImportJobResponseDTO mapToResponseDto(ImportJob job) {
        if (job == null) {
            return null;
        }
        ImportJobResponseDTO dto = boqMapper.toDto(job);
        dto.setUploadedBy(getUserEmail(job.getUploadedBy()));
        return dto;
    }

    private String getUserEmail(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse(userId.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoqItemDTO> getAllBoqItems(int page, int size, String search) {
        log.info("Fetching all BOQ items with search filter: {}", search);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return boqItemRepository.findAllWithSearch(search, pageable)
                .map(this::mapToBoqItemDto);
    }

    @Override
    @Transactional
    public BoqItemDTO updateBoqItem(UUID id, BoqItemDTO dto) {
        log.info("Updating BOQ Item with ID: {}", id);
        BoqItem item = boqItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOQ Item not found with ID: " + id));

        item.setMainHeading(dto.getMainHeading());
        item.setSubHeading(dto.getSubHeading());
        item.setDescription(dto.getDescription());
        item.setUnit(dto.getUnit());
        item.setRate(dto.getRate());

        // Amount calculation
        BigDecimal rate = dto.getRate() != null ? dto.getRate() : BigDecimal.ZERO;
        BigDecimal totalQty = item.getTotalQty() != null ? item.getTotalQty() : BigDecimal.ONE;
        item.setAmount(rate.multiply(totalQty));

        if (dto.getQuotationType() != null) {
            item.setQuotationType(dto.getQuotationType());
        }

        if (dto.getStatus() != null) {
            try {
                item.setStatus(BoqItemStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        BoqItem saved = boqItemRepository.save(item);
        return mapToBoqItemDto(saved);
    }

    @Override
    @Transactional
    public void deleteBoqItem(UUID id) {
        log.info("Deleting BOQ Item with ID: {}", id);
        BoqItem item = boqItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOQ Item not found with ID: " + id));
        boqItemRepository.delete(item);
    }
}
