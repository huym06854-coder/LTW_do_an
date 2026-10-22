package vn.iotstar.dto.address;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public record AddressDto(Long id, String recipientName, String phone, String province,
        String district, String ward, String addressDetail, boolean isDefault) {
    public String fullAddress() {
        return Stream.of(addressDetail, ward, district, province)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }
}
