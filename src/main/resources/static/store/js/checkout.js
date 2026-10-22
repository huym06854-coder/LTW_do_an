(() => {
    const form = document.getElementById('checkoutForm');
    if (!form) return;
    const delivery = form.elements.deliveryMethod, address = form.elements.addressId;
    const submit = document.getElementById('placeOrder'), message = document.getElementById('quoteMessage');
    let generation = 0, requests = 0, submitting = false;
    const stockBlocked = submit.disabled;
    const sync = () => {
        const ship = delivery.value === 'SHIP_TO_ADDRESS', manual = !ship || !address.value;
        document.getElementById('savedAddressFields').hidden = !ship;
        document.getElementById('manualRecipientFields').hidden = !manual;
        document.getElementById('newAddressFields').hidden = !ship || !manual;
        for (const key of ['recipientName','recipientPhone']) form.elements[key].required = manual;
        for (const key of ['province','district','ward','addressDetail']) form.elements[key].required = ship && manual;
    };
    const quote = async () => {
        const version = ++generation;
        requests++; submit.disabled = true;
        try {
            const response = await fetch('/checkout/quote', {method:'POST', body:new FormData(form), credentials:'same-origin', headers:{Accept:'application/json'}});
            if (response.redirected && new URL(response.url).pathname === '/login') {location.href = '/login';return false;}
            if (!(response.headers.get('Content-Type') || '').includes('application/json'))
                throw new Error('Không thể cập nhật tổng tiền. Vui lòng tải lại trang và thử lại.');
            const data = await response.json();
            if (version !== generation) return false;
            if (!response.ok) throw new Error(data.error || 'Không thể tính tổng đơn hàng.');
            for (const key of ['subtotal','shippingFee','discountAmount','totalAmount']) {
                form.querySelector(`[data-quote="${key}"]`).textContent = `${key === 'discountAmount' ? '−' : ''}${new Intl.NumberFormat('vi-VN').format(data[key])}₫`;
            }
            message.hidden = true; return true;
        } catch (error) {
            if (version === generation) {message.textContent = error.message || 'Không thể cập nhật tổng tiền. Vui lòng thử lại.';message.hidden = false;}
            return false;
        } finally {
            requests--;
            submit.disabled = stockBlocked || requests > 0 || !message.hidden || submitting;
        }
    };
    delivery.addEventListener('change', () => {sync();quote();});
    address.addEventListener('change', sync);
    form.elements.couponCode.addEventListener('input', () => {generation++;submit.disabled = true;message.textContent='Bấm cập nhật ưu đãi để xác nhận tổng tiền.';message.hidden=false;});
    document.getElementById('refreshQuote').addEventListener('click', quote);
    form.addEventListener('submit', async event => {
        if (submitting) {event.preventDefault();return;}
        event.preventDefault();
        if (requests > 0 || !form.reportValidity() || stockBlocked) return;
        submitting = true; submit.disabled = true; submit.textContent = 'Đang kiểm tra đơn hàng…';
        if (await quote()) {submit.disabled = true;submit.textContent='Đang đặt hàng…';HTMLFormElement.prototype.submit.call(form);}
        else {submitting=false;submit.textContent='Xác nhận đặt hàng';submit.disabled=stockBlocked || requests > 0 || !message.hidden;}
    });
    sync();
    submit.disabled = stockBlocked || !message.hidden;
})();
