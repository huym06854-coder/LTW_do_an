package vn.iotstar.dto.stock;

import java.util.List;
import vn.iotstar.domain.entity.Transfer;
import vn.iotstar.domain.entity.TransferStatusHistory;

public record TransferDetailDto(Transfer transfer, List<TransferStatusHistory> history) {}
