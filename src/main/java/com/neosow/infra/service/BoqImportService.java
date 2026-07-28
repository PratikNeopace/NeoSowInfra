package com.neosow.infra.service;

import com.neosow.infra.dto.boq.ApprovedBoqItemDTO;
import com.neosow.infra.dto.boq.BoqItemDTO;
import com.neosow.infra.dto.boq.ImportJobResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

public interface BoqImportService {
    ImportJobResponseDTO importBoq(MultipartFile file);
    Page<ImportJobResponseDTO> getImportJobs(int page, int size);
    ByteArrayInputStream generateTemplate();
    ByteArrayInputStream generateSummary(UUID jobId);

    Page<BoqItemDTO> getPendingBoqItems(int page, int size);
    BoqItemDTO approveBoqItem(UUID id);
    BoqItemDTO rejectBoqItem(UUID id);
    List<ApprovedBoqItemDTO> getApprovedBoqItems();
    BoqItemDTO createManualBoqItem(BoqItemDTO dto);

    Page<BoqItemDTO> getAllBoqItems(int page, int size, String search);
    BoqItemDTO updateBoqItem(UUID id, BoqItemDTO dto);
    void deleteBoqItem(UUID id);
}

