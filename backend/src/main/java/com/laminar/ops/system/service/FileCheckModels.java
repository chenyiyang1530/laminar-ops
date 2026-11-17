package com.laminar.ops.system.service;

import java.util.List;

public final class FileCheckModels {

    private FileCheckModels() {
    }

    public static final List<FileRef> REFS = List.of(
            new FileRef("biz_certificate", "cert_id", "file_url"),
            new FileRef("biz_company_qualification", "qualification_id", "file_url"),
            new FileRef("biz_contract", "contract_id", "attachment"),
            new FileRef("biz_document", "doc_id", "file_url"),
            new FileRef("biz_inspection_item", "item_id", "image_url"),
            new FileRef("biz_workorder", "workorder_id", "finish_image"),
            new FileRef("biz_workorder", "workorder_id", "acceptance_image"),
            new FileRef("biz_workorder_archive", "workorder_id", "finish_image"),
            new FileRef("biz_workorder_archive", "workorder_id", "acceptance_image")
    );

    public record FileRef(String table, String id, String column) {
    }

    public record Context(Long companyId, Long hospitalId) {
    }

    public record Missing(FileRef ref, Long recordId, String path, Context context,
                          String fileType, String severity, int slaHours) {
    }

    public static String key(Missing missing) {
        return missing.ref().table() + "|" + missing.recordId() + "|" + missing.ref().column();
    }
}
