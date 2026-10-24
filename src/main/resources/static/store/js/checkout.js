(() => {
    const form = document.getElementById('checkoutForm');
    if (!form) return;
    const delivery = form.elements.deliveryMethod, addresses = form.querySelectorAll('input[name="addressId"]');
    const submit = document.getElementById('placeOrder'), message = document.getElementById('quoteMessage');
    const branchSelect = document.getElementById('checkoutBranchSelect');
    const branchApply = document.getElementById('checkoutBranchApply');
    const branchFeedback = document.getElementById('branchFeedback');
    const branchId = form.elements.branchId;
    const recipient = form.elements.recipientName, phone = form.elements.recipientPhone;
    let manualRecipient = recipient.value, manualPhone = phone.value;
    recipient.addEventListener('input', () => { if (!recipient.readOnly) manualRecipient = recipient.value; });
    phone.addEventListener('input', () => { if (!phone.readOnly) manualPhone = phone.value; });
    let generation = 0, requests = 0, submitting = false;
    let stockBlocked = submit.disabled, branchBusy = false;
    const branchDirty = () => branchSelect.value !== branchId.value;
    const updateBranchControls = () => {
        branchApply.disabled = branchBusy || !branchDirty();
        submit.disabled = stockBlocked || branchBusy || branchDirty() || requests > 0 || !message.hidden || submitting;
    };
    const sync = () => {
        const ship = form.querySelector('input[name="deliveryMethod"]:checked')?.value === 'SHIP_TO_ADDRESS';
        const selectedAddress = form.querySelector('input[name="addressId"]:checked');
        const manual = !ship || !selectedAddress?.value;
        recipient.readOnly = phone.readOnly = !manual;
        recipient.value = manual ? manualRecipient : selectedAddress.dataset.recipient || '';
        phone.value = manual ? manualPhone : selectedAddress.dataset.phone || '';
        document.getElementById('shippingFields').hidden = !ship;
        document.getElementById('manualRecipientFields').hidden = false;
        document.getElementById('newAddressFields').hidden = !ship || !manual;
        for (const key of ['recipientName','recipientPhone']) form.elements[key].required = manual;
        for (const key of ['province','district','ward','addressDetail']) form.elements[key].required = ship && manual;
        addresses.forEach(input => input.closest('label').classList.toggle('is-selected', input.checked));
    };
    const quote = async () => {
        const version = ++generation;
        requests++; submit.disabled = true;
        try {
            const response = await fetch(form.dataset.quoteUrl, {method:'POST', body:new FormData(form), credentials:'same-origin', headers:{Accept:'application/json'}});
            if (response.redirected && new URL(response.url).pathname === '/login') {location.href = '/login';return false;}
            if (!(response.headers.get('Content-Type') || '').includes('application/json'))
                throw new Error('Không thể cập nhật tổng tiền. Vui lòng tải lại trang và thử lại.');
            const data = await response.json();
            if (version !== generation) return false;
            if (!response.ok) throw new Error(data.error || 'Không thể tính tổng đơn hàng.');
            for (const key of ['subtotal','shippingFee','discountAmount','totalAmount']) {
                document.querySelector(`[data-quote="${key}"]`).textContent = `${key === 'discountAmount' ? '−' : ''}${new Intl.NumberFormat('vi-VN').format(data[key])}₫`;
            }
            message.hidden = true; return true;
        } catch (error) {
            if (version === generation) {message.textContent = error.message || 'Không thể cập nhật tổng tiền. Vui lòng thử lại.';message.hidden = false;}
            return false;
        } finally {
            requests--;
            updateBranchControls();
        }
    };
    branchSelect.addEventListener('change', () => {
        branchFeedback.textContent = branchDirty()
            ? 'Xác nhận chi nhánh để kiểm tra kho và cập nhật đơn hàng.'
            : 'Đang dùng chi nhánh đã chọn.';
        branchFeedback.classList.remove('is-error');
        updateBranchControls();
    });
    branchApply.addEventListener('click', async () => {
        if (!branchDirty() || branchBusy) return;
        branchBusy = true;
        generation++;
        branchFeedback.textContent = 'Đang kiểm tra tồn kho tại chi nhánh…';
        updateBranchControls();
        try {
            const data = new FormData(form);
            data.set('branchId', branchSelect.value);
            const response = await fetch(form.dataset.branchUrl, {method:'POST', body:data,
                credentials:'same-origin', headers:{Accept:'application/json'}});
            if (response.redirected && new URL(response.url).pathname === '/login') {location.href = '/login';return;}
            if (!(response.headers.get('Content-Type') || '').includes('application/json'))
                throw new Error('Không thể đổi chi nhánh. Vui lòng tải lại trang.');
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || 'Chi nhánh không đủ hàng.');
            branchId.value = String(result.branchId);
            document.getElementById('checkoutBranchName').textContent = result.branchName;
            document.getElementById('checkoutBranchAddress').textContent = result.branchAddress;
            stockBlocked = !result.checkoutAllowed;
            branchFeedback.classList.remove('is-error');
            branchFeedback.textContent = `✓ Đã chọn ${result.branchName}.`;
            await quote();
        } catch (error) {
            branchSelect.value = branchId.value;
            branchFeedback.classList.add('is-error');
            branchFeedback.textContent = error.message || 'Không thể đổi chi nhánh. Vui lòng thử lại.';
        } finally {
            branchBusy = false;
            updateBranchControls();
        }
    });
    delivery.forEach(input => input.addEventListener('change', () => {sync();quote();}));
    addresses.forEach(input => input.addEventListener('change', sync));
    form.elements.couponCode.addEventListener('input', () => {generation++;message.textContent='Bấm cập nhật ưu đãi để xác nhận tổng tiền.';message.hidden=false;updateBranchControls();});
    document.getElementById('refreshQuote').addEventListener('click', quote);
    form.addEventListener('submit', async event => {
        if (submitting) {event.preventDefault();return;}
        event.preventDefault();
        if (requests > 0 || branchBusy || branchDirty() || !form.reportValidity() || stockBlocked) return;
        submitting = true; submit.disabled = true; submit.textContent = 'Đang kiểm tra đơn hàng…';
        if (await quote()) {submit.disabled = true;submit.textContent='Đang đặt hàng…';HTMLFormElement.prototype.submit.call(form);}
        else {submitting=false;submit.textContent='Xác nhận đặt hàng →';updateBranchControls();}
    });
    sync();
    updateBranchControls();
})();
