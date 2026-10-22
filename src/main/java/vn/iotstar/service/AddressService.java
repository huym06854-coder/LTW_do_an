package vn.iotstar.service;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.entity.UserAddress;
import vn.iotstar.dto.address.AddressDto;
import vn.iotstar.dto.address.AddressRequest;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.UserAddressRepository;
import vn.iotstar.repository.UserRepository;

@Service
@Validated
@Transactional(readOnly = true)
public class AddressService {
    private final UserAddressRepository addresses;
    private final UserRepository users;

    public AddressService(UserAddressRepository addresses, UserRepository users) {
        this.addresses = addresses;
        this.users = users;
    }

    public List<AddressDto> list(Long userId) {
        return addresses.findByUserIdOrderByIsDefaultDescIdAsc(userId).stream().map(this::toDto).toList();
    }

    public AddressDto getOwned(Long userId, Long addressId) {
        return toDto(owned(userId, addressId));
    }

    @Transactional
    public AddressDto save(Long userId, Long addressId, @NotNull @Valid AddressRequest request) {
        User owner = lockOwner(userId);
        UserAddress address = addressId == null ? new UserAddress() : owned(userId, addressId);
        address.setUser(owner);
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setDistrict(request.district().trim());
        address.setWard(request.ward().trim());
        address.setAddressDetail(request.addressDetail().trim());
        List<UserAddress> existing = addresses.findByUserIdOrderByIsDefaultDescIdAsc(userId);
        boolean makeDefault = request.defaultAddress() || address.isDefault()
                || existing.stream().noneMatch(UserAddress::isDefault);
        if (makeDefault)
            clearOtherDefaults(existing, address.getId());
        address.setDefault(makeDefault);
        return toDto(addresses.saveAndFlush(address));
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        lockOwner(userId);
        UserAddress address = owned(userId, addressId);
        boolean wasDefault = address.isDefault();
        addresses.delete(address);
        addresses.flush();
        if (wasDefault) {
            addresses.findByUserIdOrderByIsDefaultDescIdAsc(userId).stream().findFirst()
                    .ifPresent(next -> next.setDefault(true));
        }
    }

    @Transactional
    public void makeDefault(Long userId, Long addressId) {
        lockOwner(userId);
        UserAddress address = owned(userId, addressId);
        clearOtherDefaults(addresses.findByUserIdOrderByIsDefaultDescIdAsc(userId), addressId);
        address.setDefault(true);
    }

    private void clearOtherDefaults(List<UserAddress> existing, Long selectedId) {
        existing.stream().filter(UserAddress::isDefault)
                .filter(address -> !Objects.equals(address.getId(), selectedId))
                .forEach(address -> address.setDefault(false));
        // Release the unique default slot before promoting another address.
        addresses.flush();
    }

    private User lockOwner(Long userId) {
        return users.lockForAddressBook(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    private UserAddress owned(Long userId, Long addressId) {
        return addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ giao hàng không hợp lệ"));
    }

    private AddressDto toDto(UserAddress address) {
        return new AddressDto(address.getId(), address.getRecipientName(), address.getPhone(),
                address.getProvince(), address.getDistrict(), address.getWard(),
                address.getAddressDetail(), address.isDefault());
    }
}
