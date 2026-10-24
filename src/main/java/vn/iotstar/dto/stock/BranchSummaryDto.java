package vn.iotstar.dto.stock;

import vn.iotstar.domain.enums.BranchStatus;

public record BranchSummaryDto(Long id, String code, String name, String address, BranchStatus status,
        long managerCount, long staffCount, long pendingOrders, long pendingTransfers, long lowStockCount) {}
